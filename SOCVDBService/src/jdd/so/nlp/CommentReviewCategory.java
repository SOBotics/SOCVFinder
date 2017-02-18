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

public class CommentReviewCategory {

	private List<Pattern> regexReview;

	public CommentReviewCategory() throws Exception {
		initModel();
	}

	public static String getDescription(int closeType) {
		return "review-comment";
		
	}

	private void initModel() throws Exception {

		// Regex model
		regexReview = getRegexs("ini/review.txt");

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

	public synchronized boolean classifyComment(Comment c) throws Exception {
		String comment = c.getBody();

		String regexText = PreProcesser.preProccesForRegex(comment);
		// regex it
		String regExHit = getRegexHit(regexText, regexReview);
		if (regExHit != null) {
			c.setRegExHit(regExHit);
			return true;
		}

	
		return false;

	}

	private String getRegexHit(String classifyText, List<Pattern> regexClassifier) {
		for (Pattern pattern : regexClassifier) {
			boolean match = pattern.matcher(classifyText).find();
			if (match) {
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

		CommentReviewCategory cc = new CommentReviewCategory();
		Comment c = new Comment();
		c.setBody("review");
		System.out.println(cc.classifyComment(c));

	}

}
