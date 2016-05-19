package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class BatchDoneCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(done)";
	}
	
	@Override
	public boolean matchReply(boolean reply){
		return reply;
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "Done reviewing batch";
	}

	@Override
	public String getCommandDescription() {
		return "Reports that you have reviewed last batch served";
	}

	@Override
	public String getCommandUsage() {
		return "done";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		room.replyTo(event.getMessageId(), "Thank you for your effort");
	}

}
