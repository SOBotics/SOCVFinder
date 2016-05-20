package jdd.so.bot;

import fr.tunaki.stackoverflow.chat.Room;

/**
 * The wrapped, ChatRoom, holding info on room
 * @author Petter Friberg
 *
 */
public class ChatRoom {
	
	private Room room;
	private int currentBatchNumber;
	private long lastPossibileDupComment; //unix time stamp of last comment

	public ChatRoom(Room room, int startBatchNumber){
		this.room = room;
		this.currentBatchNumber = startBatchNumber;
	}

	public Room getRoom() {
		return room;
	}

	public fr.tunaki.stackoverflow.chat.User getUser(long userId) {
		return room.getUser(userId);
	}

	public void replyTo(long messageId, String message) {
		room.replyTo(messageId, message);
	}

	public void send(String message) {
		room.send(message);
	}

	public long getRoomId() {
		return room.getRoomId();
	}

	public void delete(long messageId) {
		room.delete(messageId);
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
}
