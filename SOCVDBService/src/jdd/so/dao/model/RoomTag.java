package jdd.so.dao.model;

public class RoomTag {
	
	private long roomId;
	private String tag;
	
	public RoomTag(){
		super();
	}
	
	public RoomTag(long roomId, String tag) {
		super();
		this.roomId = roomId;
		this.tag = tag;
	}
	public long getRoomId() {
		return roomId;
	}
	public void setRoomId(long roomId) {
		this.roomId = roomId;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	

}
