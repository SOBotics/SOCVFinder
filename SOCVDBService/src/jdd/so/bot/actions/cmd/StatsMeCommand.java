package jdd.so.bot.actions.cmd;

import java.sql.SQLException;
import java.util.List;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.model.MyStats;

public class StatsMeCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(stats me|stat me)";
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
		try {
			List<MyStats> stats = new BatchDAO().getMyStats(CloseVoteFinder.getInstance().getConnection(), userId);
			if (stats.isEmpty()){
				room.replyTo(event.getMessageId(), "There is no stats available use the done command when you have finished with batch to save review data");
				return;
			}
			String retVal = "This is your effort that I have registred\n";
			for (MyStats s : stats) {
				retVal +="[" + s.getTag() + "] " + s.getCvCount() + "CV - " + s.getClosedCount() + " closed\n";
			}
			room.replyTo(event.getMessageId(), retVal);
		} catch (SQLException e) {
			room.replyTo(event.getMessageId(), "Sorry an error occured while trying to get your stats @Petter");
		}
		
	}

}
