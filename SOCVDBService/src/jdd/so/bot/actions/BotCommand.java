package jdd.so.bot.actions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.actions.cmd.CherryPickCommand;

public abstract class BotCommand {

	public static final int ACCESS_LEVEL_NONE = 0;
	public static final int ACCESS_LEVEL_REVIEWER = 1;
	public static final int ACCESS_LEVEL_OWNER = 2;

	public static String getAccessLevelName(int value) {
		switch (value) {
		case ACCESS_LEVEL_NONE:
			return "Guest";
		case ACCESS_LEVEL_REVIEWER:
			return "Reviewer";
		case ACCESS_LEVEL_OWNER:
			return "Owner";
		default:
			return "None";
		}
	}

	public abstract String getMatchCommandRegex();

	public boolean isCommand(String message, boolean reply, int messageEdits) {
		Pattern p = Pattern.compile(getMatchCommandRegex());
		return p.matcher(message).find() && matchReply(reply) && matchEdits(messageEdits);
	}

	public boolean matchReply(boolean reply) {
		return true;
	}

	public boolean matchEdits(int numberOfEdits) {
		return true;
	}

	public String getTags(String message) {
		String regex = "(?i)(\\[.*?\\])";
		Matcher matcher = Pattern.compile(regex).matcher(message);
		String result = "";
		String sep = "";
		while (matcher.find()) {
			String tagMark = matcher.group();
			result += sep + tagMark.substring(1, tagMark.length() - 1).replaceAll(" ", "");
			sep = ";";
		}
		return result;
	}

	public abstract int getRequiredAccessLevel();

	public abstract String getCommandName();

	public abstract String getCommandDescription();

	public abstract String getCommandUsage();

	public abstract void runCommand(Room room, PingMessageEvent event);

	@Override
	public String toString() {
		return getCommandName();
	}

	public static void main(String[] args) {
		CherryPickCommand cmd = new CherryPickCommand();
		String test = " [java] asd [php] dupes";
		System.out.println(cmd.getTags(test));
	}
}
