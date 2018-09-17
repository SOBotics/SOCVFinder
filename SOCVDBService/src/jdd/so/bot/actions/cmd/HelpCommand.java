package jdd.so.bot.actions.cmd;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class HelpCommand extends  BotCommand{

	@Override
	public String getMatchCommandRegex() {
		return "(?i)\\shelp\\b";
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
		return "Display information about the chat bot";
	}

	@Override
	public String getCommandUsage() {
		return "help";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String retMsg = "This bot is running two different registered stack apps: [SOCVFinder](http://stackapps.com/questions/6910/) that notify about possible duplicates and cherry pick questions to review and [Heat Detector](http://stackapps.com/questions/7001/) that analyse comments to find heat, see [quick guide](https://github.com/jdd-software/SOCVFinder/blob/master/quickGuide.md) for commands.";
		if (room.getAllowedCommands()!=null){
			retMsg += " In this room **commands are limited**  send `commands` to display available";
		}
		room.send(retMsg);
	}
	
	

}
