package jdd.so.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;

/**
 * Pre processer
 * 
 * @author Petter Friberg
 *
 */
public class PreProcesser {
	
	/**
	 * According to
	 * http://www.cs.cmu.edu/~lingwang/papers/sp250-xiang.pdf, page 2
	 * 
	 * 
	 * 1. Removed non-English?  (using LingPipe [1] with Hadoop).
	   2. Remove the shortened URLs.
	   3. Remove @username
	   4. In tweets remove removed all hashtags from the tweets	
	   5.   To tackle the problem of intentional repetitions, we designed
			a heuristic to condense 3 or more than 3 repetitive letters into
			a single letter, e.g., hhhheeeello to hello.
	   6. For sequences of 2 repetitive letters, we counted how many
		  such sequences each word in a tweet has, and condensed each
		  such sequence into a single letter if the number of such sequences
		  is over a threshold1
		  For example, yyeeaahh will be reduced to yeah, while committee remains intact.
	   7. We removed all stopwords.
	    8. We defined a word to be a sequence of letters, - or ’, and removed all tokens not satisfying this requirement.
	    
	     SO related 
	     
	     9. Remove code
	     10. Remove html
	 
	 */
	public static String preProcessComment(String comment, boolean tweet){
		//1. 
		if (comment==null || isNonEnglish(comment)){
			return null;
		}
		String result = comment;
		
		//9. remove <code>sys</code> 
		result = removeCodeBlocks(result);
	
		//10 remove html
		result = removeHtml(result);
		
		//3 Remove username
		result = removeUserNames(result);
		
		//2 Remove the shortened URLs (URL's in general)
		result = removeUrls(result);
		
		//4. Remove hashtags
		result = removeHastags(result);
		
		if (tweet){
			//Remove RT
			result = result.replaceAll("RT", "");
		}
		
		//5-6. Remove intentional repetitions
		result = removeIntentionalRepetitions(result);
		
		//7. Remove stop words
		result = removeStopWords(result);
		
		//8. Remove chars not in sentance
		result = removeNonSentanceChars(result);
		
		//Remove all double space or more 
		result = removeDoubleSpaces(result);
		
		//Upper case what do we do with this?
		
		//Repeated words?
		
		return result;
	}

	
	private static String removeDoubleSpaces(String result) {
		return result.replaceAll("[ ]{2,}", " ").trim();
	}


	private static String removeNonSentanceChars(String result) {
		//Valid chars are a-zA-z ' ’ - 
		return result.replaceAll("[^a-zA-Z'’,.?!\\- \r\n]", "");
	}


	private static String removeStopWords(String result) {
		// TODO Implement was does this mean?
		return result;
	}


	private static String removeIntentionalRepetitions(String result) {
		// TODO Improve implementation
		//1 first if 3 letter or more
		//2 if word contains 2 letters in sequenze and is over threshold
		return result.replaceAll("(.)\\1{2,}", "$1");
	}


	private static String removeHastags(String result) {
		// Remove only the hastag for now, maybe if tweet remove also text
		return result.replaceAll("#", "");
	}


	private static String removeUrls(String result) {
		return result.replaceAll("((https?|ftp|gopher|telnet|file|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", "");
	}


	private static String removeUserNames(String result) {
		return result.replaceAll("@(\\S+)?", "");
	}


	private static String removeHtml(String result) {
		return Jsoup.parse(result).text();
	}


	private static boolean isNonEnglish(String comment) {
		// TODO Implement (can also be used to notify on non english comments)
		return false;
	}
	
	private static String removeCodeBlocks(String comment) {
		return comment.replaceAll("<code>(.+?)</code>", "");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		
		System.out.println(removeIntentionalRepetitions("Teeeest thissssssss"));
	
		//testComments("dev/model_comments_good.txt",2000);
		
		testComments("dev/model_comments_bad.txt",2000);
		
		//testTweets("dev/twitter-hate-speech-processed.csv",2000);	
	}


	private static void testComments(String fileName, long readTime) throws IOException, InterruptedException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
		    String line = br.readLine();
		    while (line != null) {
		        System.out.println(line);
		        System.out.println(preProcessComment(line, false));
		        System.out.println();
		        line = br.readLine();
		        Thread.sleep(readTime);
		    }
		} finally {
		    br.close();
		}
	}
	
	private static void testTweets(String fileName, long readTime) throws IOException, InterruptedException{
		CSVParser parser = CSVParser.parse(new File(fileName), Charset.forName("Cp1252"), CSVFormat.DEFAULT);
		for (CSVRecord r : parser) {
			String classif = r.get(0);
			if (classif.equalsIgnoreCase("The tweet is not offensive")){
				continue;
			}
			String line = r.get(2);
			System.out.println(line);
	        System.out.println(preProcessComment(line, true));
	        System.out.println();
	        Thread.sleep(readTime);
		}
		
	}
	
	
	
}
