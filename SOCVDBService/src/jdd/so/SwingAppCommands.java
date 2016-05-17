package jdd.so;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;

import jdd.so.api.ApiHandler;
import jdd.so.api.CherryPickResult;
import jdd.so.bot.actions.CommandException;
import jdd.so.bot.actions.filter.AnswersType;
import jdd.so.model.ApiResult;

@Deprecated
public class SwingAppCommands {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(SwingAppCommands.class);

	private NotifyMe notifyMe;
	private ApiHandler api;

	private int loadPages;

	private boolean pushToRestApi;

	public SwingAppCommands() {
		this(20, true);
	}

	public SwingAppCommands(int maxApiPages, boolean pushToRestApi) {
		super();
		this.loadPages = maxApiPages;
		this.pushToRestApi = pushToRestApi;
		api = new ApiHandler();
	}

	
	/**
	 * Get a batch of possibile duplicates in tag of choice
	 * 
	 * @param chatRoomId,
	 *            chartroom id
	 * @param userId,
	 *            users requesting the batch
	 * @param tag,
	 *            the tag of choice formated "java" not [tag:java]
	 * @param maxQuestions,
	 *            maximum questions to return
	 * @return a CherryPickResult, the result can be retrived by getHTML();
	 * @throws CommandException,
	 *             if command can not be executed
	 */
	public CherryPickResult getPossibileDuplicatesBatch(long chatRoomId, long userId, String tag, int maxQuestions) throws CommandException {

		if (maxQuestions <= 0) {
			throw new CommandException("number of questions needs to more then 0, no questions no fun");
		}
		try {
			CherryPickResult cpr = getCherryPick(chatRoomId, userId, tag, null, null, null, null);
			cpr.filterDuplicates(maxQuestions);
			if (pushToRestApi) {
				cpr.pushToRestApi();
			}
			return cpr;
		} catch (JSONException | IOException e) {
			logger.error("getPossibileDuplicatesBatch(long, long, String, int)", e);
			throw new CommandException("Falid to get messages: " + e.getMessage(), e);
		}
	}

	/**
	 * Get a cherry pick batch on tag of choice
	 * 
	 * @param chatRoomId,
	 *            chatroom id
	 * @param userId,
	 *            userId of users who is requesting it
	 * @param tag,
	 *            the tag of choice es. "java"
	 * @param maxQuestions,
	 *            maximum number of questions to return
	 * @param cvFilter,
	 *            close vote filter es. ">4","=3" ecc
	 * @param scoreFilter,
	 *            question score filter es. "=0"
	 * @param answerFilter,
	 *            Need to make an Enum on this. nr,na ecc.
	 * @param minCreationDate,
	 *            The question needs to be newer then this (unix timestamp)
	 * @returna CherryPickResult, the result can be retrived by getHTML();
	 * @throws CommandException,
	 *             if command can not be executed
	 */
	public CherryPickResult getCherryPickBatch(long chatRoomId, long userId, String tag, int maxQuestions, String cvFilter, String scoreFilter,
			AnswersType answerType, Long minCreationDate) throws CommandException {

		if (maxQuestions <= 0) {
			throw new CommandException("number of questions needs to more then 0, no questions no fun");
		}
		try {
			CherryPickResult cpr = getCherryPick(chatRoomId, userId, tag, cvFilter, scoreFilter, answerType, minCreationDate);
			cpr.filterCherry(maxQuestions, cvFilter, scoreFilter, answerType);
			if (pushToRestApi) {
				try {
					cpr.pushToRestApi();
				} catch (Exception e) {
					logger.error("getPossibileDuplicatesBatch() - Sam's server is done", e);
				}
			}
			return cpr;
		} catch (JSONException | IOException e) {
			logger.error("getPossibileDuplicatesBatch(long, long, String, int)", e);
			throw new CommandException("Opps an error occured: " + e.getMessage(), e);
		}

	}

	private CherryPickResult getCherryPick(long chatRoomId, long userId, String tag, String cvFilter, String scoreFilter, AnswersType answerType,
			Long minCreationDate) throws JSONException, IOException {
		ApiResult apiResult = null;

		// 1. Check if tag is avialable in DB.
		boolean tagMonitored = CloseVoteFinder.getInstance().isTagMonitored(tag);

		if (tagMonitored) {
			// 1. Load best from db.
			// 2. query api on 100 the question id
			// 3. and set api result as this
		} else {
			// Get the latest pages from tag
			apiResult = api.getQuestions(tag, loadPages, true, notifyMe);
		}

		return new CherryPickResult(apiResult, chatRoomId, tag);
	}

	public void setBatchResult(long chatRoomId, long userId, long batchId, List<Long> removeQuestionIds) throws CommandException {

	}

	public NotifyMe getNotifyMe() {
		return notifyMe;
	}

	public void setNotifyMe(NotifyMe notifyMe) {
		this.notifyMe = notifyMe;
	}

	public boolean isPushToRestApi() {
		return pushToRestApi;
	}

	public void setPushToRestApi(boolean pushToRestApi) {
		this.pushToRestApi = pushToRestApi;
	}

}
