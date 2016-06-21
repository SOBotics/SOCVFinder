package jdd.so.nlp;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.jsoup.Jsoup;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.util.InvalidFormatException;

/**
 * Code by Technik Empire from this gist 
 * https://gist.github.com/TechnikEmpire/4a76aa0b844e15c639f08066655847e7
 * @author Technik Empire (adapted by Petter)
 *
 */

public class CommentCategory {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CommentCategory.class);
	
	private DoccatModel m;
	private DocumentCategorizerME myCategorizer;
	
	public CommentCategory() throws InvalidFormatException, IOException{
		initModel();
	}
	
	private void initModel() throws InvalidFormatException, IOException {

		m = new DoccatModel(new File("ini/CommentsTrainingModel.model"));
		myCategorizer = new DocumentCategorizerME(m);
	}

	public double getThresholdBad(String comment){

		String line = Jsoup.parse(comment).text(); //remove html
		line = line.replaceAll("@(\\S+)?", "").trim(); //remove username
		line = line.replaceAll("[^a-zA-Z\r\n]", " ").trim(); //remove strange chars
		line = line.replaceAll("[ ]{2,}", " "); //remove spaces
		
		double[] outcomes = myCategorizer.categorize(line);
		if (outcomes[0]>=0.9){
			Logger.getLogger(LogThresholdHit.class).debug(line + " | " + outcomes[0] + " | "  + outcomes[1]);
		}else{
			Logger.getLogger(LogThresholdNonHit.class).debug(line + " | " + outcomes[0] + " | " + outcomes[1]);
		}
		double returndouble = outcomes[0];
		return returndouble;
	}

	
	/**
	 * Original Technik Empire code
	 * @param args
	 */
	public static void main(String[] args) {
		if (logger.isDebugEnabled()) {
			logger.debug("main(String[]) - start");
		}

		DoccatModel m = null;
		try {
			m = new DoccatModel(new File("PATH_TO\\CommentsTrainingModel.model"));
		} catch (IOException e) {
			logger.error("main(String[])", e);

			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (m != null) {
			DocumentCategorizerME myCategorizer = new DocumentCategorizerME(m);

			double threshold = .90;

			// In our sample data, one "line" here equals one comment.
			try (BufferedReader br = new BufferedReader(new FileReader(new File("PATH_TO_SAMPLE_DATA")))) {
				String line;
				while ((line = br.readLine()) != null) {

					line = line.replaceAll("[^a-zA-Z\r\n]", " ").trim();
					line = line.replaceAll("[ ]{2,}", " ");
					double[] outcomes = myCategorizer.categorize(line);
					String category = myCategorizer.getBestCategory(outcomes);

					if (category.equalsIgnoreCase("Bad") && outcomes[0] > threshold) {
						// System.out.println(Arrays.toString(outcomes));
						System.out.println(line);
						// System.out.println(category);
					}

				}
			} catch (IOException e) {
				logger.error("main(String[])", e);

				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("main(String[]) - end");
		}
	}
}
