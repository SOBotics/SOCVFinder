package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.CommentsNotifyDAO;
import jdd.so.dao.model.CommentsNotify;

public class CommentsNotifyCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentsNotifyCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(\\s(notify|unnotify))";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_RO;
	}

	@Override
	public String getCommandName() {
		return "notify/unnotify";
	}

	@Override
	public String getCommandDescription() {
		return "register/unregister to comments feed";
	}

	@Override
	public String getCommandUsage() {
		return "notify [score]/unnotify";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		
		if (room.getRoomId()!=111347){
			room.replyTo(event.getMessage().getId(), "This command can only be executed in the [SOCVFinder](http://chat.stackoverflow.com/rooms/111347/socvfinder) room");
			return;
		}

		CommentsNotify cn = CloseVoteFinder.getInstance().getCommentsNotify(event.getUserId());
		if (event.getMessage().getPlainContent().contains("unnotify")) {
			unNotify(room, event, cn);
		} else {
			if (cn == null) {
				cn = new CommentsNotify(event.getUserId(), true, 4);
				CloseVoteFinder.getInstance().getCommentsNotify().add(cn);
			}
			notify(room, event, cn);
		}

	}

	private void notify(ChatRoom room, PingMessageEvent event, CommentsNotify cn) {
		int score = 4;
		String message = event.getMessage().getPlainContent();
		message = message.replace("[", "").replace("]", "");
		int sIndex = message.indexOf("notify") + 6;
		if (sIndex < message.length()) {
			String sc = message.substring(sIndex);
			try {
				score = Integer.parseInt(sc.replace(" ", ""));
				if (score < 4) {
					score = 4;
				}
				if (score > 10) {
					score = 10;
				}
			} catch (NumberFormatException e) {
				logger.error("Could not parse " + sc + " as int");
			}
		}

		cn.setNotify(true);
		cn.setScore(score);
		try {
			new CommentsNotifyDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), cn);
			room.replyTo(event.getMessage().getId(), "Notification added if score is >=" + score);
		} catch (SQLException e) {
			room.replyTo(event.getMessage().getId(), "Database error adding/updating notifications @Petter");
			logger.error("unNotify(ChatRoom, PingMessageEvent, CommentsNotify)", e);
		}
	}

	private void unNotify(ChatRoom room, PingMessageEvent event, CommentsNotify cn) {
		if (cn == null || !cn.isNotify()) {
			room.replyTo(event.getMessage().getId(), "You are not notified, no need to remove notification");
			return;
		}
		cn.setNotify(false);
		try {
			new CommentsNotifyDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), cn);
			room.replyTo(event.getMessage().getId(), "Notification from comments feed removed");
		} catch (SQLException e) {
			room.replyTo(event.getMessage().getId(), "Database error remvoing notifications @Petter");
			logger.error("unNotify(ChatRoom, PingMessageEvent, CommentsNotify)", e);
		}
	}

}
