package jdd.so.model;

import java.util.Comparator;
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
			//3. Score (negative)
			retVal=o1.getScore()-o2.getScore();
			if (retVal==0){
				//4. Creation date
				retVal=(int) (o2.getCreationDate()-o1.getCreationDate());
			}
		}
		return retVal;
	}

}
