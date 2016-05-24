package jdd.so.dao.model;

public class RoomStats {
	
	private long roomId;
	private int cvCount;
	private int closedCount;
	
	public RoomStats(){
		super();
	}
	
	public RoomStats(long roomId, int cvCount, int closedCount) {
		super();
		this.roomId = roomId;
		this.cvCount = cvCount;
		this.closedCount = closedCount;
	}
	public long getRoomId() {
		return roomId;
	}
	public void setRoomId(long roomId) {
		this.roomId = roomId;
	}
	public int getCvCount() {
		return cvCount;
	}
	public void setCvCount(int cvCount) {
		this.cvCount = cvCount;
	}
	public int getClosedCount() {
		return closedCount;
	}
	public void setClosedCount(int closedCount) {
		this.closedCount = closedCount;
	}
	
	

}
