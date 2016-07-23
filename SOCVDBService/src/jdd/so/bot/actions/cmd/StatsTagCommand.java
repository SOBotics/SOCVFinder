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

public class StatsTagCommand extends StatsCommandAbstract{
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(StatsTagCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(stats tag|stats tags|tag stats|tag stat)";
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
		String messageContent = event.getMessage().getContent();
		long messageId = event.getMessage().getId();
		long fromDate = getFromDate(messageContent);
		try {
			
			List<Stats> stats = new BatchDAO().getTagsStats(CloseVoteFinder.getInstance().getConnection(),fromDate);
			if (stats.isEmpty()){
				room.replyTo(messageId, "There are no stats available");
				return;
			}
			String retVal = "    Tag statistics" + getFilteredTitle(messageContent) + getStats(stats, false); 
			room.send(retVal);
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(messageId, "Sorry an error occured while trying to get the stats @Petter");
		}
	}

}
