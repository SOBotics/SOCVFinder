package jdd.so.bot.actions.cmd;

import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.List;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.model.Stats;

public class StatsTagCommand extends StatsCommandAbstract{
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(StatsTagCommand.class);

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
		long fromDate = getFromDate(event.getContent());
		try {
			
			List<Stats> stats = new BatchDAO().getTagsStats(CloseVoteFinder.getInstance().getConnection(),fromDate);
			if (stats.isEmpty()){
				room.replyTo(event.getMessageId(), "There are no stats available");
				return;
			}
			String retVal = "    Tag statistics" + getFilteredTitle(event.getContent()) + getStats(stats, false); 
			room.send(retVal);
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessageId(), "Sorry an error occured while trying to get the stats @Petter");
		}
	}

}
