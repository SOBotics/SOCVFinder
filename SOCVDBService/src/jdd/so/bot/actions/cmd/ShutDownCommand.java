package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.actions.BotCommand;

public class ShutDownCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(die|shutdown)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_OWNER;
	}

	@Override
	public String getCommandName() {
		return "Shutdown the bot";
	}

	@Override
	public String getCommandDescription() {
		return "Will terminate the bot";
	}

	@Override
	public String getCommandUsage() {
		return "die or shutdown";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		room.send("Bye bye");
	}

}
