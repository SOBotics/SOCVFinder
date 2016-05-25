package jdd.so.bot.actions.cmd;

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.model.Stats;

public class StatsRoomCommand extends StatsCommandAbstract{
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(StatsRoomCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(stats rooms|stats room)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_NONE;
	}

	@Override
	public String getCommandName() {
		return "Statistics rooms";
	}

	@Override
	public String getCommandDescription() {
		return "Show statistics on different rooms";
	}

	@Override
	public String getCommandUsage() {
		return "stats rooms";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		long fromDate = getFromDate(event.getContent());
		try {
			List<Stats> stats = new BatchDAO().getRoomStats(CloseVoteFinder.getInstance().getConnection(),fromDate);
			if (stats.isEmpty()){
				room.replyTo(event.getMessageId(), "There are no stats available");
				return;
			}
			//set decription
			for (Stats s : stats) {
				String roomName = room.getBot().getRoomName(s.getId());
				s.setDescription(roomName);
			}
			String retVal = "    Room statistics" + getFilteredTitle(event.getContent()) + getStats(stats, true);;
			room.send(retVal);
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessageId(), "Sorry an error occured while trying to get the stats @Petter");
		}
	}

}
