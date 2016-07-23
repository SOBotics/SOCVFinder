package jdd.so.bot.actions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.tunaki.stackoverflow.chat.Message;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;

/**
 * Abstract class of a BotCommand
 * @author Petter Friberg
 *
 */
public abstract class BotCommand implements Comparable<BotCommand> {

	public static final int ACCESS_LEVEL_NONE = 0;
	public static final int ACCESS_LEVEL_REVIEWER = 1;
	public static final int ACCESS_LEVEL_HAMMER = 2;
	public static final int ACCESS_LEVEL_TAG_OWNER = 3;
	public static final int ACCESS_LEVEL_BOT_OWNER = 4;
	public static final int ACCESS_LEVEL_RO = 6; //Speciall level check actually RO

	public static String getAccessLevelName(int value) {
		switch (value) {
		case ACCESS_LEVEL_NONE:
			return "Guest";
		case ACCESS_LEVEL_REVIEWER:
			return "Reviewer";
		case ACCESS_LEVEL_HAMMER:
			return "Hammer";
		case ACCESS_LEVEL_TAG_OWNER:
			return "Tag owner";
		case ACCESS_LEVEL_BOT_OWNER:
			return "Bot owner";
		case ACCESS_LEVEL_RO:
			return "Room owner of chat room";
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

	public String getTags(Message message) {
		String regex = "(?i)\\[(?:tag:)?(.*?)\\]";
		Matcher matcher = Pattern.compile(regex).matcher(message.getPlainContent());
		String result = "";
		String sep = "";
		while (matcher.find()) {
			String tagMark = matcher.group(1);
			result += sep + tagMark.replaceAll(" ", "").toLowerCase();
			sep = ";";
		}
		return result;
	}

	public abstract int getRequiredAccessLevel();

	public abstract String getCommandName();

	public abstract String getCommandDescription();

	public abstract String getCommandUsage();

	public abstract void runCommand(ChatRoom room, PingMessageEvent event);

	@Override
	public String toString() {
		return getCommandName();
	}
	
	@Override
	public int compareTo(BotCommand o) {
		int i = getRequiredAccessLevel() - o.getRequiredAccessLevel();
		if (i!=0){
			return i;
		}
		return getCommandName().compareTo(o.getCommandName());
	}
}
