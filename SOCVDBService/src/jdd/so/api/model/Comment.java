package jdd.so.api.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A comment on questiton
 * 
 * @author Petter Friberg
 *
 */
public class Comment {

	private long creationDate;
	private long userId;
	private long reputation;
	private String body;
	private int score;
	private String duplicateTargetTitle;
	private int duplicateTargetScore;

	// only when searching on comments
	private long commentId;
	private long postId;
	private String postType;
	private String link;

	public static Comment getComment(JSONObject json) throws JSONException {
		Comment c = new Comment();
		c.setBody(json.getString("body"));
		c.setScore(json.getInt("score"));
		c.setCreationDate(json.getLong("creation_date"));
		if (json.has("comment_id")) {
			c.setCommentId(json.getLong("comment_id"));
		}
		if (json.has("post_id")) {
			c.setPostId(json.getLong("post_id"));
		}
		if (json.has("post_type")) {
			c.setPostType(json.getString("post_type"));
		}
		if (json.has("link")) {
			c.setLink(json.getString("link"));
		}
		if (json.has("owner")) {
			JSONObject o = json.getJSONObject("owner");
			if (o.has("user_id")) {
				c.setUserId(o.getLong("user_id"));
			}
			if (o.has("reputation")) {
				c.setReputation(o.getLong("reputation"));
			}
		}
		return c;
	}

	public JSONObject getJSONObject() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("creation_date", creationDate);
		json.put("user_id", userId);
		json.put("reputation", reputation);
		json.put("body", body);
		json.put("score", score);
		long dupQuestionId = getDuplicateQuestionID();
		if (dupQuestionId > 0) {
			json.put("duplicated_target_id", dupQuestionId);
		}
		if (duplicateTargetTitle != null) {
			json.put("duplicated_target_title", duplicateTargetTitle);
			json.put("duplicated_target_score", duplicateTargetScore);
		}

		return json;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getReputation() {
		return reputation;
	}

	public void setReputation(long reputation) {
		this.reputation = reputation;
	}

	public boolean isPossibleDuplicateComment() {
		return body != null && (body.toLowerCase().contains("possible duplicate") || body.toLowerCase().contains("duplicate of"));
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public String getDuplicateTargetTitle() {
		return duplicateTargetTitle;
	}

	public void setDuplicateTargetTitle(String duplicateTargetTitle) {
		this.duplicateTargetTitle = duplicateTargetTitle;
	}

	public int getDuplicateTargetScore() {
		return duplicateTargetScore;
	}

	public void setDuplicateTargetScore(int duplicateTargetScore) {
		this.duplicateTargetScore = duplicateTargetScore;
	}

	public long getDuplicateQuestionID() {
		if (body == null) {
			return 0L;
		}
		String soLink = "stackoverflow.com/questions/";

		int pos = body.indexOf(soLink);
		if (pos > 0) {
			int startPos = pos + soLink.length();
			int endPos = body.indexOf('/', startPos);
			String qi = body.substring(startPos, endPos);
			try {
				return Long.parseLong(qi);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	public long getPostId() {
		return postId;
	}

	public void setPostId(long postId) {
		this.postId = postId;
	}

	public String getPostType() {
		return postType;
	}

	public void setPostType(String postType) {
		this.postType = postType;
	}

	@Override
	public boolean equals(Object o2) {
		if (o2 instanceof Comment) {
			long dif = this.getCreationDate() - ((Comment) o2).getCreationDate();
			if (dif == 0) {
				return this.getUserId() == ((Comment) o2).getUserId();
			}
		}
		return false;
	}

	public long getCommentId() {
		return commentId;
	}

	public void setCommentId(long commentId) {
		this.commentId = commentId;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}
}
