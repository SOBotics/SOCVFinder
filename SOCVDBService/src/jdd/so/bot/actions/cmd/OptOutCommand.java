package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.actions.BotCommand;

public class OptOutCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(opt[\\-\\s]*out)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "opt-out";
	}

	@Override
	public String getCommandDescription() {
		return "Remove yourself from duplication notifications indicate tag";
	}

	@Override
	public String getCommandUsage() {
		return "@Queen opt-out [tag:tagToMonitor]";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		String retMsg = "opt-out command is under dev, please return later";
		room.send(retMsg);
	}

}
