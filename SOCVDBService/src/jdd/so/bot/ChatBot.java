package jdd.so.bot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import fr.tunaki.stackoverflow.chat.StackExchangeClient;
import fr.tunaki.stackoverflow.chat.event.EventType;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.actions.BotCommand;
import jdd.so.bot.actions.BotCommandsRegistry;
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
import jdd.so.bot.actions.cmd.RoomTagAdd;
import jdd.so.bot.actions.cmd.RoomTagList;
import jdd.so.bot.actions.cmd.RoomTagRemove;
import jdd.so.bot.actions.cmd.ShutDownCommand;
import jdd.so.dao.UserDAO;
import jdd.so.dao.model.User;
import jdd.so.dup.DupeHunterComments;

/**
 * The main ChatBot handling the ChatRooms
 *
 * @author Petter Friberg
 */
public class ChatBot {

	private static final String BOT_NAME = "Queen";

	private static final Logger logger = Logger.getLogger(ChatBot.class);

	private StackExchangeClient client;

	private Properties properties;

	private CountDownLatch messageLatch; // Wait before exiting

	private Bot aiBot;

	private Map<Long, ChatRoom> rooms = Collections.synchronizedMap(new HashMap<>());

	private DupeHunterComments dupeHunter;

	private static final int REPUTATION_CV_REVIEWER = 3000;

	public ChatBot(Properties properties, CountDownLatch messageLatch) {
		this.properties = properties;
		this.messageLatch = messageLatch;
		aiBot = new Bot("QUEEN", MagicStrings.root_path, "chat");
		MagicStrings.default_bot_name = "Queen";
		// aiBot.deleteLearnfCategories();
	}

	public boolean loginIn() {
		client = new StackExchangeClient(properties.getProperty("email"), properties.getProperty("password"));
		if (logger.isDebugEnabled()) {
			logger.debug("loginIn() - Client logged in");
		}
		return (client != null);
	}

	/**
	 * 
	 * @param domain,
	 *            SE network domain
	 * @param roomId,
	 *            roomId
	 * @param allowedCommands,
	 *            list of allowed commands pass <code>null</null> to allow all
	 * @param enableAi,
	 *            if to enable AI
	 * @return
	 */
	public boolean joinRoom(String domain, long roomId, List<Class<? extends BotCommand>> allowedCommands, int dupNotifyStrategy, boolean enableAi) {
		int batchNumber = CloseVoteFinder.getInstance().getBatchNumber(roomId); // Get
																				// it
																				// from
																				// db
		ChatRoom room = new ChatRoom(this, client.joinRoom(domain, roomId), batchNumber, allowedCommands,dupNotifyStrategy, enableAi);

		if (logger.isDebugEnabled()) {
			logger.debug("joinRoom(String, int) - Client joined room: " + roomId);
		}

		// room.getRoom().addEventListener(EventType.MESSAGE_POSTED, event ->
		// roomPost(room, event));

		room.getRoom().addEventListener(EventType.MESSAGE_REPLY, event -> roomEvent(room, event, true));
		room.getRoom().addEventListener(EventType.USER_MENTIONED, event -> roomEvent(room, event, false));

		long id = room.getRoomId();
		rooms.put(id, room);

		return id != 0;
	}

	// private void roomPost(ChatRoom room, MessagePostedEvent event) {
	// System.out.println(event);
	// if (event.getContent().equalsIgnoreCase("que f") ||
	// event.getContent().equalsIgnoreCase("que f")) {
	// CompletableFuture<Long> lastMessage = room.getLastMessage();
	// if (lastMessage != null && lastMessage.isDone()) {
	// try {
	// Long mId = lastMessage.get();
	// if (mId != null) {
	// Message lastMesage = room.getRoom().getMessage(mId);
	// if (lastMesage != null) {
	// System.out.println(lastMessage);
	// }
	// }
	// } catch (InterruptedException | ExecutionException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// }
	// }

	protected void roomEvent(ChatRoom room, PingMessageEvent event, boolean isReply) {
		if (logger.isDebugEnabled()) {
			logger.debug("roomEvent(ChatRoom, PingMessageEvent, boolean) - Incomming message: " + event.toString());
		}

		if (event.getEditCount() > 0) {
			// long parentId = event.getParentMessageId();
			return; // Ignore edits for now
		}

		BotCommand bc = BotCommandsRegistry.getInstance().getCommand(event.getContent(), isReply, event.getEditCount());
		if (logger.isDebugEnabled()) {
			logger.debug("roomEvent(ChatRoom, PingMessageEvent, boolean) - " + bc);
		}

		// Check if command is allowed in room
		if (!room.isAllowed(bc)) {
			// Dont' respond
			return;
		}

		if (CloseVoteFinder.getInstance().getUsers() == null) {
			room.replyTo(event.getMessageId(), "The bot has not been initialized correctly and can not execute commands");
			if (messageLatch != null) {
				messageLatch.countDown();
			} else {
				try {
					close();
					CloseVoteFinder.getInstance().shutDown();
				} catch (Exception e) {
					logger.error("roomEvent() Shutdown error", e);
				}
				System.exit(0);
			}
			return;
		}

		// Check access level
		long userId = event.getUserId();
		int accessLevel = 0;

		User u = CloseVoteFinder.getInstance().getUsers().get(userId);

		if (u != null) {
			accessLevel = u.getAccessLevel();
		} else {
			fr.tunaki.stackoverflow.chat.User user = room.getUser(userId);

			if (logger.isDebugEnabled()) {
				logger.debug("roomEvent(ChatRoom, PingMessageEvent, boolean) - User rep = " + user.getReputation());
			}

			if (user.getReputation() >= REPUTATION_CV_REVIEWER) {
				UserDAO dao = new UserDAO();
				int al = BotCommand.ACCESS_LEVEL_REVIEWER;

				if (user.isModerator()) {
					al = BotCommand.ACCESS_LEVEL_BOT_OWNER;
				}

				u = new User(user.getId(), user.getName(), al);

				try {
					dao.insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), u);
					CloseVoteFinder.getInstance().getUsers().put(u.getUserId(), u);
					accessLevel = u.getAccessLevel();
					room.send("Welcome " + u.getUserName() + ", you have been added as " + BotCommand.getAccessLevelName(u.getAccessLevel())
							+ ", standby executing your command");

				} catch (SQLException e) {
					logger.error("roomEvent(ChatRoom, PingMessageEvent, boolean)", e);
					room.send("Error inserting or updating user @Petter");
				}
			}
		}

		int requiredAccessLevel = bc.getRequiredAccessLevel();
		if (requiredAccessLevel == BotCommand.ACCESS_LEVEL_RO) {
			fr.tunaki.stackoverflow.chat.User user = room.getUser(userId);
			if (!(user.isModerator() || user.isRoomOwner())){
				room.replyTo(event.getMessageId(), "Sorry you need to be the actual room owner or moderator to run this command");
				return;
			}
		}else if (accessLevel < requiredAccessLevel) {
			room.replyTo(event.getMessageId(), "Sorry you need to be " + BotCommand.getAccessLevelName(requiredAccessLevel) + " to run this command (@Petter)");
			return;
		}

		bc.runCommand(room, event);
		if (bc instanceof ShutDownCommand) {
			if (messageLatch != null) {
				messageLatch.countDown();
			} else {
				try {
					close();
					CloseVoteFinder.getInstance().shutDown();
				} catch (Exception e) {
					logger.error("roomEvent() Shutdown error", e);
				}
				System.exit(0);
			}
		}
	}

	public void startDupeHunter() {
		dupeHunter = new DupeHunterComments(this);
		dupeHunter.start();
	}

	public Bot getAiBot() {
		return aiBot;
	}

	public ChatRoom getChatRoom(long id) {
		for (ChatRoom cr : rooms.values()) {
			if (cr.getRoomId() == id) {
				return cr;
			}
		}
		return null;
	}

	public String getRoomName(long id) {
		for (ChatRoom cr : rooms.values()) {
			if (cr.getRoomId() == id) {
				return cr.getRoomName();
			}
		}
		return null;
	}

	public void close() {
		if (dupeHunter != null) {
			dupeHunter.setShutDown(true);
			dupeHunter.interrupt();
		}

		if (client != null) {
			if (logger.isInfoEnabled()) {
				logger.info("closing client");
			}
			client.close();
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		PropertyConfigurator.configure("ini/log4j.properties");

		if (logger.isDebugEnabled()) {
			logger.debug("main(String[]) - start");
		}

		// Load AI interface
		AIMLProcessor.extension = new PCAIMLProcessorExtension();
		MagicStrings.root_path = System.getProperty("user.dir");
		MagicStrings.default_bot_name = BOT_NAME;

		// Load properties file an instance the CloseVoteFinder
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);

		// Start the bot
		CountDownLatch messageLatch = new CountDownLatch(1);
		ChatBot cb = new ChatBot(properties, messageLatch);
		try {
			cb.loginIn();
			// SOCVFinder
			cb.joinRoom("stackoverflow.com", 111347, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS,true);
			// Campagins
			cb.joinRoom("stackoverflow.com", 95290, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS,false);
			// SOCVR Testing Facility
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
			cb.joinRoom("stackoverflow.com", 68414, allowedCommands, ChatRoom.DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM,false);

			cb.startDupeHunter();
			try {
				messageLatch.await();
			} catch (InterruptedException e) {
			}
		} catch (Throwable e) {
			logger.error("main(String[])", e);
		} finally {
			cb.close();
			CloseVoteFinder.getInstance().shutDown();
		}
	}

	public Map<Long, ChatRoom> getRooms() {
		return rooms;
	}
}
