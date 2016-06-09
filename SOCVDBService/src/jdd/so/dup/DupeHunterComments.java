package jdd.so.dup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
			Thread.sleep(10*1000); //Give time to login
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		long start = System.currentTimeMillis() / 1000L - 60 * 1;
		shutDown = false;
		
		String regExTest = "(?i)(cunt|asshole|bitch|nigger|vagina|dick|fuck|(yo)?u('re| are|r)? (an? )?idiot)";
		Pattern p = Pattern.compile(regExTest);
		
		
		
		ChatRoom socvfinder = cb.getChatRoom(111347);
		
		long highTrafficTime = 3*30*1000L; //Every 1,5 minute
		long lowTrafficTime = 3*60*1000L; //Every three minutes
		long sleepTime = highTrafficTime;
		
		while (!shutDown) {
			try {
				ApiResult ap = apiHandler.getComments(start, 10, true);
				if (ap.getMaxCommentDate()>0){
					start = ap.getMaxCommentDate() + 1;
				}else{
					start = System.currentTimeMillis()/1000L;
				}
				List<Comment> comments = ap.getComments();
				if (comments.size()>40){
					if (sleepTime == lowTrafficTime){
						if (comments.size()>80){
							sleepTime = highTrafficTime;	
						}
					}
				}else{
					sleepTime = lowTrafficTime;
				}
				logger.info("Number of commments:" + comments.size() + " Number of pages: " + ap.getNrOfPages());
				List<Comment> possibileDupes = new ArrayList<>();
				for (Comment c : comments) {
					if (c.isPossibleDuplicateComment()){
						if (!getLastPostIds().contains(c.getPostId())){
							addPostIdToQue(c.getPostId());
							possibileDupes.add(c);
						}
					}
					//Test on report
					if (p.matcher(c.getBody()).find()){
						if (logger.isDebugEnabled()) {
							logger.debug("run() - " + c.getPostId() + ": " + c.getCommentId() + " " + c.getBody());
						}
						socvfinder.send("@Petter incomming possibile rude/abusive comment for testing");
						if (c.getLink()!=null){
							socvfinder.send(c.getLink());	
						}else{
							String message = "http://stackoverflow.com/questions/" + c.getPostId() + "/#comment" + c.getCommentId() + "_" + c.getPostId();
							socvfinder.send(message);
						}
					}
				}

				
				//
				if (!possibileDupes.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("run() - ---- GET QUESTIONS -----");
					}
					Thread.sleep(3 * 1000L);
					if (shutDown){
						return;
					}
					StringBuilder qQuery = new StringBuilder();
					String sep = "";
					for (Comment c : possibileDupes) {
						qQuery.append(sep + c.getPostId());
						sep = ";";
					}
					ApiResult arQ = apiHandler.getQuestions(qQuery.toString(), null, false, null);
					List<Question> questions = arQ.getQuestions();
					for (Question q : questions) {
						if (logger.isDebugEnabled()) {
							logger.debug("run() - " + q);
						}
					}
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
				System.out.println("Waiting: " + sleepTime/1000 + "s until next call");
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				logger.error("run()", e);
			}
		}

	}
	
	public void addPostIdToQue(long postId){
		this.lastPostIds.add(postId);
		if (this.lastPostIds.size()>MAX_POST_ID_QUE){
			this.lastPostIds.poll();
		}
	}

	private void notifyRooms(List<Question> notifyTheseQuestions) {
		if (cb == null) {
			return;
		}

		for (Question q : notifyTheseQuestions) {

			Map<Long, List<DuplicateNotifications>> chatRoomsHunters = getRoomsAndHunters(q);
			for (Entry<Long, List<DuplicateNotifications>> roomHunters : chatRoomsHunters.entrySet()) {
				ChatRoom cr = cb.getChatRoom(roomHunters.getKey());
				String message = "[tag:possible-duplicate] " + getTags(q) + "[" + Parser.unescapeEntities(q.getTitle(), false)
						+ "](http://stackoverflow.com/questions/" + q.getQuestionId() + ")";
				message += getNotifyHunters(cr, roomHunters.getValue());
				cr.send(message);
			}
//			if (chatRoomsHunters.isEmpty()){
//				ChatRoom cr = cb.getChatRoom(111347);
//				String message = "[tag:possible-duplicate] " + getTags(q) + "[" + Parser.unescapeEntities(q.getTitle(), false)
//						+ "](http://stackoverflow.com/questions/" + q.getQuestionId() + ")";
//				cr.send(message);
//			}
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
		if (huntersInTag == null) {
			return message;
		}
		Set<Long> nHunt = new HashSet<>();
		for (DuplicateNotifications dn : huntersInTag) {
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
		
		String message = "fuck that was easy!";
		String regExTest = "(?i)(cunt|asshole|bitch|nigger|vagina|dick|fuck y|(yo)?u('re| are|r)? (an? )?idiot)";
		List<String> list = new ArrayList<String>();
		Pattern p = Pattern.compile(regExTest);
		Matcher m = p.matcher(message);
		while (m.find()) {
		    list.add(m.group());
		}
		System.out.println(list);
		
		DupeHunterComments dc = new DupeHunterComments(null);
		if (!dc.getLastPostIds().contains(111)){
			dc.addPostIdToQue(111);
		}
		if (!dc.getLastPostIds().contains(112)){
			dc.addPostIdToQue(112);
		}

		if (!dc.getLastPostIds().contains(112)){
			dc.addPostIdToQue(112);
		}
		
		if (!dc.getLastPostIds().contains(113)){
			dc.addPostIdToQue(113);
		}
		if (!dc.getLastPostIds().contains(114)){
			dc.addPostIdToQue(114);
		}
		
		System.out.println(dc.getLastPostIds());
	}

	

}
