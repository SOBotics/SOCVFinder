package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.DuplicateNotificationsDAO;
import jdd.so.dao.model.DuplicateNotifications;

public class OptInCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(OptInCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(opt[\\-\\s]*in)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_HAMMER;
	}

	@Override
	public String getCommandName() {
		return "opt-in";
	}

	@Override
	public String getCommandDescription() {
		return "Add yourself to duplication notifications in tag of choice";
	}

	@Override
	public String getCommandUsage() {
		return "opt-in [tag]";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String tag = getTags(event.getMessage());
		if (tag.length()==0 || tag.contains(";")){
			room.replyTo(event.getMessage().getId(), "Please opt-in for 1 tag and max 1 tag");
			return;
		}
		
		if (!CloseVoteFinder.getInstance().isRoomTag(room.getRoomId(),tag)){
			room.replyTo(event.getMessage().getId(), "The tag [" + tag + "] is not monitored in this room contact RO's for more info");
			return;
		}
		
		try {
			DuplicateNotifications dn = CloseVoteFinder.getInstance().getHunter(room.getRoomId(), event.getUserId(), tag);
			if (dn!=null && dn.isOptIn()){
				room.replyTo(event.getMessage().getId(), "You have already opt-in for the tag [tag:" + tag + "] in this room");
				return;	
			}
			dn = new DuplicateNotifications(room.getRoomId(), event.getUserId(), tag, true);
			int result = new DuplicateNotificationsDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), dn);
			CloseVoteFinder.getInstance().getDupHunters().add(dn);
			if (logger.isDebugEnabled()) {
				logger.debug("runCommand(ChatRoom, PingMessageEvent) - " + result);
			}
			room.replyTo(event.getMessage().getId(), "Thanks, you have opted-in to be notified if a duplicate is found in tag [tag:" + tag + "] and you are present in room");
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessage().getId(),"Sorry problem updating data, tell @Petter to check the stack trace");
		}
	}

}
