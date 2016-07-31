package jdd.so.bot.actions.cmd;

import org.apache.log4j.Logger;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.api.model.Comment;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.nlp.CommentCategory;

public class CommentTestCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentTestCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(\\stest\\s)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "test comment";
	}

	@Override
	public String getCommandDescription() {
		return "Test a comment";
	}

	@Override
	public String getCommandUsage() {
		return "test Text that you like to test";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		CommentCategory cc = room.getBot().getCommentCategory();
		if (cc == null){
			room.replyTo(event.getMessage().getId(),"Comment controller has not been loaded");
			return;
		}
		
		String test = event.getMessage().getContent();
		int fromIndex = test.toLowerCase().indexOf("test")+5;
		test = test.substring(fromIndex);
		Comment c = new Comment();
		c.setBody(test);
		
		try {
			cc.classifyComment(c);
			StringBuilder message = room.getBot().getCommentsController().getHeatMessageResult(c, null);
			room.replyTo(event.getMessage().getId(),message.toString());
			return;
		} catch (Exception e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
		}
		
		
	
	}

}
