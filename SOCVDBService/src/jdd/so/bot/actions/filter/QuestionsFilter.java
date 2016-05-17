package jdd.so.bot.actions.filter;

import jdd.so.CloseVoteFinder;
import jdd.so.model.Question;

public class QuestionsFilter {
	private String command;
	private int numberOfQuestions=CloseVoteFinder.getInstance().getDefaultNumberOfQuestion();
	private NumberFilter closeVotes;
	private NumberFilter scores;
	private AnswersType answerType;
	private NumberFilter days;
	private boolean filterDupes;
	
	public QuestionsFilter(String command){
		this.command = command.replace("&lt;", "<").replace("&gt;", ">");
		init();
	}
	
	private void init(){
		String[] t = command.toLowerCase().split(" ");
		boolean beforeTag = true;
		for (int i = 0; i < t.length; i++) {
			String s = t[i].trim();
			if (s.length()==0 || s.contains("@")){
				continue;
			}
			if (s.contains("[")){
				beforeTag = false;
				continue;
			}
			if (beforeTag){
				try {
					this.numberOfQuestions = Integer.parseInt(s);
					if (this.numberOfQuestions>40){
						this.numberOfQuestions =40;
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
				}else if (s.contains("s")){
					scores = new NumberFilter(s.replace("s", ""));
				}else if (s.contains("d")){
					days = new NumberFilter(s.replace("d", ""));
					days.validateDayFilter();
				}else{
					//check for answer type
					switch (s){
					case "nr":
						answerType = AnswersType.NO_ROOMBA;
						break;
					case "a":
						answerType = AnswersType.HAS_ANSWER;
						break;
					case "cr":
						answerType = AnswersType.CLICK_FROM_ROOMBA;
						break;
					case "aa":
						answerType = AnswersType.HAS_ACCEPTED_ANSWER;
						break;
					case "na":
						answerType = AnswersType.HAS_NO_ANSWER;
						break;
					case "naa":
						answerType = AnswersType.HAS_NO_ACCEPTED_ANSWER;
						break;
					}
				}
			}
		}
	}
	
	public boolean isAccepted(Question q){
		
		if (filterDupes){
			if (!q.isPossibileDuplicate()){
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
		
		
		//Try only by querying api
//		if (days!=null && days.isFilterActive()){
//			int daysAgo =(int) ((System.currentTimeMillis()/1000)-q.getCreationDate())/(60*60*24);
//			if (!days.inRange(daysAgo)){
//				return false;
//			}
//		}
		
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
	
}
