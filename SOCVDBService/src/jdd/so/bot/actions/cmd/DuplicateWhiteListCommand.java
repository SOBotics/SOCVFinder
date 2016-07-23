package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.Message;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.WhitelistDAO;
import jdd.so.dao.model.WhiteList;

public class DuplicateWhiteListCommand extends DuplicateResponseAbstract {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(DuplicateWhiteListCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(@que[a-zA-Z]* f(\\s|$))";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_HAMMER;
	}

	@Override
	public String getCommandName() {
		return "Response to duplicate notification";
	}

	@Override
	public String getCommandDescription() {
		return "Indicate as non duplicate if wl add also to white list";
	}

	@Override
	public String getCommandUsage() {
		return "f <wl>";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		long parentMessageId = event.getParentMessageId();
		Message pdm = room.getRoom().getMessage(parentMessageId);
		if (pdm == null) {
			room.replyTo(event.getMessage().getId(), "Could not find message your are replying to");
			return;
		}
		String c = pdm.getPlainContent();
		if (!c.contains("[tag:possible-duplicate]")) {
			room.replyTo(event.getMessage().getId(), "Your reply was not direct to a possible duplicate notification");
			return;
		}

		markFalsePositive(room, event, c);
	}

	public void markFalsePositive(ChatRoom room, PingMessageEvent event, String content) {
		long questionId;
		try {
			questionId = getQuestionId(content);
		} catch (RuntimeException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessage().getId(), "Sorry could not retrive question id, question is not white listed");
			return;
		}

		if (content.toLowerCase().contains(" wl")) {
			WhiteList wl = new WhiteList(questionId, event.getUserId(), System.currentTimeMillis() / 1000L);
			try {
				new WhitelistDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), wl);
				CloseVoteFinder.getInstance().getWhiteList().add(questionId);
				room.replyTo(event.getMessage().getId(), "Question have been whitelisted");
			} catch (SQLException e) {
				logger.error("whiteList(ChatRoom, long, long, long, long)", e);
				room.send("Whitelist to database faild, check stack trace @Petter");
				return;

			}
		}
		
		try {
			saveToDatabase(questionId, event.getUserId(), room.getRoomId(), false);
		} catch (SQLException e) {
			logger.error("markFalsePositive(ChatRoom, PingMessageEvent, String)", e);
		}

		String edit = getEdit(event, content, false);
		
		//boolean replyIfNoEdit = !wled;
		room.edit(event.getParentMessageId(), edit).handleAsync((mId, thr) -> {
//			if (thr != null && replyIfNoEdit) {
//				return room.replyTo(event.getMessageId(), "Marked as non duplicate").join();
//			}
			return mId;
		});
	}
}
