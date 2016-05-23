package jdd.so.api;

import java.util.Comparator;

import jdd.so.api.model.Question;
/**
 * Sort close vote question before output
 * @author Petter Friberg
 *
 */
public class CloseVoteComparator implements Comparator<Question> {

	@Override
	public int compare(Question o1, Question o2) {
		//1. CV Count
		int retVal = o2.getCloseVoteCount()-o1.getCloseVoteCount();
		if (retVal==0){
			//2. Roomba
			if (o1.isRoomba()!=o2.isRoomba()){
				if (o1.isRoomba()){
					return 1;
				}
				return -1;
			}
			retVal=(int) (o1.getCreationDate()-o2.getCreationDate());
		}
		return retVal;
	}

}
