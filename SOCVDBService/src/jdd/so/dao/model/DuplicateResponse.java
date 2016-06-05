package jdd.so.dao.model;

public class DuplicateResponse {
	
	private long questionId;
	private long userId;
	private long roomId;
	private boolean confirmed;
	private String tag;
	private long responseDate;
	
	
	public DuplicateResponse() {
		super();
	}
	
	public DuplicateResponse(long questionId, long userId, long roomId, boolean confirmed, String tag,long responseDat1e) {
		super();
		this.questionId = questionId;
		this.userId = userId;
		this.roomId = roomId;
		this.confirmed = confirmed;
		this.tag = tag;
		responseDate = responseDat1e;
	}
	public long getQuestionId() {
		return questionId;
	}
	public void setQuestionId(long questionId) {
		this.questionId = questionId;
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
	public void setRoomId(long roomId) {
		this.roomId = roomId;
	}
	public boolean isConfirmed() {
		return confirmed;
	}
	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public long getResponseDate() {
		return responseDate;
	}

	public void setResponseDate(long responseDate) {
		this.responseDate = responseDate;
	}

}
