package jdd.so.dao.model;

public class WhiteList {
	
	private long questionId;
	private long userId;
	private long creationDate;
	
	
	
	public WhiteList() {
		super();
	}
	
	public WhiteList(long questionId, long userId, long creationDate) {
		super();
		this.questionId = questionId;
		this.userId = userId;
		this.creationDate = creationDate;
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
	public long getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

}
