package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.Message;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class DuplicateConfirmCommand extends DuplicateResponseAbstract {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(DuplicateConfirmCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(@que[a-zA-Z]* k(\\s|$))";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_HAMMER;
	}

	@Override
	public String getCommandName() {
		return "Confirm duplicate notification";
	}

	@Override
	public String getCommandDescription() {
		return "Confirm that possible duplicate is closed";
	}

	@Override
	public String getCommandUsage() {
		return "k";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		long parentMessage = event.getParentMessageId();
		Message pdm = room.getRoom().getMessage(parentMessage);
		if (pdm == null) {
			room.replyTo(event.getMessage().getId(), "Could not find message your are replying to");
			return;
		}
		String c = pdm.getPlainContent();
		if (!c.contains("[tag:possible-duplicate]")) {
			room.replyTo(event.getMessage().getId(), "Your reply was not direct to a possible duplicate notification");
			return;
		}

		confirm(room, event, c);
	}

	public void confirm(ChatRoom room, PingMessageEvent event, String content) {
		
		long questionId;
		try {
			questionId = getQuestionId(content);
		} catch (RuntimeException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessage().getId(), "Sorry could not retrive question id");
			return;
		}
		
		try {
			saveToDatabase(questionId, event.getUserId(), room.getRoomId(), true);
		} catch (SQLException e) {
			logger.error("confirm(ChatRoom, PingMessageEvent, String)", e);
		}
		

		String edit = getEdit(event, content, true);

		room.edit(event.getParentMessageId(), edit).handleAsync((mId, thr) -> {
//			if (thr != null)
//				return room.replyTo(event.getMessageId(), "Thank you for confirming the duplicate").join();
			return mId;
		});

	}

	

}
