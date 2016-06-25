package jdd.so.nlp;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jsoup.Jsoup;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

/**
 * This class is to process file and create model for the nlp programs
 * somoe code is based on TechnikEmpire's CategoryCatTrainer
 * 
 * @author Petter Friberg
 *
 */
public class ModelCreator {
	
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
	public static String preProcessComment(String comment, boolean isTweet){
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
		
		//5-6. Remove intentional repetitions
		result = removeIntentionalRepetitions(result);
		
		//7. Remove stop words
		result = removeStopWords(result);
		
		//8. Remove chars not in sentance
		result = removeNonSentanceChars(result);
		
		//Remove all double space or more 
		result = removeDoubleSpaces(result);
		
		
		return result;
	}

	
	private static String removeDoubleSpaces(String result) {
		return result.replaceAll("[ ]{2,}", " ").trim();
	}


	private static String removeNonSentanceChars(String result) {
		//Valid chars are a-zA-z ' ’ - 
		return result.replaceAll("[^a-zA-Z'’\\- \r\n]", "");
	}


	private static String removeStopWords(String result) {
		// TODO Implement was does this mean?
		return result;
	}


	private static String removeIntentionalRepetitions(String result) {
		// TODO To implement
		//1 first if 3 letter or more
		//2 if word contains 2 letters in sequenze and is over threshold
		return result;
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

	public static void main(String[] args) {
		DoccatModel model = null;

		InputStream dataIn = null;
		try {
			// Note that train file format is "CAT_NAME some sample data" - One
			// entry per line.
			dataIn = new FileInputStream("PATH_TO_TRAIN_FILE\\CommentsTrainingModel.txt");
			ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
			ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

			TrainingParameters params = TrainingParameters.defaultParams();
			System.out.println(params.algorithm());
			
//			TrainingParameters params = new TrainingParameters();
//			params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(0));
//			params.put(TrainingParameters.ALGORITHM_PARAM, NaiveBayesTraine.NAIVE_BAYES_VALUE);
//

			// params.put(TrainingParameters.ALGORITHM_PARAM, "PERCEPTRON");
			// You have to tweak cutoff and iterations and test what works best.
			// Cutoff is how many times something
			// has to be appear to be noticed.
			params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(0));
			params.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(4000));

			model = DocumentCategorizerME.train("en", sampleStream, params);

			OutputStream modelOut = null;
			try {
				modelOut = new BufferedOutputStream(new FileOutputStream("PATH_TO_OUTPUT\\CommentsTrainingModel.model"));
				model.serialize(modelOut);
			} catch (IOException e) {
				// Failed to save model
				e.printStackTrace();
			} finally {
				if (modelOut != null) {
					try {
						modelOut.close();
					} catch (IOException e) {
						// Failed to correctly save model.
						// Written model might be invalid.
						e.printStackTrace();
					}
				}
			}

		} catch (IOException e) {
			// Failed to read or parse training data, training failed
			e.printStackTrace();
		} finally {
			if (dataIn != null) {
				try {
					dataIn.close();
				} catch (IOException e) {
					// Not an issue, training already finished.
					// The exception should be logged and investigated
					// if part of a production system.
					e.printStackTrace();
				}
			}
		}

	}

}
