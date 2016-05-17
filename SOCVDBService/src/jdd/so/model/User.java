package jdd.so.model;

public class User {
	
	private long userId;
	private String userName;
	private int accessLevel;
	
	public User(){
		super();
	}
	
	public User(long userId, String userName, int accessLevel) {
		super();
		this.userId = userId;
		this.userName = userName;
		this.accessLevel = accessLevel;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public int getAccessLevel() {
		return accessLevel;
	}
	public void setAccessLevel(int accessLevel) {
		this.accessLevel = accessLevel;
	}

}
