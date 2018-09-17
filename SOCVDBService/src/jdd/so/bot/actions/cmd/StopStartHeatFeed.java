package jdd.so.bot.actions.cmd;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;

public class StopStartHeatFeed extends  BotCommand{

	@Override
	public String getMatchCommandRegex() {
		return "(?i)((stop|start)\\sfeed)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_RO;
	}

	@Override
	public String getCommandName() {
		return "Stop and start heat comments feed";
	}

	@Override
	public String getCommandDescription() {
		return "Stop and start heat comments feed";
	}

	@Override
	public String getCommandUsage() {
		return "stop|start feed";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		
		
		if (event.getMessage().getPlainContent().contains("start")){
			if (!CloseVoteFinder.getInstance().isFeedHeat()){
				room.replyTo(event.getMessage().getId(), "Comments heat feed started");		
			}else{
				room.replyTo(event.getMessage().getId(), "Comments heat feed is already running");						
			}
			CloseVoteFinder.getInstance().setFeedHeat(true);
		}else{
			if (CloseVoteFinder.getInstance().isFeedHeat()){
				room.replyTo(event.getMessage().getId(), "Comments heat feed stopped");		
			}else{
				room.replyTo(event.getMessage().getId(), "Comments heat feed is already stopped");						
			}
			CloseVoteFinder.getInstance().setFeedHeat(false);	
		}
	}

}
