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
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PreferenceConstants;

public class ChatProvider extends ContentProvider {

	public static final String AUTHORITY = "com.hdu.cfryan.provider.Chats";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + PreferenceConstants.TABLE_CHATS);

	private static final UriMatcher URI_MATCHER = new UriMatcher(
			UriMatcher.NO_MATCH);

	private static final int MESSAGES = 1;
	private static final int MESSAGE_ID = 2;

	static {
		URI_MATCHER.addURI(AUTHORITY, PreferenceConstants.TABLE_CHATS, MESSAGES);
		URI_MATCHER.addURI(AUTHORITY, PreferenceConstants.TABLE_CHATS + "/#", MESSAGE_ID);
	}

	private static final String TAG = "ChatProvider";

	private SQLiteOpenHelper mOpenHelper;

	public ChatProvider() {
	}

	@Override
	public int delete(Uri url, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (URI_MATCHER.match(url)) {

		case MESSAGES:
			count = db.delete(PreferenceConstants.TABLE_CHATS, where, whereArgs);
			break;
		case MESSAGE_ID:
			String segment = url.getPathSegments().get(1);

			if (TextUtils.isEmpty(where)) {
				where = "_id=" + segment;
			} else {
				where = "_id=" + segment + " AND (" + where + ")";
			}

			count = db.delete(PreferenceConstants.TABLE_CHATS, where, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Cannot delete from URL: " + url);
		}

		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}

	@Override
	public String getType(Uri url) {
		int match = URI_MATCHER.match(url);
		switch (match) {
		case MESSAGES:
			return ChatConstants.CONTENT_TYPE;
		case MESSAGE_ID:
			return ChatConstants.CONTENT_ITEM_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URL");
		}
	}

	@Override
	public Uri insert(Uri url, ContentValues initialValues) {
		if (URI_MATCHER.match(url) != MESSAGES) {
			throw new IllegalArgumentException("Cannot insert into URL: " + url);
		}

		ContentValues values = (initialValues != null) ? new ContentValues(
				initialValues) : new ContentValues();

		for (String colName : ChatConstants.getRequiredColumns()) {
			if (values.containsKey(colName) == false) {
				throw new IllegalArgumentException("Missing column: " + colName);
			}
		}
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		long rowId = db.insert(PreferenceConstants.TABLE_CHATS, ChatConstants.DATE, values);

		if (rowId < 0) {
			throw new SQLException("Failed to insert row into " + url);
		}

		Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
		getContext().getContentResolver().notifyChange(noteUri, null);
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

		switch (match) {
		case MESSAGES:
			qBuilder.setTables(PreferenceConstants.TABLE_CHATS);
			break;
		case MESSAGE_ID:
			qBuilder.setTables(PreferenceConstants.TABLE_CHATS);
			qBuilder.appendWhere("_id=");
			qBuilder.appendWhere(url.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}

		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = ChatConstants.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}
		
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		
		Cursor ret = qBuilder.query(db, projectionIn, selection, selectionArgs,
				null, null, orderBy);

		if (ret == null) {
			infoLog("ChatProvider.query: failed");
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
		case MESSAGES:
			count = db.update(PreferenceConstants.TABLE_CHATS, values, where, whereArgs);
			break;
		case MESSAGE_ID:
			String segment = url.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			count = db.update(PreferenceConstants.TABLE_CHATS, values, "_id=" + rowId, null);
			break;
		default:
			throw new UnsupportedOperationException("Cannot update URL: " + url);
		}

		infoLog("*** notifyChange() rowId: " + rowId + " url " + url);

		getContext().getContentResolver().notifyChange(url, null);
		return count;

	}

	private static void infoLog(String data) {
		L.i(TAG, data);
	}

//	private static class ChatDatabaseHelper extends SQLiteOpenHelper {
//
//		private static final String DATABASE_NAME = "chat.db";
//		private static final int DATABASE_VERSION = 4;
//
//		public ChatDatabaseHelper(Context context) {
//			super(context, DATABASE_NAME, null, DATABASE_VERSION);
//		}
//
//		@Override
//		public void onCreate(SQLiteDatabase db) {
//			infoLog("creating new chat table");
//
//			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + BaseColumns._ID
//					+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
//					+ ChatConstants.DATE + " INTEGER,"
//					+ ChatConstants.DIRECTION + " INTEGER,"
//					+ ChatConstants.JID	+ " TEXT,"
//					+ ChatConstants.MESSAGE + " TEXT,"
//					+ ChatConstants.MEDIA_TYPE + " TEXT,"
//					+ ChatConstants.MEDIA_URL + " TEXT,"
//					+ ChatConstants.MEDIA_SIZE + " TEXT,"
//					+ ChatConstants.DELIVERY_STATUS + " INTEGER,"
//					+ ChatConstants.PACKET_ID + " TEXT);");
//		}
//
//		@Override
//		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//			infoLog("onUpgrade: from " + oldVersion + " to " + newVersion);
//			switch (oldVersion) {
//			case 3:
//				db.execSQL("UPDATE " + TABLE_NAME + " SET READ=1");
//			case 4:
//				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD "
//						+ ChatConstants.PACKET_ID + " TEXT");
//				break;
//			default:
//				db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
//				onCreate(db);
//			}
//		}
//
//	}

	public static final class ChatConstants implements BaseColumns {

		private ChatConstants() {
		}

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.yaxim.chat";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.yaxim.chat";
		public static final String DEFAULT_SORT_ORDER = "_id ASC"; // sort by
																	// auto-id

		public static final String DATE = "date";
		public static final String DIRECTION = "from_jid";
		public static final String JID = "to_jid";
		public static final String FROM = "from";
		public static final String TO = "to";
		public static final String MESSAGE = "message";
		public static final String DELIVERY_STATUS = "read"; // SQLite can not
																// rename
																// columns,
																// reuse old
																// name
		public static final String PACKET_ID = "pid";
		
		public static final String MEDIA_TYPE = "mediaType";
		public static final String MEDIA_URL = "mediaUrl";
		public static final String MEDIA_SIZE = "mediaSize";
		
		public static final String MEDIA_TYPE_NORMAL = "Normal";
		public static final String MEDIA_TYPE_AUDIO = "Audio";
		public static final String MEDIA_TYPE_IMAGE = "Image";
		public static final String MEDIA_TYPE_PHOTO = "Photo";
		public static final String MEDIA_TYPE_FILE = "File";

		// boolean mappings
		public static final int INCOMING = 0;
		public static final int OUTGOING = 1;
		public static final int DS_NEW = 0; // < this message has not been
											// sent/displayed yet
		public static final int DS_SENT_OR_READ = 1; // < this message was sent
														// but not yet acked, or
														// it was received and
														// read
		//�������Ϣ�ѱ��Է��Ķ���ʵ�ʱ���Ŀ��δ���ϴ�״̬���е�С���⢘
		public static final int DS_ACKED = 2; // < this message was XEP-0184
												// acknowledged
		public static final int DS_UPLOADING = 3;
		public static final int DS_UPLOAD_FAILED = 4;
		public static final int DS_UPLOAD_SUCCESS = 5;
		
		public static final int DS_DOWNLOADING = 6;
		public static final int DS_DOWNLOAD_FAILED = 7;
		public static final int DS_DOWNLOAD_SUCCESS = 8;
		public static final int DS_DOWNLOAD_READY = 9;

		public static ArrayList<String> getRequiredColumns() {
			ArrayList<String> tmpList = new ArrayList<String>();
			tmpList.add(DATE);
			tmpList.add(DIRECTION);
			tmpList.add(JID);
			tmpList.add(MESSAGE);
			return tmpList;
		}

	}

}
