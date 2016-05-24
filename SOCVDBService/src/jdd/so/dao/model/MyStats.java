package jdd.so.dao.model;

public class MyStats {
	
	private String tag;
	private int cvCount;
	private int closedCount;
	
	public MyStats() {
		super();
	}
	
	public MyStats(String tag, int cvCount, int closedCount) {
		super();
		this.tag = tag;
		this.cvCount = cvCount;
		this.closedCount = closedCount;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
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
