package jdd.so.nlp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import jdd.so.CloseVoteFinder;
import jdd.so.api.model.Comment;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
/**
 * Categorize comment
 * 
 * @author Petter Friberg
 *
 */

public class CommentHeatCategory {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentHeatCategory.class);

	public static final double OPEN_NLP_THRESHOLD = .998d;
	public static final double WEKA_NB_THRESHOLD = .995d;
	public static final double PERSPECTIVE_THRESHOLD = .85d;
	
	public static final double OPEN_NLP_MIN= .2d;
	public static final double WEKA_NB_MIN = .2d;
	public static final double PERSPECTIVE_MIN = .2d;
	
	public static final double OPEN_NLP_NO= .05d;
	public static final double WEKA_NB_NO = .05d;
	public static final double PERSPECTIVE_NO = .05d;
	
	public static final int REGEX_HIT_NONE= 0;
	public static final int REGEX_HIT_LOW = 1;
	public static final int REGEX_HIT_MEDIUM = 2;
	public static final int REGEX_HIT_HIGH = 3;

	private Perspective perspective;
	private List<Pattern> regexClassifierHighScore;
	private List<Pattern> regexClassifierMediumScore;
	private List<Pattern> regexClassifierLowScore;
	private DocumentCategorizerME openNLPClassifier;
	private Classifier wekaNBClassifier;


	private Instances wekaARFF;

	// private StringToWordVector filter;

	public CommentHeatCategory() throws Exception {
		perspective = new Perspective();
		initModel();
	}

	private void initModel() throws Exception {

		// Regex model
		regexClassifierHighScore = getRegexs("ini/regex_high_score.txt");
		regexClassifierMediumScore = getRegexs("ini/regex_medium_score.txt");
		regexClassifierLowScore = getRegexs("ini/regex_low_score.txt");

		// Open NLP classifier
		long timer = System.currentTimeMillis();
		
		DoccatModel m = new DoccatModel(new File("model/open_comments.model"));
		openNLPClassifier = new DocumentCategorizerME(m);
		
		System.out.println("OpenNLP Time to load: " + (System.currentTimeMillis() - timer));
		//System.out.println(getSize((Serializable) openNLPClassifier));

		timer = System.currentTimeMillis();
		
		// Weka NaiveBayes classifier
		wekaNBClassifier = (Classifier) SerializationHelper.read(new FileInputStream("model/nb_comments.model"));

		System.out.println("Weka Time to load: " + (System.currentTimeMillis() - timer));
		
		//System.out.println(getSize((Serializable) wekaNBClassifier));
		
		// This needs to be removed, only used to copy the structure when
		// classifing
		wekaARFF = getInstancesFromARFF("model/comments.arff");
		wekaARFF.setClassIndex(wekaARFF.numAttributes() - 1);

	}
	
	public static long getSize(Serializable ser){
		 ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 ObjectOutputStream oos=null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(ser);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		 return baos.size();
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

		/**
		 * Call perspective
		 */

		JSONObject jp = c.getJSONPerspective();

		String cp = PreProcesser.removeHtml(comment);
		


		JSONObject response = perspective.getResponse(jp, cp);
		if (response != null) {
			PerspectiveResult result;
			try {
				result = perspective.getResult(response, cp);
				c.setPerspectiveResult(result);
				if (logger.isDebugEnabled()) {
					logger.debug("Perspective  - " + result);
				}
			} catch (Exception e) {
				logger.error("classifyComment(Comment)", e);
			}
		}


		String classifyText = PreProcesser.preProcessComment(comment, false);

		Instances instance = createArff(classifyText);

		// weka Naive Bayes
		double[] outcomeWeka = classifyMessageNaiveBayes(wekaNBClassifier, wekaARFF, instance);
		c.setNaiveBayesGood(outcomeWeka[0]);
		c.setNaiveBayesBad(outcomeWeka[1]);


		// // open nlp
		double[] outcomeNlp = classifyMessageOpenNLP(openNLPClassifier, classifyText);
		c.setOpenNlpGood(outcomeNlp[0]);
		c.setOpenNlpBad(outcomeNlp[1]);

		String regexText = PreProcesser.preProccesForRegex(comment);
		// regex it
		int regexHitValue = REGEX_HIT_NONE;
		String regExHit = getRegexHit(regexText, regexClassifierHighScore);
		if (regExHit != null) {
			regexHitValue = REGEX_HIT_HIGH;
		} else {
			regExHit = getRegexHit(regexText, regexClassifierMediumScore);
			if (regExHit != null) {
				regexHitValue = REGEX_HIT_MEDIUM;
			} else {
				regExHit = getRegexHit(regexText, regexClassifierLowScore);
				if (regExHit!=null){
					regexHitValue = REGEX_HIT_LOW;
				}
			}
		}
		c.setRegExHit(regExHit);
		c.setRegExHitValue(regexHitValue);

		double pScore = 0d;
		if (c.getPerspectiveResult()!=null){
			pScore = c.getPerspectiveResult().getScore();
		}
		
		c.setScore(getScore(outcomeWeka[1], outcomeNlp[1], pScore, regexHitValue));
		
		logger.info("s:" + c.getScore() + " (" + outcomeWeka[1] + ", " + outcomeNlp[1] +  ", " + pScore + ", " + regExHit +")" + " - " + PreProcesser.removeHtml(c.getBody()));

		return c.getScore();

	}

	public int getScore(double nbWeka, double openNlp, double perspective, int regExHitType) {
		int score = 0;
		// Weka naive bayes
		if (nbWeka > 0.95) {
			score++;
			if (nbWeka > 0.99) {
				score++;
				if (nbWeka > 0.999) {
					score++;
				}
			}
		}
		// Open nlp
		if (openNlp > 0.95) {
			score++;
			if (openNlp > 0.99) {
				score++;
			}
		}

		// Perspective
		if (perspective > 0.7) {
			score++;
			if (perspective > 0.8) {
				score++;
				if (perspective > 0.9) {
					score++;
				}
			}
		}
		
		//Regex
		if (regExHitType>0){
			score +=1 +regExHitType;
		}
		
		if (score>10){
			score = 10;
		}else if (score<6) {
			//Auto report:
			if (nbWeka > WEKA_NB_THRESHOLD && openNlp> OPEN_NLP_MIN && perspective>PERSPECTIVE_MIN){
				score = 6;
			}
			if (openNlp > OPEN_NLP_THRESHOLD && nbWeka>WEKA_NB_MIN && perspective>PERSPECTIVE_MIN){
				score = 6;
			}
			if (perspective > PERSPECTIVE_THRESHOLD && (nbWeka>WEKA_NB_NO||openNlp>OPEN_NLP_NO)){
				score = 6;
			}
			if (regExHitType==REGEX_HIT_HIGH){
				score = 6;
			}
		}
		
		return score;
	}

	private String getRegexHit(String classifyText, List<Pattern> regexClassifier) {
		for (Pattern pattern : regexClassifier) {
			boolean match = pattern.matcher(classifyText).find();
			if (match) {
				// System.out.println(pattern.toString());
				return pattern.toString();
			}
		}
		return null;
	}

	public double[] classifyMessageOpenNLP(DocumentCategorizerME classifier, String classifyText) {
		double[] outcomes = classifier.categorize(classifyText);
		String category = classifier.getBestCategory(outcomes);

		//System.out.println("Open nlp classified as " + category + " Threshold: bad=" + outcomes[1] + ", good=" + outcomes[0]);
		return outcomes;
	}

	public double[] classifyMessageNaiveBayes(Classifier classifier, Instances trainingData, Instances instance) throws Exception {

		double predicted = classifier.classifyInstance(instance.get(0));

		double outcomes[] = classifier.distributionForInstance(instance.get(0));
		// Output class value.
		//System.out.println("NaivieBayes classified as: " + trainingData.classAttribute().value((int) predicted) + " Threshold: bad=" + outcomes[1] + ", good="
		//		+ outcomes[0]);

		return outcomes;

	}

	public double[] classifyMessageSGD(Classifier classifier, Instances trainingData, Instances instance) throws Exception {

		double predicted = classifier.classifyInstance(instance.get(0));

		double outcomes[] = classifier.distributionForInstance(instance.get(0));
		// Output class value.
		System.out.println(
				"SGD classified as: " + trainingData.classAttribute().value((int) predicted) + " Threshold: bad=" + outcomes[1] + ", good=" + outcomes[0]);

		return outcomes;

	}

	public double[] classifyMessageWithFilter(Classifier classifier, Instances trainingData, Filter filter, Instances instance) throws Exception {

		Instances instanceFiltered = Filter.useFilter(instance, filter);
		instanceFiltered.setClassIndex(0);

		double predicted = classifier.classifyInstance(instanceFiltered.get(0));

		double outcomes[] = classifier.distributionForInstance(instanceFiltered.get(0));
		// Output class value.
		System.out.println(classifier.getClass().getName() + " classified as: " + trainingData.classAttribute().value((int) predicted) + ": " + predicted
				+ " Threshold: bad=" + outcomes[1] + ", good=" + outcomes[0]);

		return outcomes;

	}

	public Instances createArff(String comment) {

		ArrayList<Attribute> atts = new ArrayList<Attribute>();
		ArrayList<String> classes = new ArrayList<String>();
		classes.add("good");
		classes.add("bad");

		atts.add(new Attribute("text", (ArrayList<String>) null));

		// make sure that the name of the class attribute is unlikely to
		// clash with any attribute created via the StringToWordVector filter
		atts.add(new Attribute("@@class@@", classes));
		Instances data = new Instances("weka_SO_comments_model", atts, 0);
		data.setClassIndex(data.numAttributes() - 1);

		DenseInstance instance = new DenseInstance(2);
		Attribute messageAtt = data.attribute("text");
		instance.setValue(messageAtt, messageAtt.addStringValue(comment));

		data.add(instance);

		// System.out.println(data.toString());

		return data;
	}

	/**
	 * Get Instances from ARFF file
	 * 
	 * @param fileLocation
	 *            path to ARFF file
	 * @return Instances of given ARFF file
	 */
	private Instances getInstancesFromARFF(String fileLocation) {
		Instances instances = null;
		try {
			DataSource dataSource = new DataSource(fileLocation);
			instances = dataSource.getDataSet();
		} catch (Exception ex) {
			System.out.println("Can't find ARFF file at given location: " + fileLocation);
		}

		return instances;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);
		
		CommentHeatCategory cc = new CommentHeatCategory();
		Comment c = new Comment();
		c.setBody("@CZoellner Fuck you you nigga");
		System.out.println(cc.classifyComment(c));
		System.out.println("Score: " + c.getScore());

		c.setBody(
				"Your conclusion has been as clear as wrong since the beginning. Since you make a statement and then refuse to properly argue your position, but instead attempt to blame others for your own actions like a child. I suggest you go back to your reviewing hobby, junior");
		System.out.println(cc.classifyComment(c));
		System.out.println("Score: " + c.getScore());

		c.setBody("@DanielH I consider myself a newbie on the standard library and tbh imho if one doesnt know how to use a vector or <code>std::find</code> then its time to learn it. There is nothing weird or obscure in the code. If you dont confront beginners with something just because they might not know it, then you dont give them the chance to learn it");
		System.out.println(cc.classifyComment(c));
		System.out.println("Score: " + c.getScore());

		// cc.classifyComment(c);

	}

}
