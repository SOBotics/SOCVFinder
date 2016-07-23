package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.CommentDAO;

public abstract class CommentResponseAbstract extends BotCommand {

	public long getCommentId(String c){
		String match = "#comment";
		int startPos = c.lastIndexOf(match);
		String qId = c.substring(startPos + match.length(), c.indexOf('_', startPos + match.length()));
		return Long.parseLong(qId);
	}
	
	public void saveToDatabase(long commentId, boolean confirmed) throws SQLException{
		if (confirmed){
			new CommentDAO().tpComment(CloseVoteFinder.getInstance().getConnection(), commentId);
		}else{
			new CommentDAO().fpComment(CloseVoteFinder.getInstance().getConnection(), commentId);
		}
	}
	
	protected String getEdit(PingMessageEvent event, String content, boolean confirm) {
		String cmd = " - tp";
		if (!confirm){
			cmd = " - fp";
		}
		
		return cmd;
	}
	
}
