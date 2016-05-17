package jdd.so.bot.actions;

import java.util.ArrayList;
import java.util.List;

import jdd.so.bot.actions.cmd.AddUserCommand;
import jdd.so.bot.actions.cmd.ApiQuotaCommand;
import jdd.so.bot.actions.cmd.BatchDoneCommand;
import jdd.so.bot.actions.cmd.CherryPickCommand;
import jdd.so.bot.actions.cmd.DeleteCommentCommand;
import jdd.so.bot.actions.cmd.HelpCommand;
import jdd.so.bot.actions.cmd.OptInCommand;
import jdd.so.bot.actions.cmd.OptOutCommand;
import jdd.so.bot.actions.cmd.PingCommand;
import jdd.so.bot.actions.cmd.RandomChatCommand;
import jdd.so.bot.actions.cmd.ShutDownCommand;

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
		commands.add(new PingCommand());
		commands.add(new HelpCommand());
		commands.add(new OptInCommand());
		commands.add(new OptOutCommand());
		commands.add(new BatchDoneCommand());
		commands.add(new ApiQuotaCommand());
		// ...

		commands.add(new CherryPickCommand());
		commands.add(new AddUserCommand());
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
		return new RandomChatCommand();
	}
	

	public static void main(String[] args) {
		BotCommand bc = BotCommandsRegistry.getInstance().getCommand(" cmd ", false,0);
		if (bc != null) {
			System.out.println(bc);
		}

		BotCommand bc2 = BotCommandsRegistry.getInstance().getCommand("[tag:java] 4c", false,0);
		if (bc2 != null) {
			System.out.println(bc2);
		}
		BotCommand bc4 = BotCommandsRegistry.getInstance().getCommand("opt-in [tag:java] 4c", false,0);
		if (bc4 != null) {
			System.out.println(bc4);
		}
		BotCommand bc5 = BotCommandsRegistry.getInstance().getCommand("whats up", false,0);
		if (bc5 != null) {
			System.out.println(bc5);
		}
	}

}
