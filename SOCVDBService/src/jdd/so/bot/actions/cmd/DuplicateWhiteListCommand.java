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
			room.replyTo(event.getMessageId(), "Could not find message your are replying to");
			return;
		}
		String c = pdm.getPlainContent();
		if (!c.contains("[tag:possible-duplicate]")) {
			room.replyTo(event.getMessageId(), "Your reply was not direct to a possible duplicate notification");
			return;
		}

		long questionId;
		try {
			String match = "stackoverflow.com/questions/";
			int startPos = c.lastIndexOf(match);
			String qId = c.substring(startPos + match.length(), c.indexOf(')', startPos + match.length()));
			questionId = Long.parseLong(qId);
			if (logger.isDebugEnabled()) {
				logger.debug("runCommand(ChatRoom, PingMessageEvent) - " + questionId);
			}
		} catch (RuntimeException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessageId(), "Sorry could not retrive question id");
			return;
		}

		whiteList(room, questionId, event, c);
	}

	public void whiteList(ChatRoom room, long questionId, PingMessageEvent event, String content) {
		boolean wled = false;
		if (content.toLowerCase().contains(" wl")){
			WhiteList wl = new WhiteList(questionId, event.getUserId(), new Date().getTime() / 1000L);
			try {
				new WhitelistDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), wl);
				CloseVoteFinder.getInstance().getWhiteList().add(questionId);
				room.replyTo(event.getMessageId(), "Question have been whitelisted");
				wled = true;
			} catch (SQLException e) {
				logger.error("whiteList(ChatRoom, long, long, long, long)", e);
				room.send("Whitelist to database faild, check stack trace @Petter");
				return;
			}
		}

		String edit = content;
		if (edit.contains("@")) {
			edit = edit.substring(0, edit.indexOf('@')).trim();
		}
		if (edit.contains("--- f") || edit.contains("--- k")) {
			edit += ", f" + event.getUserName();
		} else {
			int lastTag = edit.lastIndexOf("[tag:");
			int closeTag = edit.indexOf(']', lastTag);
			if (closeTag > 0) {
				edit = edit.substring(0, closeTag + 1) + " ---" + edit.substring(closeTag + 1, edit.length()).trim() + "--- f by " + event.getUserName();
			}
		}
		boolean replyIfNoEdit =!wled;
		room.edit(event.getParentMessageId(), edit).handleAsync((mId, thr) -> {
			if (thr != null&&replyIfNoEdit){
				return room.replyTo(event.getMessageId(), "Marked as non duplicate").join();
			}
			return mId;
		});
	}

}
