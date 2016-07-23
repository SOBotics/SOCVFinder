package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.UserDAO;
import jdd.so.dao.model.User;

public class AddUserCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(add user|update user)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_TAG_OWNER;
	}

	@Override
	public String getCommandName() {
		return "Add new user";
	}

	@Override
	public String getCommandDescription() {
		return "User >3k are added automatically, this cmd can add or update manually a users";
	}

	@Override
	public String getCommandUsage() {
		return "add user [id_user] [name] <access_level>";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String message = event.getMessage().getContent();
		String data = message.substring(message.toLowerCase().indexOf("user") + 4, message.length());
		String[] cmdArray = data.trim().split(" ");
		long messageId = event.getMessage().getId();
		if (cmdArray.length < 3) {
			room.replyTo(messageId, "Incorrect command usage: " + getCommandUsage());
			return;
		}
		long idUser = 0;
		try {
			idUser = Long.parseLong(cmdArray[0]);
		} catch (NumberFormatException e) {
			room.replyTo(messageId, "User id is not an integer usage: " + getCommandUsage());
			return;
		}
		User userAdding = CloseVoteFinder.getInstance().getUsers().get(event.getUserId());
		
		User userToAdd = CloseVoteFinder.getInstance().getUsers().get(idUser);
		
		if (userToAdd!=null && userToAdd.getAccessLevel()>=userAdding.getAccessLevel() && (userToAdd.getUserId()!=userAdding.getUserId())){
			room.replyTo(messageId, "You can not modify user " + userToAdd.getUserName() + " with same or higher access level");
			return;
		}
		
		int accessLevel=0;
		try {
			accessLevel = Integer.parseInt(cmdArray[cmdArray.length - 1]);
		} catch (NumberFormatException e) {
			room.replyTo(messageId, "Access level is not an int usage: " + getCommandUsage());
			return;
		}
		
		
		
		if (accessLevel<BotCommand.ACCESS_LEVEL_NONE || accessLevel>BotCommand.ACCESS_LEVEL_BOT_OWNER){
			room.replyTo(messageId, "Access level should be between 0-" + BotCommand.ACCESS_LEVEL_BOT_OWNER);
			return;
		}
		
		if (accessLevel>userAdding.getAccessLevel()){
			room.replyTo(messageId, "Sorry you can only add users with same access level as yours: " + userAdding.getAccessLevel() + ": " + BotCommand.getAccessLevelName(userAdding.getAccessLevel()));
			return;
		}
		
		String userName = "";
		for (int i = 1; i < cmdArray.length - 1; i++) {
			if (cmdArray[i].trim().length() == 0) {
				continue;
			}
			if (userName.length() > 0) {
				userName += " ";
			}
			userName += cmdArray[i];
		}
		if (userName.length()==0){
			room.replyTo(messageId, "No user name has been specified: " + getCommandUsage());
			return;
		}
		
		UserDAO dao = new UserDAO();
		User u = new User(idUser, userName,accessLevel);
		try {
			dao.insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), u);
			String action = "added";
			if (userToAdd!=null){
				action="updated";
			}
			CloseVoteFinder.getInstance().getUsers().put(u.getUserId(),u);
			room.send("User: ("  +idUser + ") " + userName + " as " + getAccessLevelName(accessLevel) + " " + action);

		} catch (SQLException e) {
			e.printStackTrace();
			room.send("Error inserting or updating user: " + e.getMessage());
		}
		
		
	}

}
