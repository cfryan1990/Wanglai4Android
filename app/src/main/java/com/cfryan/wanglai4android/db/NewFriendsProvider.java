package com.cfryan.wanglai4android.db;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PreferenceConstants;

public class NewFriendsProvider extends ContentProvider
{
	public static final String AUTHORITY = "com.hdu.cfryan.provider.NewFriends";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + PreferenceConstants.TABLE_NEW_FRIENDS);
	public static final String QUERY_ALIAS = "main_result";

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH);
	private static final int CONTACTS = 1;
	private static final int CONTACT_ID = 2;

	static {
		URI_MATCHER.addURI(AUTHORITY, PreferenceConstants.TABLE_NEW_FRIENDS, CONTACTS);
		URI_MATCHER.addURI(AUTHORITY, PreferenceConstants.TABLE_NEW_FRIENDS + "/#", CONTACT_ID);
	}

	private static final String TAG = "NewFriendsProvider";

	private Runnable mNotifyChange = new Runnable() {
		@Override
		public void run() {
			Log.d(TAG, "notifying change");
			getContext().getContentResolver().notifyChange(CONTENT_URI, null);
		}
	};
	private Handler mNotifyHandler = new Handler();

	private SQLiteOpenHelper mOpenHelper;

	public NewFriendsProvider() {
	}

	@Override
	public int delete(Uri url, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (URI_MATCHER.match(url)) {

		case CONTACTS:
			count = db.delete(PreferenceConstants.TABLE_NEW_FRIENDS, where, whereArgs);
			break;

		case CONTACT_ID:
			String segment = url.getPathSegments().get(1);

			if (TextUtils.isEmpty(where)) {
				where = "_id=" + segment;
			} else {
				where = "_id=" + segment + " AND (" + where + ")";
			}

			count = db.delete(PreferenceConstants.TABLE_NEW_FRIENDS, where, whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Cannot delete from URL: " + url);
		}

	//	getContext().getContentResolver().notifyChange(GROUPS_URI, null);
		notifyChange();

		return count;
	}

	@Override
	public String getType(Uri url) {
		int match = URI_MATCHER.match(url);
		switch (match) {
		case CONTACTS:
			return NewFriendsConstants.CONTENT_TYPE;
		case CONTACT_ID:
			return NewFriendsConstants.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URL");
		}
	}

	@Override
	public Uri insert(Uri url, ContentValues initialValues) {
		if (URI_MATCHER.match(url) != CONTACTS) {
			throw new IllegalArgumentException("Cannot insert into URL: " + url);
		}

		ContentValues values = (initialValues != null) ? new ContentValues(
				initialValues) : new ContentValues();

//		for (String colName : AvatarConstants.getRequiredColumns()) {
//			if (values.containsKey(colName) == false) {
//				throw new IllegalArgumentException("Missing column: " + colName);
//			}
//		}
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		long rowId = db.insert(PreferenceConstants.TABLE_NEW_FRIENDS, NewFriendsConstants.JID, values);

		if (rowId < 0) {
			throw new SQLException("Failed to insert row into " + url);
		}

		Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);

		notifyChange();

		return noteUri;
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = WanglaiDatabaseHelper.getInstance(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri url, String[] projectionIn, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
		int match = URI_MATCHER.match(url);
		String groupBy = null;

		switch (match) {


		case CONTACTS:
			qBuilder.setTables(PreferenceConstants.TABLE_NEW_FRIENDS + " " + QUERY_ALIAS);
			break;

		case CONTACT_ID:
			qBuilder.setTables(PreferenceConstants.TABLE_NEW_FRIENDS + " " + QUERY_ALIAS);
			qBuilder.appendWhere("_id=");
			qBuilder.appendWhere(url.getPathSegments().get(1));
			break;

		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		String orderBy;
		if (TextUtils.isEmpty(sortOrder) && match == CONTACTS) {
			orderBy = NewFriendsConstants.DEFAULT_SORT_ORDER;//Ĭ�ϰ�����״̬����
		} else {
			orderBy = sortOrder;
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor ret = qBuilder.query(db, projectionIn, selection, selectionArgs,
				groupBy, null, orderBy);

		if (ret == null) {
			infoLog("RosterProvider.query: failed");
		} else {
			ret.setNotificationUri(getContext().getContentResolver(), url);
		}

		return ret;
	}

	@Override
	public int update(Uri url, ContentValues values, String where,
			String[] whereArgs) {
		int count;
		long rowId = 0;
		int match = URI_MATCHER.match(url);
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		switch (match) {
		case CONTACTS:
			count = db.update(PreferenceConstants.TABLE_NEW_FRIENDS, values, where, whereArgs);
			break;
		case CONTACT_ID:
			String segment = url.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			count = db.update(PreferenceConstants.TABLE_NEW_FRIENDS, values, "_id=" + rowId, whereArgs);
			break;
		default:
			throw new UnsupportedOperationException("Cannot update URL: " + url);
		}

		notifyChange();

		return count;

	}

	/*
	 * delay change notification, cancel previous attempts. this implements rate
	 * throttling on fast update sequences
	 */
	long last_notify = 0;

	private void notifyChange() {
		mNotifyHandler.removeCallbacks(mNotifyChange);
		long ts = System.currentTimeMillis();
		if (ts > last_notify + 500)
			mNotifyChange.run();
		else
			mNotifyHandler.postDelayed(mNotifyChange, 200);
		last_notify = ts;
	}

	private static void infoLog(String data) {
		L.i(TAG, data);
	}

//	private static class RosterDatabaseHelper extends SQLiteOpenHelper {
//
//		private static final String DATABASE_NAME = "NewFriends.db";
//		private static final int DATABASE_VERSION = 4;
//
//		public RosterDatabaseHelper(Context context) {
//			super(context, DATABASE_NAME, null, DATABASE_VERSION);
//		}
//
//		@Override
//		public void onCreate(SQLiteDatabase db) {
//			infoLog("creating new newfriends table");
//
//			db.execSQL("CREATE TABLE " + TABLE_PHONE + " ("
//					+ BaseColumns._ID
//					+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
//					+ NewFriendsConstants.JID
//					+ " TEXT UNIQUE ON CONFLICT REPLACE, "
//					+ NewFriendsConstants.STATUS + " TEXT, "
//					+ NewFriendsConstants.NAME + " TEXT);");
//		}
//
//		@Override
//		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//			infoLog("onUpgrade: from " + oldVersion + " to " + newVersion);
//			switch (oldVersion) {
//			default:
//				db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHONE);
//				onCreate(db);
//			}
//		}
//	}

	public static final class NewFriendsConstants implements BaseColumns {

		private NewFriendsConstants() {
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.yaxim.NewFriends";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.yaxim.NewFriends";

		public static final String NAME = "name";//�洢����
		public static final String JID = "jid";//����洢
		public static final String STATUS = "status";//�������ȷ��״̬
		public static final String DEFAULT_SORT_ORDER = STATUS + " COLLATE NOCASE";

		public static ArrayList<String> getRequiredColumns() {
			ArrayList<String> tmpList = new ArrayList<String>();
			tmpList.add(NAME);
			tmpList.add(JID);
			tmpList.add(STATUS);
			
			return tmpList;
		}

	}
}

