package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatBot;
import jdd.so.bot.actions.BotCommand;

public class RandomChatCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(.*)"; //All others
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_NONE;
	}

	@Override
	public String getCommandName() {
		return "Random chat response";
	}

	@Override
	public String getCommandDescription() {
		return "Just some random stuff";
	}

	@Override
	public String getCommandUsage() {
		return "When I don't understand I will just try to interpret";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		String message = event.getContent();
		String lm = message.toLowerCase();
	
		if (lm.contains("done")){
			room.replyTo(event.getMessageId(), "Done with what?, if batch reply to the batch you have done");
			return;
		}
		
		if (lm.contains("awesome")||lm.contains("sweet")||lm.contains("nice")){
			room.send("Thanks honey");
			return;
		}
		if (lm.contains("plop")||lm.contains("blob")||lm.contains("meow")){
			room.send("Buzz buzz");
			return;
		}
		if (lm.contains("\\o")||lm.contains("o/")){
			room.send("\\o/");
			return;
		}
		
		if (lm.contains("morning")){
			room.send("morning");
			return;
		}
		
		if (lm.contains("cya")||lm.contains("bye")){
			room.send("cya");
			return;
		}
		
		if (lm.contains("alive")||lm.contains("fly")){
			room.send("Sure thing buzzing around");
			return;
		}
		
		String response = ChatBot.getResponse(message.replaceAll("(?i)@qu(\\w+)", "").trim());
		room.replyTo(event.getMessageId(), response);
		
	}

	private String getRandomICantUnderstandCmd() {
		int r  = (int)(Math.random()*5);
		switch (r){
		case 0:
			return "Sorry, will close that as unclear";
		case 1:
			return "He?, I guess POB?";
		case 2:
			return "Please clarify your question";
		case 3:
			return "Na, that seems strange";
		}
		return "What are you trying to tell me honey?";
	}

}
