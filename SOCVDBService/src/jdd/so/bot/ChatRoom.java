package jdd.so.bot;

import java.util.concurrent.CompletableFuture;

import org.alicebot.ab.Chat;

import fr.tunaki.stackoverflow.chat.Room;

/**
 * The wrapped, ChatRoom, holding info on room
 * @author Petter Friberg
 *
 */
public class ChatRoom {
	
	private ChatBot bot;
	private Room room;
	private int currentBatchNumber;
	private long lastPossibileDupComment; //unix time stamp of last comment
	private boolean enableAi;
	private Chat chatSession;
	

	public ChatRoom(ChatBot bot, Room room, int startBatchNumber, boolean enableAi){
		this.bot = bot;
		this.room = room;
		this.currentBatchNumber = startBatchNumber;
		if (enableAi){
			chatSession =  new Chat(bot.getAiBot());
		}
	}
	
	public String getUnkownCommandResponse(String message) {
		if (chatSession==null){
			return "Sorry I did not recognize your command and the AI functions are disabled";
		}
		String msg = chatSession.multisentenceRespond(message);

		if (msg == null || (msg.toLowerCase().contains("google") || msg.contains("<search>"))) {
			return "Sorry, I do not know";
		}

		if (msg.length() > 400 && !msg.contains("\n")) {
			msg = "Well\n" + msg;
		}
		return msg.replaceAll("<br/>", "\n");

	}


	public Room getRoom() {
		return room;
	}

	public fr.tunaki.stackoverflow.chat.User getUser(long userId) {
		return room.getUser(userId);
	}

	public CompletableFuture<Long> replyTo(long messageId, String message) {
		return room.replyTo(messageId, message);
	}

	public CompletableFuture<Long> send(String message) {
		return room.send(message);
	}
	
	public CompletableFuture<Void> edit(long messageId, String message){
		return room.edit(messageId,message);
	}

	public long getRoomId() {
		return room.getRoomId();
	}

	public CompletableFuture<Void> delete(long messageId) {
		return room.delete(messageId);
	}

	public int getCurrentBatchNumber() {
		return currentBatchNumber;
	}
	
	public int getNextBatchNumber() {
		currentBatchNumber++;
		return currentBatchNumber;
	}

	public void setCurrentBatchNumber(int currentBatchNumber) {
		this.currentBatchNumber = currentBatchNumber;
	}

	public long getLastPossibileDupComment() {
		return lastPossibileDupComment;
	}

	public void setLastPossibileDupComment(long lastPossibileDupComment) {
		this.lastPossibileDupComment = lastPossibileDupComment;
	}

	public boolean isEnableAi() {
		return enableAi;
	}

	public void setEnableAi(boolean enableAi) {
		this.enableAi = enableAi;
	}
}
