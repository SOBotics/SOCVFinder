package jdd.so.bot;

import java.util.concurrent.CompletableFuture;

import org.alicebot.ab.Chat;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.User;

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

		if (msg.length() > 250 && !msg.contains("\n")) {
			msg = "Well\n" + Jsoup.clean(msg, Whitelist.basic());
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
	
	public CompletableFuture<Long> edit(long messageId, String message){
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
	
	public String getRoomName(){
		return room.getThumbs().getName();
	}

	public ChatBot getBot() {
		return bot;
	}

	public User getPingableUser(long userId) {
		for (User u : room.getPingableUsers()) {
			if (u.getId()==userId){
				return u;
			}
		}
		return null;
	}
}
