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
		return "Confirm possibile duplicate is closed";
	}

	@Override
	public String getCommandUsage() {
		return "k";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		long parentMessage = event.getParentMessageId();
		Message pdm = room.getRoom().getMessage(parentMessage);
		if (pdm==null){
			room.replyTo(event.getMessageId(), "Could not find message your are replying to");
			return;
		}
		String c = pdm.getContent();
		if (!c.contains("possible-duplicate")){
			room.replyTo(event.getMessageId(), "Your reply was not direct to a possibile duplicate notification");
			return;
		}
		
		confirm(room, event.getMessageId(), parentMessage);
	}
	
	public void confirm(ChatRoom room, long messageId, long parentMessageId){
		
		room.delete(parentMessageId).handleAsync((mId, thr) -> {
			if (thr != null)
				room.replyTo(messageId, "Thank you for confirming the duplicate").join();
			return mId;
		});
	}

}
