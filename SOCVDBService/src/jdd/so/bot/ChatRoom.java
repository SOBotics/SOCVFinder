package jdd.so.bot;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicStrings;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.User;
import jdd.so.bot.actions.BotCommand;

/**
 * The wrapped, ChatRoom, holding info on room
 * @author Petter Friberg
 *
 */
public class ChatRoom {

	public static final int DUPLICATION_NOTIFICATIONS_NONE = 0;
	public static final int DUPLICATION_NOTIFICATIONS_ALL = 1;
	public static final int DUPLICATION_NOTIFICATIONS_TAGS = 2;
	public static final int DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM = 3;


	private ChatBot bot;
	private Room room;
	private int currentBatchNumber;
	private long lastPossibleDupComment; //unix time stamp of last comment
	private boolean enableAi;
	private Chat chatSession;
	private CompletionStage<Long> lastMessage;
	private List<Class<? extends BotCommand>> allowedCommands;
	private int dupNotifyStrategy;


	public ChatRoom(ChatBot bot, Room room, int startBatchNumber, List<Class<? extends BotCommand>> allowedCommands,int dupNotifyStrategy, boolean enableAi){
		this.bot = bot;
		this.room = room;
		this.currentBatchNumber = startBatchNumber;
		this.allowedCommands = allowedCommands;
		this.dupNotifyStrategy = dupNotifyStrategy;
		if (enableAi){
			chatSession =  new Chat(bot.getAiBot());
		}
	}

	public String getUnkownCommandResponse(String message,String userName) {
		if (chatSession==null){
			return "Sorry I did not recognize your command and the AI functions are disabled";
		}
		MagicStrings.default_Customer_id = userName;
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

	public CompletionStage<Long> replyTo(long messageId, String message) {
		this.lastMessage = room.replyTo(messageId, message);
		return this.lastMessage;
	}

	public CompletionStage<Long> send(String message) {
		this.lastMessage = room.send(message);
		return this.lastMessage;
	}

	public CompletionStage<Long> edit(long messageId, String message){
		return room.edit(messageId,message);
	}

	public long getRoomId() {
		return room.getRoomId();
	}

	public CompletionStage<Void> delete(long messageId) {
		return room.delete(messageId);
	}

	public void leave(){
		this.room.leave();
		this.bot.getRooms().remove(getRoomId());
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

	public long getLastPossibleDupComment() {
		return lastPossibleDupComment;
	}

	public void setLastPossibleDupComment(long lastPossibleDupComment) {
		this.lastPossibleDupComment = lastPossibleDupComment;
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

	public CompletionStage<Long> getLastMessage() {
		return lastMessage;
	}

	public List<Class<? extends BotCommand>> getAllowedCommands() {
		return allowedCommands;
	}

	public void setAllowedCommands(List<Class<? extends BotCommand>> allowedCommands) {
		this.allowedCommands = allowedCommands;
	}

	public boolean isAllowed(BotCommand bc) {
		if (this.allowedCommands==null){
			return true;
		}
		return this.allowedCommands.contains(bc.getClass());
	}

	public int getDupNotifyStrategy() {
		return dupNotifyStrategy;
	}

	public void setDupNotifyStrategy(int duplicationNotifications) {
		this.dupNotifyStrategy = duplicationNotifications;
	}
}
