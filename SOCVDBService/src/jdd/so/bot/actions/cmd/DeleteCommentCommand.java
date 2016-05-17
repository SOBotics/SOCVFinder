package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.actions.BotCommand;

public class DeleteCommentCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(delete|remove)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "Remove chat message";
	}

	@Override
	public String getCommandDescription() {
		return "Remove the chat message";
	}

	@Override
	public String getCommandUsage() {
		return "remove";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		long parentId = event.getParentMessageId();
		if (parentId > 0) {
			room.delete(parentId);
		}
	}

}
