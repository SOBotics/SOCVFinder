package jdd.so.bot.actions.cmd;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class ShutDownCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(die|shutdown)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_BOT_OWNER;
	}

	@Override
	public String getCommandName() {
		return "Shutdown the bot";
	}

	@Override
	public String getCommandDescription() {
		return "Shutdown the bot";
	}

	@Override
	public String getCommandUsage() {
		return "die";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		room.send("Bye bye");
	}

}
