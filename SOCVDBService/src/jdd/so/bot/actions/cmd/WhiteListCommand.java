package jdd.so.bot.actions.cmd;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.WhitelistDAO;
import jdd.so.dao.model.WhiteList;

public class WhiteListCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(WhiteListCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(wl|white-list|white list)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_HAMMER;
	}

	@Override
	public String getCommandName() {
		return "White list question";
	}

	@Override
	public String getCommandDescription() {
		return "Whitelist one or mulitple questions";
	}

	@Override
	public String getCommandUsage() {
		return "wl <question_id>*";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String message = event.getMessage().getContent();
		//which separator is used;
		String sep=" ";
		if (message.contains(",")){
			sep = ",";
		}else if (message.contains(";")){
			sep= ";";
		}
		
		String[] qlist = message.split(sep);
		List<Long> questions = new ArrayList<>();
		for (String qid : qlist) {
			try {
				long id = Long.parseLong(qid.replaceAll("[^0-9]", ""));
				questions.add(id);
			} catch (NumberFormatException e) {
				// just ignore
			}
		}
		
		long messageId = event.getMessage().getId();
		if (questions.isEmpty()){
			room.replyTo(messageId, "Could not find any questions ids in your command use space, comma or ; as separator");
			return;
		}
		
		WhitelistDAO wldao = new WhitelistDAO();
		String result = "";
		String retSep = "";
		for (Long questionId : questions) {
			try {
				wldao.insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), new WhiteList(questionId, event.getUserId(), new Date().getTime() / 1000L));
				CloseVoteFinder.getInstance().getWhiteList().add(questionId);
				result+=retSep + String.valueOf(questionId);
				retSep = sep;
			} catch (SQLException e) {
				logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
				room.replyTo(messageId, "Error while trying to white list, check stack trace @Petter");
				return;
			}
		}
		room.replyTo(messageId, "Thank you, the following questions have been whitelisted " + result);
		
	}

}
