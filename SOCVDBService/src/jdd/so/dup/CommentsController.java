package jdd.so.dup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.jsoup.parser.Parser;

import fr.tunaki.stackoverflow.chat.User;
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
import jdd.so.nlp.CommentCategory;

public class CommentsController extends Thread {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentsController.class);

	private static final int MAX_POST_ID_QUE = 10;
	private ApiHandler apiHandler;
	private CommentCategory commentCategory;
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
			commentCategory = new CommentCategory();
		} catch (Exception e) {
			logger.error("DupeHunterComments(ChatBot) - Comment categorizzer could not be instanced", e);
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
				if (comments.size() > 40) {
					if (sleepTime == lowTrafficTime) {
						if (comments.size() > 80) {
							sleepTime = highTrafficTime;
						}
					}
				} else {
					sleepTime = lowTrafficTime;
				}
				logger.info("Number of commments:" + comments.size() + " Number of pages: " + ap.getNrOfPages());
				List<Comment> possibileDupes = new ArrayList<>();
				List<Comment> possibileRude = new ArrayList<>();
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

					boolean hit = classifyComment(socvfinder, c);
					if (hit) {
						possibileRude.add(c);
					}
				}

				if (!possibileRude.isEmpty()) {
					try {
						commentDao.insertComment(CloseVoteFinder.getInstance().getConnection(), possibileRude);
					} catch (SQLException e) {
						logger.error("run()", e);
					}
				}

				if (!possibileDupes.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("run() ----- GET QUESTIONS -----");
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
					notifyRooms(questions);
					if (arQ.getBackoff() > 0) {
						logger.warn("run() - Backoff " + arQ.getBackoff());
						Thread.sleep(arQ.getBackoff() * 1000L);

					}
				}

			} catch (JSONException | IOException | InterruptedException e) {
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

	private boolean classifyComment(ChatRoom socvfinder, Comment c) {
		boolean hit = false;
		try {
			int score = commentCategory.classifyComment(c);
			hit = score >= 4;
			if (hit && CloseVoteFinder.getInstance().isFeedHeat()) {
				if (isUserWithScorePresent(socvfinder, score)) {
					notifyComment(socvfinder, c);
				}
			}

		} catch (Exception e) {
			logger.error("run()", e);
		}
		return hit;
	}

	private boolean isUserWithScorePresent(ChatRoom socvfinder, int score) {
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

	private void notifyComment(ChatRoom socvfinder, Comment c) {
		String commentLink = c.getLink();
		if (commentLink == null) {
			commentLink = "http://stackoverflow.com/questions/" + c.getPostId() + "/#comment" + c.getCommentId() + "_" + c.getPostId();
		}

		StringBuilder message = getHeatMessageResult(c, commentLink);
		message.append(" cc: ");
		for (CommentsNotify cn : CloseVoteFinder.getInstance().getCommentsNotify()) {
			if (cn.isNotify() && cn.getScore() <= c.getScore()) {
				User u = socvfinder.getUser(cn.getUserId());
				if (u != null && u.isCurrentlyInRoom()) {
					message.append("@").append(u.getName().replaceAll(" ", "")).append(" ");
				}
			}
		}
		socvfinder.send(message.toString());

		CompletableFuture<Long> mid = socvfinder.send(commentLink);

		mid.thenAccept(new Consumer<Long>() {

			@Override
			public void accept(Long t) {
				EditRudeCommentThread erct = new EditRudeCommentThread(socvfinder, t, c.getLink());
				erct.start();
			}
		});
	}

	public StringBuilder getHeatMessageResult(Comment c, String commentLink) {
		StringBuilder message = new StringBuilder("[ [Heat Detector](http://stackapps.com/questions/7001/) ]");
		message.append(" SCORE: ").append(c.getScore()).append(" ").append(getStars(c.getScore()));
		message.append(" (").append(getBoldRegexHit(c.isRegExHit())).append("Regex").append(getBoldRegexHit(c.isRegExHit())).append(":").append(c.isRegExHit());
		message.append(" ").append(isHitBold(c.getNaiveBayesBad(), CommentCategory.WEKA_NB_THRESHOLD)).append("NaiveBayes")
				.append(isHitBold(c.getNaiveBayesBad(), CommentCategory.WEKA_NB_THRESHOLD)).append(":").append(nfThreshold.format(c.getNaiveBayesBad()));
		message.append(" ").append(isHitBold(c.getJ48Bad(), CommentCategory.WEKA_J48_THRESHOLD)).append("J48")
				.append(isHitBold(c.getJ48Bad(), CommentCategory.WEKA_J48_THRESHOLD)).append(":").append(nfThreshold.format(c.getJ48Bad()));
		// message.append("
		// ").append(isHitBold(c.getSmoBad(),CommentCategory.WEKA_SMO_THRESHOLD)).append("SMO").append(isHitBold(c.getSmoBad(),CommentCategory.WEKA_NB_THRESHOLD)).append(":").append(nfThreshold.format(c.getSmoBad()));
		message.append(" ").append(isHitBold(c.getOpenNlpBad(), CommentCategory.OPEN_NLP_THRESHOLD)).append("OpenNLP")
				.append(isHitBold(c.getOpenNlpBad(), CommentCategory.OPEN_NLP_THRESHOLD)).append(":").append(nfThreshold.format(c.getOpenNlpBad())).append(")");
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

	private void notifyRooms(List<Question> notifyTheseQuestions) {
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

	private String getSanitizedTitle(Question q) {
		return Parser.unescapeEntities(q.getTitle(), false).replaceAll("(\\[|\\]|_|\\*|`)", "\\\\$1");
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

		List<String> list = new ArrayList<String>();
		Pattern p = Pattern.compile(regExTest);
		Matcher m = p.matcher(message);
		while (m.find()) {
			list.add(m.group());
		}
		System.out.println(list);
	}

	public CommentCategory getCommentCategory() {
		return commentCategory;
	}

}
