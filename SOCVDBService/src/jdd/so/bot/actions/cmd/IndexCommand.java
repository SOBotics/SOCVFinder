package jdd.so.bot.actions.cmd;

import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONException;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.scan.QuestionScanner;

public class IndexCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(index)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_OWNER;
	}

	@Override
	public String getCommandName() {
		return "Index tag";
	}

	@Override
	public String getCommandDescription() {
		return "Update the index for tag executing api calls on 20 days (200) and save to db";
	}

	@Override
	public String getCommandUsage() {
		return "index [tag]";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String message = event.getContent();
		String tags = getTags(message);
		if (tags==null||tags.contains(";")||tags.trim().length()==0){
			room.send("You should index only on single tag");
			return;
		}
		boolean tagMonitored = CloseVoteFinder.getInstance().isTagMonitored(tags);
		if (!tagMonitored){
			room.send("This tag is not monitored it needs to be added manually to database @Petter");
			return;
		}
		
		room.send("Starting to index tag: " + tags);
		try {
			new QuestionScanner().scan(tags, 20, 3);
		} catch (JSONException | IOException | SQLException e) {
			room.replyTo(event.getMessageId(), "Error occured while indexing");
			return;
		}
		room.replyTo(event.getMessageId(), "Index of tag " + tags + " completed");
		
	}

}
