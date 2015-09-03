package com.cfryan.wanglai4android.util;

import java.util.Comparator;

import com.cfryan.wanglai4android.model.ContactModel;


/**
 * 
 * @author xiaanming
 *
 */
public class PinyinComparator implements Comparator<ContactModel> {

	@Override
	public int compare(ContactModel o1, ContactModel o2) {
		if (o1.getSortLetters().equals("@")
				|| o2.getSortLetters().equals("#")) {
			return -1;
		} else if (o1.getSortLetters().equals("#")
				|| o2.getSortLetters().equals("@")) {
			return 1;
		} else {
			return o1.getSortLetters().compareTo(o2.getSortLetters());
		}
	}

}
