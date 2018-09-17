package jdd.so.bot.actions.cmd;

import org.sobotics.chatexchange.chat.Message;
import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class TestPlain extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)xt";
	}

	@Override
	public int getRequiredAccessLevel() {
		return 0;
	}

	@Override
	public String getCommandName() {
		return "just test";
	}

	@Override
	public String getCommandDescription() {
		return "just test";
	}

	@Override
	public String getCommandUsage() {
		return "testing";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		Message m  = event.getMessage();
		String plain = m.getPlainContent();
		System.out.println(plain);
		room.send(plain);
	}

}
