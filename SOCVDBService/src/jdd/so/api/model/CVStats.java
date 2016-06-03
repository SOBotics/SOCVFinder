package jdd.so.api.model;

public class CVStats {
	
	private int numberOfQuestions;
	private int numberOfClosed;
	private int[] cvCounts;
	private int[] cvCountsNoRoomba;
	private int possibleDupes;
	private int possibleDupesNoRoomba;
	private String dayKey;
	
	public CVStats(){
		cvCounts = new int[]{0,0,0,0};
		cvCountsNoRoomba = new int[]{0,0,0,0};
	}

	
	public CVStats(String dayKey) {
		this();
		this.dayKey = dayKey;
		
	}


	public int[] getCvCounts() {
		return cvCounts;
	}

	public void setCvCounts(int[] cvCounts) {
		this.cvCounts = cvCounts;
	}


	public int getNumberOfQuestions() {
		return numberOfQuestions;
	}


	public void setNumberOfQuestions(int numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}


	public int getNumberOfClosed() {
		return numberOfClosed;
	}


	public void setNumberOfClosed(int numberOfClosed) {
		this.numberOfClosed = numberOfClosed;
	}


	public int[] getCvCountsNoRoomba() {
		return cvCountsNoRoomba;
	}


	public void setCvCountsNoRoomba(int[] cvCountsNoRoomba) {
		this.cvCountsNoRoomba = cvCountsNoRoomba;
	}


	public int getPossibleDupes() {
		return possibleDupes;
	}


	public void setPossibleDupes(int possibleDupes) {
		this.possibleDupes = possibleDupes;
	}


	public int getPossibleDupesNoRoomba() {
		return possibleDupesNoRoomba;
	}


	public void setPossibleDupesNoRoomba(int possibleDupesNoRoomba) {
		this.possibleDupesNoRoomba = possibleDupesNoRoomba;
	}


	public String getDayKey() {
		return dayKey;
	}


	public void setDayKey(String dayKey) {
		this.dayKey = dayKey;
	}
	
	public String getCVCountAt(int cvNr){
		if (cvNr<=0 || cvNr>4){
			return "";
		}
		return cvCounts[cvNr-1] + " (" + cvCountsNoRoomba[cvNr-1] + ")";
	}

	public String getCVPossibleDupeCount(){
		return possibleDupes + "(" + possibleDupesNoRoomba + ")";
	}
	public void addToStats(Question q) {
		this.numberOfQuestions++;
		if (q.getClosedDate()>0){
			this.numberOfClosed++;
			return;
		}
		int cvCount = q.getCloseVoteCount();
		if (cvCount>0){
			cvCounts[cvCount-1]++;
			if (!q.isRoomba()){
				cvCountsNoRoomba[cvCount-1]++;
			}
		}
		if (q.isPossibleDuplicate()){
			this.possibleDupes++;
			if (!q.isRoomba()){
				this.possibleDupesNoRoomba++;
			}
		}
		
	}
	
}
