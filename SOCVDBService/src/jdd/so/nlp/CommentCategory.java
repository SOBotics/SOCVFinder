package jdd.so.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

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
import weka.filters.unsupervised.attribute.StringToWordVector;

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

	public static final double OPEN_NLP_THRESHOLD = .99d;
	public static final double WEKA_NB_THRESHOLD = .99d;
	public static final double WEKA_SMO_THRESHOLD = .99d;
	public static final double WEKA_J48_THRESHOLD = .99d;

	
	private List<Pattern> regexClassifier;
	private DocumentCategorizerME openNLPClassifier;
	private Classifier wekaNBClassifier;
	private Classifier wekaJ48Classifier;
	private Classifier wekaSMOClassifier;

	private Instances wekaARFF;

	private StringToWordVector filter;

	

	
	public CommentCategory() throws Exception {
		initModel();
	}

	private void initModel() throws Exception {

		// Regex model
		//String regExTest = "(?is)\\b((yo)?u suck|8={3,}D|nigg(a|er)|ass ?hole|kiss my ass|dumbass|fag(got)?|slut|moron|daf[au][qk]|(mother)?fuc?k+(ing?|e?(r|d)| off+| y(ou|e)(rself)?| u+|tard)?|shit(t?er|head)|idiot|dickhead|pedo|whore|(is a )?cunt|cocksucker|ejaculated?|butthurt|(private|pussy) show|lesbo|bitches|suck\\b.{0,20}\\bdick|dee[sz]e? nut[sz])s?\\b|^.{0,250}\\b(shit face)\\b.{0,100}$";
		regexClassifier = getRegexs();

		// Open NLP classifier
		DoccatModel m = new DoccatModel(new File("model/open_comments.model"));
		openNLPClassifier = new DocumentCategorizerME(m);

		// Weka NaiveBayes classifier (all comments)
		wekaNBClassifier =   (Classifier)SerializationHelper.read(new FileInputStream("model/nb_comments.model"));

//		// Weka classifer J48
		wekaJ48Classifier =   (Classifier)SerializationHelper.read(new FileInputStream("model/j48_comments.model"));

		
		// Weka only SO comments
		wekaSMOClassifier =   (Classifier)SerializationHelper.read(new FileInputStream("model/smo_comments.model"));
		
		
		
		//This needs to be removed, only used to copy the structure when classifing
		wekaARFF = getInstancesFromARFF("model/comments.arff");
		wekaARFF.setClassIndex(wekaARFF.numAttributes() - 1);
		
		
		ObjectInputStream oin = new ObjectInputStream(new FileInputStream("model/StringToWordVector.filter")); 
        filter = (StringToWordVector) oin.readObject(); 
        oin.close();
        filter.setInputFormat(wekaARFF); 
        
        Instances trainFiltered = Filter.useFilter(wekaARFF, filter); 
        trainFiltered.setClassIndex(0);

	}

	private List<Pattern> getRegexs() throws IOException {
		
		List<Pattern> patterns = new ArrayList<>();
		
		BufferedReader br = new BufferedReader(new FileReader("ini/regex.txt"));
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();
		    line = br.readLine(); //skip first
		    while (line != null) {
		    	if (line.trim().length()>0){
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
		String regExHit = getRegexHit(regexText);
		c.setRegExHit(regExHit);

		String classifyText = PreProcesser.preProcessComment(comment, false);

		System.out.println("Classifing text: " + classifyText);
		
		
//		// open nlp
		double[] outcomeNlp = classifyMessageOpenNLP(openNLPClassifier, classifyText);
		c.setOpenNlpGood(outcomeNlp[0]);
		c.setOpenNlpBad(outcomeNlp[1]);

		Instances instance = createArff(classifyText);
		
		// weka Naive Bayes
		double[] outcomeWeka = classifyMessageNaiveBayes(wekaNBClassifier, wekaARFF, instance);
		c.setNaiveBayesGood(outcomeWeka[0]);
		c.setNaiveBayesBad(outcomeWeka[1]);
		

		// weka J48
		double[] outcomeJ48 = classifyMessageWithFilter(wekaJ48Classifier, wekaARFF, filter,instance);
		c.setJ48Good(outcomeJ48[0]);
		c.setJ48Bad(outcomeJ48[1]);

		
		// weka SMO
		double[] outcomeWekaSMO = classifyMessageWithFilter(wekaSMOClassifier, wekaARFF, filter, instance);
		c.setSMOGood(outcomeWekaSMO[0]);
		c.setSMOBad(outcomeWekaSMO[1]);

		

		
		String logMessage = "\""+ c.getNaiveBayesBad() + "\",\"" + c.getJ48Bad() + "\",\"" + classifyText + "\",\"" + c.getBody() + "\"";
		
		if (c.getNaiveBayesBad()>0.6){
			Logger.getLogger(LogNaiveBayes.class).debug(logMessage);
		}
		
		if (c.getJ48Bad()>0.6){
			Logger.getLogger(LogOpenNLP.class).debug(logMessage);
		}
		
		int score = 0;
		if (regExHit!=null){
			score+=3;
		}
		

		if (outcomeWeka[1] > 0.9){
			score++;
		}
		
		if (outcomeWeka[1] > 0.95){
			score++;
		}
			
		
		if (outcomeWeka[1] > 0.995){
			score++;
		}
		
		if (outcomeJ48[1] > 0.95){
			score++;
		}
		
		if (outcomeWekaSMO[1] > 0.9){
			score++;
		}
		
		if (outcomeNlp[1]>0.95){
			score++;
		}
		
		if (outcomeNlp[1]>0.99){
			score++;
		}
	
		c.setScore(score);
		
		return score;

	}

	private String getRegexHit(String classifyText) {
		String hit = null;
		for (Pattern pattern : regexClassifier) {
			boolean match = pattern.matcher(classifyText).find();
			if (match){
				System.out.println(pattern.toString());
				return pattern.toString();
			}
		}
		return null;
	}

	public double[] classifyMessageOpenNLP(DocumentCategorizerME classifier, String classifyText) {
		double[] outcomes = classifier.categorize(classifyText);
		String category = classifier.getBestCategory(outcomes);
		
		System.out.println("Open nlp classified as " + category + " Threshold: bad=" + outcomes[1] + ", good=" + outcomes[0]);
		return outcomes;
	}

	public double[] classifyMessageNaiveBayes(Classifier classifier, Instances trainingData, Instances instance) throws Exception {

		
		double predicted = classifier.classifyInstance(instance.get(0));

		double outcomes[] = classifier.distributionForInstance(instance.get(0)); 
		// Output class value.
		System.out.println("NaivieBayes classified as: " + trainingData.classAttribute().value((int) predicted) + " Threshold: bad=" + outcomes[1] + ", good=" + outcomes[0]);

		return outcomes;

	}

	public double[] classifyMessageWithFilter(Classifier classifier, Instances trainingData, Filter filter, Instances instance) throws Exception {

		Instances instanceFiltered = Filter.useFilter(instance, filter);
		instanceFiltered.setClassIndex(0);

		double predicted = classifier.classifyInstance(instanceFiltered.get(0));

		double outcomes[] = classifier.distributionForInstance(instanceFiltered.get(0)); 
		// Output class value.
		System.out.println(classifier.getClass().getName() + " classified as: " + trainingData.classAttribute().value((int) predicted) + ": " + predicted + " Threshold: bad=" + outcomes[1] + ", good=" + outcomes[0]);

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
 
        //System.out.println(data.toString()); 
 
        return data; 
    } 
 

	/**
     * Get Instances from ARFF file 
     * 
     * @param fileLocation path to ARFF file 
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
		
		
		CommentCategory cc = new CommentCategory();
		Comment c = new Comment();
		c.setBody("rep whoreing");
		System.out.println(cc.classifyComment(c));
		
		
		c.setBody("Yes sorry it's a mistake but it doesn't solve my problem");
		System.out.println(cc.classifyComment(c));
		
		c.setBody("I'm surprised someone with 115k reputation would give such a terrible answer. This answer is so obviously bad I don't feel like I need to explain why it's bad. A single link, telling someone to just google it? Jesus.");
		System.out.println(cc.classifyComment(c));
		
		cc.classifyComment(c);
		
		
	}
}
