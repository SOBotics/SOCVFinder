package jdd.so.bot.actions.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.parser.Parser;

import org.sobotics.chatexchange.chat.event.PingMessageEvent;
import jdd.so.api.ApiHandler;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Comment;
import jdd.so.api.model.Question;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.nlp.CommentCloseCategory;

public class SearchCommentForCV extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(RoomTagRemove.class);


	public SearchCommentForCV() {
		super();
	}

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(search)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_RO;
	}

	@Override
	public String getCommandName() {
		return "Search comments to cv";
	}

	@Override
	public String getCommandDescription() {
		return "Search for question to cv via comments";
	}

	@Override
	public String getCommandUsage() {
		return "search [tag] #maxQuestion";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String tag = getTags(event.getMessage());
		long messageId = event.getMessage().getId();
		if (tag.length() == 0 || tag.contains(";")) {
			room.replyTo(messageId, "Please indicate 1 tag and only 1 tag to add");
			return;
		}

		int reportNr = 10;
		int maxApi = 10;
		if (event.getMessage().getPlainContent().contains("burn")){
			maxApi = 30;
		}
		ApiHandler api = new ApiHandler(true);
		
		try {
			ApiResult result = api.getQuestions(0L, 0L, maxApi, tag, false);
			List<Question> questions = result.getQuestions();
			int nr = 1;
			String questionId = "";
			// we go in block 100 by 100
			Map<Long,String> titles = new HashMap<>();
			List<Comment> closeQuestions = new ArrayList<>();
			for (Question q : questions) {
				if (q.getCloseVoteCount() > 0 || q.isClosed()) {
					continue;
				}

				if (!questionId.isEmpty()) {
					questionId += ";";
				}
				questionId += Long.toString(q.getQuestionId());
				titles.put(q.getQuestionId(), q.getTitle());
				nr++;
				if (nr >= 99) {
					closeQuestions.addAll(getOffTopicComments(room,titles, questionId));
					nr = 1;
					questionId = "";
				}
			}
			if (!questionId.isEmpty()) {
				closeQuestions.addAll(getOffTopicComments(room,titles, questionId));
			}
			room.send(questions.size() + " question scanned, " + closeQuestions.size() + " found, reporting " + Math.min(closeQuestions.size(), reportNr));
			for (Comment c : closeQuestions) {
				reportComment(room, tag, c);
			}
			
			
		} catch (Exception e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.send("Error while querying API: " + e.getMessage());
		}

	}

	private List<Comment> getOffTopicComments(ChatRoom room, Map<Long,String> titles,String questionId) throws Exception {
		List<Comment> retList = new ArrayList<>();
		ApiHandler api = new ApiHandler();
		ApiResult result = api.getComments(questionId, 20);
		List<Comment> comments = result.getComments();
		for (Comment c : comments) {
			String title = titles.get(c.getPostId());
			if (title==null){
				continue;
			}
			int type = room.getBot().getCommentsController().classifyCloseComment(room, c);
			if (type != CommentCloseCategory.HIT_NONE) {
				c.setQuestionTitle(title);
				retList.add(c);
			}
			
		}
		return retList;

	}
	
	private void reportComment(ChatRoom room,String tag, Comment c){
		String typeDescr = CommentCloseCategory.getDescription(c.getCloseType());
		String message = "[ [SOCVFinder](https://www.youtube.com/watch?v=wauzrPn0cfg) ] [tag:" + typeDescr + "] [tag:" + tag + "] ["
				+ getSanitizedTitle(c.getQuestionTitle()) + "](//stackoverflow.com/q/" + c.getPostId() + ")";
		room.send(message);
	}

	private String getSanitizedTitle(String title) {
		return Parser.unescapeEntities(title, false).replaceAll("(\\[|\\]|_|\\*|`)", "\\\\$1").trim();
	}
}
