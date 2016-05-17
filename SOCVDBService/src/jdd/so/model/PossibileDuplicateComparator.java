package jdd.so.model;

import java.util.Comparator;

/**
 * Order the possibile duplicates before output
 * @author Petter Friberg
 *
 */
public class PossibileDuplicateComparator implements Comparator<Question> {

	@Override
	public int compare(Question o1, Question o2) {
		//For now only creation date 
		return (int) (o2.getCreationDate()-o1.getCreationDate());
	}

}
