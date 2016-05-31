package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.Message;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class DuplicateConfirmCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(@que[a-zA-Z]* k(\\s|$))";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_HAMMER;
	}

	@Override
	public String getCommandName() {
		return "Confirm duplicate notification";
	}

	@Override
	public String getCommandDescription() {
		return "Confirm that possibile duplicate is closed";
	}

	@Override
	public String getCommandUsage() {
		return "k";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		long parentMessage = event.getParentMessageId();
		Message pdm = room.getRoom().getMessage(parentMessage);
		if (pdm == null) {
			room.replyTo(event.getMessageId(), "Could not find message your are replying to");
			return;
		}
		String c = pdm.getPlainContent();
		if (!c.contains("[tag:possible-duplicate]")) {
			room.replyTo(event.getMessageId(), "Your reply was not direct to a possible duplicate notification");
			return;
		}

		confirm(room, event, c);
	}

	public void confirm(ChatRoom room, PingMessageEvent event, String content) {

		String edit = content;
		if (edit.contains("@")) {
			edit = edit.substring(0, edit.indexOf('@')).trim();
		}
		if (edit.contains("--- f") || edit.contains("--- k")) {
			edit += ", k" + event.getUserName();
		} else {
			int lastTag = edit.lastIndexOf("[tag:");
			int closeTag = edit.indexOf(']', lastTag);
			if (closeTag > 0) {
				edit = edit.substring(0, closeTag + 2) + " ---" + edit.substring(closeTag + 2, edit.length()).trim() + "--- k by " + event.getUserName();
			}
		}

		room.edit(event.getParentMessageId(), edit).handleAsync((mId, thr) -> {
			if (thr != null)
				return room.replyTo(event.getMessageId(), "Thank you for confirming the duplicate").join();
			return mId;
		});

	}

}
