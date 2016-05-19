package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
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
		return "api-quota";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		if (CloseVoteFinder.getInstance().getApiQuota() < 0) {
			room.send("I have not done any api request since start-up so I do not know");
		} else {
			room.send("The current api quota is: " + CloseVoteFinder.getInstance().getApiQuota());
		}
	}

}
