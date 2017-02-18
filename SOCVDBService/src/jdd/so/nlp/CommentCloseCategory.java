package jdd.so.nlp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import jdd.so.api.model.Comment;

/**
 * Categorize comment
 * 
 * @author Petter Friberg
 *
 */

public class CommentCloseCategory {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentCloseCategory.class);

	public static final int HIT_NONE = 0;
	public static final int HIT_TYPO = 1;
	public static final int HIT_OFF_SITE = 2;
	public static final int HIT_OFF_TOPIC = 3;
	public static final int HIT_OPINION = 4;
	public static final int HIT_UNCLEAR = 5;
	public static final int HIT_TOO_BROAD = 6;
	public static final int HIT_MCVE = 7;
	
	private List<Pattern> regexTypo;
	private List<Pattern> regexOffSite;
	private List<Pattern> regexOffTopic;
	private List<Pattern> regexOpinion;
	private List<Pattern> regexUnclear;
	private List<Pattern> regexTooBroad;
	private List<Pattern> regexMCVE;

	
	public CommentCloseCategory() throws Exception {
		initModel();
	}

	public static String getDescription(int closeType) {
		switch(closeType){
		case HIT_TYPO:
			return "possible-typo";
		case HIT_OFF_SITE:
			return "possible-offsite";
		case HIT_OFF_TOPIC:
			return "possible-off-topic";
		case HIT_OPINION:
			return "possible-opinion-based";
		case HIT_UNCLEAR:
			return "possible-unclear";
		case HIT_TOO_BROAD:
			return "possible-too-broad";
		case HIT_MCVE:
			return "possible-mcve";
					
		default:
			return "possible-unknown";
		}
	}
	
	private void initModel() throws Exception {

		// Regex model
		regexTypo = getRegexs("ini/regex_typo.txt");
		regexOffSite = getRegexs("ini/regex_off_site.txt");
		regexOffTopic= getRegexs("ini/regex_offtopic.txt");
		regexOpinion = getRegexs("ini/regex_opinion.txt");
		regexTooBroad = getRegexs("ini/regex_toobroad.txt");
		regexUnclear = getRegexs("ini/regex_unclear.txt");
		regexMCVE= getRegexs("ini/regex_mcve.txt");
		
	}

	private List<Pattern> getRegexs(String fileName) throws IOException {

		List<Pattern> patterns = new ArrayList<>();

		BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
			String line = br.readLine();
			line = br.readLine(); // skip first
			while (line != null) {
				if (line.trim().length() > 0) {
					Pattern p = Pattern.compile(line);
					patterns.add(p);
				}
				line = br.readLine();
			}
		} finally {
			br.close();
		}
		return patterns;
	}

	public synchronized int classifyComment(Comment c) throws Exception {
		String comment = c.getBody();

		
		String regexText = PreProcesser.preProccesForRegex(comment);
		// regex it
		
		
		String regExHit = getRegexHit(regexText, regexOffSite);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_OFF_SITE;
		} 
		
		regExHit = getRegexHit(regexText, regexOffTopic);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_OFF_TOPIC;
		}
		
		regExHit = getRegexHit(regexText, regexOpinion);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_OPINION;
		}
		
		regExHit = getRegexHit(regexText, regexTooBroad);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_TOO_BROAD;
		}
		
		regExHit = getRegexHit(regexText, regexTypo);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_TYPO;
		}
		
		regExHit = getRegexHit(regexText, regexUnclear);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_UNCLEAR;
		} 
		
		
		regExHit = getRegexHit(regexText, regexMCVE);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_MCVE;
		} 
		return 0;

	}

	private String getRegexHit(String classifyText, List<Pattern> regexClassifier) {
		for (Pattern pattern : regexClassifier) {
			boolean match = pattern.matcher(classifyText).find();
			if (match) {
				//System.out.println(pattern.toString());
				return pattern.toString();
			}
		}
		return null;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		CommentCloseCategory cc = new CommentCloseCategory();
		Comment c = new Comment();
		c.setBody("opinion- based question");
		System.out.println(cc.classifyComment(c));
		

	}

	
}
