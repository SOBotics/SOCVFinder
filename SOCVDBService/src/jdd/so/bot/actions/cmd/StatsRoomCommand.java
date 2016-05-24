package jdd.so.bot.actions.cmd;

import java.sql.SQLException;
import java.util.List;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.model.MyStats;
import jdd.so.dao.model.RoomStats;

public class StatsRoomCommand extends BotCommand{

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
		try {
			List<RoomStats> stats = new BatchDAO().getRoomStats(CloseVoteFinder.getInstance().getConnection());
			if (stats.isEmpty()){
				room.replyTo(event.getMessageId(), "There is no stats available");
				return;
			}
			String retVal = "This is the effort that I have registred\n";
			int totCv = 0;
			int totClosed = 0;
			for (RoomStats s : stats) {
				String roomName = room.getBot().getRoomName(s.getRoomId());
				retVal +=" " + roomName + ": " + s.getCvCount() + "CV - " + s.getClosedCount() + " closed\n";
				totCv +=s.getCvCount();
				totClosed +=s.getClosedCount();
			}
			retVal +=" TOTAL: " + totCv + "CV - " + totClosed + " closed"; 
			room.replyTo(event.getMessageId(), retVal);
		} catch (SQLException e) {
			room.replyTo(event.getMessageId(), "Sorry an error occured while trying to get the stats @Petter");
		}
	}

}
