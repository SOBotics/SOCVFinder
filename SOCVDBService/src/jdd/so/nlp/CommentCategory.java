package jdd.so.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.util.InvalidFormatException;

/**
 * Code by Technik Empire for this gist 
 * https://gist.github.com/TechnikEmpire/4a76aa0b844e15c639f08066655847e7
 * @author Technik Empire (adapted by Petter)
 *
 */

public class CommentCategory {
	
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
		String line = comment;
		line = line.replaceAll("lt", "");
		line = line.replaceAll("gt", "");
		line = line.replaceAll("@(\\S+)?", "").trim();
		line = line.replaceAll("[^a-zA-Z\r\n]", " ").trim();
		line = line.replaceAll("[ ]{2,}", " ");
		
		double[] outcomes = myCategorizer.categorize(line);
		if (outcomes[0]>=0.9){
			System.out.println(line);
		}
		return outcomes[0];
	}

	
	/**
	 * Original Technik Empire code
	 * @param args
	 */
	public static void main(String[] args) {
		DoccatModel m = null;
		try {
			m = new DoccatModel(new File("PATH_TO\\CommentsTrainingModel.model"));
		} catch (IOException e) {
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
