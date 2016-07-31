package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.Message;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class CommentOffensiveFpCommand extends CommentResponseAbstract {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentOffensiveFpCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(\\sfp(\\s|$|,|;|-))";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "False rude comment";
	}

	@Override
	public String getCommandDescription() {
		return "Report that comment is not offensive";
	}

	@Override
	public String getCommandUsage() {
		return "fp";
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
		if (!c.contains("#comment")) {
			room.replyTo(event.getMessage().getId(), "Your reply was not direct to an offensive comment");
			return;
		}

		falsePositive(room, event, c);
	}

	public void falsePositive(ChatRoom room, PingMessageEvent event, String content) {
		
		long commentId;
		try {
			commentId = getCommentId(content);
		} catch (RuntimeException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessage().getId(), "Sorry could not retrive comment id");
			return;
		}
		
		try {
			saveToDatabase(commentId, false);
		} catch (SQLException e) {
			logger.error("falsePositive(ChatRoom, PingMessageEvent, String)", e);
		}
		

		String edit = getEdit(event, content, false);

		room.edit(event.getParentMessageId(), content + edit).handleAsync((mId, thr) -> {
//			if (thr != null)
//				return room.replyTo(event.getMessageId(), "Thank you for confirming the duplicate").join();
			return mId;
		});

	}

	

}
