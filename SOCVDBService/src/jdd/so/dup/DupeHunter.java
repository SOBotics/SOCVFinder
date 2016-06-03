package jdd.so.dup;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
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

public class DupeHunter extends Thread {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(DupeHunter.class);

	public static final long INTERUPT = 1000 * 60 * 10L; // 10 minutes
	private boolean shutDown = false;
	private ApiHandler apiHandler;
	private ChatBot cb;

	Map<String, Long> lastCommentDate;

	public DupeHunter(ChatBot cb) {
		this.setName("DupeHunter");
		this.cb = cb;
		this.apiHandler = new ApiHandler();
		lastCommentDate = new HashMap<>();

	}

	@Override
	public void run() {
		while (!shutDown) {
			// 1. get active tags
			List<String> tags = CloseVoteFinder.getInstance().getHunterTags();
			Set<Long> questionsNotifed = new HashSet<>();

			
			
			//RUN ON SINGLE TAG
			for (String tag : tags) {
				if (logger.isInfoEnabled()) {
					logger.info("run() - Running dupe hunt on tag: " + tag);
				}
				
				if (!isHuntersInRoom(tag)){
					logger.info("run() - No hunter in room: " + tag);
					lastCommentDate.put(tag,System.currentTimeMillis()/1000L);
					continue;
				}
				
				ApiResult ar;
				try {
					ar = apiHandler.getQuestions(tag, 2, false, null);
					List<Question> questions = ar.getPossibileDuplicates();
					Collections.sort(questions, new LastCommentComparator());
					List<Question> notifyTheseQuestions = new ArrayList<>();
					Long currentCommentDate = lastCommentDate.get(tag);
					long maxCreatingDate = 1L;
					for (Question q : questions) {
						Comment c = q.getDuplicatedComment();
						if (currentCommentDate == null) { // avoid first run
							maxCreatingDate = c.getCreationDate();
							break;
						}
						if (c.getCreationDate() > maxCreatingDate) {
							maxCreatingDate = c.getCreationDate();
						}
						if (c.getCreationDate() > currentCommentDate) {
							notifyTheseQuestions.add(q);
						}
					}
					lastCommentDate.put(tag, maxCreatingDate);

					if (logger.isDebugEnabled()) {
						logger.debug("run() - Notification questions size: " + notifyTheseQuestions.size());
					}

					if (!notifyTheseQuestions.isEmpty()) {
						notifyRooms(notifyTheseQuestions, questionsNotifed);
					}
					if (ar.getBackoff() > 0) {
						try {
							logger.warn("run() - Backoff " + ar.getBackoff());
							Thread.sleep(ar.getBackoff() * 1000L);
						} catch (InterruptedException e) {
							logger.error("run()", e);
						}
					}

				} catch (Throwable e) {
					logger.error("run()", e);
				}

				try {
					logger.info("Sleep between tags 30s");
					Thread.sleep(30 * 1000L); // 30 sec between tags
				} catch (InterruptedException e) {
					logger.error("run()", e);
				}

				if (shutDown) {
					return;
				}

			}
			
			//RUN ON ALL TAGS
			for (int i = 0; i < 2; i++) {
				if (logger.isInfoEnabled()) {
					logger.info("run() - Running dupe hunt in all tags runnr: " + i);
				}
//				notifyAllDupes(questionsNotifed);
				try {
					logger.info("Sleep after all tags 5min");
					Thread.sleep(5 * 60 * 1000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if (shutDown) {
				return;
			}

			try {
				Thread.sleep(1*60*1000); //1 min
			} catch (InterruptedException e) {
				logger.error("run()", e);
			}
		}
	}

	private boolean isHuntersInRoom(String tag) {
		if (cb==null){
			return false;
		}
		Map<Long, List<DuplicateNotifications>> huntMap = CloseVoteFinder.getInstance().getHunterInRooms();
		for (List<DuplicateNotifications> dupNot : huntMap.values()) {
			for (DuplicateNotifications dn : dupNot) {
				if (dn.getTag().equalsIgnoreCase(tag)){
					ChatRoom cr = cb.getChatRoom(dn.getRoomId());
					if (cr!=null){
						User u = cr.getUser(dn.getUserId());
						if (u!=null && u.isCurrentlyInRoom()){
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void notifyAllDupes(Set<Long> questionsNotifed) {
		ApiResult ar;
		try {
			ar = apiHandler.getQuestions(null, 4, false, null);
			List<Question> questions = ar.getPossibileDuplicates();
			Collections.sort(questions, new LastCommentComparator());
			List<Question> notifyTheseQuestions = new ArrayList<>();
			Long currentCommentDate = lastCommentDate.get("all");
			long maxCreatingDate = 1L;
			for (Question q : questions) {
				Comment c = q.getDuplicatedComment();
				if (currentCommentDate == null) { // avoid first run
					maxCreatingDate = c.getCreationDate();
					break;
				}
				if (c.getCreationDate() > maxCreatingDate) {
					maxCreatingDate = c.getCreationDate();
				}
				if (c.getCreationDate() > currentCommentDate) {
					notifyTheseQuestions.add(q);
				}
			}
			lastCommentDate.put("all", maxCreatingDate);

			if (logger.isDebugEnabled()) {
				logger.debug("run() - Notification questions size all: " + notifyTheseQuestions.size());
			}

			if (!notifyTheseQuestions.isEmpty()) {
				notifyRoomAll(111347,notifyTheseQuestions, questionsNotifed);
			}
		} catch (Throwable e) {
			logger.error("run()", e);
		}
	}

	private void notifyRooms(List<Question> notifyTheseQuestions, Set<Long> questionsNotifed) {
		if (cb == null) {
			return;
		}

		for (Question q : notifyTheseQuestions) {
			if (questionsNotifed.contains(q.getQuestionId())) {
				continue;
			}
			questionsNotifed.add(q.getQuestionId());
			Map<Long, List<DuplicateNotifications>> chatRoomsHunters = getRoomsAndHunters(q);
			for (Entry<Long, List<DuplicateNotifications>> roomHunters : chatRoomsHunters.entrySet()) {
				ChatRoom cr = cb.getChatRoom(roomHunters.getKey());
				String message = "[tag:possible-duplicate] " + getTags(q) + "[" + Parser.unescapeEntities(q.getTitle(), false)
						+ "](http://stackoverflow.com/questions/" + q.getQuestionId() + ")";
				message += getNotifyHunters(cr, roomHunters.getValue());
				cr.send(message);
			}
		}
	}
	
	
	private void notifyRoomAll(long roomId, List<Question> notifyTheseQuestions, Set<Long> questionsNotifed) {
		if (cb == null) {
			return;
		}

		for (Question q : notifyTheseQuestions) {
			if (questionsNotifed.contains(q.getQuestionId())) {
				continue;
			}
			questionsNotifed.add(q.getQuestionId());
			ChatRoom cr = cb.getChatRoom(roomId);
			String message = "[tag:possible-duplicate] " + getTags(q) + "[" + Parser.unescapeEntities(q.getTitle(), false)
			+ "](http://stackoverflow.com/questions/" + q.getQuestionId() + ")";
			Map<Long, List<DuplicateNotifications>> chatRoomsHunters = getRoomsAndHunters(q);
			message += getNotifyHunters(cr, chatRoomsHunters.get(roomId));
			cr.send(message);
			System.out.println("Notifying on all search");
			
		}
	}

	private Map<Long, List<DuplicateNotifications>> getRoomsAndHunters(Question q) {
		Map<Long, List<DuplicateNotifications>> retMap = new HashMap<>();

		Map<Long, List<DuplicateNotifications>> huntersMap = CloseVoteFinder.getInstance().getHunterInRooms();
		for (Entry<Long, List<DuplicateNotifications>> roomHunters : huntersMap.entrySet()) {
			List<DuplicateNotifications> huntersInTag = getHuntersInTag(roomHunters.getValue(), q);
			if (!huntersInTag.isEmpty()) {
				retMap.put(roomHunters.getKey(), huntersInTag);
			}
		}

		return retMap;
	}

	private List<DuplicateNotifications> getHuntersInTag(List<DuplicateNotifications> allHunters, Question q) {
		List<DuplicateNotifications> retList = new ArrayList<>();
		for (DuplicateNotifications dn : allHunters) {
			boolean hasTag = false;
			for (String tag : q.getTags()) {
				if (dn.getTag().equalsIgnoreCase(tag)) {
					hasTag = true;
					break;
				}
			}
			if (hasTag) {
				retList.add(dn);
			}
		}
		return retList;
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

	private String getNotifyHunters(ChatRoom cr, List<DuplicateNotifications> huntersInTag) {
		String message = "";
		if (huntersInTag==null){
			return message;
		}
		Set<Long> nHunt = new HashSet<>();
		for (DuplicateNotifications dn : huntersInTag) {
			long userId = dn.getUserId();
			if (nHunt.contains(userId)){
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

	public static void main(String[] args) throws InterruptedException, FileNotFoundException, IOException {
		PropertyConfigurator.configure("ini/log4j.properties");

		// Load properties file an instance the CloseVoteFinder
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);

		List<String> tags = new ArrayList<>();
		tags.add("java");
		DupeHunter dp = new DupeHunter(null);
		dp.start();
		Thread.sleep(500);
		dp.setShutDown(true);
	}

}
