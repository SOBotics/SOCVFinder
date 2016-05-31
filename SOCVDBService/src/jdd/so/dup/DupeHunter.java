package jdd.so.dup;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

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
			
			for (String tag : tags) {
				if (logger.isInfoEnabled()) {
					logger.info("run() - Running dupe hunt on tag: " + tag);
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
						notifyRooms(tag, notifyTheseQuestions);
					}
					if (ar.getBackoff()>0){
						try {
							Thread.sleep(ar.getBackoff()*1000L);
						} catch (InterruptedException e) {
							logger.error("run()", e);
						}
					}

				} catch (JSONException | IOException e) {
					logger.error("run()", e);
				}

				try {
					Thread.sleep(30 * 1000L); // 30 sec between tags
				} catch (InterruptedException e) {
					logger.error("run()", e);
				}
				
				if (shutDown){
					return;
				}

			}

			try {
				Thread.sleep(INTERUPT);
			} catch (InterruptedException e) {
				logger.error("run()", e);
			}
		}
	}

	private void notifyRooms(String tag, List<Question> notifyTheseQuestions) {
		if (cb==null){
			return;
		}
		Map<Long, List<DuplicateNotifications>> huntersMap = CloseVoteFinder.getInstance().getHunterInRooms();
		for (Entry<Long, List<DuplicateNotifications>> roomHunters : huntersMap.entrySet()) {
			List<DuplicateNotifications> huntersInTag = getHuntersInTag(roomHunters.getValue(),tag);
			if (huntersInTag.isEmpty()){
				continue;
			}
			ChatRoom cr = cb.getChatRoom(roomHunters.getKey());
			for (int i = 0; i < notifyTheseQuestions.size(); i++) {
				Question q = notifyTheseQuestions.get(i);
				String message = "[tag:possible-duplicate] " + getTags(q) + "[" + Jsoup.clean(q.getTitle(), Whitelist.simpleText()) + "](http://stackoverflow.com/questions/"
						+ q.getQuestionId() + ")";
				if (i==0){
					message += getNotifyHunters(cr, huntersInTag);
				}
				cr.send(message);
			}
		}
		
		
	}

	private String getTags(Question q) {
		String retVal = "";
		if (q!=null && q.getTags()!=null){
			for (String tag : q.getTags()) {
				retVal += "[tag:" + tag + "] ";  
			}
		}
		return retVal;
	}

	private String getNotifyHunters(ChatRoom cr, List<DuplicateNotifications> huntersInTag) {
		String message = "";
		for (DuplicateNotifications dn : huntersInTag) {
			long userId = dn.getUserId();
			User u = cr.getUser(userId);
			if (u!=null && u.isCurrentlyInRoom()){
				message += " @" + u.getName().replaceAll(" ", "");
			}
		}
		return message;
	}

	private List<DuplicateNotifications> getHuntersInTag(List<DuplicateNotifications> allHunters, String tag) {
		List<DuplicateNotifications> retList = new ArrayList<>();
		for (DuplicateNotifications dn : allHunters) {
			if (dn.getTag().equalsIgnoreCase(tag)){
				retList.add(dn);
			}
		}
		return retList;
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
