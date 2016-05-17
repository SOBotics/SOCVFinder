package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
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
		return "Reports that you have finished reviwing batch served";
	}

	@Override
	public String getCommandUsage() {
		return "reply to batch with done";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		room.replyTo(event.getMessageId(), "Thank you for your effort");
	}

}
