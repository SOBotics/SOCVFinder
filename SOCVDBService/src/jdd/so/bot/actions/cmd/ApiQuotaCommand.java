package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.actions.BotCommand;

public class ApiQuotaCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		 return "(?i)(apiquota|api-quota|api quota)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_NONE;
	}

	@Override
	public String getCommandName() {
		return "Api quota";
	}

	@Override
	public String getCommandDescription() {
		return "Get the current api quota";
	}

	@Override
	public String getCommandUsage() {
		return "api-qouta";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		room.send("The current api quota is: " + CloseVoteFinder.getInstance().getApiQuota());
	}

}
