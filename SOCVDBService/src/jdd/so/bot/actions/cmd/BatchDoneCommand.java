package jdd.so.bot.actions.cmd;

import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;

import org.apache.log4j.Logger;
import org.json.JSONException;

import fr.tunaki.stackoverflow.chat.event.MessageReplyEvent;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.api.ApiHandler;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Question;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.model.Batch;

public class BatchDoneCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(BatchDoneCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(done)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "Done reviewing batch";
	}

	@Override
	public String getCommandDescription() {
		return "Reports that you have reviewed last batch served, question will not be displayed again";
	}

	@Override
	public String getCommandUsage() {
		return "done";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		long pmId = 0;
		BatchDAO bd = new BatchDAO();
		if (event instanceof MessageReplyEvent) {
			pmId = event.getParentMessageId();
		} else {
			try {
				pmId = bd.getLastMessageId(CloseVoteFinder.getInstance().getConnection(), event.getUserId());
			} catch (SQLException e) {
				logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			}
		}

		Batch b = null;
		if (pmId > 0) {
			try {
				b = bd.getBatch(CloseVoteFinder.getInstance().getConnection(), pmId);
				
			} catch (SQLException e) {
				logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			}
		}

		if (b == null) {
			room.replyTo(event.getMessageId(), "Sorry, could not understand the batch that you are referring to.");
			return;
		}
		
		if (b.getUserId()!=event.getUserId()){
			room.replyTo(event.getMessageId(), "Sorry, this is not your batch, please check your reply message");
			return;
		}
		
		if (b.getBatchDateEnd()>0){
			room.replyTo(event.getMessageId(), "You have already completed this batch");
			return;
		}

		String questions = b.getQuestions();
		if (questions != null && questions.trim().length() > 0) {
			questions = questions.substring(1, questions.length()-1);
			ApiHandler api = new ApiHandler();
			try {
				ApiResult cpr = api.getQuestions(questions, null, false, null);
				int cvCount = 0;
				int closedCount = 0;
				for (Question q : cpr.getQuestions()) {
					if (q.isClosed()){
						cvCount+=5;
						closedCount++;
					}else{
						cvCount+=q.getCloseVoteCount();
					}
				}
				
				b.setBatchDateEnd(System.currentTimeMillis()/1000);
				b.setCvCountAfter(cvCount);
				b.setClosedCount(closedCount);
				bd.update(CloseVoteFinder.getInstance().getConnection(), b);
				int cvc = cvCount-b.getCvCountBefore();
				if (cvc>cpr.getQuestions().size()){
					cvc = cpr.getQuestions().size();
				}
				double perc = (cvc) / (double)cpr.getQuestions().size();
				room.replyTo(event.getMessageId(), "Thank you for your effort, you reviewed " + cpr.getQuestions().size() + " questions,  I counted " +cvc + " (" + NumberFormat.getPercentInstance().format(perc) +  ") close votes and " + closedCount + " questions closed");
			} catch (JSONException | IOException | SQLException e) {
				logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
				room.replyTo(event.getMessageId(), "Error while storing your data, I guees you need to review another");		
			}
		}
		
	}

}
