package jdd.so.nlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class TweetsToCommentFormat {

	public static void main(String[] args) throws IOException {
		CSVParser parser = CSVParser.parse(new File("dev/twitter-hate-speech-processed.csv"), Charset.forName("Cp1252"), CSVFormat.DEFAULT);
		try (PrintWriter writer = new PrintWriter("training/bad/model_comments_bad_tweets.txt", "UTF-8")) {
			boolean skipFirst = true;
			for (CSVRecord r : parser) {
				if (skipFirst){
					skipFirst=false;
					continue;
				}
				String classif = r.get(0);
				if (classif.equalsIgnoreCase("The tweet is not offensive")) {
					continue;
				}
				writer.println(r.get(2));
				
			}
		}finally {
			parser.close();
		}
	}

}
