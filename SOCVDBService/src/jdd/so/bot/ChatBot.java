package jdd.so.bot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.StackExchangeClient;
import fr.tunaki.stackoverflow.chat.event.EventType;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.actions.BotCommand;
import jdd.so.bot.actions.BotCommandsRegistry;
import jdd.so.bot.actions.cmd.ShutDownCommand;
import jdd.so.model.User;

public class ChatBot {
	private static final Logger logger = Logger.getLogger(ChatBot.class);

	public static final int EVENT_REPLY = 18;

	private StackExchangeClient client;

	private Properties properties;

	private CountDownLatch messageLatch;

	private static Chat chatSession;

	public ChatBot(Properties properties, CountDownLatch messageLatch) {
		this.properties = properties;
		this.messageLatch = messageLatch;
		Bot bot = new Bot("QUEEN", MagicStrings.root_path, "chat");
		bot.deleteLearnfCategories();
		chatSession = new Chat(bot);
		bot.brain.nodeStats();
	}

	public static synchronized String getResponse(String message) {
		String msg = chatSession.multisentenceRespond(message);
		
		if (msg != null && (msg.toLowerCase().contains("google")||msg.contains("<search>"))){
			return "Sorry, I do not know";
		}

		if (msg != null && msg.length() > 400 && !msg.contains("\n")) {
			msg = "Well\n" + msg;
		}
		return msg.replaceAll("<br/>", "\n");

	}

	public boolean loginIn() {
		client = new StackExchangeClient(properties.getProperty("email"), properties.getProperty("password"));
		System.out.println("Client logged in");
		return (client != null);
	}

	public boolean joinRoom(String domain, int roomId) {
		Room room = client.joinRoom(domain, roomId);
		System.out.println("Client join room: " + roomId);
		room.addEventListener(EventType.MESSAGE_REPLY, event -> roomEvent(room, event, true));
		room.addEventListener(EventType.USER_MENTIONED, event -> roomEvent(room, event, false));
		return room.getRoomId() != 0;
	}

	protected void roomEvent(Room room, PingMessageEvent event, boolean isReply) {
		System.out.println("Incomming message: " + event.toString());
		if (event.getEditCount() > 0) {
			//long parentId = event.getParentMessageId();
			return; // Ignore edits for now
		}
		BotCommand bc = BotCommandsRegistry.getInstance().getCommand(event.getContent(), isReply, event.getEditCount());
		System.out.println(bc);

		// Check access level
		long userId = event.getUserId();
		int accessLevel = 0;
		if (CloseVoteFinder.getInstance().getUsers() != null) {
			User u = CloseVoteFinder.getInstance().getUsers().get(userId);
			if (u != null) {
				accessLevel = u.getAccessLevel();
			}

			if (accessLevel < bc.getRequiredAccessLevel()) {
				room.replyTo(event.getMessageId(),
						"Sorry you need to be " + BotCommand.getAccessLevelName(bc.getRequiredAccessLevel()) + " to run this command (@Petter)");
				return;
			}
		}

		bc.runCommand(room, event);
		if (bc instanceof ShutDownCommand) {
			messageLatch.countDown();
		}
	}

	public void close() {
		if (client != null) {
			client.close();
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		/**
		 * Logger for this class
		 */

		PropertyConfigurator.configure("ini/log4j.properties");

		if (logger.isDebugEnabled()) {
			logger.debug("main(String[]) - start");
		}

		// Load AI interface
		AIMLProcessor.extension = new PCAIMLProcessorExtension();
		MagicStrings.root_path = System.getProperty("user.dir");
		MagicStrings.default_bot_name = "Queen";

		// load properties file
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);
		CountDownLatch messageLatch = new CountDownLatch(1);
		ChatBot cb = new ChatBot(properties, messageLatch);
		try {
			cb.loginIn();
			cb.joinRoom("stackoverflow.com", 111347);
		    cb.joinRoom("stackoverflow.com", 95290);

			try {
				messageLatch.await();
			} catch (InterruptedException e) {
			}

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			CloseVoteFinder.getInstance().shutDown();
			cb.close();
		}
	}

}
