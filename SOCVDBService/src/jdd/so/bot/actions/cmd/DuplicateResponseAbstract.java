package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.DuplicateResponseDAO;
import jdd.so.dao.model.DuplicateResponse;

public abstract class DuplicateResponseAbstract extends BotCommand {

	public long getQuestionId(String c){
		String match = "stackoverflow.com/questions/";
		int startPos = c.lastIndexOf(match);
		String qId = c.substring(startPos + match.length(), c.indexOf(')', startPos + match.length()));
		return Long.parseLong(qId);
	}
	
	public void saveToDatabase(long questionId, long userId, long roomId, boolean confirmed) throws SQLException{
		DuplicateResponse dr = new DuplicateResponse(questionId,userId,roomId,confirmed,null,System.currentTimeMillis()/1000L);
		new DuplicateResponseDAO().insertOrUpdate(CloseVoteFinder.getInstance().getConnection(), dr);
		
	}
	
	protected String getEdit(PingMessageEvent event, String content, boolean confirm) {
		String cmd = "k";
		if (!confirm){
			cmd = "f";
		}
		
		String edit = content;
		if (edit.contains("@")) {
			edit = edit.substring(0, edit.indexOf('@')).trim();
		}
		if (edit.contains("--- f") || edit.contains("--- k")) {
			edit += ", k" + event.getUserName();
		} else {
			int lastTag = edit.lastIndexOf("[tag:");
			int closeTag = edit.indexOf(']', lastTag);
			if (closeTag > 0) {
				edit = edit.substring(0, closeTag + 2) + " ---" + edit.substring(closeTag + 2, edit.length()).trim() + "--- " + cmd + " by " + event.getUserName();
			}
		}
		return edit;
	}
	
}
