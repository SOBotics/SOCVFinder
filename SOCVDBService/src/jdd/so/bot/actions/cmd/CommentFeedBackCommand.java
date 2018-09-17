package jdd.so.bot.actions.cmd;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.sobotics.chatexchange.chat.Message;
import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.api.model.Comment;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.CommentDAO;
import jdd.so.higgs.FeedBack;
import jdd.so.nlp.CommentHeatCategory;
import jdd.so.nlp.PreProcesser;

public class CommentFeedBackCommand extends CommentResponseAbstract {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentFeedBackCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(\\b" + FeedBack.TP.toString() + "\\b)" + "|(\\b" + FeedBack.FP.toString() + "\\b)" + "|(\\b" + FeedBack.NC.toString() + "\\b)" + "|(\\b"
				+ FeedBack.SK.toString() + "\\b)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_REVIEWER;
	}

	@Override
	public String getCommandName() {
		return "Feedback on heat detector command";
	}

	@Override
	public String getCommandDescription() {
		return "Report that comment is tp=True postive, fp=False postive, nc=non constructive, sk=Skip";
	}

	@Override
	public String getCommandUsage() {
		return "tp|fp|nc|sk";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {

		String c;
		boolean isFeedback = false;
		if (event.getMessage().getPlainContent().toLowerCase().contains("feedback")) {
			isFeedback = true;
			c = event.getMessage().getPlainContent();
		} else {
			long parentMessage = event.getParentMessageId();
			Message pdm = room.getRoom().getMessage(parentMessage);
			if (pdm == null) {
				room.replyTo(event.getMessage().getId(), "Could not find message your are replying to");
				return;
			}
			c = pdm.getPlainContent();
		}
		if (!c.contains("#comment")) {
			room.replyTo(event.getMessage().getId(),
					"I could not find comment link #comment, it needs to be included in message or you should reply to message containing it.");
			return;
		}

		long commentId;
		try {
			commentId = getCommentId(c);
		} catch (RuntimeException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessage().getId(), "Sorry could not retrive comment id");
			return;
		}

		FeedBack feedBackType = null;

		String cmd = event.getMessage().getPlainContent();

		if (cmd.matches("(?i)(.*\\b" + FeedBack.TP.toString() + "\\b.*)")) {
			feedBackType = FeedBack.TP;
		} else if (cmd.matches("(?i)(.*\\b" + FeedBack.FP.toString() + "\\b.*)")) {
			feedBackType = FeedBack.FP;
		} else if (cmd.matches("(?i)(.*\\b" + FeedBack.NC.toString() + "\\b).*")) {
			feedBackType = FeedBack.NC;
		} else if (cmd.matches("(?i)(.*\\b" + FeedBack.SK.toString() + "\\b.*)")) {
			feedBackType = FeedBack.SK;
		}

		if (feedBackType == null) {
			room.replyTo(event.getMessage().getId(), "You hit feedback on comment but this is not right.. @petter ");
			return;
		}

		int higgsId;
		try {
			higgsId = new CommentDAO().getHiggsReportId(CloseVoteFinder.getInstance().getConnection(), commentId);
		} catch (SQLException e1) {
			room.replyTo(event.getMessage().getId(), "Database error searching for HiggsId .. @petter ");
			return;
		}
		if (higgsId <= 0) {
//			if (feedBackType==FeedBack.TP){
//				new CommentReportCommand().runCommand(room, event);
//			}
			return;
		}

		sendFeedbackToHiggs(higgsId, event.getUserId(), feedBackType);

		if (feedBackType == FeedBack.TP) {
			try {
				saveToDatabase(commentId, true);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (event.getMessage().getPlainContent().toUpperCase().contains("SOCVR")) {
				sendToSOCVR(room, event, commentId, higgsId);
			}
		}

		if (feedBackType == FeedBack.FP) {
			try {
				saveToDatabase(commentId, false);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		
		long parentMessage = event.getParentMessageId();
		Message pdm = room.getRoom().getMessage(parentMessage);
		if (pdm == null) {
			return;
		}
		c = pdm.getPlainContent();
		if (!c.contains("#comment")){
			return;
		}
		String edit = PreProcesser.removeUserNames(c) + " -" + feedBackType.toString();
		room.edit(event.getParentMessageId(), edit).handleAsync((mId, thr) -> {
			return mId;
		});
	}

	public void sendToSOCVR(ChatRoom room, PingMessageEvent event, long commentId, int higgsId) {
		Comment c = getCommentFromApi(commentId);
		c.setHiggsReportId(higgsId);
		CommentHeatCategory cc = room.getBot().getCommentCategory();
		if (c != null && cc != null) {
			try {
				cc.classifyComment(c);
				StringBuilder message = room.getBot().getCommentsController().getHeatMessageResult(c, c.getLink());
				message.append(" Confirmed by: ").append(event.getUserName());
				room.getBot().getSOCVRRoom().send(message.toString());
			} catch (Exception e) {
				logger.error("confirm(ChatRoom, PingMessageEvent, String)", e);
			}
		} else {
			room.replyTo(event.getMessage().getId(), "Sorry, could not retrive comment from api, maybe already deleted?");
		}
	}

	public static void main(String[] args) {
		String cmd = "@que tp ";
		FeedBack feedBackType = null;
		if (cmd.matches("(?i)(.*\\b" + FeedBack.TP.toString() + "\\b.*)")) {
			feedBackType = FeedBack.TP;
		} else if (cmd.matches("(?i)(\\b" + FeedBack.FP.toString() + "\\b)")) {
			feedBackType = FeedBack.FP;
		} else if (cmd.matches("(?i)(\\b" + FeedBack.NC.toString() + "\\b)")) {
			feedBackType = FeedBack.NC;
		} else if (cmd.matches("(?i)(\\b" + FeedBack.SK.toString() + "\\b)")) {
			feedBackType = FeedBack.SK;
		}

		System.out.println(feedBackType);
	}
}
