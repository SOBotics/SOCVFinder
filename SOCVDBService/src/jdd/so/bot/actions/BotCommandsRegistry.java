package jdd.so.bot.actions;

import java.util.ArrayList;
import java.util.List;

import jdd.so.bot.actions.cmd.AddUserCommand;
import jdd.so.bot.actions.cmd.AiChatCommand;
import jdd.so.bot.actions.cmd.ApiQuotaCommand;
import jdd.so.bot.actions.cmd.BatchDoneCommand;
import jdd.so.bot.actions.cmd.BatchIgnoreCommand;
import jdd.so.bot.actions.cmd.CherryPickCommand;
import jdd.so.bot.actions.cmd.CommandsCommand;
import jdd.so.bot.actions.cmd.DeleteCommentCommand;
import jdd.so.bot.actions.cmd.DuplicateConfirmCommand;
import jdd.so.bot.actions.cmd.DuplicateWhiteListCommand;
import jdd.so.bot.actions.cmd.HelpCommand;
import jdd.so.bot.actions.cmd.IndexCommand;
import jdd.so.bot.actions.cmd.OptInCommand;
import jdd.so.bot.actions.cmd.OptOutCommand;
import jdd.so.bot.actions.cmd.PingCommand;
import jdd.so.bot.actions.cmd.RoomTagAdd;
import jdd.so.bot.actions.cmd.RoomTagList;
import jdd.so.bot.actions.cmd.RoomTagRemove;
import jdd.so.bot.actions.cmd.ShutDownCommand;
import jdd.so.bot.actions.cmd.StatsMeCommand;
import jdd.so.bot.actions.cmd.StatsRoomCommand;
import jdd.so.bot.actions.cmd.StatsTagCommand;
import jdd.so.bot.actions.cmd.UserListCommand;
import jdd.so.bot.actions.cmd.WhiteListCommand;


/**
 * The registry (instance) of all possible commands
 * @author Petter Friberg
 *
 */
public class BotCommandsRegistry {


	
	private static BotCommandsRegistry instance;
	private List<BotCommand> commands;

	private BotCommandsRegistry() {
		init();
	}

	public static BotCommandsRegistry getInstance() {
		if (instance == null) {
			instance = new BotCommandsRegistry();
		}
		return instance;
	}

	private void init() {
		commands = new ArrayList<>();
		/**
		 * add manually since we want them in order, the cherry pick only needs
		 * [tag: run (and this is also use in other commands)
		 */
		commands.add(new PingCommand()); //Removing this fun
		commands.add(new CommandsCommand());
		commands.add(new HelpCommand());
		commands.add(new OptInCommand());
		commands.add(new OptOutCommand());
		commands.add(new ApiQuotaCommand());
		commands.add(new BatchDoneCommand());
		commands.add(new BatchIgnoreCommand());
		commands.add(new IndexCommand());
		commands.add(new RoomTagList());
		commands.add(new RoomTagAdd());
		commands.add(new RoomTagRemove());
		commands.add(new CherryPickCommand());
		commands.add(new StatsMeCommand());
		commands.add(new StatsTagCommand());
		commands.add(new StatsRoomCommand());
		// ...

		commands.add(new UserListCommand());
		commands.add(new AddUserCommand());
		commands.add(new WhiteListCommand());
		commands.add(new DuplicateWhiteListCommand());
		commands.add(new DuplicateConfirmCommand());
		
		
		
		commands.add(new DeleteCommentCommand());
		commands.add(new ShutDownCommand());
	}

	public synchronized BotCommand getCommand(String content, boolean reply, int messageEdits) {	
		for (BotCommand abc : commands) {
			// give first on that is ok
			if (abc.isCommand(content,reply,messageEdits)) {
				return abc;
			}
		}
		return new AiChatCommand();
	}
	
	public List<BotCommand> getCommands(){
		//return a new list since we do not want commands to be reordered
		List<BotCommand> commands = new ArrayList<>();
		commands.addAll(this.commands);
		return commands;
	}
}
