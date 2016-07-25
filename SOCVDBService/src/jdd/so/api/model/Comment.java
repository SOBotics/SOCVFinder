package jdd.so.api.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import jdd.so.CloseVoteFinder;
import jdd.so.api.ApiHandler;

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
	
	
	//Classification attribute
	private String regExHit;
	private double j48Good;
	private double j48Bad;
	private double naiveBayesGood;
	private double naiveBayesBad;
	
	private double smoGood;
	private double smoBad;
	
	private double openNlpGood;
	private double openNlpBad;
	
	
	
	//Data feedback
	private boolean deleted;
	private boolean flaggedBad;
	private boolean flaggedGood;

	
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
		return body != null && ((body.toLowerCase().contains("possible duplicate") || body.toLowerCase().contains("duplicate of")) && getDuplicateQuestionID()>0);
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
	
	public static String clean(Comment c){
		String line = c.getBody();
		
		line = line.replaceAll("<code>(.+?)</code>", "").trim(); //remove code
		//Split on [?!.]+(\s|\z) then check if spaces?
		
		line = Jsoup.parse(line).text(); //remove html
		line = line.replaceAll("@(\\S+)?", "").trim(); //remove username
		line = line.replaceAll("[ ]{2,}", " "); //remove spaces
		return line;
	}
	
	public static void main(String[] args) throws JSONException, IOException {
		PropertyConfigurator.configure("ini/log4j.properties");
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -120);
		
		PrintWriter writerNormal = new PrintWriter("dev/model_comments_normal.txt", "UTF-8");
		PrintWriter writerGood = new PrintWriter("dev/model_comments_good.txt", "UTF-8");
		

		ApiHandler handler = new ApiHandler();
		ApiResult res = handler.getComments(cal.getTimeInMillis()/1000L, 30, true);
		List<Comment> comment = res.getComments();
		for (Comment c : comment) {
			if (c.getScore()>0){
				writerGood.println(c.getBody());
				
			}else{
				writerNormal.println(c.getBody());
			}
		}
		writerGood.close();
		writerNormal.close();
		
	}

	public boolean isRegExHit() {
		return regExHit!=null;
	}
	
	public String getRegExHit() {
		return this.regExHit;
	}

	public void setRegExHit(String regExHit) {
		this.regExHit = regExHit;
	}

	public double getJ48Good() {
		return j48Good;
	}

	public void setJ48Good(double j48Good) {
		this.j48Good = j48Good;
	}

	public double getJ48Bad() {
		return j48Bad;
	}

	public void setJ48Bad(double j48Bad) {
		this.j48Bad = j48Bad;
	}

	public double getNaiveBayesGood() {
		return naiveBayesGood;
	}

	public void setNaiveBayesGood(double naiveBayesGood) {
		this.naiveBayesGood = naiveBayesGood;
	}

	public double getNaiveBayesBad() {
		return naiveBayesBad;
	}

	public void setNaiveBayesBad(double naiveBayesBad) {
		this.naiveBayesBad = naiveBayesBad;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isFlaggedBad() {
		return flaggedBad;
	}

	public void setFlaggedBad(boolean flaggedBad) {
		this.flaggedBad = flaggedBad;
	}

	public boolean isFlaggedGood() {
		return flaggedGood;
	}

	public void setFlaggedGood(boolean flaggedGood) {
		this.flaggedGood = flaggedGood;
	}

	public double getSmoGood() {
		return smoGood;
	}

	public void setSMOGood(double smoGood) {
		this.smoGood = smoGood;
	}

	public double getSmoBad() {
		return smoBad;
	}

	public void setSMOBad(double smoBad) {
		this.smoBad = smoBad;
	}

	public double getOpenNlpGood() {
		return openNlpGood;
	}

	public void setOpenNlpGood(double openNlpGood) {
		this.openNlpGood = openNlpGood;
	}

	public double getOpenNlpBad() {
		return openNlpBad;
	}

	public void setOpenNlpBad(double openNlpBad) {
		this.openNlpBad = openNlpBad;
	}
	
	
}
