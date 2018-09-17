package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.RoomTagDAO;
import jdd.so.dao.model.RoomTag;

public class RoomTagRemove extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(RoomTagRemove.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(remove " + getRegexTag() +")";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_RO;
	}

	@Override
	public String getCommandName() {
		return "remove tag from room";
	}

	@Override
	public String getCommandDescription() {
		return "Remove duplicate notifications in tag";
	}

	@Override
	public String getCommandUsage() {
		return "remove [tag]";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String tag = getTags(event.getMessage());
		long messageId = event.getMessage().getId();
		if (tag.length()==0 || tag.contains(";")){
			room.replyTo(messageId, "Please indicate 1 tag and only 1 tag to add");
			return;
		}
		
		try {
			
			if (!CloseVoteFinder.getInstance().isRoomTag(room.getRoomId(),tag)){
				room.replyTo(messageId, "The tag [tag:" + tag + "] is **not** available in this room");
				return;
			}
			
			RoomTag rt = new RoomTag(room.getRoomId(),tag);
			int result = new RoomTagDAO().delete(CloseVoteFinder.getInstance().getConnection(), rt);
			CloseVoteFinder.getInstance().removeRoomTag(rt);
			if (logger.isDebugEnabled()) {
				logger.debug("runCommand(ChatRoom, PingMessageEvent) - " + result);
			}
			room.replyTo(messageId, "Tag [tag:" + tag + "] has been removed from this room");
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(messageId,"Sorry problem updating data, @Petter need to check the stack trace");
		}
	}

}
