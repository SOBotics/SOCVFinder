package jdd.so.dup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.jsoup.parser.Parser;

import org.sobotics.chatexchange.chat.User;
import jdd.so.CloseVoteFinder;
import jdd.so.api.ApiHandler;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Comment;
import jdd.so.api.model.Question;
import jdd.so.bot.ChatBot;
import jdd.so.bot.ChatRoom;
import jdd.so.dao.CommentDAO;
import jdd.so.dao.model.CommentsNotify;
import jdd.so.dao.model.DuplicateNotifications;
import jdd.so.higgs.Higgs;
import jdd.so.nlp.CommentCloseCategory;
import jdd.so.nlp.CommentHeatCategory;
import jdd.so.nlp.CommentReviewCategory;

public class CommentsController extends Thread {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentsController.class);

	private static final int MAX_POST_ID_QUE = 10;
	private ApiHandler apiHandler;
	private CommentHeatCategory commentHeatCategory;
	private CommentCloseCategory commentCloseCategory;
	private CommentReviewCategory commentReviewCategory;
	private ChatBot cb;
	private Queue<Long> lastPostIds;

	private boolean shutDown;

	private NumberFormat nfThreshold;

	public CommentsController(ChatBot cb) {
		this.setName("DupeHunterComments");
		this.cb = cb;
		this.apiHandler = new ApiHandler();
		this.lastPostIds = new ArrayDeque<>();
		nfThreshold = NumberFormat.getNumberInstance(Locale.US);
		nfThreshold.setMaximumFractionDigits(2);
		nfThreshold.setMinimumFractionDigits(2);
		try {
			commentHeatCategory = new CommentHeatCategory();
		} catch (Exception e) {
			logger.error("DupeHunterComments(ChatBot) - Comment heat categorizzer could not be instanced", e);
		}

		try {
			commentCloseCategory = new CommentCloseCategory();
		} catch (Exception e) {
			logger.error("DupeHunterComments(ChatBot) - Comment close categorizzer could not be instanced", e);
		}

		try {
			commentReviewCategory = new CommentReviewCategory();
		} catch (Exception e) {
			logger.error("DupeHunterComments(ChatBot) - Comment review categorizzer could not be instanced", e);
		}
	}

	@Override
	public void run() {

		try {
			Thread.sleep(10 * 1000); // Give time to login
		} catch (InterruptedException e1) {
			logger.error("run()", e1);
		}

		long start = System.currentTimeMillis() / 1000L - 60 * 1;
		shutDown = false;

		ChatRoom socvfinder = cb.getChatRoom(111347);
		CommentDAO commentDao = new CommentDAO();

		long highTrafficTime = 3 * 30 * 1000L; // Every 1,5 minute
		long lowTrafficTime = 4 * 60 * 1000L; // Every four minutes
		long goodApiQuota = 61 * 1000L; //Every 61 s
		long sleepTime = highTrafficTime;
		while (!shutDown) {

			try {

				ApiResult ap = apiHandler.getComments(start, 10);
				if (ap.getBackoff() > 0) {
					logger.warn("run() - Backoff " + ap.getBackoff());
					Thread.sleep(ap.getBackoff() * 1000L);
				}
				if (ap.getMaxCommentDate() > 0) {
					start = ap.getMaxCommentDate() + 1;
				} else {
					start = System.currentTimeMillis() / 1000L;
				}
				// Set sleep time between tags based on traffic
				List<Comment> comments = ap.getComments();
				if (CloseVoteFinder.getInstance().getApiQuota() > 5000) {
					sleepTime = goodApiQuota;
				} else {
					if (comments.size() > 40) {
						if (sleepTime == lowTrafficTime) {
							if (comments.size() > 80) {
								sleepTime = highTrafficTime;
							}
						}
					} else {
						sleepTime = lowTrafficTime;
					}
				}
				logger.info("Number of commments:" + comments.size() + " Number of pages: " + ap.getNrOfPages());
				List<Comment> possibileDupes = new ArrayList<>();
				List<Comment> possibileRude = new ArrayList<>();
				List<Comment> possibileClose = new ArrayList<>();
				List<Comment> reviewComment = new ArrayList<>();
				// Test run to find rude comments
				for (Comment c : comments) {
					if (c.isPossibleDuplicateComment()) {
						if (!getLastPostIds().contains(c.getPostId())) {
							addPostIdToQue(c.getPostId());
							possibileDupes.add(c);
						}
					}

					/**
					 * RUDE OFFENSIVE TEST
					 */

					boolean hit = classifyHeatComment(socvfinder, c);
					if (hit) {
						possibileRude.add(c);
					}

					int type = classifyCloseComment(socvfinder, c);
					if (type != CommentCloseCategory.HIT_NONE) {
						possibileClose.add(c);
					}

					/**
					 * Disabling review for now
					 */

					// boolean hitReview = classifyReviewComment(socvfinder, c);
					// if (hitReview) {
					// reviewComment.add(c);
					// }

				}

				if (!possibileRude.isEmpty()) {
					try {
						commentDao.insertComment(CloseVoteFinder.getInstance().getConnection(), possibileRude);
					} catch (Exception e) {
						logger.error("run()", e);
					}
				}

				if (!possibileDupes.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("run() ----- DUPE GET QUESTIONS -----");
					}
					Thread.sleep(3 * 1000L);
					if (shutDown) {
						return;
					}
					StringBuilder qQuery = new StringBuilder();
					String sep = "";
					for (Comment c : possibileDupes) {
						qQuery.append(sep).append(c.getPostId());
						sep = ";";
					}
					ApiResult arQ = apiHandler.getQuestions(qQuery.toString(), null, false, null);
					List<Question> questions = arQ.getQuestions();
					// Removed closed questions
					notifyRoomsForDupes(questions);
					if (arQ.getBackoff() > 0) {
						logger.warn("run() - Backoff " + arQ.getBackoff());
						Thread.sleep(arQ.getBackoff() * 1000L);

					}
				}

				if (!possibileClose.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("run() ----- CLOSE GET QUESTIONS -----");
					}
					Thread.sleep(3 * 1000L);
					if (shutDown) {
						return;
					}
					StringBuilder qQuery = new StringBuilder();
					String sep = "";
					for (Comment c : possibileClose) {
						qQuery.append(sep).append(c.getPostId());
						sep = ";";
					}
					ApiResult arQ = apiHandler.getQuestions(qQuery.toString(), null, false, null);
					List<Question> questions = arQ.getQuestions();

					notifyCloseQuestions(questions, possibileClose, socvfinder);
					if (arQ.getBackoff() > 0) {
						logger.warn("run() - Backoff " + arQ.getBackoff());
						Thread.sleep(arQ.getBackoff() * 1000L);
					}
				}

				if (!reviewComment.isEmpty()) {
					notifyReviewComment(reviewComment, socvfinder);
				}

			} catch (Throwable e) {
				logger.error("run()", e);
			}

			if (shutDown) {
				return;
			}

			try {
				if (logger.isDebugEnabled()) {
					logger.debug("run() - Waiting: " + sleepTime / 1000 + "s until next call");
				}
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				logger.error("run()", e);
			}
		}

	}

	private boolean classifyHeatComment(ChatRoom socvfinder, Comment c) {
		boolean hit = false;
		boolean notifyHeat = false;
		try {
			int score = commentHeatCategory.classifyComment(c);
			hit = score >= 4;
			if (hit && CloseVoteFinder.getInstance().isFeedHeat()) {
				if (isNotifyComment(socvfinder, score)) {
					notifyHeat = true;
					notifyHeatComment(socvfinder, c);
				}
			}

//			if (!notifyHeat && c.getPerspectiveResult() != null && c.getPerspectiveResult().isHit()) {
//				notifyPerspectiveComment(socvfinder, c);
//			}

		} catch (Exception e) {
			logger.error("run()", e);
		}
		return hit;
	}

	public int classifyCloseComment(ChatRoom socvfinder, Comment c) {
		int hit = CommentCloseCategory.HIT_NONE;
		try {
			hit = commentCloseCategory.classifyComment(c);
			c.setCloseType(hit);
		} catch (Exception e) {
			logger.error("run()", e);
		}
		return hit;
	}

	public boolean classifyReviewComment(ChatRoom socvfinder, Comment c) {
		boolean hit = false;
		try {
			hit = commentReviewCategory.classifyComment(c);
		} catch (Exception e) {
			logger.error("run()", e);
		}
		return hit;
	}

	private boolean isNotifyComment(ChatRoom socvfinder, int score) {
		if (score >= 6) {
			return true;
		}
		List<CommentsNotify> cnList = CloseVoteFinder.getInstance().getCommentsNotify();
		for (CommentsNotify cn : cnList) {
			if (cn.isNotify() && cn.getScore() <= score) {
				User u = socvfinder.getUser(cn.getUserId());
				if (u != null && u.isCurrentlyInRoom()) {
					return true;
				}
			}
		}
		return false;
	}

	private void notifyHeatComment(ChatRoom socvfinder, Comment c) {
		String commentLink = c.getLink();
		if (commentLink == null) {
			commentLink = "http://stackoverflow.com/questions/" + c.getPostId() + "/#comment" + c.getCommentId() + "_" + c.getPostId();
			c.setLink(commentLink);
		}
		
		if (c.getHiggsReportId()<=0){
			try{
				logger.error("notifyHeatComment(ChatRoom, Comment) sending comment to Higgs");
				c.setHiggsReportId(Higgs.getInstance().registrerComment(c));
			} catch (Throwable e) {
				logger.error("notifyHeatComment(ChatRoom, Comment) could not send comment to Higgs:" + e.getMessage());
			}
		}
		
		

		StringBuilder message = getHeatMessageResult(c, commentLink);
		boolean ccAp = false;
		for (CommentsNotify cn : CloseVoteFinder.getInstance().getCommentsNotify()) {
			if (cn.isNotify() && cn.getScore() <= c.getScore()) {
				User u = socvfinder.getUser(cn.getUserId());
				if (u != null && u.isCurrentlyInRoom()) {
					if (!ccAp) {
						message.append(" cc: ");
						ccAp = true;
					}
					message.append("@").append(u.getName().replaceAll(" ", "")).append(" ");
				}
			}
		}
		socvfinder.send(message.toString());

		CompletionStage<Long> mid = socvfinder.send(commentLink);

		mid.thenAccept(new Consumer<Long>() {

			@Override
			public void accept(Long t) {
				EditRudeCommentThread erct = new EditRudeCommentThread(socvfinder, t, c.getLink());
				erct.start();
			}
		});
	}

	private void notifyPerspectiveComment(ChatRoom socvfinder, Comment c) {
		String commentLink = c.getLink();
		if (commentLink == null) {
			commentLink = "http://stackoverflow.com/questions/" + c.getPostId() + "/#comment" + c.getCommentId() + "_" + c.getPostId();
		}

		StringBuilder message = getPerspectiveMessageResult(c, commentLink);
		socvfinder.send(message.toString());

		CompletionStage<Long> mid = socvfinder.send(commentLink);

		mid.thenAccept(new Consumer<Long>() {

			@Override
			public void accept(Long t) {
				EditRudeCommentThread erct = new EditRudeCommentThread(socvfinder, t, c.getLink());
				erct.start();
			}
		});
	}

	public StringBuilder getPerspectiveMessageResult(Comment c, String commentLink) {
		StringBuilder message = new StringBuilder("[ [Heat Detector](http://stackapps.com/questions/7001/) ]");
		message.append(" [**PERSPECTIVE**](https://www.perspectiveapi.com/) SCORE:")
				.append(NumberFormat.getInstance(Locale.US).format(c.getPerspectiveResult().getScore())).append(" ");
		message.append(" Language:").append(c.getPerspectiveResult().getLanguage());
		message.append(" Type:").append(c.getPerspectiveResult().getType());
		message.append(" HeatScore:").append(c.getScore());
		if (commentLink != null) {
			message.append(" [comment](").append(commentLink).append(")");
		}
		return message;
	}

	public StringBuilder getHeatMessageResult(Comment c, String commentLink) {
		StringBuilder message = new StringBuilder("[ [Heat Detector](http://stackapps.com/questions/7001/)");
		if (c.getHiggsReportId()>0){
			message.append(" | [Hydrant](http://higgs.sobotics.org/Hydrant/report/").append(c.getHiggsReportId()).append(")");
			
		}
		message.append(" ]");
		message.append(" SCORE: ").append(c.getScore()).append(" ").append(getStars(c.getScore()));
		String regex = "NO";
		if (c.getRegExHit() != null) {
			regex = c.getRegExHit();
		}
		message.append(" (").append(getBoldRegexHit(c.isRegExHit())).append("Regex").append(getBoldRegexHit(c.isRegExHit())).append(":").append(regex);
		message.append(" ").append(isHitBold(c.getNaiveBayesBad(), CommentHeatCategory.WEKA_NB_THRESHOLD)).append("NaiveBayes")
				.append(isHitBold(c.getNaiveBayesBad(), CommentHeatCategory.WEKA_NB_THRESHOLD)).append(":").append(nfThreshold.format(c.getNaiveBayesBad()));
		// message.append(" ").append(isHitBold(c.getJ48Bad(),
		// CommentCategory.WEKA_J48_THRESHOLD)).append("J48")
		// .append(isHitBold(c.getJ48Bad(),
		// CommentCategory.WEKA_J48_THRESHOLD)).append(":").append(nfThreshold.format(c.getJ48Bad()));
		// message.append("
		// ").append(isHitBold(c.getSmoBad(),CommentCategory.WEKA_SMO_THRESHOLD)).append("SMO").append(isHitBold(c.getSmoBad(),CommentCategory.WEKA_NB_THRESHOLD)).append(":").append(nfThreshold.format(c.getSmoBad()));
		message.append(" ").append(isHitBold(c.getOpenNlpBad(), CommentHeatCategory.OPEN_NLP_THRESHOLD)).append("OpenNLP")
				.append(isHitBold(c.getOpenNlpBad(), CommentHeatCategory.OPEN_NLP_THRESHOLD)).append(":").append(nfThreshold.format(c.getOpenNlpBad()));
		if (c.getPerspectiveResult() != null) {
			message.append(" ").append(isHitBold(c.getPerspectiveResult().getScore(), CommentHeatCategory.PERSPECTIVE_THRESHOLD)).append("Perspective").append(isHitBold(c.getPerspectiveResult().getScore(), CommentHeatCategory.PERSPECTIVE_THRESHOLD)).append(":").append(nfThreshold.format(c.getPerspectiveResult().getScore()));
		}

		message.append(")");
		if (commentLink != null) {
			message.append(" [comment](").append(commentLink).append(")");
		}
		return message;
	}

	private String getStars(int score) {
		String stars = "";
		int n = 0;
		for (int i = 0; i < (score / 2); i++) {
			stars += "★";
			n++;
		}
		for (int i = n; i < 5; i++) {
			stars += "☆";
		}
		return stars;
	}

	private String getBoldRegexHit(boolean regExHit) {
		if (regExHit) {
			return "**";
		}
		return "";
	}

	private String isHitBold(double badThreshold, double threshold) {
		if (badThreshold >= threshold) {
			return "**";
		}
		return "";
	}

	public void addPostIdToQue(long postId) {
		this.lastPostIds.add(postId);
		if (this.lastPostIds.size() > MAX_POST_ID_QUE) {
			this.lastPostIds.poll();
		}
	}

	private void notifyRoomsForDupes(List<Question> notifyTheseQuestions) {
		if (cb == null) {
			return;
		}

		List<ChatRoom> rooms = new ArrayList<>();
		rooms.addAll(cb.getRooms().values());

		for (Question q : notifyTheseQuestions) {
			if (q.isClosed()) {
				continue;
			}
			for (ChatRoom cr : rooms) {
				String message = "[ [SOCVFinder](http://stackapps.com/questions/6910/) ] [tag:possible-duplicate] " + getTags(cr, q) + " ["
						+ getSanitizedTitle(q) + "](//stackoverflow.com/q/" + q.getQuestionId() + ")";
				if (isQuestionToBeNotified(cr, q)) {
					String send = message + getNotifyHunters(cr, q);
					cr.send(send);
				}
			}
		}
	}

	private void notifyCloseQuestions(List<Question> notifyTheseQuestions, List<Comment> fromComments, ChatRoom sobotics) {

		if (sobotics == null || notifyTheseQuestions.isEmpty()) {
			return;
		}

		if (true) {
			return;
		}

		for (Question q : notifyTheseQuestions) {
			if (q.isClosed()) {
				continue;
			}
			String typeDescr = "possibile-close";
			Comment c = getComment(fromComments, q.getQuestionId());
			if (c != null) {
				typeDescr = CommentCloseCategory.getDescription(c.getCloseType());
			}
			String message = "[ [SOFun](https://www.youtube.com/watch?v=wauzrPn0cfg) ] [tag:" + typeDescr + "] " + getFirstTags(q) + " [" + getSanitizedTitle(q)
					+ "](//stackoverflow.com/q/" + q.getQuestionId() + ")";
			sobotics.send(message);

		}
	}

	private void notifyReviewComment(List<Comment> fromComments, ChatRoom sobotics) {

		if (sobotics == null) {
			return;
		}

		for (Comment comment : fromComments) {
			String message = "[ [SOReviewers](https://www.youtube.com/watch?v=wauzrPn0cfg) ] [tag:lqpq-review] [Post under review](" + comment.getLink()
					+ ") by [" + comment.getDisplayName() + "](http://stackoverflow.com/users/" + comment.getUserId() + ")";
			sobotics.send(message);

		}

	}

	private Comment getComment(List<Comment> fromComments, long questionId) {
		for (Comment c : fromComments) {
			if (c.getPostId() == questionId) {
				return c;
			}
		}
		return null;
	}

	private String getSanitizedTitle(Question q) {
		return Parser.unescapeEntities(q.getTitle(), false).replaceAll("(\\[|\\]|_|\\*|`)", "\\\\$1").trim();
	}

	private boolean isQuestionToBeNotified(ChatRoom cr, Question q) {
		switch (cr.getDupNotifyStrategy()) {
		case ChatRoom.DUPLICATION_NOTIFICATIONS_ALL:
			return true;
		case ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS:
			List<String> roomTags = CloseVoteFinder.getInstance().getRoomTags().get(cr.getRoomId());
			if (roomTags == null) {
				return false;
			}
			for (String tag : q.getTags()) {
				if (roomTags.contains(tag)) {
					return true;
				}
			}
			return false;
		case ChatRoom.DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM:
			List<String> rt = CloseVoteFinder.getInstance().getRoomTags().get(cr.getRoomId());
			boolean inTags = false;
			for (String tag : q.getTags()) {
				if (rt.contains(tag)) {
					inTags = true;
					break;
				}
			}

			if (!inTags) {
				return false;
			}

			List<DuplicateNotifications> dupNotifs = CloseVoteFinder.getInstance().getHunters(cr.getRoomId(), q.getTags());
			for (DuplicateNotifications dn : dupNotifs) {
				User u = cr.getUser(dn.getUserId());
				if (u != null && u.isCurrentlyInRoom()) {
					return true;
				}
			}
			return false;
		case ChatRoom.DUPLICATION_NOTIFICATIONS_NONE:
		default:
			return false;
		}
	}

	private String getFirstTags(Question q) {
		return "[tag:" + q.getTags().get(0) + "]";
	}

	private String getTags(ChatRoom cr, Question q) {
		List<String> hammerTags = CloseVoteFinder.getInstance().getHunters(cr.getRoomId(), q.getTags()).stream().map(DuplicateNotifications::getTag).distinct()
				.collect(Collectors.toCollection(ArrayList::new));
		q.getTags().stream().filter(t -> !hammerTags.contains(t)).findFirst().ifPresent(hammerTags::add);
		return hammerTags.stream().map(t -> "[tag:" + t + "]").collect(Collectors.joining(" "));
	}

	private String getNotifyHunters(ChatRoom cr, Question q) {
		String message = "";
		Set<Long> nHunt = new HashSet<>();
		List<DuplicateNotifications> hunters = CloseVoteFinder.getInstance().getHunters(cr.getRoomId(), q.getTags());
		for (DuplicateNotifications dn : hunters) {
			long userId = dn.getUserId();
			if (nHunt.contains(userId)) {
				continue;
			}
			nHunt.add(userId);
			User u = cr.getUser(userId);
			if (u != null && u.isCurrentlyInRoom()) {
				message += " @" + u.getName().replaceAll(" ", "");
			}
		}
		return message;
	}

	public boolean isShutDown() {
		return shutDown;
	}

	public void setShutDown(boolean shutDown) {
		this.shutDown = shutDown;
	}

	public ChatBot getCb() {
		return cb;
	}

	public void setCb(ChatBot cb) {
		this.cb = cb;
	}

	public Queue<Long> getLastPostIds() {
		return lastPostIds;
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {

		String message = "DICKhead";
		String regExTest = "(?i)(cunt|rude|asshole|rape|bitch|whore|gay|nigger|faggot|slut|cock|eat my|dumbass|pussy|vagina|dick|fuck y|(yo)?u('re| are|r)? (an? )?idiot|(yo)?u('re| are|r)? (an? )?retard)";
		regExTest = "(?is)\\b((yo)?u suck|8={3,}D|nigg(a|er)|ass ?hole|kiss my ass|dumbass|fag(got)?|slut|daf[au][qk]|(mother)?fuc?k+(ing?|e?(r|d)| off+| y(ou|e)(rself)?| u+|tard)?|shit(t?er|head)|dickhead|pedo|whore|(is a )?cunt|cocksucker|ejaculated?|butthurt|(private|pussy) show|lesbo|bitches|suck\\b.{0,20}\\bdick|dee[sz]e? nut[sz])s?\\b|^.{0,250}\\b(shit face)\\b.{0,100}$";

		List<String> list = new ArrayList<>();
		Pattern p = Pattern.compile(regExTest);
		Matcher m = p.matcher(message);
		while (m.find()) {
			list.add(m.group());
		}
		System.out.println(list);
	}

	public CommentHeatCategory getCommentHeatCategory() {
		return commentHeatCategory;
	}

}
