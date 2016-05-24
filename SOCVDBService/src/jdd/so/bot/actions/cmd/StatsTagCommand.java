package jdd.so.bot.actions.cmd;

import java.sql.SQLException;
import java.util.List;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.model.MyStats;

public class StatsTagCommand extends BotCommand{

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(stats tag|stats tags)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_NONE;
	}

	@Override
	public String getCommandName() {
		return "Statistics on tags";
	}

	@Override
	public String getCommandDescription() {
		return "Show statistics on different tags";
	}

	@Override
	public String getCommandUsage() {
		return "stats tags";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		try {
			List<MyStats> stats = new BatchDAO().getTagsStats(CloseVoteFinder.getInstance().getConnection());
			if (stats.isEmpty()){
				room.replyTo(event.getMessageId(), "There is no stats available");
				return;
			}
			String retVal = "This is the effort that I have registred\n";
			int totCv = 0;
			int totClosed = 0;
			for (MyStats s : stats) {
				retVal +=" [" + s.getTag() + "]: " + s.getCvCount() + "CV - " + s.getClosedCount() + " closed\n";
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
