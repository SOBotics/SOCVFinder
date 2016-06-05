package jdd.so.bot.actions.cmd;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class AiChatCommand extends BotCommand {

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
		return "Respond to commands I don't understand";
	}

	@Override
	public String getCommandUsage() {
		return "When I don't understand I will just try to interpret";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		if (event.getUserId()==event.getTargetUserId()){ //avoid talking to myself
			return;
		}
		String message = event.getContent();
		String lm = message.toLowerCase();
	
		if (lm.contains("[tag:")){
			room.send("Seems that you like to cherry pick but you should not use [tag:, just indicate the tag within [] example [java]");
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
		
		String response = room.getUnkownCommandResponse(message.replaceAll("(?i)@qu(\\w+)", "").trim(),event.getUserName());
		room.replyTo(event.getMessageId(), response);
		
	}

}
