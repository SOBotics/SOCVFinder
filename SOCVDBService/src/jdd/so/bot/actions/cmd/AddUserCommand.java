package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.actions.BotCommand;
import jdd.so.db.UserDAO;
import jdd.so.model.User;

public class AddUserCommand extends BotCommand {

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(add user)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "Add new user";
	}

	@Override
	public String getCommandDescription() {
		return "Adds users and set access level";
	}

	@Override
	public String getCommandUsage() {
		return "add user <id_user> <name> <access_level>";
	}

	@Override
	public void runCommand(Room room, PingMessageEvent event) {
		String message = event.getContent();
		String data = message.substring(message.toLowerCase().indexOf("user") + 4, message.length());
		String[] cmdArray = data.trim().split(" ");
		if (cmdArray.length < 3) {
			room.replyTo(event.getMessageId(), "Incorrect command usage: " + getCommandUsage());
			return;
		}
		long idUser = 0;
		try {
			idUser = Long.parseLong(cmdArray[0]);
		} catch (NumberFormatException e) {
			room.replyTo(event.getMessageId(), "User id is not an int usage: " + getCommandUsage());
			return;
		}
		int accessLevel=0;
		try {
			accessLevel = Integer.parseInt(cmdArray[cmdArray.length - 1]);
		} catch (NumberFormatException e) {
			room.replyTo(event.getMessageId(), "Access level is not an int usage: " + getCommandUsage());
			return;
		}
		if (accessLevel<0 || accessLevel>3){
			room.replyTo(event.getMessageId(), "Access level should be between 0-2");
			return;
		}
		
		User ua = CloseVoteFinder.getInstance().getUsers().get(event.getUserId());
		if (accessLevel>ua.getAccessLevel()){
			room.replyTo(event.getMessageId(), "Sorry you can only add users with same access level as yours: " + ua.getAccessLevel() + ": " + BotCommand.getAccessLevelName(ua.getAccessLevel()));
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
			room.replyTo(event.getMessageId(), "No user name has been specified: " + getCommandUsage());
			return;
		}
		
		UserDAO dao = new UserDAO();
		User u = new User(idUser, userName,accessLevel);
		try {
			dao.insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), u);
			String action = "added";
			if (CloseVoteFinder.getInstance().getUsers().containsKey(u.getUserId())){
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
