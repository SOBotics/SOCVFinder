package jdd.so.bot.actions.cmd;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.json.JSONException;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.api.ApiHandler;
import jdd.so.api.CherryPickResult;
import jdd.so.bot.actions.BotCommand;
import jdd.so.bot.actions.CommandException;
import jdd.so.bot.actions.filter.Arithmetics;
import jdd.so.bot.actions.filter.QuestionsFilter;
import jdd.so.model.ApiResult;
import jdd.so.model.Question;

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
	public void runCommand(Room room, PingMessageEvent event) {
		String message = event.getContent();
		String tags = getTags(message);
		boolean isDupCommand = message.contains("dup");
		boolean serveSome = false;
		String retMsg = "Elaborating";
		CherryPickResult cpr=null;
		room.send("All right, dear, working on it...");
		try {
			if (isDupCommand) {
				System.out.println("Dupes: " + tags);
				cpr = getPossibileDuplicatesBatch(room.getRoomId(), 0L, tags, new QuestionsFilter(message));
				retMsg = "[tag:dup-hunt] Use you mjolnir wisely ";
			} else {
				System.out.println("Cherry: " + tags);
				cpr = getCherryPickBatch(room.getRoomId(), event.getUserId(), tags, new QuestionsFilter(message));
				retMsg = "[tag:cherry-pick] Serving you " + tags + " ";
			}
			
			if (cpr.getFilterdQuestions().size()==0){
				room.replyTo(event.getMessageId(), "Sorry your query did not produce any result");
				return;
			}
			
			String batchUrl = cpr.getBatchUrl();
			if (batchUrl == null) {
				retMsg = "Sam's server is out partying, but do not dispear, I will provide you some";
				serveSome = true;
			} else {
				retMsg += "[batch " + cpr.getBatchNumber() + "](" + cpr.getBatchUrl() + ")";
			}
		} catch (CommandException e) {
			logger.error("runCommand(Room, String)", e);
			retMsg = "Opps an error has occured check the logs";
		}

		room.replyTo(event.getMessageId(), retMsg);
		if (serveSome && cpr!=null){
			String questions = "Here ya go: ";
			int i = 0;
			for (Question q : cpr.getFilterdQuestions()) {
				questions +="[" + q.getTitle() + "](http://stackoverflow.com/questions/" + q.getQuestionId() + ") " + q.getCloseVoteCount() + "CV ";				
				i++;
				if (i>=3){
					break;
				}
			}
			room.send(questions);
		}

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
	public CherryPickResult getPossibileDuplicatesBatch(long chatRoomId, long userId, String tag, QuestionsFilter questionFilter) throws CommandException {
		
		questionFilter.setFilterDupes(true);
		
		if (questionFilter.getNumberOfQuestions() <= 0) {
			throw new CommandException("number of questions needs to more then 0, no questions no fun");
		}

		try {
			CherryPickResult cpr = getCherryPick(chatRoomId, userId, tag, questionFilter);
			cpr.filterDuplicates(questionFilter);
			try {
				cpr.pushToRestApi();
			} catch (Exception e) {
				logger.error("getPossibileDuplicatesBatch() - Sam's server is done", e);
			}
			return cpr;
		} catch (JSONException | IOException e) {
			logger.error("getPossibileDuplicatesBatch(long, long, String, int)", e);
			throw new CommandException("Falid to get messages: " + e.getMessage(), e);
		}
	}

	public CherryPickResult getCherryPickBatch(long chatRoomId, long userId, String tag,QuestionsFilter questionFilter) throws CommandException {

		if (questionFilter.getNumberOfQuestions() <= 0) {
			throw new CommandException("number of questions needs to more then 0, no questions no fun");
		}
		try {
			CherryPickResult cpr = getCherryPick(chatRoomId, userId, tag, questionFilter);
			cpr.filterCherry(questionFilter);
			try {
				cpr.pushToRestApi();
			} catch (Exception e) {
				logger.error("getPossibileDuplicatesBatch() - Sam's server is done", e);
			}
			return cpr;
		} catch (JSONException | IOException e) {
			logger.error("getPossibileDuplicatesBatch(long, long, String, int)", e);
			throw new CommandException("Opps an error occured: " + e.getMessage(), e);
		}

	}

	private CherryPickResult getCherryPick(long chatRoomId, long userId, String tag, QuestionsFilter questionFilter) throws JSONException, IOException {
		ApiResult apiResult = null;

		// 1. Check if tag is avialable in DB.
		boolean tagMonitored = CloseVoteFinder.getInstance().isTagMonitored(tag);

		if (tagMonitored) {
			// 1. Load best from db.
			// 2. query api on 100 the question id
			// 3. and set api result as this
		} else {
			// Get the latest pages from tag
			if (questionFilter.getDays()==null||!questionFilter.getDays().isFilterActive()){
				apiResult = api.getQuestions(tag, CloseVoteFinder.getApiCallNrPages(), true, null);
			}else{
				long fromDate = 0L;
				long toDate = 0L;
				int days = questionFilter.getDays().getNumber();
				if (questionFilter.getDays().getArithmetics()==null){
					questionFilter.getDays().setArithmetics(Arithmetics.NONE);
				}
					
				switch(questionFilter.getDays().getArithmetics()){
					case EQUALS:
						//get 10 days ago hour 0  from, hour 24 to
						fromDate = getUnixDate(days);
						toDate = getUnixDate(days-1);
						break;
					case LESS:
						toDate = getUnixDate(days+1);
						break;
					case LESS_EQUALS: 
						toDate = getUnixDate(days);
						break;
					case MORE:
						fromDate = getUnixDate(days-1);
						break;
					case MORE_EQUALS:
						fromDate = getUnixDate(days-1);
						break;
					case NONE:
					default: //Lets do EQUALS for now
						fromDate = getUnixDate(days);
						toDate = getUnixDate(days-1);
				}
				apiResult = api.getQuestions(null,fromDate,toDate,tag, CloseVoteFinder.getApiCallNrPages(), true, null);
			}
		}

		return new CherryPickResult(apiResult, chatRoomId, tag);
	}

	private long getUnixDate(int daysAgo) {
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE,-daysAgo);
		return cal.getTimeInMillis()/1000;
	}

}
