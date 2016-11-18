package jdd.so.test;

import java.io.IOException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.language.detect.LanguageWriter;
import org.xml.sax.SAXException;

public class LanguageDetection {

	public static void main(String args[])throws IOException, SAXException, TikaException {

//		 LanguageIdentifier identifier = new LanguageIdentifier("You can use your jquery scripts by following function");
//	      String language = identifier.getLanguage();
//	      System.out.println("Language of the given content is : " + language);
	   
		  LanguageDetector detector = new OptimaizeLangDetector().loadModels();
	        LanguageWriter writer = new LanguageWriter(detector);
	        writer.append("Should be if ((me.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0) ...or if ((me.getModifiers() & InputEvent.BUTTON1_MASK) != 0) ...");
	        LanguageResult result = writer.getLanguage();
	        System.out.println(result.getLanguage());
	        writer.close();
	}
	
}
