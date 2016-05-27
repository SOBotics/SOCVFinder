package jdd.so.dup;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;

import jdd.so.CloseVoteFinder;
import jdd.so.api.ApiHandler;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Comment;
import jdd.so.api.model.Question;
import jdd.so.bot.ChatBot;
import jdd.so.bot.ChatRoom;

public class DupeHunter extends Thread {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(DupeHunter.class);

	public static long INTERUPT = 1000*60*10L; //10 minutes 
	private boolean shutDown = false;
	private ApiHandler apiHandler;
	private ChatBot cb;
	private List<String> tags;
	
	Map<String,Long> lastCommentDate; 

	public DupeHunter(ChatBot cb, List<String> tags){
		this.setName("DupeHunter");
		this.cb = cb;
		this.tags = tags;
		this.apiHandler = new ApiHandler();
		lastCommentDate = new HashMap<>();
		for (String tag : tags) {
			lastCommentDate.put(tag, 0L);
		}
	}
	
	@Override
	public void run(){
		while (!shutDown){
			for (String tag : tags) {
				if (logger.isInfoEnabled()) {
					logger.info("run() - Running dupe hunt on tag: " + tag);
				}
				ApiResult ar;
				try {
					ar = apiHandler.getQuestions(tag, 2, false, null);
					List<Question> questions = ar.getPossibileDuplicates();
					Collections.sort(questions,new LastCommentComparator());
					List<Question> notifyTheseQuestions = new ArrayList<>();
					long currentCommentDate = lastCommentDate.get(tag);
					long maxCreatingDate = 1L;
					for (Question q : questions) {
						Comment c = q.getDuplicatedComment();
						if (currentCommentDate==0){ //avoid first run
							maxCreatingDate=c.getCreationDate();
							break;
						}
						if (c.getCreationDate()>maxCreatingDate){
							maxCreatingDate = c.getCreationDate();
						}
						if (c.getCreationDate()>currentCommentDate){
							notifyTheseQuestions.add(q);
						}
					}
					lastCommentDate.put(tag, maxCreatingDate);
					
					if (logger.isDebugEnabled()) {
						logger.debug("run() - Notification questions size: " + notifyTheseQuestions.size());
					}
			
					if (!notifyTheseQuestions.isEmpty() && cb!=null){
						ChatRoom cr = cb.getChatRoom(111347);
						if (cr!=null){
							for (Question q : notifyTheseQuestions) {
								cr.send("[tag:possible-duplicate] [tag:" + tag + "] [" + q.getTitle() + "](http://stackoverflow.com/questions/" + q.getQuestionId() + ")");
							}
						}
					}
					
				} catch (JSONException | IOException e) {
					logger.error("run()", e);
				}
			
				try {
					Thread.sleep(10*1000L); //10 sec between tags
				} catch (InterruptedException e) {
					logger.error("run()", e);
				}
				
			}
			
			try {
				Thread.sleep(INTERUPT);
			} catch (InterruptedException e) {
				logger.error("run()", e);
			}
		}
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
		DupeHunter dp = new DupeHunter(null,tags);
		dp.start();
		Thread.sleep(500);
		dp.setShutDown(true);
	}
	
}
