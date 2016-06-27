package jdd.so.service;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatBot;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.bot.actions.cmd.AddUserCommand;
import jdd.so.bot.actions.cmd.AiChatCommand;
import jdd.so.bot.actions.cmd.ApiQuotaCommand;
import jdd.so.bot.actions.cmd.CommandsCommand;
import jdd.so.bot.actions.cmd.DeleteCommentCommand;
import jdd.so.bot.actions.cmd.DuplicateConfirmCommand;
import jdd.so.bot.actions.cmd.DuplicateWhiteListCommand;
import jdd.so.bot.actions.cmd.HelpCommand;
import jdd.so.bot.actions.cmd.OptInCommand;
import jdd.so.bot.actions.cmd.OptOutCommand;
import jdd.so.bot.actions.cmd.RoomLeaveCommand;
import jdd.so.bot.actions.cmd.RoomTagAdd;
import jdd.so.bot.actions.cmd.RoomTagList;
import jdd.so.bot.actions.cmd.RoomTagRemove;
import jdd.so.bot.actions.cmd.ShutDownCommand;

public class SOCVFinderServiceWrapper implements WrapperListener {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(SOCVFinderServiceWrapper.class);

	static {
		PropertyConfigurator.configure("ini/log4j.properties");
	}

	private ChatBot cb;

	/*---------------------------------------------------------------
	 * Constructors
	 *-------------------------------------------------------------*/
	/**
	 * Creates an instance of a WrapperSimpleApp.
	 * 
	 * @param The
	 *            full list of arguments passed to the JVM.
	 */
	protected SOCVFinderServiceWrapper(String args[]) {

		// Initialize the WrapperManager class on startup by referencing it.
		@SuppressWarnings("unused")
		Class<WrapperManager> wmClass = WrapperManager.class;

		// Start the application. If the JVM was launched from the native
		// Wrapper then the application will wait for the native Wrapper to
		// call the application's start method. Otherwise the start method
		// will be called immediately.
		WrapperManager.start(this, args);

		// This thread ends, the WrapperManager will start the application after
		// the Wrapper has
		// been properly initialized by calling the start method above.
	}

	public static void main(String[] args) {
		new SOCVFinderServiceWrapper(args);
	}

	/*---------------------------------------------------------------
	 * WrapperListener Methods
	 *-------------------------------------------------------------*/
	/**
	 * The start method is called when the WrapperManager is signalled by the
	 * native wrapper code that it can start its application. This method call
	 * is expected to return, so a new thread should be launched if necessary.
	 * If there are any problems, then an Integer should be returned, set to the
	 * desired exit code. If the application should continue, return null.
	 */
	@Override
	public Integer start(String[] args) {
		if (logger.isInfoEnabled()) {
			logger.info("start(String[]) - start");
		}

		// Load AI interface
		AIMLProcessor.extension = new PCAIMLProcessorExtension();
		MagicStrings.root_path = System.getProperty("user.dir");
		MagicStrings.default_bot_name = "Queen";

		// Load properties file an instance the CloseVoteFinder

		// Start the bot
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream("ini/SOCVService.properties"));
			CloseVoteFinder.initInstance(properties);
			cb = new ChatBot(properties, null);
			SOLoginThread login = new SOLoginThread(); // takes to much time
			login.start();
			if (logger.isDebugEnabled()) {
				logger.debug("start(String[]) - Start completed");
			}
		} catch (Throwable e) {
			logger.error("start service", e);
			CloseVoteFinder.getInstance().shutDown();
			cb.close();
			return 1;
		}

		if (logger.isInfoEnabled()) {
			logger.info("start(String[]) - end");
		}
		return null;
	}

	/**
	 * Called when the application is shutting down.
	 */
	@Override
	public int stop(int exitCode) {
		if (logger.isInfoEnabled()) {
			logger.info("stop() - exitCode:" + exitCode);
		}
		try {
			if (cb != null) {
				cb.close();
			}
			CloseVoteFinder.getInstance().shutDown();
		} catch (Throwable e) {
			logger.error("stop(int)", e);
		}
		if (logger.isInfoEnabled()) {
			logger.info("stop() end");
		}
		return exitCode;
	}

	/**
	 * Called whenever the native wrapper code traps a system control signal
	 * against the Java process. It is up to the callback to take any actions
	 * necessary. Possible values are: WrapperManager.WRAPPER_CTRL_C_EVENT,
	 * WRAPPER_CTRL_CLOSE_EVENT, WRAPPER_CTRL_LOGOFF_EVENT, or
	 * WRAPPER_CTRL_SHUTDOWN_EVENT
	 */
	@Override
	public void controlEvent(int event) {
		if ((event == WrapperManager.WRAPPER_CTRL_LOGOFF_EVENT) && WrapperManager.isLaunchedAsService()) {
			// Ignore
			if (logger.isInfoEnabled()) {
				logger.info("ServiceWrapper: controlEvent(" + event + ") Ignored");
			}

		} else {
			if (logger.isInfoEnabled()) {
				logger.info("ServiceWrapper: controlEvent(" + event + ") Stopping");
			}
			WrapperManager.stop(0);
			// Will not get here.
		}
	}

	private class SOLoginThread extends Thread {
		/**
		 * Logger for this class
		 */
		private final Logger logger = Logger.getLogger(SOLoginThread.class);

		@Override
		public void run() {
			if (logger.isDebugEnabled()) {
				logger.debug("start(String[]) - Start login");
			}
			cb.loginIn();
			if (logger.isDebugEnabled()) {
				logger.debug("start(String[]) - Join rooms");
			}
			// SOCVFinder
			cb.joinRoom("stackoverflow.com", 111347, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, true);
			// Campagins
			cb.joinRoom("stackoverflow.com", 95290, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
			// SOCVR
			List<Class<? extends BotCommand>> allowedCommands = new ArrayList<>();
			allowedCommands.add(HelpCommand.class);
			allowedCommands.add(CommandsCommand.class);
			allowedCommands.add(ApiQuotaCommand.class);
			allowedCommands.add(AddUserCommand.class);
			allowedCommands.add(OptInCommand.class);
			allowedCommands.add(OptOutCommand.class);
			allowedCommands.add(DuplicateConfirmCommand.class);
			allowedCommands.add(DuplicateWhiteListCommand.class);
			allowedCommands.add(DeleteCommentCommand.class);
			allowedCommands.add(RoomTagList.class);
			allowedCommands.add(RoomTagAdd.class);
			allowedCommands.add(RoomTagRemove.class);
			allowedCommands.add(AiChatCommand.class);
			allowedCommands.add(ShutDownCommand.class);
			allowedCommands.add(RoomLeaveCommand.class);
			// SOCVRTesting
			// cb.joinRoom("stackoverflow.com", 68414, allowedCommands,
			// ChatRoom.DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM, false);
			// SOCVR
			cb.joinRoom("stackoverflow.com", 41570, allowedCommands, ChatRoom.DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM, false);

			// R Room
			List<Class<? extends BotCommand>> allowedCommandsR = new ArrayList<>();
			allowedCommandsR.add(HelpCommand.class);
			allowedCommandsR.add(CommandsCommand.class);
			allowedCommandsR.add(ApiQuotaCommand.class);
			allowedCommandsR.add(AddUserCommand.class);
			allowedCommandsR.add(OptInCommand.class);
			allowedCommandsR.add(OptOutCommand.class);
			allowedCommandsR.add(DuplicateConfirmCommand.class);
			allowedCommandsR.add(DuplicateWhiteListCommand.class);
			allowedCommandsR.add(DeleteCommentCommand.class);
			allowedCommandsR.add(RoomTagList.class);
			allowedCommandsR.add(RoomTagAdd.class);
			allowedCommandsR.add(RoomTagRemove.class);
			allowedCommandsR.add(AiChatCommand.class);
			allowedCommandsR.add(ShutDownCommand.class);
			allowedCommandsR.add(RoomLeaveCommand.class);
			
			cb.joinRoom("stackoverflow.com", 25312, allowedCommandsR, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);

			// SOCVRTesting
			// cb.joinRoom("stackoverflow.com", 68414, allowedCommands,
			// ChatRoom.DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM, false);

			cb.startDupeHunter();
		}

	}

}
