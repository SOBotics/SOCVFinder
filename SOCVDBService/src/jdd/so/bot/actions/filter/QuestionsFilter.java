package jdd.so.bot.actions.filter;

import jdd.so.CloseVoteFinder;
import jdd.so.api.model.Question;

/**
 * The filter to get from API result all desired questions
 * @author Petter Friberg
 *
 */
public class QuestionsFilter {
	private String command;
	private int numberOfQuestions=CloseVoteFinder.getInstance().getDefaultNumberOfQuestion();
	private NumberFilter closeVotes;
	private NumberFilter scores;
	private AnswersType answerType;
	private NumberFilter days;
	private boolean filterDupes;
	private int numberOfApiCalls = CloseVoteFinder.getInstance().getApiCallNrPages();
	private String excludeQuestions;
	
	
	public QuestionsFilter(){
		super();
	}
	
	public QuestionsFilter(String command){
		super();
		this.command = command.replace("&lt;", "<").replace("&gt;", ">");
		init();
	}
	
	private void init(){
		String[] t = command.toLowerCase().split(" ");
		boolean beforeTag = true;
		for (int i = 0; i < t.length; i++) {
			String s = t[i].trim();
			//remove @message
			if (s.length()==0 || s.contains("@")){
				continue;
			}
			
			//duplicate
			if (s.contains("dup")){
				filterDupes = true;
				continue;
			}
			
			//if before number is number of question
			if (s.contains("[")){
				beforeTag = false;
				continue;
			}
			if (beforeTag){
				try {
					this.numberOfQuestions = Integer.parseInt(s);
					if (this.numberOfQuestions>60){
						this.numberOfQuestions =60;
					}else if (this.numberOfQuestions<=0){
						this.numberOfQuestions=5;
					}
				} catch (NumberFormatException e) {
					//Nothing use default
				}
			}else{
				if (s.contains("cv")){
					closeVotes = new NumberFilter(s.replace("cv", ""));
					closeVotes.validateCloseFilter();
				}else if (s.endsWith("s")){
					scores = new NumberFilter(s.replace("s", ""));
				}else if (s.endsWith("d")){
					days = new NumberFilter(s.replace("d", ""));
					days.validateDayFilter();
				}else{
					//check for answer type
					this.answerType = getAnswerType(s);
				}
			}
		}
	}
	
	private AnswersType getAnswerType(String s) {
		if (s==null){
			return null;
		}
		switch (s){
		case "nr":
			return AnswersType.NO_ROOMBA;
		case "a":
			return  AnswersType.HAS_ANSWER;
		case "cr":
			return AnswersType.CLICK_FROM_ROOMBA;
		case "aa":
			return AnswersType.HAS_ACCEPTED_ANSWER;
		case "na":
			return AnswersType.HAS_NO_ANSWER;
		case "naa":
			return AnswersType.HAS_NO_ACCEPTED_ANSWER;
		default:
			return null;
		}
	}

	/**
	 * Check if question is accpeted by filter
	 * Note: Day filter is already done on api call
	 * @param q
	 * @return true if q is ok
	 */
	public boolean isAccepted(Question q){
		
		if (excludeQuestions!=null && excludeQuestions.contains(";"+q.getQuestionId() + ";")){
			return false;
		}
		
		if (filterDupes){
			if (!q.isPossibleDuplicate()){
				return false;
			}
		}
		
		if (closeVotes!=null && closeVotes.isFilterActive()){
			if (!closeVotes.inRange(q.getCloseVoteCount())){
				return false;
			}
		}
		if (scores!=null && scores.isFilterActive()){
			if (!scores.inRange(q.getScore())){
				return false;
			}
		}
		
		
		if (answerType!=null){
			switch (answerType){
			case CLICK_FROM_ROOMBA:
				return q.isAlmostRoomba();
			case HAS_ANSWER:
				return q.getAnswerCount()>0;
			case HAS_ACCEPTED_ANSWER:
				return q.isAnswerAccepted();
			case HAS_NO_ACCEPTED_ANSWER:
				return !q.isAnswerAccepted();
			case HAS_NO_ANSWER:
				return q.getAnswerCount()<=0;
			case NO_ROOMBA:
				return !q.isRoomba();
			case NO_FILTER:
				return true;
			}
		}
		return true;
	}
	
	

	public static void main(String[] args) {
		CloseVoteFinder.initInstance(null);
		String test =  "@quee [java] [php] cr";
		QuestionsFilter qf = new QuestionsFilter(test);
		System.out.println(qf);
		
	}

	public int getNumberOfQuestions() {
		return numberOfQuestions;
	}

	public void setNumberOfQuestions(int numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}

	public AnswersType getAnswerType() {
		return answerType;
	}

	public void setAnswerType(AnswersType answerType) {
		this.answerType = answerType;
	}

	public NumberFilter getCloseVotes() {
		return closeVotes;
	}

	public void setCloseVotes(NumberFilter closeVotes) {
		this.closeVotes = closeVotes;
	}

	public NumberFilter getScores() {
		return scores;
	}

	public void setScores(NumberFilter scores) {
		this.scores = scores;
	}

	public NumberFilter getDays() {
		return days;
	}

	public void setDays(NumberFilter days) {
		this.days = days;
	}

	public String getCommand() {
		return command;
	}

	public boolean isFilterDupes() {
		return filterDupes;
	}

	public void setFilterDupes(boolean filterDupes) {
		this.filterDupes = filterDupes;
	}

	public int getNumberOfApiCalls() {
		return numberOfApiCalls;
	}

	public void setNumberOfApiCalls(int numberOfApiCalls) {
		this.numberOfApiCalls = numberOfApiCalls;
	}

	public String getExcludeQuestions() {
		return excludeQuestions;
	}

	public void setExcludeQuestions(String excludeQuestions) {
		this.excludeQuestions = excludeQuestions;
	}
	
}
