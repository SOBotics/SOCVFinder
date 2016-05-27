package jdd.so.dao.model;

public class DuplicateNotifications {
	private long userId;
	private long roomId;
	private String tag;
	private boolean optIn;
	
	public DuplicateNotifications(){
		super();
	}
	
	public DuplicateNotifications(long roomId, long userId, String tag, boolean optIn) {
		super();
		this.roomId = roomId;
		this.userId = userId;
		this.tag = tag;
		this.optIn = optIn;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public long getRoomId() {
		return roomId;
	}
	public void setRoomId(long room_id) {
		this.roomId = room_id;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public boolean isOptIn() {
		return optIn;
	}
	public void setOptIn(boolean optIn) {
		this.optIn = optIn;
	}
	

}
