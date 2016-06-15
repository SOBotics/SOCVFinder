package jdd.so.dup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import jdd.so.dao.model.DuplicateNotifications;

public class DupeHunterComments extends Thread {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(DupeHunterComments.class);

	private static final int MAX_POST_ID_QUE = 10;

	ApiHandler apiHandler;
	private ChatBot cb;
	Queue<Long> lastPostIds;

	private boolean shutDown;

	public DupeHunterComments(ChatBot cb) {
		this.setName("DupeHunterComments");
		this.cb = cb;
		this.apiHandler = new ApiHandler();
		this.lastPostIds = new ArrayDeque<>();
	}

	public void run() {

		try {
			Thread.sleep(10 * 1000); // Give time to login
		} catch (InterruptedException e1) {
			logger.error("run()", e1);
		}

		long start = System.currentTimeMillis() / 1000L - 60 * 1;
		shutDown = false;

		// String regExTest = "(?i)(cunt|rude|asshole|
		// rape|bitch|whore|gay|nigger|faggot|slut|cock|eat
		// my|dumbass|pussy|vagina|dick|fuck y|(yo)?u('re| are|r)? (an?
		// )?idiot|(yo)?u('re| are|r)? (an? )?retard)";
		String regExTest = "(?is)\\b((yo)?u suck|8={3,}D|nigg(a|er)|ass ?hole|kiss my ass|dumbass|fag(got)?|slut|moron|daf[au][qk]|(mother)?fuc?k+(ing?|e?(r|d)| off+| y(ou|e)(rself)?| u+|tard)?|shit(t?er|head)|dickhead|pedo|whore|(is a )?cunt|cocksucker|ejaculated?|butthurt|(private|pussy) show|lesbo|bitches|suck\\b.{0,20}\\bdick|dee[sz]e? nut[sz])s?\\b|^.{0,250}\\b(shit face)\\b.{0,100}$";
		Pattern p = Pattern.compile(regExTest);

		ChatRoom socvfinder = cb.getChatRoom(111347);

		long highTrafficTime = 3 * 30 * 1000L; // Every 1,5 minute
		long lowTrafficTime = 4 * 60 * 1000L; // Every four minutes
		long sleepTime = highTrafficTime;

		while (!shutDown) {
			try {
				ApiResult ap = apiHandler.getComments(start, 10, true);
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
				// Test run to find rude comments
				for (Comment c : comments) {
					if (c.isPossibleDuplicateComment()) {
						if (!getLastPostIds().contains(c.getPostId())) {
							addPostIdToQue(c.getPostId());
							possibileDupes.add(c);
						}
					}
					// Test on report
					if (p.matcher(c.getBody()).find()) {
						if (logger.isDebugEnabled()) {
							logger.debug("run() - " + c.getPostId() + ": " + c.getCommentId() + " " + c.getBody());
						}
						socvfinder.send("@Petter, @Kyll incomming possibile rude/abusive comment for testing");
						if (c.getLink() != null) {
							socvfinder.send(c.getLink());
						} else {
							String message = "http://stackoverflow.com/questions/" + c.getPostId() + "/#comment" + c.getCommentId() + "_" + c.getPostId();
							socvfinder.send(message);
						}
					}
				}

				if (!possibileDupes.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("run() - ---- GET QUESTIONS -----");
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

		for (Question q : notifyTheseQuestions) {
			String message = "[tag:possible-duplicate] " + getTags(q) + "[" + Parser.unescapeEntities(q.getTitle(), false)
					+ "](http://stackoverflow.com/questions/" + q.getQuestionId() + ")";

			Collection<ChatRoom> rooms = cb.getRooms().values();
			for (ChatRoom cr : rooms) {
				
				if (isQuestionToBeNotified(cr,q)){
					String send = message + getNotifyHunters(cr, q);
					cr.send(send);
				}

			}

		}
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
					inTags= true;
					break;
				}
			}
			
			if (!inTags){
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


	private String getTags(Question q) {
		String retVal = "";
		if (q != null && q.getTags() != null) {
			for (String tag : q.getTags()) {
				retVal += "[tag:" + tag + "] ";
			}
		}
		return retVal;
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

}
