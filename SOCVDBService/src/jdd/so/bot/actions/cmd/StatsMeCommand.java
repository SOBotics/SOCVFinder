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

public class StatsMeCommand extends StatsCommandAbstract {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(StatsMeCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(stats me|stat me|stats my|stats mine|my stats|my stat)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "My statistics";
	}

	@Override
	public String getCommandDescription() {
		return "Show my review statistics";
	}

	@Override
	public String getCommandUsage() {
		return "stats me";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		long userId = event.getUserId();
		String messageContent = event.getMessage().getContent();
		long messageId = event.getMessage().getId();
		long fromDate = getFromDate(messageContent);
		try {
			List<Stats> stats = new BatchDAO().getTagStats(CloseVoteFinder.getInstance().getConnection(), userId,fromDate);
			if (stats.isEmpty()){
				room.replyTo(messageId, "There is no stats available use the done command when you have finished with batch to save review data");
				return;
			}
			String retVal = "    This is your effort that I have registered" + getFilteredTitle(messageContent) + getStats(stats,false);
			room.send(retVal);
		} catch (SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(messageId, "Sorry an error occured while trying to get your stats @Petter");
		}
		
	}


}
