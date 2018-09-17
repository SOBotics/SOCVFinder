package jdd.so.bot.actions.cmd;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONException;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import io.swagger.client.ApiException;
import jdd.so.CloseVoteFinder;
import jdd.so.api.ApiHandler;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Comment;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.CommentDAO;
import jdd.so.higgs.FeedBack;
import jdd.so.higgs.Higgs;

public abstract class CommentResponseAbstract extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentResponseAbstract.class);

	public long getCommentId(String c){
		String match = "#comment";
		int startPos = c.lastIndexOf(match);
		String qId = c.substring(startPos + match.length(), c.indexOf('_', startPos + match.length()));
		return Long.parseLong(qId);
	}
	
	public Comment getCommentFromApi(long id){
		ApiHandler api = new ApiHandler();
		ApiResult ar;
		try {
			ar = api.getComments(String.valueOf(id));
			
		} catch (JSONException | IOException e1) {
			e1.printStackTrace();
			return null;
		}
		
		if (ar.getComments().isEmpty()){
			return null;
		}
		
		return ar.getComments().get(0);

	}
	
	public void sendFeedbackToHiggs(int higgsId,long userId, FeedBack feedBackType){
		
		try {
			if (higgsId>0){
				logger.info("sendFeedbackToHiggs() - userId: " + userId + " feedback: " + feedBackType  );
				Higgs.getInstance().sendFeedBack(higgsId,((Long)userId).intValue()	, feedBackType);
			}
		} catch (ApiException e) {
			logger.error("sendFeedbackToHiggs(long, long, FeedBack)", e);
		}

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
