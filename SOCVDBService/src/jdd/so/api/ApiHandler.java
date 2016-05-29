package jdd.so.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jdd.so.CloseVoteFinder;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Comment;
import jdd.so.api.model.Question;
import jdd.so.swing.NotifyMe;

/**
 * Handle the SO API and return result @see CherryPickResult based on
 * parameters set
 * @author Petter Friberg
 *
 */
public class ApiHandler {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(ApiHandler.class);

	/**
	 * Get the questions by date
	 * 
	 * @see getQuestions(long fromDate, long toDate, String tag, NotifyMe
	 *      notifyMe), passing notifyMe = null
	 */
	public ApiResult getQuestions(long fromDate, long toDate, int totPages, String tag, boolean loadDupTarget) throws JSONException, IOException {
		return getQuestions(null, fromDate, toDate, tag, totPages, loadDupTarget, null);
	}

	/**
	 * Get the question by questionId's
	 *
	 */
	public ApiResult getQuestions(String questions, String tag, boolean loadDupTarget, NotifyMe notifyMe) throws JSONException, IOException {
		return getQuestions(questions, 0L, 0L, tag, 1, loadDupTarget, notifyMe);
	}

	/**
	 * Get latest questions
	 *
	 */
	public ApiResult getQuestions(String tag, int totPages, boolean loadDupTarget, NotifyMe notifyMe) throws JSONException, IOException {
		return getQuestions(null, 0L, 0L, tag, totPages, loadDupTarget, notifyMe);
	}

	/**
	 * Get desired question between dates on tag
	 * 
	 * @param questions,
	 *            question id in format questionId1;questionId2, set
	 *            <code>null</code> for no filter
	 * @param fromDate,
	 *            start date in unix timestamp, set 0 for no filter
	 * @param toDate,
	 *            end date in unix timestamp, set 0 for no filter
	 * @param tag,
	 *            the tag name ex. "java", set null for no filter
	 * @param notifyMe,
	 *            Interface if you like to be notfied (can be null)
	 * @return
	 * @throws JSONException
	 * @throws IOException
	 */
	public ApiResult getQuestions(String questions, long fromDate, long toDate, String tag, int totPages, boolean loadDupTarget, NotifyMe notifyMe)
			throws JSONException, IOException {
		boolean includeAll = questions!=null&&questions.trim().length()>0; //If you send me questionIds I return all
		ApiResult qr = new ApiResult(includeAll);
		int page = 1;
		while (page <= totPages && (page <= CloseVoteFinder.MAX_PAGES) && qr.isHasMore()) {
			addQuestions(qr, questions, page, fromDate, toDate, tag, notifyMe);
			if (notifyMe != null) {
				notifyMe.message("Page: " + page + " done");
			}
			qr.setNrOfPages(page);
			page++;
		}
		if (loadDupTarget) {
			addDuplicationTarget(qr, notifyMe);
		}
		if (notifyMe != null) {
			notifyMe.message("All done creating file");
		}
		return qr;
	}

	private void addDuplicationTarget(ApiResult qr, NotifyMe notifyMe) throws JSONException, IOException {
		List<Question> dupes = qr.getPossibileDuplicates();
		StringBuilder questionQuery = new StringBuilder();
		Map<Long, Question> dupMap = new HashMap<Long, Question>();
		int cq = 0;
		for (Question q : dupes) {
			long dupeId = q.getDuplicateQuestionID();
			if (dupeId > 0) {
				dupMap.put(dupeId, q);
				questionQuery.append(dupeId + ";");
				cq++;
				if (cq >= 100) {
					// max 100 questions
					break;
				}
			}
		}
		// remove last separator
		String questions = questionQuery.toString();
		if (logger.isDebugEnabled()) {
			logger.debug("addDuplicationTarget(ApiResult, NotifyMe) - question query = " + questionQuery);
		}
		if (questions.length() == 0) {
			return;
		}
		questions = questions.substring(0, questions.length() - 1);

		ApiResult arDupeTarget = getQuestions(questions, null, false, notifyMe);
		for (Question q : arDupeTarget.getQuestions()) {
			long id = q.getQuestionId();
			Question org = dupMap.get(id);
			if (org == null) {
				continue;
			}
			Comment dupComment = org.getDuplicatedComment();
			if (dupComment != null) {
				dupComment.setDuplicateTargetTitle(q.getTitle());
				dupComment.setDuplicateTargetScore(q.getScore());
			}
		}

	}


	/**
	 * Add all question in single page
	 * 
	 * @param ar,
	 *            The ApiResult to add to
	 * @param page,
	 *            the page number
	 * @param fromDate
	 * @param toDate
	 * @param tag
	 * @param notifyMe
	 * @throws JSONException
	 * @throws IOException
	 */
	private void addQuestions(ApiResult ar, String questions, int page, long fromDate, long toDate, String tag, NotifyMe notifyMe)
			throws JSONException, IOException {
		String url = CloseVoteFinder.getInstance().getApiUrl(questions, page, fromDate, toDate, tag);
		addQuestions(ar, url, tag, notifyMe);
	}

	private void addQuestions(ApiResult ar, String url, String mainTag, NotifyMe notifyMe) throws IOException, JSONException {
		JSONObject response = CloseVoteFinder.getInstance().getJSONObject(url, notifyMe);
		ar.setQuotaRemaining(response.getInt("quota_remaining"));
		CloseVoteFinder.getInstance().setApiQuota(ar.getQuotaRemaining());
		if (!response.has("items")) {
			ar.setHasMore(false);
			return;
		}
		JSONArray items = response.getJSONArray("items");
		int length = items.length();
		for (int i = 0; i < length; i++) {
			JSONObject item = items.getJSONObject(i); // question
			Question q = Question.getQuestion(item, mainTag);
			ar.addQuestion(q);
		}
		ar.setHasMore(response.getBoolean("has_more"));
		if (response.has("backoff")){
			long backOff = response.getLong("backoff")*1000L; //its in seconds
			CloseVoteFinder.getInstance().setBackOffUntil(System.currentTimeMillis()+backOff);
			ar.setBackoff(backOff);
			ar.setHasMore(false); //we retrun with what we have
		}
	}

}
