package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.actions.BotCommand;

public class HelpCommand extends  BotCommand{

	@Override
	public String getMatchCommandRegex() {
		return "(help|command|cmds)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_NONE;
	}

	@Override
	public String getCommandName() {
		return "Help and commands";
	}

	@Override
	public String getCommandDescription() {
		return "Display information about the chat bot and it's commands";
	}

	@Override
	public String getCommandUsage() {
		return "help or commands";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		String retMsg = "This is a chat bot to notify about possibile duplicates and cherry pick questions to review. Follow links for [commands](https://github.com/jdd-software/SOCVFinder/blob/master/commands.md) and more [information](https://github.com/jdd-software/SOCVFinder)";
		room.send(retMsg);
	}
	
	

}
