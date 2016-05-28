package jdd.so.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdd.so.api.CherryPickResult;
import jdd.so.api.CloseVoteComparator;
import jdd.so.api.PossibileDuplicateComparator;

/**
 * The result from API call
 * @author Petter Friberg
 *
 */
public class ApiResult {

	private List<Question> questions;
	private boolean includeAll;
	private boolean hasMore;
	private int nrOfPages;
	private int quotaRemaining;
	private ScanStats scanStatistics;

	public ApiResult(boolean includeAll) {
		this.includeAll = includeAll;
		questions = new ArrayList<>();
		scanStatistics = new ScanStats();
		hasMore = true;
	}
	
	

	public boolean addQuestion(Question q) {
		if (this.questions.contains(q)){
			return false;
		}
		scanStatistics.addToStats(q);
		if (includeAll || q.isMonitor()) {
			return this.questions.add(q);
		}
		return false;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public void setQuestions(List<Question> questions) {
		this.questions = questions;
	}

	public boolean isIncludeAll() {
		return includeAll;
	}

	public void setIncludeAll(boolean includeClosed) {
		this.includeAll = includeClosed;
	}

	public boolean isHasMore() {
		return hasMore;
	}

	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}

	public int getNrOfPages() {
		return nrOfPages;
	}

	public void setNrOfPages(int nrOfPages) {
		this.nrOfPages = nrOfPages;
	}

	public int getQuotaRemaining() {
		return quotaRemaining;
	}

	public void setQuotaRemaining(int quotaRemaining) {
		this.quotaRemaining = quotaRemaining;
	}

	@Override
	public String toString() {
		String retVal = "TOTAL CALLS: " + nrOfPages + "\n";
		int nrDupes = 0;
		int nrDupesNoRoomba = 0;
		int[] cvCnt = new int[] { 0, 0, 0, 0, 0 };
		int[] cvCntNoRoomba = new int[] { 0, 0, 0, 0, 0 };
		int roomba = 0;
		for (Question question : questions) {
			if (question.isAlmostRoomba()) {
				roomba++;
			}
			if (question.isPossibileDuplicate()) {
				nrDupes++;
				if (!question.isRoomba()) {
					nrDupesNoRoomba++;
				}
			}
			int cvs = question.getCloseVoteCount();
			cvCnt[cvs] += 1;
			if (!question.isRoomba()) {
				cvCntNoRoomba[cvs] += 1;
			}
			retVal += question.toString() + "\n";
		}
		retVal += "DUP=" + nrDupes + "(" + nrDupesNoRoomba + ")";
		for (int i = 1; i < cvCnt.length; i++) {
			retVal += " CV" + i + "=" + cvCnt[i] + "(" + cvCntNoRoomba[i] + ")";
		}
		retVal += " Roomba=" + roomba;
		return retVal;
	}
	
	public List<Question> getPossibileDuplicates(){
		List<Question> pd = new ArrayList<>();
		for (Question question : questions) {
			if (question.isPossibileDuplicate()){
				pd.add(question);
			}
		}
		return pd;
	}

	public String getHTML(String searchTitle) {
		//TODO: This is only temporary code remove after testing
		String html = "<html><head><title>" + searchTitle + "</title></head><body>";

		html += "<h1>" + searchTitle + "</h1>";

		html += "<p>Total api calls: " + nrOfPages + ", questions scanned: " + scanStatistics.getNumberOfQuestions() + ", API quota remaing: " + quotaRemaining + "</p>";

		html += "<h2>Summary</h2>";
		int nrDupes = 0;
		int nrDupesNoRoomba = 0;
		int[] cvCnt = new int[] { 0, 0, 0, 0, 0 };
		int[] cvCntNoRoomba = new int[] { 0, 0, 0, 0, 0 };
		int closeToRoomba = 0;
		List<Question> pdQ = new ArrayList<>();// dup list
		List<Question> cvQ = new ArrayList<>();// cv list

		for (Question question : questions) {
			if (question.isAlmostRoomba()) {
				closeToRoomba++;
			}
			if (question.isPossibileDuplicate()) {
				nrDupes++;
				if (!question.isRoomba()) {
					nrDupesNoRoomba++;
				}
				pdQ.add(question);
			} else {
				cvQ.add(question);
			}
			int cvs = question.getCloseVoteCount();
			cvCnt[cvs] += 1;
			if (!question.isRoomba()) {
				cvCntNoRoomba[cvs] += 1;
			}
		}

		html += "<p>P. DUP=" + nrDupes + "(" + nrDupesNoRoomba + ")";
		for (int i = 1; i < cvCnt.length; i++) {
			html += " CV" + i + "=" + cvCnt[i] + "(" + cvCntNoRoomba[i] + ")";
		}
		html += " Close to roomba=" + closeToRoomba + "</p>";

		html += "<h2>Questions</h2>";
		if (!pdQ.isEmpty()) {
			Collections.sort(pdQ,new PossibileDuplicateComparator());
			html += "<h3>Possibile duplicates</h3>";
			html += "<table width=\"100%\" border=\"1\" style=\"border-collapse: collapse;\">" + CherryPickResult.getTableHeader();
			int nr = 1;
			for (Question question : pdQ) {
				html+=question.getHTML(nr);
			}
			html+="</table>";
		}
		if (!cvQ.isEmpty()) {
			Collections.sort(cvQ,new CloseVoteComparator());
			html += "<h3>Close voted question</h3>";
			html += "<table width=\"100%\" border=\"1\" style=\"border-collapse: collapse;\">" + CherryPickResult.getTableHeader();
			int nr = 1;
			for (Question question : cvQ) {
				html+=question.getHTML(nr);
			}
			html+="</table>";
		}

		html += "</body></html>";
		return html;

	}



	public int getNrOfQuestionScanned() {
		return scanStatistics.getNumberOfQuestions();
	}

	public long getFirstDate() {
		return scanStatistics.getFirstDate();
	}

	public long getLastDate() {
		return scanStatistics.getLastDate();
	}



	public ScanStats getScanStatistics() {
		return scanStatistics;
	}

}
