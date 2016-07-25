package jdd.so.nlp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 * This class is to process file and create model for the nlp programs somoe
 * code is based on TechnikEmpire's CategoryCatTrainer
 * 
 * @author Petter Friberg
 *
 */
public class ModelCreator {

	
	public static final int MAX_COMMENTS = 1000;
	public static final String MODEL_FOLDER = "model_so";
	public static final String TRANING_FOLDER = "training_v2";
	public static void main(String[] args) throws IOException {
		


		// Weka arff model
		Instances dataSet = getWekaDataSet();
		BufferedWriter writer = new BufferedWriter(new FileWriter(MODEL_FOLDER + "/comments.arff"));
		writer.write(dataSet.toString());
		writer.flush();
		writer.close();

		// Open nlp model
		File traningFileNlp = new File(MODEL_FOLDER + "/openNPLTraining.txt");
		setupNLPTraning(traningFileNlp);

		DoccatModel model = getNLPModel(traningFileNlp);
		OutputStream modelOut = null;
		try {
			modelOut = new BufferedOutputStream(new FileOutputStream(MODEL_FOLDER + "/open_comments.model"));
			model.serialize(modelOut);
		} catch (IOException e) {
			// Failed to save model
			e.printStackTrace();
		} finally {
			if (modelOut != null) {
				modelOut.close();
			}
		}

	}

	public static void setupNLPTraning(File file) throws IOException {
		System.out.println("Creating nlp file");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		ArrayList<String> classes = new ArrayList<String>();
		classes.add("good");
		classes.add("bad");
		int max = MAX_COMMENTS;
		for (int c = 0; c < classes.size(); c++) {
			int nrInClass = 0;
			String cls = classes.get(c);
			File dir = new File(TRANING_FOLDER + "/" + cls);
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				System.out.println("Reading file: " + files[i].getName() + " " + nrInClass);
				try {

					BufferedReader br = new BufferedReader(new FileReader(files[i]));
					String line = br.readLine();
					while (line != null && nrInClass <= max) {
						String comment = PreProcesser.preProcessComment(line, files[i].getName().contains("tweet"));
						if (comment != null && comment.trim().length() > 2) {
							writer.write(cls + " " + comment);
							writer.newLine();
							nrInClass++;
						}
						line = br.readLine();

					}
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (nrInClass >= max) {
					break;
				}
			}

			max = nrInClass;
		}
		writer.close();

	}

	public static Instances getWekaDataSet() throws IOException {

		System.out.println("Creating weka file");

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

		int max = MAX_COMMENTS;
		for (int c = 0; c < classes.size(); c++) {
			int nrInClass = 0;
			String cls = classes.get(c);

			File dir = new File(TRANING_FOLDER + "/" + cls);
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				System.out.println("Reading file: " + files[i].getName()+ " " + nrInClass);

				try {

					BufferedReader br = new BufferedReader(new FileReader(files[i]));
					String line = br.readLine();
					while (line != null && nrInClass < max) {
						String comment = PreProcesser.preProcessComment(line, files[i].getName().contains("tweet"));
						if (comment != null && comment.trim().length() > 2) {
							double[] newInst = new double[2];
							newInst[0] = (double) data.attribute(0).addStringValue(comment);
							newInst[1] = c;
							data.add(new DenseInstance(1.0, newInst));
							nrInClass++;
						}
						line = br.readLine();
					}
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (nrInClass >= max) {
					break;
				}

			}
			max = nrInClass;
		}

		return data;

	}

	public static DoccatModel getNLPModel(File openNLPTraining) throws IOException {
		DoccatModel model = null;
		InputStream dataIn = null;
		try {
			// Note that train file format is "CAT_NAME some sample data" - One
			// entry per line.
			dataIn = new FileInputStream(openNLPTraining);
			ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
			ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

			TrainingParameters params = TrainingParameters.defaultParams();
			System.out.println(params.algorithm());

			// TrainingParameters params = new TrainingParameters();
			// params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(0));
			// params.put(TrainingParameters.ALGORITHM_PARAM,
			// NaiveBayesTraine.NAIVE_BAYES_VALUE);
			//

			// params.put(TrainingParameters.ALGORITHM_PARAM, "PERCEPTRON");
			// You have to tweak cutoff and iterations and test what works best.
			// Cutoff is how many times something
			// has to be appear to be noticed.
			params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(0));
			params.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(4000));

			model = DocumentCategorizerME.train("en", sampleStream, params);

			return model;

		} finally {
			if (dataIn != null) {
				try {
					dataIn.close();
				} catch (IOException e) {
				}
			}
		}

	}
}
