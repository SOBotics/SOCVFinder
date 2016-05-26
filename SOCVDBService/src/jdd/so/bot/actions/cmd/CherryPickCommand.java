package jdd.so.bot.actions.cmd;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

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
import jdd.so.dao.BatchDAO;
import jdd.so.dao.QuestionIndexDao;
import jdd.so.dao.model.Batch;
import jdk.nashorn.internal.runtime.Context.ThrowErrorManager;

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
		return "<max-questions> [tag]* <dupes> <cv-count>cv <q-score>s <answerType> <age>d -all";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String message = event.getContent();
		String tags = getTags(message);
		QuestionsFilter filter;
		try {
			filter = new QuestionsFilter(message);
		} catch (Throwable qf) {
			qf.printStackTrace();
			room.replyTo(event.getMessageId(), "Sorry could not elaborate your question filter, please check your command");
			return;
		}

		CompletableFuture<Long> sentId = room.send("All right, dear, working on it...");
		// load previous questions displayed
		if (!message.contains("-all")) {
			try {
				filter.setExcludeQuestions(new BatchDAO().getLastQuestionsReviewed(CloseVoteFinder.getInstance().getConnection(), event.getUserId()));
			} catch (SQLException e1) {
				logger.error("runCommand(ChatRoom, PingMessageEvent) - Error loading previous reviews", e1);
			}
		}

		try {
			// Get questions
			CherryPickResult cpr = getCherryPick(room.getRoomId(), event.getUserId(), room.getNextBatchNumber(), tags, filter);
			// Filter as requested
			cpr.filter(filter);
			if (cpr.getFilterdQuestions().isEmpty()) {
				room.replyTo(event.getMessageId(), "Sorry your query did not produce any result");
				return;
			}

			// Push to rest api
			String batchUrl = null;
			try {
				batchUrl = cpr.pushToRestApi();
			} catch (Exception e) {
				logger.error("runCommand() - Sam's server is done", e);
			}

			if (batchUrl != null) {
				String retMsg;
				if (filter.isFilterDupes()) {
					retMsg = "[tag:dup-hunt]  ";
				} else {
					retMsg = "[tag:cherry-pick] in " + tags + " ";
				}
				SimpleDateFormat df = new SimpleDateFormat("MMM dd HH:mm", Locale.US);
				retMsg += "scanned " + cpr.getApiResult().getNrOfQuestionScanned() + " questions between "
						+ df.format(new Date(cpr.getApiResult().getFirstDate() * 1000)) + " and " + df.format(new Date(cpr.getApiResult().getLastDate() * 1000))
						+ " filterd and order " + cpr.getFilterdQuestions().size() + " in " + " [batch " + cpr.getBatchNumber() + "](" + cpr.getBatchUrl()
						+ ")";

				final String editMessage = "@" + event.getUserName().replaceAll(" ", "") + " " + retMsg;
				final String replyMessage = retMsg;

				sentId.thenAccept(messageId -> {
					room.edit(messageId, editMessage).handleAsync((mId, thr) -> {
						if (thr != null)
							return room.replyTo(event.getMessageId(), replyMessage).join();
						return mId;
					}).thenAccept(mId -> insertBatch(cpr, event.getUserId(), mId));
				});

				// CompletableFuture<Long> messageId;
				// if (sentId.isDone()){
				// try {
				// String userName = event.getUserName().replaceAll(" ", "");
				// room.edit(sentId.get(), "@" + userName + " " +retMsg);
				// messageId = sentId;
				// } catch (CompletionException e) {
				// messageId = room.replyTo(event.getMessageId(), retMsg);
				// }
				// }else{
				// sentId.cancel(true);
				// messageId = room.replyTo(event.getMessageId(), retMsg);
				// }
				// messageId.whenComplete(new InsertBatch(cpr,
				// event.getUserId()));
			} else {
				sendSomeQuestionsIfServerIsDown(room, event, cpr);
			}
		} catch (Exception e) {
			logger.error("runCommand(Room, String)", e);
			room.replyTo(event.getMessageId(), "Opps an error has occured check the logs");
		}

	}

	private Object insertBatch(CherryPickResult cpr, long userId, Long messageId) {
		if (messageId != null) {
			System.out.println(messageId);
			Batch b = cpr.getBatch(messageId, userId);
			try {
				new BatchDAO().insert(CloseVoteFinder.getInstance().getConnection(), b);
			} catch (SQLException e) {
				logger.error("accept(Long, Throwable)", e);
			}
		}
		return null;
	}

	private void sendSomeQuestionsIfServerIsDown(ChatRoom room, PingMessageEvent event, CherryPickResult cpr) {
		room.replyTo(event.getMessageId(), "Sam's server is out partying, but do not dispear, I will provide you some:");
		String questions = "Here ya go: ";
		for (int n = 0; n < cpr.getFilterdQuestions().size() && n < 3; n++) {
			Question q = cpr.getFilterdQuestions().get(n);
			questions += "[" + q.getTitle() + "](http://stackoverflow.com/questions/" + q.getQuestionId() + ") " + q.getCloseVoteCount() + "CV ";
		}
		room.send(questions);
	}

	private CherryPickResult getCherryPick(long chatRoomId, long userId, int batchNumber, String tag, QuestionsFilter questionFilter)
			throws JSONException, IOException {
		// 1. Check if tag is avialable in DB.
		boolean tagMonitored = CloseVoteFinder.getInstance().isTagMonitored(tag);

		// set api call
		if (tagMonitored && !questionFilter.isFilterDupes()) {
			questionFilter.setNumberOfApiCalls(10);
		} else {
			questionFilter.setNumberOfApiCalls(20);
		}

		// Get the latest pages from tag
		long fromDate = 0L;
		long toDate = 0L;
		// check if to filter on date
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
		
		ApiResult apiResult = api.getQuestions(null, fromDate, toDate, tag, questionFilter.getNumberOfApiCalls(), true, null);

		// If tag is monitored load it from also from db
		if (tagMonitored && !questionFilter.isFilterDupes()) {
			try {
				String oldies = new QuestionIndexDao().getQueryString(CloseVoteFinder.getInstance().getConnection(),tag);
				if (oldies != null && oldies.length() > 0) {
					ApiResult oldQuestions = api.getQuestions(oldies, tag, false, null);
					for (Question question : oldQuestions.getQuestions()) {
						apiResult.addQuestion(question);
					}
				}
			} catch (SQLException e) {
				logger.error("getCherryPick(long, long, int, String, QuestionsFilter)", e);
			}
		}

		return new CherryPickResult(apiResult, chatRoomId, tag, batchNumber);
	}

	private long getUnixDate(int daysAgo) {
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -daysAgo);
		return cal.getTimeInMillis() / 1000;
	}

	protected class InsertBatch implements BiConsumer<Long, Throwable> {
		/**
		 * Logger for this class
		 */
		private final Logger logger = Logger.getLogger(InsertBatch.class);

		private CherryPickResult cpr;
		private Long userId;

		protected InsertBatch(CherryPickResult cpr, Long userId) {
			this.cpr = cpr;
			this.userId = userId;

		}

		@Override
		public void accept(Long messageId, Throwable u) {
			if (messageId != null) {
				Batch b = cpr.getBatch(messageId, userId);
				try {
					new BatchDAO().insert(CloseVoteFinder.getInstance().getConnection(), b);
				} catch (SQLException e) {
					logger.error("accept(Long, Throwable)", e);
				}
			}
		}

	}

}
