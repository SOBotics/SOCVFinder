package jdd.so.bot.actions.cmd;

import java.util.Collections;
import java.util.List;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.actions.BotCommand;
import jdd.so.bot.actions.BotCommandsRegistry;

public class CommandsCommand extends  BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(command|commands|cmd|cmds)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_NONE;
	}

	@Override
	public String getCommandName() {
		return "Bot commands";
	}

	@Override
	public String getCommandDescription() {
		return "Display this list";
	}

	@Override
	public String getCommandUsage() {
		return "commands";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		List<BotCommand> commands = BotCommandsRegistry.getInstance().getCommands();
		Collections.sort(commands);
		room.replyTo(event.getMessageId(), "These are available commands");
		StringBuilder retMsg = new StringBuilder("");
		int al = -1;
		for (BotCommand bc : commands) {
			if (bc instanceof AiChatCommand){
				continue;//Do not included this
			}
			if (bc.getRequiredAccessLevel()!=al){
				if (al>-1){
					retMsg.append("\n");	
				}
				al = bc.getRequiredAccessLevel();
				
				retMsg.append("    " + BotCommand.getAccessLevelName(al));
			}
			retMsg.append("\n        " +bc.getCommandUsage() + " - " + bc.getCommandDescription());
			if (bc instanceof CherryPickCommand){
				retMsg.append("\n              answerType: a=Has answer, aa=Has accepted answer, na=Has no answer, naa=Has no accepted answer, nr=No roomba");
			}
		}
		retMsg.append("");
		room.send(retMsg.toString());
	}


	
	
	

}
