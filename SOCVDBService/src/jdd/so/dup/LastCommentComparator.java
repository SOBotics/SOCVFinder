package jdd.so.dup;

import java.util.Comparator;

import jdd.so.api.model.Comment;
import jdd.so.api.model.Question;

public class LastCommentComparator implements Comparator<Question> {

	@Override
	public int compare(Question o1, Question o2) {

		Comment c1 = o1.getDuplicatedComment();
		Comment c2 = o2.getDuplicatedComment();

		if (c1 == null && c2 == null) {
			return 0;
		}
		if (c2 == null) {
			return 1;
		}
		if (c1 == null) {
			return -1;
		}
		return (int)(c1.getCreationDate()-c2.getCreationDate());
	}

}
