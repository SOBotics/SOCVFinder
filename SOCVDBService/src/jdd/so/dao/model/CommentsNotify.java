package jdd.so.dao.model;

public class CommentsNotify {
	
	private long userId;
	
	public CommentsNotify(){
		super();
	}
	
	public CommentsNotify(long userId, boolean notify, int score) {
		super();
		this.userId = userId;
		this.notify = notify;
		this.score = score;
	}
	private boolean notify;
	private int score;
	
	

	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public boolean isNotify() {
		return notify;
	}
	public void setNotify(boolean noitfy) {
		this.notify = noitfy;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}

}
