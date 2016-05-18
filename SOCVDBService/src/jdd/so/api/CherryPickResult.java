package jdd.so.api;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import jdd.so.bot.actions.filter.AnswersType;
import jdd.so.bot.actions.filter.QuestionsFilter;
import jdd.so.model.ApiResult;
import jdd.so.model.CloseVoteComparator;
import jdd.so.model.PossibileDuplicateComparator;
import jdd.so.model.Question;
import jdd.so.rest.RESTApiHandler;

public class CherryPickResult {
	
	private ApiResult apiResult;
	
	
	private long batchNumber;
	private String searchTag;
	private long timestamp;

	private List<Question> filterdQuestions;
	private boolean filteredOnDuplicates;
	private String batchUrl;

	private long roomId;

	public CherryPickResult(ApiResult apiResult, long roomId, String tag){
		this.apiResult = apiResult;
		this.roomId = roomId; 
		this.searchTag = tag;
		this.timestamp = System.currentTimeMillis();
		//Just some random number for now
		this.batchNumber = (long) (Math.random()*100);
	}
	
	public void filterCherry(QuestionsFilter questionFilter) {
		filteredOnDuplicates = false;
		filter(questionFilter,new CloseVoteComparator());
	}
	
	public void filterDuplicates(QuestionsFilter questionFilter){
		filteredOnDuplicates = true;
		filter(questionFilter,new PossibileDuplicateComparator());
	}
	
	
	private void filter(QuestionsFilter questionFilter, Comparator<Question> sorter){
		filterdQuestions = new ArrayList<Question>(apiResult.getQuestions().size());
		for (Question q : apiResult.getQuestions()) {
			if (questionFilter.isAccepted(q)){
				if (!filterdQuestions.contains(q)){
					filterdQuestions.add(q);
				}
			}
		}
		Collections.sort(filterdQuestions,sorter);
		if (filterdQuestions.size()>questionFilter.getNumberOfQuestions()){
			filterdQuestions.subList(questionFilter.getNumberOfQuestions(),filterdQuestions.size()).clear();
		}
	}
	
	@Deprecated
	public void filterCherry(int maxQuestions, String cvFilter, String scoreFilter, AnswersType answerType){
		filteredOnDuplicates = false;
		filterdQuestions = new ArrayList<Question>(apiResult.getQuestions().size());
		filterdQuestions.addAll(apiResult.getQuestions());

		Collections.sort(filterdQuestions,new CloseVoteComparator());
		if (filterdQuestions.size()>maxQuestions){
			filterdQuestions.subList(maxQuestions,filterdQuestions.size()).clear();
		}
	}

	@Deprecated
	public void filterDuplicates(int maxQuestions){
		filteredOnDuplicates = true;
		filterdQuestions = new ArrayList<Question>(apiResult.getPossibileDuplicates().size());
		for (Question question : apiResult.getQuestions()) {
			if (question.isPossibileDuplicate()){
				filterdQuestions.add(question);
			}
		}
		//TODO: Implement the comparator that filters
		Collections.sort(filterdQuestions,new PossibileDuplicateComparator());
		if (filterdQuestions.size()>maxQuestions){
			filterdQuestions.subList(maxQuestions,filterdQuestions.size()).clear();
		}
	}
	
	public void pushToRestApi() throws IOException {
		RESTApiHandler restApi = new RESTApiHandler();
		this.batchUrl = restApi.getRemoteURL(this);
	}
	
	//Get result as JSON
	public JSONObject getJSONObject() {
		JSONObject json = new JSONObject();
		json.put("timestamp", getTimestamp());
		json.put("room_id", getRoomId());
		json.put("batch_nr", getBatchNumber());
		if (getSearchTag() != null) {
			json.put("search_tag", getSearchTag());
		}
		json.put("is_filtered_duplicates", isFilteredOnDuplicates());
		json.put("api_quota", getApiResult().getQuotaRemaining());

		JSONArray questions = new JSONArray();
		json.put("questions", questions);
		int n = 1;
		for (Question q : getFilterdQuestions()) {
			JSONObject qj = q.getJSONObject(n);
			questions.put(qj);
			n++;
		}
		return json;
	}

	/**
	 * Get the result as html
	 * @return
	 */
	public String getHTML() {
		
		String title ="Batch " + batchNumber + ": "  + searchTag + " generated at " + new SimpleDateFormat("yyy-MM-dd HH:mm").format(new Date());
		
		StringBuilder html = new StringBuilder();
		html.append("<html><head><title>" + title + "</title></head><body>\n");
		html.append("<h1>" + title + "</h1>\n");
		html.append("<h2>Questions to review</h2>\n");
		html.append("<table width=\"100%\" border=\"1\" style=\"border-collapse: collapse;\">\n");
		html.append(CherryPickResult.getTableHeader());
		int nr = 1;
		for (Question question : filterdQuestions) {
			html.append(question.getHTML(nr));
			nr ++;
		}
		html.append("</table>");
		html.append("</body></html>");
		return html.toString();
	}
	
	public static String getTableHeader() {
		return "<tr><th align=\"center\">NR</th><th align=\"left\">Question</th><th align=\"center\">Time ago</th><th align=\"center\">CV</th><th align=\"center\">Score</th><th align=\"center\">Answers</th><th>Views</th><th align=\"center\">Comm. cnt</td></tr>\n";
	}

	public List<Question> getFilterdQuestions() {
		return filterdQuestions;
	}


	public long getBatchNumber() {
		return batchNumber;
	}

	public ApiResult getApiResult() {
		return apiResult;
	}

	

	public String getSearchTag() {
		return searchTag;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isFilteredOnDuplicates() {
		return filteredOnDuplicates;
	}

	public String getBatchUrl() {
		return batchUrl;
	}

	public long getRoomId() {
		return roomId;
	}

	public void setRoomId(long roomId) {
		this.roomId = roomId;
	}

	

	
}
