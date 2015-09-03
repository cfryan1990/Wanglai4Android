package com.cfryan.wanglai4android.model;

/**
 * Created by feng on 2015/8/31.
 */
public class ContactItem {

    public static final int ITEM = 0;
    public static final int SECTION = 1;

    public final int type;
    public final String letter;

    public ContactModel model;

    public int sectionPosition;
    public int listPosition;

    public ContactItem(int type, String letter) {
        this.type = type;
        this.letter = letter;
    }
}
