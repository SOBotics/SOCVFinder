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
	
	private List<Pattern> regexTypo;
	private List<Pattern> regexOffSite;

	
	
	public CommentCloseCategory() throws Exception {
		initModel();
	}

	public static String getDescription(int closeType) {
		switch(closeType){
		case HIT_TYPO:
			return "possibile-typo";
		case HIT_OFF_SITE:
			return "possibile-offsite";
		default:
			return "possibile-unkown";
		}
	}
	
	private void initModel() throws Exception {

		// Regex model
		regexTypo = getRegexs("ini/regex_typo.txt");
		regexOffSite = getRegexs("ini/regex_off_site.txt");
	
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
		String regExHit = getRegexHit(regexText, regexTypo);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_TYPO;
		} 
		
		regExHit = getRegexHit(regexText, regexOffSite);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return HIT_OFF_SITE;
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
		c.setBody("@CZoellner Fuck you you nigga");
		System.out.println(cc.classifyComment(c));
		System.out.println("Score: " + c.getScore());

		c.setBody(
				"Your conclusion has been as clear as wrong since the beginning. Since you make a statement and then refuse to properly argue your position, but instead attempt to blame others for your own actions like a child. I suggest you go back to your reviewing hobby, junior");
		System.out.println(cc.classifyComment(c));
		System.out.println("Score: " + c.getScore());

		c.setBody("be-nice");
		System.out.println(cc.classifyComment(c));
		System.out.println("Score: " + c.getScore());

		// cc.classifyComment(c);

	}

	
}
