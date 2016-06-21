package jdd.so.dup;

import jdd.so.bot.ChatRoom;

public class EditRudeCommentThread extends Thread {
	
	private ChatRoom room;
	private long messageId;
	private String link;

	public EditRudeCommentThread(ChatRoom room, long messageId, String link){
		this.room = room;
		this.messageId = messageId;
		this.link = link;
		
	}
	
	@Override
	public void run(){
		try {
			Thread.sleep(100*1000L);
		} catch (InterruptedException e) {}
		room.edit(messageId, link + " ...");
	}

}
