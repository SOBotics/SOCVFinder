package jdd.so.dup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import jdd.so.api.ApiHandler;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Question;
import jdd.so.bot.ChatBot;

public class DupeHunter extends Thread {

	public static long INTERUPT = 1000*60*10; //10 minutes 
	private boolean shutDown = false;
	private ApiHandler apiHandler;
	private ChatBot cb;
	private long lastCommentDate;

	public DupeHunter(ChatBot cb){
		this.cb = cb;
		this.apiHandler = new ApiHandler();
	}
	
	@Override
	public void run(){
		List<String> tags = new ArrayList<>();
		tags.add("java");
		while (!shutDown){
			for (String tag : tags) {
				ApiResult ar;
				try {
					ar = apiHandler.getQuestions(tag, 2, false, null);
					List<Question> questions = ar.getPossibileDuplicates();
					
					
				} catch (JSONException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			try {
				Thread.sleep(INTERUPT);
			} catch (InterruptedException e) {
				e.printStackTrace();
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
	
	
	
}
