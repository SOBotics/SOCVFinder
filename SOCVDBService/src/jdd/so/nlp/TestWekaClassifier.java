package jdd.so.nlp;

import java.io.BufferedReader;
import java.io.FileReader;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayesMultinomialText;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class TestWekaClassifier {

	public static void main(String[] args) throws Exception {

		BufferedReader reader = new BufferedReader(new FileReader("model/comments.arff"));
		Instances data = new Instances(reader);
		data.setClassIndex(data.numAttributes() - 1);

		reader.close();
		
		
		
	//	Classifier classifier = new J48();
		
		Classifier classifier = new NaiveBayesMultinomialText();
		classifier.buildClassifier(data);
		
		String testMessage = "Spencer, my tone? You sir are political correctness gone mad!";
				
		double[] ret = classifyMessage(classifier, data, testMessage);
		System.out.println(ret[0]+ ":" + ret[1]);
		
		
//		Classifier jbClassifier = new J48();
//      // Filter instance.
//      StringToWordVector m_Filter = new StringToWordVector();
//      m_Filter.setInputFormat(data);
//
//      // Generate word counts from the training data.
//      Instances filteredData  = Filter.useFilter(data, m_Filter);
//
//      // Rebuild classifier.
//      jbClassifier.buildClassifier(filteredData);
//      ret = classifyMessage(jbClassifier,m_Filter, data, testMessage);
//      System.out.println(ret[0]+ ":" + ret[1]);
	
      
	}

	
	 public static double[] classifyMessage(Classifier classifier, Instances trainingData,String message) throws Exception {
         
       

         Instances testset = trainingData.stringFreeStructure();

         // Make message into test instance.
         Instance instance = makeInstance(message, testset);

//         // Filter instance.
//         m_Filter.input(instance);
//         Instance filteredInstance = m_Filter.output();

         // Get index of predicted class value.
         Instance filteredInstance = instance;
         double predicted = classifier.classifyInstance(filteredInstance);

         // Output class value.
         System.err.println("Message classified as : " +
     		       trainingData.classAttribute().value((int)predicted));

         return classifier.distributionForInstance(filteredInstance);

     }

	 public static double[] classifyMessage(Classifier classifier,StringToWordVector m_Filter, Instances trainingData,String message) throws Exception {
         
	       

         Instances testset = trainingData.stringFreeStructure();

         // Make message into test instance.
         Instance instance = makeInstance(message, testset);

//         // Filter instance.
         m_Filter.input(instance);
         Instance filteredInstance = m_Filter.output();

         // Get index of predicted class value.
//         Instance filteredInstance = instance;
         double predicted = classifier.classifyInstance(filteredInstance);

         // Output class value.
         System.err.println("Message classified as : " +
     		       trainingData.classAttribute().value((int)predicted));

         return classifier.distributionForInstance(filteredInstance);

     }

	 
	 
	  /**
	   * Method that converts a text message into an instance.
	   */
	  private static Instance makeInstance(String text, Instances data) {

	    // Create instance of length two.
	    DenseInstance instance = new DenseInstance(2);

	    // Set value for message attribute
	    Attribute messageAtt = data.attribute("text");
	    instance.setValue(messageAtt, messageAtt.addStringValue(text));

	    // Give instance access to attribute information from the dataset.
	    instance.setDataset(data);
	    return instance;
	  }

}
