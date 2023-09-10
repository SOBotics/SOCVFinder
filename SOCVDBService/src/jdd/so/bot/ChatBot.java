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

import org.alicebot.ab.Bot;
import org.alicebot.ab.MagicStrings;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.sobotics.chatexchange.chat.ChatHost;
import org.sobotics.chatexchange.chat.StackExchangeClient;
import org.sobotics.chatexchange.chat.event.EventType;
import org.sobotics.chatexchange.chat.event.MessagePostedEvent;
import org.sobotics.chatexchange.chat.event.PingMessageEvent;

import jdd.so.CloseVoteFinder;
import jdd.so.bot.actions.BotCommand;
import jdd.so.bot.actions.BotCommandsRegistry;
import jdd.so.bot.actions.cmd.AddUserCommand;
import jdd.so.bot.actions.cmd.AiChatCommand;
import jdd.so.bot.actions.cmd.ApiQuotaCommand;
import jdd.so.bot.actions.cmd.CommandsCommand;
import jdd.so.bot.actions.cmd.CommentReportCommand;
import jdd.so.bot.actions.cmd.CommentTestCommand;
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
import jdd.so.dao.UserDAO;
import jdd.so.dao.model.User;
import jdd.so.dup.CommentsController;
import jdd.so.nlp.CommentHeatCategory;

/**
 * The main ChatBot handling the ChatRooms
 *
 * @author Petter Friberg
 */
public class ChatBot {

	public static final String BOT_NAME = "Queen";

	private static final Logger logger = Logger.getLogger(ChatBot.class);

	private StackExchangeClient client;

	private Properties properties;

	private CountDownLatch messageLatch; // Wait before exiting

	private Bot aiBot;

	private Map<Long, ChatRoom> rooms = Collections.synchronizedMap(new HashMap<>());

	private CommentsController commentsController;

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

	public void joinOnlySOCVFinder() {
		// SOCVFinder
		this.joinRoom(ChatHost.STACK_OVERFLOW, 111347, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, true);
		//http://chat.stackoverflow.com/rooms/141300/burnination-progress-for-the-apple-tag
				this.joinRoom(ChatHost.STACK_OVERFLOW, 141300, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
			
	}

	public void joinRooms() {
		// SOCVFinder
		this.joinRoom(ChatHost.STACK_OVERFLOW, 111347, null, ChatRoom.DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM, false);
		// Campagins
		//this.joinRoom(ChatHost.STACK_OVERFLOW, 95290, null, ChatRoom.DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM, false);
		// SOCVR
		this.joinRoom(ChatHost.STACK_OVERFLOW, 41570, getDupeNotificationsOnlyCommands(), ChatRoom.DUPLICATION_NOTIFICATIONS_HAMMER_IN_ROOM, false);
		// GMTs
		this.joinRoom(ChatHost.STACK_OVERFLOW, 75819, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
		// Ruby
		// http://chat.stackoverflow.com/rooms/44914/ruby-sometimes-on-rails
		this.joinRoom(ChatHost.STACK_OVERFLOW, 44914, getDupeNotificationsOnlyCommands(), ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
		// http://chat.stackoverflow.com/rooms/117458/duplicate-posts
		this.joinRoom(ChatHost.STACK_OVERFLOW, 117458, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
		
		
		// http://chat.stackoverflow.com/rooms/98569/bin-bash
		this.joinRoom(ChatHost.STACK_OVERFLOW, 98569, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
		// http://chat.stackoverflow.com/rooms/25767/regex-regular-expressions
		this.joinRoom(ChatHost.STACK_OVERFLOW, 25767, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);

		//http://chat.stackoverflow.com/rooms/127924/apache-spark
		this.joinRoom(ChatHost.STACK_OVERFLOW, 127924, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);

		
		//https://chat.stackoverflow.com/rooms/167908/sobotics-workshop
		this.joinRoom(ChatHost.STACK_OVERFLOW, 167908, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
		
		//https://chat.stackoverflow.com/rooms/174431/burnination-progress-for-the-ibm-tag
		this.joinRoom(ChatHost.STACK_OVERFLOW, 174431, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
		
		// R Collective
		this.joinRoom(ChatHost.STACK_OVERFLOW, 252171, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
		
		/**
		 * REQUERST LIMIT
		 */
		//http://chat.stackoverflow.com/rooms/108192/room-for-bhargav-rao-and-tunaki
		//this.joinRoom(ChatHost.STACK_OVERFLOW, 108192, null, ChatRoom.DUPLICATION_NOTIFICATIONS_TAGS, false);
				
	}

	public ChatRoom getSOCVFinderRoom() {
		return rooms.get(111347L);
	}

	public ChatRoom getSOCVRRoom() {
		return rooms.get(41570L);
	}

	private List<Class<? extends BotCommand>> getDupeNotificationsOnlyCommands() {
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
		allowedCommands.add(CommentReportCommand.class);
		allowedCommands.add(CommentTestCommand.class);
		allowedCommands.add(AiChatCommand.class);
		allowedCommands.add(ShutDownCommand.class);
		allowedCommands.add(RoomLeaveCommand.class);
		return allowedCommands;
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
	public boolean joinRoom(ChatHost domain, int roomId, List<Class<? extends BotCommand>> allowedCommands, int dupNotifyStrategy, boolean enableAi) {

		int batchNumber = CloseVoteFinder.getInstance().getBatchNumber(roomId);

		ChatRoom room;
		try {
			room = new ChatRoom(this, client.joinRoom(domain, roomId), batchNumber, allowedCommands, dupNotifyStrategy, enableAi);
		} catch (Throwable e) {
			logger.error("joinRoom() - Could not join room: " + roomId, e);
			return false;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("joinRoom(String, int) - Client joined room: " + roomId);
		}

		room.getRoom().addEventListener(EventType.MESSAGE_REPLY, event -> roomEvent(room, event, true));
		room.getRoom().addEventListener(EventType.USER_MENTIONED, event -> roomEvent(room, event, false));
		room.getRoom().addEventListener(EventType.MESSAGE_POSTED, event -> roomEventAlive(room, event));

		long id = room.getRoomId();
		rooms.put(id, room);

		return id != 0;
	}

	private void roomEventAlive(ChatRoom room, MessagePostedEvent event) {
		String cnt = event.getMessage().getContent();
		if (cnt != null && cnt.toLowerCase().startsWith("@bots")) {
			room.send("[Alive and kicking](https://www.youtube.com/watch?v=ljIQo1OHkTI)");
		}

		if (cnt != null && cnt.length() > 0) {
			int cp = Character.codePointAt(cnt, 0);
			if (cp == 128642 || (cp>=128644 && cp<=128650)) {
				room.send("[🚃](https://www.youtube.com/watch?v=joLLEFvFaCA)");
			}
//			if (cnt.toLowerCase().contains("feeds @petter")){
//				room.send("@Kyll and @BhargavRao, table is prepared, remember to first wash your hands");
//				
//			}
			
//			if (cnt.toLowerCase().contains("prayer")){
//				room.send("O Lord my @Kyll, I now, at this moment, readily and willingly accept at @BhargavRao hands whatever kind of death it may please You to send me, will all its pains, penalties and sorrows. Amen.");
//			}
		}

	}

	protected void roomEvent(ChatRoom room, PingMessageEvent event, boolean isReply) {
		if (logger.isDebugEnabled()) {
			logger.debug("roomEvent(ChatRoom, PingMessageEvent, boolean) - Incomming message: " + event.toString());
		}

		if (event.getMessage().getEditCount() > 0) {
			// long parentId = event.getParentMessageId();
			return; // Ignore edits for now
		}

		BotCommand bc = BotCommandsRegistry.getInstance().getCommand(event.getMessage(), isReply, event.getMessage().getEditCount());
		if (logger.isDebugEnabled()) {
			logger.debug("roomEvent(ChatRoom, PingMessageEvent, boolean) - " + bc);
		}

		// Check if command is allowed in room
		if (!room.isAllowed(bc)) {
			// Dont' respond
			return;
		}

		if (CloseVoteFinder.getInstance().getUsers() == null) {
			room.replyTo(event.getMessage().getId(), "The bot has not been initialized correctly and can not execute commands");
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

		if (bc instanceof AiChatCommand) {
			//disable with AI
//			if (room.increment(userId)) {
//				room.send("That's enough, I'm tired of this");
//				return;
//			}
		}

		User u = CloseVoteFinder.getInstance().getUsers().get(userId);

		if (u != null) {
			accessLevel = u.getAccessLevel();
		} else {
			org.sobotics.chatexchange.chat.User user = room.getUser(userId);

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
			org.sobotics.chatexchange.chat.User user = room.getUser(userId);
			if (!(user.isModerator() || user.isRoomOwner())) {
				room.replyTo(event.getMessage().getId(), "Sorry you need to be the actual room owner or moderator to run this command");
				return;
			}
		} else if (accessLevel < requiredAccessLevel) {
			room.replyTo(event.getMessage().getId(),
					"Sorry you need to be " + BotCommand.getAccessLevelName(requiredAccessLevel) + " to run this command (@Petter)");
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
		commentsController = new CommentsController(this);
		commentsController.start();
	}

	public CommentHeatCategory getCommentCategory() {
		if (commentsController != null) {
			return commentsController.getCommentHeatCategory();
		}
		return null;
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
		if (commentsController != null) {
			commentsController.setShutDown(true);
			commentsController.interrupt();
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

		if (logger.isInfoEnabled()) {
			logger.info("main(String[]) - start");
		}

		
		
		// Load AI interface
//		AIMLProcessor.extension = new PCAIMLProcessorExtension();
//		MagicStrings.root_path = System.getProperty("user.dir");
//		MagicStrings.default_bot_name = BOT_NAME;

		// Load properties file an instance the CloseVoteFinder
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);

		//Redunda service
		if (logger.isInfoEnabled()) {
			logger.info("Starting redunda ping service");
		}
//		PingService redunda = new PingService("b2f12d074632a1d9b2f55c3955326cf2c44b6d0f2210717bb467b18006161f91", CloseVoteFinder.VERSION);
//		redunda.start();
//		
		// Start the bot
		CountDownLatch messageLatch = new CountDownLatch(1);
		ChatBot cb = new ChatBot(properties, messageLatch);
		try {
			cb.loginIn();
//			cb.joinRooms();
			cb.joinOnlySOCVFinder();
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

	public CommentsController getCommentsController() {
		return commentsController;
	}
}
