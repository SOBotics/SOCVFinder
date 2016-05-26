package jdd.so.dao.model;

public class Stats {
	
	private long id;
	private String description;
	private int nrQuestions;
	private int virtualCvCount;
	private int cvCount;
	private int closedCount;
	
	public Stats() {
		super();
	}
	
	public Stats(long id, int nrQuestions, int virtualCvCount, int cvCount, int closedCount) {
		super();
		this.id = id;
		this.nrQuestions = nrQuestions;
		this.virtualCvCount = virtualCvCount;
		this.cvCount = cvCount;
		this.closedCount = closedCount;
	}
	
	public Stats(String tag, int nrQuestions,int virtualCvCount, int cvCount, int closedCount) {
		super();
		this.description = tag;
		this.nrQuestions = nrQuestions;
		this.virtualCvCount = virtualCvCount;
		this.cvCount = cvCount;
		this.closedCount = closedCount;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String tag) {
		this.description = tag;
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

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getVirtualCvCount() {
		return virtualCvCount;
	}

	public void setVirtualCvCount(int cvEffect) {
		this.virtualCvCount = cvEffect;
	}

	public int getNrQuestions() {
		return nrQuestions;
	}

	public void setNrQuestions(int nrQuestions) {
		this.nrQuestions = nrQuestions;
	}

}
