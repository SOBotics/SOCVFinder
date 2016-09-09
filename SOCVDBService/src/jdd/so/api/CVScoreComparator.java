package jdd.so.api;

import java.util.Comparator;

import jdd.so.api.model.Question;
/**
 * Sort close vote question before output
 * @author Petter Friberg
 *
 */
public class CVScoreComparator implements Comparator<Question> {

	
	@Override
	public int compare(Question o1, Question o2) {
		//1. CV Count
		int retVal = o2.getCloseVoteCount()-o1.getCloseVoteCount();
		if (retVal==0){
			retVal=o1.getScore()-o2.getScore();
		}
		return retVal;
	}

}
