package jdd.so.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

import jdd.so.api.model.Comment;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayesMultinomialText;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 * Categorize comment
 * 
 * DocumentCategorizerME Code by Technik Empire from this gist
 * https://gist.github.com/TechnikEmpire/4a76aa0b844e15c639f08066655847e7
 * 
 * @author Petter Friberg
 *
 */

public class CommentCategory {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentCategory.class);

	public static double OPEN_NLP_THRESHOLD = .95d;
	public static double WEKA_NB_THRESHOLD = .95d;

	private Pattern regexClassifier;
	private DocumentCategorizerME openNLPClassifier;
	private Classifier wekaClassifier;

	private Instances wekaModel;

	public CommentCategory() throws Exception {
		initModel();
	}

	private void initModel() throws Exception {

		// Regex model
		String regExTest = "(?is)\\b((yo)?u suck|8={3,}D|nigg(a|er)|ass ?hole|kiss my ass|dumbass|fag(got)?|slut|moron|daf[au][qk]|(mother)?fuc?k+(ing?|e?(r|d)| off+| y(ou|e)(rself)?| u+|tard)?|shit(t?er|head)|idiot|dickhead|pedo|whore|(is a )?cunt|cocksucker|ejaculated?|butthurt|(private|pussy) show|lesbo|bitches|suck\\b.{0,20}\\bdick|dee[sz]e? nut[sz])s?\\b|^.{0,250}\\b(shit face)\\b.{0,100}$";
		regexClassifier = Pattern.compile(regExTest);

		// Open NLP classifier
		DoccatModel m = new DoccatModel(new File("model/comments.model"));
		openNLPClassifier = new DocumentCategorizerME(m);

		// Weka NaiveBayes classifier
		BufferedReader reader = new BufferedReader(new FileReader("model/comments.arff"));
		wekaModel = new Instances(reader);
		wekaModel.setClassIndex(wekaModel.numAttributes() - 1);
		reader.close();

		wekaClassifier = new NaiveBayesMultinomialText();
		wekaClassifier.buildClassifier(wekaModel);

	}

	public boolean classifyComment(Comment c) throws Exception {
		String comment = c.getBody();
		
		String classifyText = PreProcesser.preProcessComment(comment, false);

		System.out.println("Classifing text: " + classifyText);
		
		// regex it
		boolean regExHit = isRegexHit(classifyText);
		c.setRegExHit(regExHit);

		// open nlp
		double[] outcomeNlp = classifyMessageOpenNLP(openNLPClassifier, classifyText);
		c.setOpenNlpGood(outcomeNlp[0]);
		c.setOpenNlpBad(outcomeNlp[1]);
		
		
		// weka naive bayes
		double[] outcomeWeka = classifyMessageNaiveBayes(wekaClassifier, wekaModel, classifyText);
		c.setNaiveBayesGood(outcomeWeka[0]);
		c.setNaiveBayesBad(outcomeWeka[1]);
		
		//outcomeNlp[1] > OPEN_NLP_THRESHOLD || disabled for now
		return regExHit ||  outcomeWeka[1] > WEKA_NB_THRESHOLD;

	}

	private boolean isRegexHit(String classifyText) {
		boolean match = regexClassifier.matcher(classifyText).find();
		System.out.println("Regex match: " + match);
		return match;
	}

	public double[] classifyMessageOpenNLP(DocumentCategorizerME classifier, String classifyText) {
		double[] outcomes = classifier.categorize(classifyText);
		String category = classifier.getBestCategory(outcomes);
		System.out.println("Open nlp classified as " + category + " Threshold: bad=" + outcomes[1] + ", good=" + outcomes[0]);
		return outcomes;
	}

	public double[] classifyMessageNaiveBayes(Classifier classifier, Instances trainingData, String message) throws Exception {

		// Create instance for message of length two.
		DenseInstance instance = new DenseInstance(2);
		Instances messageSet = trainingData.stringFreeStructure();

		// Set value for message attribute
		Attribute messageAtt = messageSet.attribute("text");
		instance.setValue(messageAtt, messageAtt.addStringValue(message));
		// Give instance access to attribute information from the dataset.
		instance.setDataset(messageSet);

		double predicted = classifier.classifyInstance(instance);

		double outcomes[] = classifier.distributionForInstance(instance); 
		// Output class value.
		System.out.println("NaivieBayes classified as: " + trainingData.classAttribute().value((int) predicted) + " Threshold: bad=" + outcomes[1] + ", good=" + outcomes[0]);

		return outcomes;

	}

	public double getThresholdBad(String comment) {

		String line = Jsoup.parse(comment).text(); // remove html
		line = line.replaceAll("@(\\S+)?", "").trim(); // remove username
		line = line.replaceAll("[^a-zA-Z\r\n]", " ").trim(); // remove strange
																// chars
		line = line.replaceAll("[ ]{2,}", " "); // remove spaces

		double[] outcomes = openNLPClassifier.categorize(line);
		if (outcomes[0] >= 0.9) {
			Logger.getLogger(LogThresholdHit.class).debug(comment + " | " + outcomes[0] + " | " + outcomes[1]);
		} else {
			Logger.getLogger(LogThresholdNonHit.class).debug(comment + " | " + outcomes[0] + " | " + outcomes[1]);
		}
		double returndouble = outcomes[0];
		return returndouble;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		CommentCategory cc = new CommentCategory();
		Comment c = new Comment();
		c.setBody("You are an asshole!");
		System.out.println(cc.classifyComment(c));
		
		c = new Comment();
		c.setBody("Can you plz clarify your question?");
		System.out.println(cc.classifyComment(c));
		
	}
}
