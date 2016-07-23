package jdd.so.bot.actions.cmd;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class PingCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		 return "(?i)( tell | say | ping )";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "Say";
	}

	@Override
	public String getCommandDescription() {
		return "Use bot to ping other users";
	}

	@Override
	public String getCommandUsage() {
		return "ping [Username]*";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String message = event.getMessage().getContent();
		String lm = message.toLowerCase();
		if (lm.contains("ping")){
			int start = lm.indexOf("ping")+4;
			String[] users = message.substring(start, lm.length()).split(" ");
			if (users.length==0){
				room.replyTo(event.getMessage().getId(), "Ping");
				return;
			}
			String retVal = "";
			for (String u : users) {
				if (u.trim().length()==0){
					continue;
				}
				if ("and".equalsIgnoreCase(u)){
					retVal+=" and " ;
				}
				retVal += "@" + u + " ";
			}
			room.send(retVal + " come out and play");
			return;
		}
		int startPos = 0;
		boolean ping = false;
		if (lm.contains("tell")){
			startPos = lm.indexOf("tell")+5;
			ping=true;
		}
		if (lm.contains("say")){
			startPos = lm.indexOf("say")+4;
		}
		String ret = message.substring(startPos,message.length());
		String[] ma =  ret.split(" ");
		if (ma.length<1){
			room.replyTo(event.getMessage().getId(), "What?");
			return;
		}
		
		StringBuilder msg = new StringBuilder();
		for (int i = 0; i < ma.length; i++) {
			if ("to".equalsIgnoreCase(ma[i])&&i<=2){
				continue;
			}
			if (ping){
				msg.append("@" + ma[i]);
				ping  = false;
			}else{
				msg.append(" " + ma[i]);
			}
		}
		
		
		room.send(Jsoup.clean(msg.toString(), Whitelist.basic()));
	}

}
