package jdd.so.bot.actions.cmd;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class RoomLeaveCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(RoomLeaveCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(leave)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_RO;
	}

	@Override
	public String getCommandName() {
		return "leave room";
	}

	@Override
	public String getCommandDescription() {
		return "Tell me to leave the room";
	}

	@Override
	public String getCommandUsage() {
		return "leave";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		CompletionStage<Long> mId = room.replyTo(event.getMessage().getId(), "I'm leaving this room, I'll be back if rebooted");
		mId.thenAccept(new Consumer<Long>() {

			@Override
			public void accept(Long t) {
				room.leave();
			}
		});

	}

}
