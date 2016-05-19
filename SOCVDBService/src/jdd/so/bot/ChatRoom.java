package jdd.so.bot;

import fr.tunaki.stackoverflow.chat.Room;

public class ChatRoom {
	
	private Room room;

	public ChatRoom(Room room){
		this.room = room;
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
}
