package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.DuplicateNotificationsDAO;
import jdd.so.dao.model.DuplicateNotifications;

public class OptOutCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(OptOutCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(opt[\\-\\s]*out)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_HAMMER;
	}

	@Override
	public String getCommandName() {
		return "opt-out";
	}

	@Override
	public String getCommandDescription() {
		return "Remove yourself from duplication notifications in tag";
	}

	@Override
	public String getCommandUsage() {
		return "opt-out [tag]";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String tag = getTags(event.getContent());
		if (tag.length()==0 || tag.contains(";")){
			room.replyTo(event.getMessageId(), "Please opt-out for 1 tag and max 1 tag");
			return;
		}
		
		if (!CloseVoteFinder.getInstance().isRoomTag(room.getRoomId(),tag)){
			room.replyTo(event.getMessageId(), "The tag [" + tag + "] is not monitored in this room contact RO's for more info");
			return;
		}
		
		
		try {
			DuplicateNotifications dn = CloseVoteFinder.getInstance().getHunter(room.getRoomId(), event.getUserId(), tag);
			if (dn==null || !dn.isOptIn()){
				room.replyTo(event.getMessageId(), "You have **not** opt-in for the tag [tag:" + tag + "] in this room");
				return;	
			}
			dn.setOptIn(false);
			int result = new DuplicateNotificationsDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), dn);
			CloseVoteFinder.getInstance().getDupHunters().remove(dn);
			if (logger.isDebugEnabled()) {
				logger.debug("runCommand(ChatRoom, PingMessageEvent) - " + result);
			}
			room.replyTo(event.getMessageId(), "Ok, duplicate notification in this room for the tag [tag:" + tag + "] have been removed");
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessageId(),"Sorry problem updating data, tell @Petter to check the stack trace");
		}
	}

}
