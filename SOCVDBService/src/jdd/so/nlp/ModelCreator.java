package jdd.so.nlp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import opennlp.tools.cmdline.doccat.DoccatEvaluationErrorListener;
import opennlp.tools.cmdline.doccat.DoccatFineGrainedReportListener;
import opennlp.tools.doccat.BagOfWordsFeatureGenerator;
import opennlp.tools.doccat.DoccatEvaluationMonitor;
import opennlp.tools.doccat.DoccatFactory;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerEvaluator;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.doccat.FeatureGenerator;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import opennlp.tools.util.eval.EvaluationMonitor;
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

	public static final int MAX_COMMENTS = 2000;
	public static final String MODEL_FOLDER = "model";
	public static final String TRANING_FOLDER = "training_v6";

	public static void main(String[] args) throws IOException {
		File traningFileNlp = new File(MODEL_FOLDER + "/openNPLTraining.txt");
		getNLPModel(traningFileNlp);
	}
	
	public static void main2(String[] args) throws IOException {

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
				System.out.println("Reading file: " + files[i].getName() + " " + nrInClass);

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

		FeatureGenerator[] def = { new BagOfWordsFeatureGenerator() };
		WhitespaceTokenizer tokenizer = WhitespaceTokenizer.INSTANCE;

		DoccatFactory factory = new DoccatFactory(tokenizer, def);
		InputStreamFactory isf = new MarkableFileInputStreamFactory(openNLPTraining);
		ObjectStream<String> lineStream = new PlainTextByLineStream(isf, "UTF-8");
		ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

		TrainingParameters params = TrainingParameters.defaultParams();
		System.out.println(params.algorithm());
		params.put(TrainingParameters.CUTOFF_PARAM, Integer.toString(0));
		params.put(TrainingParameters.ITERATIONS_PARAM, Integer.toString(4000));

		model = DocumentCategorizerME.train("en", sampleStream, params, factory);
		
		evaluateDoccatModel(model, openNLPTraining);

		return model;

	}
	
	public static void evaluateDoccatModel(DoccatModel model,File openNLPTraining) throws IOException{
		InputStreamFactory isf = new MarkableFileInputStreamFactory(openNLPTraining);
		ObjectStream<String> lineStream = new PlainTextByLineStream(isf, "UTF-8");
		ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

		List<EvaluationMonitor<DocumentSample>> listeners = new LinkedList<EvaluationMonitor<DocumentSample>>();
	    listeners.add(new DoccatEvaluationErrorListener());
	    listeners.add(new DoccatFineGrainedReportListener());
	    
		DocumentCategorizerEvaluator eval = new  DocumentCategorizerEvaluator(new DocumentCategorizerME(model),listeners.toArray(new DoccatEvaluationMonitor[listeners.size()]));
		eval.evaluate(sampleStream);
		System.out.println(eval);
		
	}
}
