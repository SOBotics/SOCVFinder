package jdd.so.nlp;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
