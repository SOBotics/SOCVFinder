package jdd.so.bot.actions.cmd;

import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.Message;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.WhitelistDAO;
import jdd.so.dao.model.WhiteList;

public class DuplicateWhiteListCommand extends BotCommand {
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
		return "Whitelist and remove possibile duplicate notification";
	}

	@Override
	public String getCommandUsage() {
		return "f";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		long parentMessageId = event.getParentMessageId();
		Message pdm = room.getRoom().getMessage(parentMessageId);
		if (pdm == null) {
			room.replyTo(event.getMessageId(), "Could not find message your are replying to");
			return;
		}
		String c = pdm.getContent();
		if (!c.contains("possible-duplicate")) {
			room.replyTo(event.getMessageId(), "Your reply was not direct to a possibile duplicate notification");
			return;
		}

		long questionId;
		try {
			String match = "stackoverflow.com/questions/";
			int startPos = c.lastIndexOf(match);
			String qId = c.substring(startPos + match.length(), c.indexOf("\"", startPos + match.length()));
			questionId = Long.parseLong(qId);
			System.out.println(questionId);
		} catch (RuntimeException e) {
			e.printStackTrace();
			room.replyTo(event.getMessageId(), "Sorry could not retrive question id");
			return;
		}

		whiteList(room, questionId, event.getUserId(), event.getMessageId(), parentMessageId);
	}

	public void whiteList(ChatRoom room, long questionId, long userId, long messageId, long parentMessageId) {
		WhiteList wl = new WhiteList(questionId, userId, new Date().getTime() / 1000L);
		try {
			new WhitelistDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), wl);
			CloseVoteFinder.getInstance().getWhiteList().add(questionId);
		} catch (SQLException e) {
			logger.error("whiteList(ChatRoom, long, long, long, long)", e);
			room.send("Whitelist to database faild, check stack trace @Petter");
			return;
		}

		room.delete(parentMessageId).handleAsync((mId, thr) -> {
			if (thr != null)
				room.replyTo(messageId, "The possibile duplicate has been white listed").join();
			return mId;
		});

	}

}
