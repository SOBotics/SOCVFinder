package jdd.so.bot.actions.cmd;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.log4j.Logger;
import org.json.JSONException;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.api.ApiHandler;
import jdd.so.api.CherryPickResult;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Question;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.bot.actions.filter.Arithmetics;
import jdd.so.bot.actions.filter.QuestionsFilter;

public class CherryPickCommand extends BotCommand {

	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CherryPickCommand.class);
	private ApiHandler api;

	public CherryPickCommand() {
		super();
		api = new ApiHandler();
	}

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(\\[.*?\\])";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "Cherry pick";
	}

	@Override
	public String getCommandDescription() {
		return "Cherry pick what you like to review";
	}

	@Override
	public String getCommandUsage() {
		return "<max-questions> [[tag]]* <dupes> <cv-count>cv <q-score>s <answerType> <age>d";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String message = event.getContent();
		CompletableFuture<Long> sentId = room.send("All right, dear, working on it...");
		String tags = getTags(message);
		QuestionsFilter filter = new QuestionsFilter(message);
		CherryPickResult cpr = null;
		try {
			// Get questions
			cpr = getCherryPick(room.getRoomId(), event.getUserId(), room.getNextBatchNumber(),tags, filter);
			
			//Filter as requested
			cpr.filter(filter);
			if (cpr.getFilterdQuestions().isEmpty()) {
				room.replyTo(event.getMessageId(), "Sorry your query did not produce any result");
				return;
			}
			
			//Push to rest api
			String batchUrl = null;
			try {
				batchUrl = cpr.pushToRestApi();
			} catch (Exception e) {
				logger.error("runCommand() - Sam's server is done", e);
			}
			
			if (batchUrl == null) {
				room.replyTo(event.getMessageId(), "Sam's server is out partying, but do not dispear, I will provide you some:");
				String questions = "Here ya go: ";
				for (int n = 0; n < cpr.getFilterdQuestions().size() && n < 3; n++) {
					Question q = cpr.getFilterdQuestions().get(n);
					questions += "[" + q.getTitle() + "](http://stackoverflow.com/questions/" + q.getQuestionId() + ") " + q.getCloseVoteCount() + "CV ";
				}
				room.send(questions);
			} else {
				String retMsg;
				if (filter.isFilterDupes()) {
					retMsg = "[tag:dup-hunt] Use your Mjölnir wisely ";
				} else {
					retMsg = "[tag:cherry-pick] Serving you " + tags + " ";
				}
				retMsg += "[batch " + cpr.getBatchNumber() + "](" + cpr.getBatchUrl() + ")";
				if (sentId.isDone()){
					try {
						String userName = event.getUserName();
						if (userName!=null){
							userName = userName.replaceAll(" ", "");
						}
						room.edit(sentId.get(), "@" + userName +  " " +retMsg);
					} catch (CompletionException e) {
						room.replyTo(event.getMessageId(), retMsg);
					}
				}else{
					sentId.cancel(true);
					room.replyTo(event.getMessageId(), retMsg);
				}
			}
		} catch (Exception e) {
			logger.error("runCommand(Room, String)", e);
			room.replyTo(event.getMessageId(), "Opps an error has occured check the logs");
		}

	}


	private CherryPickResult getCherryPick(long chatRoomId, long userId, int batchNumber, String tag, QuestionsFilter questionFilter) throws JSONException, IOException {
		ApiResult apiResult = null;

		// 1. Check if tag is avialable in DB.
		boolean tagMonitored = CloseVoteFinder.getInstance().isTagMonitored(tag);

		if (tagMonitored) {
			// 1. Load best from db.
			// 2. query api on 100 the question id
			// 3. and set api result as this
		} else {
			// Get the latest pages from tag
			long fromDate = 0L;
			long toDate = 0L;
			//check if to filter on date
			if (questionFilter.getDays() != null && questionFilter.getDays().isFilterActive()) {
				int days = questionFilter.getDays().getNumber();
				if (questionFilter.getDays().getArithmetics() == null) {
					questionFilter.getDays().setArithmetics(Arithmetics.NONE);
				}
				switch (questionFilter.getDays().getArithmetics()) {
				case EQUALS:
					// get 10 days ago hour 0 from, hour 24 to
					fromDate = getUnixDate(days);
					toDate = getUnixDate(days - 1);
					break;
				case LESS:
					toDate = getUnixDate(days + 1);
					break;
				case LESS_EQUALS:
					toDate = getUnixDate(days);
					break;
				case MORE:
					fromDate = getUnixDate(days - 1);
					break;
				case MORE_EQUALS:
					fromDate = getUnixDate(days - 1);
					break;
				case NONE:
				default: // Lets do EQUALS for now
					fromDate = getUnixDate(days);
					toDate = getUnixDate(days - 1);
				}
			}
			apiResult = api.getQuestions(null, fromDate, toDate, tag, CloseVoteFinder.getInstance().getApiCallNrPages(), true, null);
		}

		return new CherryPickResult(apiResult, chatRoomId, tag,batchNumber);
	}

	private long getUnixDate(int daysAgo) {
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -daysAgo);
		return cal.getTimeInMillis() / 1000;
	}

}
