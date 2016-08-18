package jdd.so.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import opennlp.tools.dictionary.Dictionary;
import opennlp.tools.sentdetect.SentenceDetectorFactory;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.sentdetect.SentenceSample;
import opennlp.tools.sentdetect.SentenceSampleStream;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.MarkableFileInputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

public class SentanceDetector {

	
	public static void main(String[] args) throws IOException {
		SentenceModel model;
		
		
		Charset charset = Charset.forName("UTF-8");
		InputStreamFactory isf = new MarkableFileInputStreamFactory(new File("model/openNPLTraining.txt"));
		ObjectStream<String> lineStream =
		  new PlainTextByLineStream(isf, charset);
		ObjectStream<SentenceSample> sampleStream = new SentenceSampleStream(lineStream);

		try {
			Dictionary dict = new Dictionary(new FileInputStream(new File("ini/stop_words.txt")));
			SentenceDetectorFactory sdf = new SentenceDetectorFactory("en",true,dict,null);
			TrainingParameters params = TrainingParameters.defaultParams();
			model = SentenceDetectorME.train("en", sampleStream, sdf,params);
		}
		finally {
		  sampleStream.close();
		}
		
		
		
		System.out.println("done");

	}
}
