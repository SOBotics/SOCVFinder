package jdd.so.api;

import java.util.Comparator;

import jdd.so.api.model.Question;

/**
 * Order the possible duplicates before output
 * @author Petter Friberg
 *
 */
public class PossibleDuplicateComparator implements Comparator<Question> {

	@Override
	public int compare(Question o1, Question o2) {
		//For now only creation date 
		return (int) (o2.getCreationDate()-o1.getCreationDate());
	}

}
