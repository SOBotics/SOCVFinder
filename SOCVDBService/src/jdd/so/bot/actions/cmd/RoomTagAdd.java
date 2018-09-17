package jdd.so.bot.actions.cmd;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.RoomTagDAO;
import jdd.so.dao.model.RoomTag;

public class RoomTagAdd extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(RoomTagAdd.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(add " + getRegexTag() + ")";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_RO;
	}

	@Override
	public String getCommandName() {
		return "add tag to room";
	}

	@Override
	public String getCommandDescription() {
		return "Allow duplicate notifications in tag";
	}

	@Override
	public String getCommandUsage() {
		return "add [tag]";
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
			List<String> tags = CloseVoteFinder.getInstance().getRoomTags().get(room.getRoomId());
			if (tags!=null && !tag.isEmpty() && CloseVoteFinder.getInstance().isRoomTag(room.getRoomId(),tag)){
				room.replyTo(messageId, "The tag [tag:" + tag + "] is already available in this room");
				return;
			}
			
			RoomTag rt = new RoomTag(room.getRoomId(),tag);
			int result = new RoomTagDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), rt);
			CloseVoteFinder.getInstance().addRoomTag(rt);
			if (logger.isDebugEnabled()) {
				logger.debug("runCommand(ChatRoom, PingMessageEvent) - " + result);
			}
			room.replyTo(messageId, "Tag [tag:" + tag + "] has been added to room and duplicate notifications for hammer in tag are available");
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(messageId,"Sorry problem updating data, @Petter need to check the stack trace");
		}
	}

}
