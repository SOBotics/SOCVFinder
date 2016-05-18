package jdd.so.bot.actions.cmd;

import java.util.Comparator;

import jdd.so.bot.actions.BotCommand;

public class ComandComparator implements Comparator<BotCommand>{

	@Override
	public int compare(BotCommand o1, BotCommand o2) {
		int i = o1.getRequiredAccessLevel() - o2.getRequiredAccessLevel();
		if (i!=0){
			return i;
		}
		return o1.getCommandName().compareTo(o2.getCommandName());
	}

}
