package com.cfryan.wanglai4android.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

import com.cfryan.wanglai4android.db.RosterProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by feng on 2015/8/30.
 */
public class ContactSortModel {

    private ContentResolver mContentResolver;
    // 联系人查询序列
    private static final String[] ROSTER_QUERY = new String[]
            {RosterProvider.RosterConstants._ID, RosterProvider.RosterConstants.JID,
                    RosterProvider.RosterConstants.ALIAS, RosterProvider.RosterConstants.STATUS_MODE,
                    RosterProvider.RosterConstants.STATUS_MESSAGE};


    /**
     * 联系人查询模型的构造函数
     *
     * @param context
     */
    public ContactSortModel(Context context) {
        mContentResolver = context.getContentResolver();
    }


    public List<RosterModel> getRosters(String groupname) {
        List<RosterModel> childList = new ArrayList<>();

        String selectWhere = RosterProvider.RosterConstants.GROUP + " = ?";
        Cursor cursor = mContentResolver.query(RosterProvider.CONTENT_URI, ROSTER_QUERY, selectWhere, new String[]
                {groupname}, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            RosterModel roster = new RosterModel();

            roster.setJid(cursor.getString(cursor.getColumnIndexOrThrow(RosterProvider.RosterConstants.JID)));
            roster.setAlias(cursor.getString(cursor.getColumnIndexOrThrow(RosterProvider.RosterConstants.ALIAS)));
            roster.setStatus_message(cursor.getString(cursor.getColumnIndexOrThrow(RosterProvider.RosterConstants.STATUS_MESSAGE)));
            roster.setStatusMode(cursor.getString(cursor.getColumnIndexOrThrow(RosterProvider.RosterConstants.STATUS_MODE)));
            childList.add(roster);
            cursor.moveToNext();
        }
        cursor.close();
        return childList;
    }


}
