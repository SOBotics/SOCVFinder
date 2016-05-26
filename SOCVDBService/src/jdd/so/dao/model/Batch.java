package jdd.so.dao.model;

import jdd.so.api.CherryPickResult;

public class Batch {

	private long roomId;
	private long messageId;
	private long batchDateStart;
	private int batchNr;
	private long userId;
	private String searchTags;
	private String questions;
	private int numberOfQuestions;
	private int cvCountBefore;
	private long batchDateEnd;
	private int cvCountAfter;
	private int closedCount;
	
	public long getRoomId() {
		return roomId;
	}
	public void setRoomId(long roomId) {
		this.roomId = roomId;
	}
	public long getBatchDateStart() {
		return batchDateStart;
	}
	public void setBatchDateStart(long batchDateStart) {
		this.batchDateStart = batchDateStart;
	}
	public int getBatchNr() {
		return batchNr;
	}
	public void setBatchNr(int batchNr) {
		this.batchNr = batchNr;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public String getSearchTags() {
		return searchTags;
	}
	public void setSearchTags(String searchTags) {
		this.searchTags = searchTags;
	}
	public String getQuestions() {
		return questions;
	}
	public void setQuestions(String questions) {
		this.questions = questions;
	}
	public int getCvCountBefore() {
		return cvCountBefore;
	}
	public void setCvCountBefore(int cvCountBefore) {
		this.cvCountBefore = cvCountBefore;
	}
	public long getBatchDateEnd() {
		return batchDateEnd;
	}
	public void setBatchDateEnd(long batchDateEnd) {
		this.batchDateEnd = batchDateEnd;
	}
	public int getCvCountAfter() {
		return cvCountAfter;
	}
	public void setCvCountAfter(int cvCountAfter) {
		this.cvCountAfter = cvCountAfter;
	}
	public int getClosedCount() {
		return closedCount;
	}
	public void setClosedCount(int closedCount) {
		this.closedCount = closedCount;
	}
	public long getMessageId() {
		return messageId;
	}
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}
	public int getNumberOfQuestions() {
		return numberOfQuestions;
	}
	public void setNumberOfQuestions(int numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}

}
