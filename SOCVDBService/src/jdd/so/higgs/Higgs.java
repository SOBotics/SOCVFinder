package jdd.so.higgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.threeten.bp.Instant;
import org.threeten.bp.OffsetDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;

import io.swagger.client.ApiException;
import io.swagger.client.Configuration;
import io.swagger.client.api.BotApi;
import io.swagger.client.model.AquireTokenRequest;
import io.swagger.client.model.AquireTokenResponse;
import io.swagger.client.model.RegisterPostReason;
import io.swagger.client.model.RegisterPostRequest;
import io.swagger.client.model.RegisterUserFeedbackRequest;
import jdd.so.api.model.Comment;
import jdd.so.nlp.CommentHeatCategory;
import jdd.so.nlp.PreProcesser;

/**
 * Report to Higgs web dashboard
 * @author Petter Friberg
 *
 */
public class Higgs {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(Higgs.class);

	private static Higgs instance;
	private String url;
	private BotApi botApi;
	
	//Private constructor (since instance)
	private Higgs(String url, String key) throws ApiException {
		super();
		this.url = url;
		initHiggs(key);
	}

	/**
	 * Start instance
	 * @param url
	 * @param secret key
	 * @throws ApiException
	 */
	public static void initInstance(String url, String key) throws ApiException {
		instance = new Higgs(url, key);
	}

	public static Higgs getInstance() throws ApiException {
		if (instance == null) {
			throw new ApiException("Higgs has not been instanced");
		}
		return instance;
	}


	/**
	 * Autentication
	 * @param key
	 * @throws ApiException
	 */
	private void initHiggs(String key) throws ApiException {
		botApi = new BotApi(Configuration.getDefaultApiClient().setBasePath(this.url));
		AquireTokenRequest atr = new AquireTokenRequest();
		atr.dashboardId(1);
		atr.setSecret(key);
		AquireTokenResponse token = botApi.botAquireTokenPost(atr);
		Configuration.getDefaultApiClient().setAccessToken(token.getToken());
		if (logger.isDebugEnabled()) {
			logger.debug("initHiggs(String) - BotApi has been succesfull instanzed");
		}

	}

	/**
	 * RegisterPost (comment)
	 * @param comment
	 * @return
	 * @throws ApiException
	 */
	public Integer registrerComment(Comment comment) throws ApiException {
		if (logger.isInfoEnabled()) {
			logger.info("registrerComment(Comment) - start");
		}

		RegisterPostRequest rpr = new RegisterPostRequest();
		rpr.authorName(comment.getDisplayName());
		rpr.authorReputation(((Long) comment.getReputation()).intValue());
		String title = Jsoup.parse(comment.getBody()).text();
		if (title.length()>80){
			int index = title.indexOf(' ',60);
			if (index>0 && index<80){
				title = title.substring(0,index);	
			}else{
				title = title.substring(0,80);
			}
		}
		if (title.length()<=1){
			title = "Empty";
		}
		rpr.setTitle(title);
		rpr.setContentId(((Long) comment.getCommentId()).intValue());
		rpr.setContent(Jsoup.parse(comment.getBody()).text());
		rpr.setContentSite("stackoverflow.com");
		rpr.setContentType("comment");
		rpr.setContentUrl(comment.getLink());
		rpr.setDetectionScore((double) comment.getScore());
		
		Instant detected = Instant.ofEpochSecond(System.currentTimeMillis()/1000);		
		rpr.setDetectedDate(OffsetDateTime.ofInstant(detected, ZoneId.systemDefault()));
		
		Instant cDate = Instant.ofEpochSecond(comment.getCreationDate());		
		rpr.setContentCreationDate(org.threeten.bp.OffsetDateTime.ofInstant(cDate, ZoneOffset.UTC));

		List<RegisterPostReason> reasons = new ArrayList<>();

		if (comment.isRegExHit()) {
			
			String regexType = "REGEX: ";
			switch (comment.getRegExHitValue()){
			case CommentHeatCategory.REGEX_HIT_LOW:
				regexType+="LOW";
				break;
			case CommentHeatCategory.REGEX_HIT_MEDIUM:
				regexType+="MEDIUM";
				break;
			case CommentHeatCategory.REGEX_HIT_HIGH:
				regexType+="HIGH";
				break;
			default:
				regexType+="NONE";
			}
			RegisterPostReason r1 = new RegisterPostReason();
			r1.setReasonName(regexType);
			r1.setTripped(true);
			r1.setConfidence((double)comment.getRegExHitValue());
			reasons.add(r1);

			if (comment.getRegExHit() != null) {
				RegisterPostReason r2 = new RegisterPostReason();
				r2.setReasonName(comment.getRegExHit());
				r2.setTripped(true);
				r2.setConfidence(1.0d);
				reasons.add(r2);
			}
		}
		
		reasons.add(getRegisterPostReason("NaiveBayes",comment.getNaiveBayesBad(),CommentHeatCategory.WEKA_NB_THRESHOLD));
		reasons.add(getRegisterPostReason("OpenNLP",comment.getOpenNlpBad(),CommentHeatCategory.OPEN_NLP_THRESHOLD));
		if (comment.getPerspectiveResult()!=null){
			reasons.add(getRegisterPostReason("Perspective",comment.getPerspectiveResult().getScore(),CommentHeatCategory.PERSPECTIVE_THRESHOLD));
		}

		rpr.setReasons(reasons);

		// allowed probably remove in future.
		rpr.setAllowedFeedback(Arrays.asList(new String[] { FeedBack.TP.toString(),FeedBack.FN.toString(), FeedBack.FP.toString(), FeedBack.NC.toString(), FeedBack.SK.toString()}));
		Integer higgsId = botApi.botRegisterPostPost(rpr);
		if (logger.isInfoEnabled()) {
			logger.info("registrerComment(Comment) higgsId=" + higgsId + " - end");
		}
		return higgsId;
	}
	
	/**
	 * Send feedback
	 * @param reportId (id return from registrer)
	 * @param userId
	 * @param feedBack
	 * @throws ApiException
	 */
	public void sendFeedBack(int reportId, int userId, FeedBack feedBack) throws ApiException{
		if (logger.isInfoEnabled()) {
			logger.info("sendFeedBack(int, int, FeedBack) - start");
		}

		RegisterUserFeedbackRequest rufr = new RegisterUserFeedbackRequest();
		rufr.setReportId(reportId);
		rufr.setFeedback(feedBack.toString());
		rufr.setUserId(userId);
		botApi.botRegisterUserFeedbackPost(rufr);

		if (logger.isInfoEnabled()) {
			logger.info("sendFeedBack(int, int, FeedBack) - end");
		}
	}

	private RegisterPostReason getRegisterPostReason(String filter, double confidence, double threshold) {
		RegisterPostReason r = new RegisterPostReason();
		r.setReasonName(filter);
		r.setTripped(confidence>=threshold);
		r.setConfidence(confidence);
		return r;
	}
}
