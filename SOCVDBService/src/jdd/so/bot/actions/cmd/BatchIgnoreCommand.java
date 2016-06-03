package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.event.MessageReplyEvent;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.model.Batch;

public class BatchIgnoreCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(BatchIgnoreCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(ignore)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "Ignore, delete batch";
	}

	@Override
	public String getCommandDescription() {
		return "Tell Queen that you will not review batch, it will be deleted";
	}

	@Override
	public String getCommandUsage() {
		return "ignore";
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

		if (b.getBatchDateEnd() > 0) {
			room.replyTo(event.getMessageId(), "You have already completed this batch, it cannot be ignored.");
			return;
		}

		try {
			new BatchDAO().delete(CloseVoteFinder.getInstance().getConnection(),b.getRoomId(),b.getMessageId());
			room.replyTo(event.getMessageId(), "The batch has been deleted, questions will be displayed in next batch request.");
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessageId(), "Error while deleting the batch @Petter");
		}
	}

}
