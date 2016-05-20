package jdd.so.dao.model;

public class User implements Comparable<User>{
	
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
		if (userName==null || userName.trim().length()==0){
			return "Not indicated";
		}
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

	@Override
	public int compareTo(User o) {
		if (o==null){
			return -1;
		}
		int al = getAccessLevel()-o.getAccessLevel();
		if (al!=0){
			return al;
		}
		return getUserName().compareTo(o.getUserName());
	}
	

}
