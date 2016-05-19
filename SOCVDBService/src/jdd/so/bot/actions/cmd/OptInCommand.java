package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class OptInCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(opt[\\-\\s]*in)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "opt-in";
	}

	@Override
	public String getCommandDescription() {
		return "Add yourself to duplication notifications in tag of choice";
	}

	@Override
	public String getCommandUsage() {
		return "opt-in [[tag]]";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String retMsg = "opt-in command is under dev, please return later";
		room.send(retMsg);
	}

}
