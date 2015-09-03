package com.cfryan.wanglai4android.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PreferenceConstants;

/**
 * 数据库基本配置，创建数据库
 */
public final class WanglaiDatabaseHelper extends SQLiteOpenHelper
{
	
	private static String DATABASE_NAME = "wanglai.db";
	private static final int DATABASE_VERSION = 1;
	private static final String TAG = "WanglaiProvider";
	
	private static WanglaiDatabaseHelper mInstance = null;

	public WanglaiDatabaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	//单例模式创建数据库
	public synchronized static WanglaiDatabaseHelper getInstance(Context context)
	{ 
		if (mInstance == null) { 
		mInstance = new WanglaiDatabaseHelper(context); 
		} 
		return mInstance; 
	};
	
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		infoLog("creating new roster table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_ROSTER + " ("
				+ BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ RosterProvider.RosterConstants.JID
				+ " TEXT UNIQUE ON CONFLICT REPLACE, "
				+ RosterProvider.RosterConstants.ALIAS + " TEXT, "
				+ RosterProvider.RosterConstants.STATUS_MODE + " INTEGER, "
				+ RosterProvider.RosterConstants.STATUS_MESSAGE + " TEXT, "
				+ RosterProvider.RosterConstants.OWNER + " TEXT, "
				+ RosterProvider.RosterConstants.GROUP + " TEXT);");
		db.execSQL("CREATE INDEX idx_roster_group ON " + PreferenceConstants.TABLE_ROSTER
				+ " (" + RosterProvider.RosterConstants.GROUP + ")");
		db.execSQL("CREATE INDEX idx_roster_alias ON " + PreferenceConstants.TABLE_ROSTER
				+ " (" + RosterProvider.RosterConstants.ALIAS + ")");
		db.execSQL("CREATE INDEX idx_roster_status ON " + PreferenceConstants.TABLE_ROSTER
				+ " (" + RosterProvider.RosterConstants.STATUS_MODE + ")");
		
		
		infoLog("creating new avatar table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_AVATAR + " ("
				+ BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ AvatarProvider.AvatarConstants.JID
				+ " TEXT UNIQUE ON CONFLICT REPLACE, "
				+ AvatarProvider.AvatarConstants.ALIAS + " TEXT, "
				+ AvatarProvider.AvatarConstants.PHOTO_HASH + " TEXT);");
		
		infoLog("creating new chat table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_CHATS + " (" + BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ ChatProvider.ChatConstants.DATE + " INTEGER,"
				+ ChatProvider.ChatConstants.DIRECTION + " INTEGER,"
				+ ChatProvider.ChatConstants.JID	+ " TEXT,"
				+ ChatProvider.ChatConstants.MESSAGE + " TEXT,"
				+ ChatProvider.ChatConstants.MEDIA_TYPE + " TEXT,"
				+ ChatProvider.ChatConstants.MEDIA_URL + " TEXT,"
				+ ChatProvider.ChatConstants.MEDIA_SIZE + " TEXT,"
				+ ChatProvider.ChatConstants.DELIVERY_STATUS + " INTEGER,"
				+ ChatProvider.ChatConstants.PACKET_ID + " TEXT);");
		
		infoLog("creating new newfriends table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_NEW_FRIENDS + " ("
				+ BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ NewFriendsProvider.NewFriendsConstants.JID
				+ " TEXT UNIQUE ON CONFLICT REPLACE, "
				+ NewFriendsProvider.NewFriendsConstants.STATUS + " TEXT, "
				+ NewFriendsProvider.NewFriendsConstants.NAME + " TEXT);");
		
		infoLog("creating new localphones table");
		db.execSQL("CREATE TABLE " + PreferenceConstants.TABLE_PHONE + " ("
				+ BaseColumns._ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ AddPhonesProvider.PhoneConstants.PHONE_NUM
				+ " TEXT UNIQUE ON CONFLICT REPLACE, "
				+ AddPhonesProvider.PhoneConstants.STATUS + " TEXT, "
				+ AddPhonesProvider.PhoneConstants.NAME + " TEXT);");
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		// TODO Auto-generated method stub
		
	}
	
	private static void infoLog(String data) {
		L.i(TAG, data);
	}

}
