package com.cfryan.wanglai4android.adapter;

import java.io.File;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.activity.DetailInfoActivity;
import com.cfryan.wanglai4android.db.ChatProvider;
import com.cfryan.wanglai4android.db.ChatProvider.ChatConstants;
import com.cfryan.wanglai4android.util.HttpDownloader;
import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PreferenceConstants;
import com.cfryan.wanglai4android.util.TimeUtil;
import com.cfryan.wanglai4android.util.XMPPHelper;

public class ChatAdapter extends SimpleCursorAdapter {
	private static final int DELAY_NEWMSG = 2000;
	private Context mContext;
	private LayoutInflater mInflater;

	private MediaPlayer mMediaPlayer = new MediaPlayer();

	public static final int TYPE_UNKNOW = 0;
	public static final int TYPE_LEFT_TEXT = 1;
	public static final int TYPE_LEFT_AUDIO = 2;
	public static final int TYPE_LEFT_IMAGE = 3;
	public static final int TYPE_LEFT_FILE = 4;
	public static final int TYPE_RIGHT_TEXT = 5;
	public static final int TYPE_RIGHT_AUDIO = 6;
	public static final int TYPE_RIGHT_IMAGE = 7;
	public static final int TYPE_RIGHT_FILE = 8;

	public ChatAdapter(Context context, Cursor cursor, String[] from) {
		// super(context, android.R.layout.simple_list_item_1, cursor, from,
		// to);
		super(context, 0, cursor, from, null);
		
		mContext = context;
		mInflater = LayoutInflater.from(context);
	}

	private int getItemViewType(int come, String mediaType) {
		if (come == ChatConstants.OUTGOING && ChatConstants.MEDIA_TYPE_NORMAL.equals(mediaType))
			return TYPE_RIGHT_TEXT;
		else if (come == ChatConstants.OUTGOING && ChatConstants.MEDIA_TYPE_AUDIO.equals(mediaType))
			return TYPE_RIGHT_AUDIO;
		else if (come == ChatConstants.OUTGOING && ChatConstants.MEDIA_TYPE_IMAGE.equals(mediaType))
			return TYPE_RIGHT_IMAGE;
		else if (come == ChatConstants.OUTGOING && ChatConstants.MEDIA_TYPE_FILE.equals(mediaType))
			return TYPE_RIGHT_FILE;
		else if (come == ChatConstants.INCOMING && ChatConstants.MEDIA_TYPE_NORMAL.equals(mediaType))
			return TYPE_LEFT_TEXT;
		else if (come == ChatConstants.INCOMING && ChatConstants.MEDIA_TYPE_AUDIO.equals(mediaType))
			return TYPE_LEFT_AUDIO;
		else if (come == ChatConstants.INCOMING && ChatConstants.MEDIA_TYPE_IMAGE.equals(mediaType))
			return TYPE_LEFT_IMAGE;
		else if (come == ChatConstants.INCOMING && ChatConstants.MEDIA_TYPE_FILE.equals(mediaType))
			return TYPE_LEFT_FILE;
		else
			return TYPE_UNKNOW;
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {
		long prevTime = GetPrevItemTime(position);		
		
		Cursor cursor = this.getCursor();
		cursor.moveToPosition(position);
		
		int packetId = cursor.getInt(cursor
				.getColumnIndex(BaseColumns._ID));
		long dateMilliseconds = cursor.getLong(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DATE));
		
		String chatTime = TimeUtil.getChatTime(dateMilliseconds);
		
		String message = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));

		String jid = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.JID));

		int come = cursor.getInt(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DIRECTION));// 消息来自
		String mediaType = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.MEDIA_TYPE));
		String mediaUrl = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.MEDIA_URL));
		String mediaSize = cursor.getString(cursor
				.getColumnIndex(ChatProvider.ChatConstants.MEDIA_SIZE));

		int deliveryStatus = cursor.getInt(cursor
				.getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));

		ViewHolder holder = new ViewHolder();
		
		int viewType = getItemViewType(come, mediaType);
		switch (viewType) {
		case TYPE_LEFT_TEXT:
			convertView = mInflater.inflate(R.layout.chat_item_left_text, null);
			holder = buildLeftTextHolder(convertView);
			
			bindTextData(holder, chatTime, jid, message, false);
			if(dateMilliseconds - prevTime < 100*1000 )
				holder.msgTime.setVisibility(View.GONE);
			break;
			
		case TYPE_LEFT_AUDIO:
			
			convertView = mInflater
					.inflate(R.layout.chat_item_left_audio, null);
			holder = buildLeftAudioHolder(convertView);
			
			bindAudioData(holder, chatTime, jid, mediaUrl, mediaSize, chatTime, false);
			
			Log.i("msgTime left audio", String.valueOf(dateMilliseconds));
			if(dateMilliseconds - prevTime < 100*1000 )
			//	holder.msgTime.setVisibility(View.GONE);
			break;

		case TYPE_LEFT_IMAGE:
			convertView = mInflater
					.inflate(R.layout.chat_item_left_image, null);
			holder = buildLeftImageHolder(convertView);
			
			bindImageData(holder, chatTime, jid, mediaUrl, deliveryStatus, false);
			break;
			
		case TYPE_LEFT_FILE:
			convertView = mInflater
					.inflate(R.layout.chat_item_left_file, null);
			holder = buildLeftFileHolder(convertView);
			
			bindFileData(holder, chatTime, jid, mediaUrl, deliveryStatus, false);
			break;

		case TYPE_RIGHT_TEXT:
			convertView = mInflater
					.inflate(R.layout.chat_item_right_text, null);
			holder = buildRightTextHolder(convertView);
			holder.avatar.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Activity currentActivity = (Activity) v.getContext();
			//		Intent intent = new Intent(currentActivity, meInfoActivity.class);
			//		currentActivity.startActivity(intent);
				}
			});
			bindTextData(holder, chatTime, jid, message, true);
			if(dateMilliseconds - prevTime < 100*1000 )
				holder.msgTime.setVisibility(View.GONE);
			break;

		case TYPE_RIGHT_AUDIO:
			convertView = mInflater.inflate(R.layout.chat_item_right_audio,
					null);
			holder = buildRightAudioHolder(convertView);
			holder.avatar.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Activity currentActivity = (Activity) v.getContext();
			//		Intent intent = new Intent(currentActivity, meInfoActivity.class);
			//		currentActivity.startActivity(intent);
				}
			});
			bindAudioData(holder, chatTime, jid, mediaUrl, mediaSize, chatTime, true);
			if(dateMilliseconds - prevTime < 100*1000 )
				holder.msgTime.setVisibility(View.GONE);
			break;

		case TYPE_RIGHT_IMAGE:
			convertView = mInflater.inflate(R.layout.chat_item_right_image,
					null);
			holder = buildRightImageHolder(convertView);
			holder.avatar.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Activity currentActivity = (Activity) v.getContext();
			//		Intent intent = new Intent(currentActivity, meInfoActivity.class);
			//		currentActivity.startActivity(intent);
				}
			});
			bindImageData(holder, chatTime, jid, mediaUrl, deliveryStatus, true);
			break;
			
		case TYPE_RIGHT_FILE:
			convertView = mInflater
					.inflate(R.layout.chat_item_right_file, null);
			holder = buildRightFileHolder(convertView);
			holder.avatar.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Activity currentActivity = (Activity) v.getContext();
				//	Intent intent = new Intent(currentActivity, meInfoActivity.class);
				//	currentActivity.startActivity(intent);
				}
			});
			bindFileData(holder, chatTime, jid, mediaUrl, deliveryStatus, true);
			break;

		default:
			convertView = mInflater.inflate(R.layout.chat_item_left_text, null);
			holder = buildLeftTextHolder(convertView);
			bindTextData(holder, chatTime, jid, message, false);
			break;
		}
		convertView.setTag(holder);
	
		if (come == ChatConstants.INCOMING
				&& deliveryStatus == ChatConstants.DS_NEW) {
			markAsReadDelayed(packetId, DELAY_NEWMSG);
		}
		return convertView;
	}

	private long GetPrevItemTime(int position) {
		long prevTime = 0;
		if(position > 0)
		{
			Cursor cursor = this.getCursor();
			cursor.moveToPosition(position - 1);
			prevTime = cursor.getLong(cursor
					.getColumnIndex(ChatProvider.ChatConstants.DATE));
		}
		return prevTime;
	}


	private void markAsReadDelayed(final int id, int delay) {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				markAsRead(id);
			}
		}, delay);
	}

	/**
	 * 标记为已读消息
	 * 
	 * @param id
	 */
	private void markAsRead(int id) {
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ PreferenceConstants.TABLE_CHATS + "/" + id);
		L.d("markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		mContext.getContentResolver().update(rowuri, values, null, null);
	}

	private ViewHolder buildLeftTextHolder(View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) convertView
				.findViewById(R.id.iv_normalLeftAvatar);
		holder.msgContent = (Button) convertView
				.findViewById(R.id.btn_normalLeftText);
		holder.msgTime = (TextView) convertView
				.findViewById(R.id.tv_normalLeftDatetime);
		return holder;
	}

	private ViewHolder buildRightTextHolder(View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) convertView
				.findViewById(R.id.iv_normalRightAvatar);
		holder.msgContent = (Button) convertView
				.findViewById(R.id.btn_normalRightText);
		holder.msgTime = (TextView) convertView
				.findViewById(R.id.tv_normalRightDatetime);
		return holder;
	}

	private ViewHolder buildLeftAudioHolder(final View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) convertView
				.findViewById(R.id.iv_audioLeftAvatar);
		holder.audio = (Button) convertView
				.findViewById(R.id.btn_audioLeftAudio);
		holder.audioTime = (TextView) convertView
				.findViewById(R.id.tv_audioLeftAudioTime);
		return holder;
	}

	private ViewHolder buildRightAudioHolder(final View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) convertView
				.findViewById(R.id.iv_audioRightAvatar);
		holder.audio = (Button) convertView
				.findViewById(R.id.btn_audioRightAudio);
		holder.audioTime = (TextView) convertView
				.findViewById(R.id.tv_audioRightAudioTime);
		holder.msgTime = (TextView) convertView.findViewById(R.id.tv_audioRightDatetime);
		return holder;
	}

	private ViewHolder buildLeftImageHolder(final View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) convertView
				.findViewById(R.id.iv_imageLeftAvatar);
		holder.image = (ImageView) convertView
				.findViewById(R.id.iv_imageLeftImage);
		holder.imageTime = (TextView) convertView
				.findViewById(R.id.tv_imageLeftDatetime);

		holder.progressBar = (ProgressBar) convertView
				.findViewById(R.id.progressBarLeftImage);
		return holder;
	}

	private ViewHolder buildRightImageHolder(final View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) convertView
				.findViewById(R.id.iv_imageRightAvatar);
		holder.image = (ImageView) convertView
				.findViewById(R.id.iv_imageRightImage);
		holder.imageTime = (TextView) convertView
				.findViewById(R.id.tv_imageRightDatetime);
		holder.uploadstatus = (ImageView) convertView
				.findViewById(R.id.upload_status);

		holder.progressBar = (ProgressBar) convertView
				.findViewById(R.id.progressBarRightImage);
		return holder;
	}
	
	private ViewHolder buildLeftFileHolder(final View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) convertView
				.findViewById(R.id.iv_fileLeftAvatar);
		holder.file = (Button) convertView
				.findViewById(R.id.btn_fileLeftFile);
	//	holder.imageTime = (TextView) convertView
	//			.findViewById(R.id.tv_imageLeftDatetime);

		holder.progressBar = (ProgressBar) convertView
				.findViewById(R.id.progressBarLeftImage);
		return holder;
	}
	
	private ViewHolder buildRightFileHolder(final View convertView) {
		ViewHolder holder = new ViewHolder();
		holder.avatar = (ImageView) convertView
				.findViewById(R.id.iv_fileRightAvatar);
		holder.file = (Button) convertView
				.findViewById(R.id.btn_fileRightFile);
	//	holder.imageTime = (TextView) convertView
	//			.findViewById(R.id.tv_imageRightDatetime);
	//	holder.uploadstatus = (ImageView) convertView
	//			.findViewById(R.id.upload_status);

		holder.progressBar = (ProgressBar) convertView
				.findViewById(R.id.progressBarRightImage);
		return holder;
	}

	private void bindTextData(ViewHolder holder, String date, String jid,
			String message, boolean isMe) {
		holder.avatar.setBackgroundResource(R.mipmap.default_mobile_avatar);
		if(!isMe)
		{
			final String jidforchat = jid;
			holder.avatar.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Activity currentActivity = (Activity) v.getContext();
					Intent detailInfoIntent = new Intent(currentActivity, DetailInfoActivity.class); 
					Uri userNameUri = Uri.parse(jidforchat);
					detailInfoIntent.setData(userNameUri);
					detailInfoIntent.putExtra(DetailInfoActivity.INTENT_EXTRA_USERNAME,
							jidforchat);
					currentActivity.startActivity(detailInfoIntent);
				}
			});
		}
		
		if(message != null){
			holder.msgContent
					.setText(XMPPHelper.convertNormalStringToSpannableString(
							mContext, message, false));
		}
		holder.msgTime.setText(date);
	}

	private void bindAudioData(ViewHolder holder, String date, String jid,
			String audioFileUrl, String audioTime, String chatTime, boolean isMe) {
		holder.avatar.setBackgroundResource(R.mipmap.default_mobile_avatar);
		if(!isMe)
		{
			final String jidforchat = jid;
			holder.avatar.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Activity currentActivity = (Activity) v.getContext();
					Intent detailInfoIntent = new Intent(currentActivity, DetailInfoActivity.class); 
					Uri userNameUri = Uri.parse(jidforchat);
					detailInfoIntent.setData(userNameUri);
					detailInfoIntent.putExtra(DetailInfoActivity.INTENT_EXTRA_USERNAME,
							jidforchat);
					currentActivity.startActivity(detailInfoIntent);
				}
			});
		}
		holder.audio.setOnClickListener(new LvButtonListener(audioFileUrl));
		holder.audioTime.setText(audioTime + "\"");
		
		int audioSize = getAudioTime(audioTime);
		
		String strAudio = "";		
		for(int i=0; i<audioSize*5; i++)
			strAudio += " ";
		
		holder.audio.setText(strAudio);
	//	holder.msgTime.setText(chatTime);
	}

	private int getAudioTime(String audioTime) {
		int audioSize = 0;
		try{
			audioSize = Integer.parseInt(audioTime);
		}catch(NumberFormatException ie){
			try {
				audioSize = Float.valueOf(audioTime).intValue();
			}catch(NumberFormatException fe){
			}
		}
		
		audioSize = audioSize > 1 ? audioSize : 1;
		return audioSize;
	}

	private void bindImageData(ViewHolder holder, String date, String jid,
			String imageFileUrl, int ds, boolean isMe) {
		holder.avatar.setBackgroundResource(R.mipmap.default_mobile_avatar);
		if(!isMe)
		{
			final String jidforchat = jid;
			holder.avatar.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Activity currentActivity = (Activity) v.getContext();
					Intent detailInfoIntent = new Intent(currentActivity, DetailInfoActivity.class); 
					Uri userNameUri = Uri.parse(jidforchat);
					detailInfoIntent.setData(userNameUri);
					detailInfoIntent.putExtra(DetailInfoActivity.INTENT_EXTRA_USERNAME,
							jidforchat);
					currentActivity.startActivity(detailInfoIntent);
				}
			});
		}
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 10;
		Bitmap bmp = null;

		if (!imageFileUrl.contains("content")) {
			bmp = BitmapFactory.decodeFile(imageFileUrl, options);
		} else if(!imageFileUrl.contains("http:")){
			try {
				Uri uri = Uri.parse(imageFileUrl);
				Log.i("Image File", imageFileUrl);
				bmp = BitmapFactory.decodeStream(mContext.getContentResolver()
						.openInputStream(uri), null, options);
			} catch (FileNotFoundException e) {
				Log.e("Exception", e.getMessage(), e);
			}
		}
		
		if(bmp != null){
			Log.i("Load Image", "success");
			//Canvas canvas = new Canvas(bmp.copy(Bitmap.Config.ARGB_8888, true));
			//Drawable drawable = new BitmapDrawable(getResource(), bmp);
			holder.image.setImageBitmap(bmp);
			/// 这一步必须要做,否则不会显示.
			//drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
			//holder.image.draw(canvas);
			//holder.image.setCompoundDrawables(drawable,null,null,null);
		} else {
			//设置图像为默认图像
		}
		holder.image.setOnClickListener(new LvButtonListener(imageFileUrl));
		holder.imageTime.setText(date);

		if (ds == ChatConstants.DS_UPLOADING) {
			holder.progressBar.setVisibility(View.VISIBLE);
			//holder.image.getBackground().setAlpha(20);
		//	holder.image.getDrawable().setAlpha(150);
		} else if (ds == ChatConstants.DS_UPLOAD_FAILED) {
			// 显示发送未成功标记
			holder.uploadstatus.setVisibility(View.VISIBLE);
			holder.progressBar.setVisibility(View.GONE);
		} else if(ds == ChatConstants.DS_UPLOAD_SUCCESS){
			holder.progressBar.setVisibility(View.GONE);
		} else if(ds == ChatConstants.DS_DOWNLOADING){
			
		} else if(ds == ChatConstants.DS_DOWNLOAD_SUCCESS){
			holder.progressBar.setVisibility(View.GONE);
		} else if(ds == ChatConstants.DS_DOWNLOAD_FAILED){
			
		}
	}
	private void bindFileData(ViewHolder holder, String chatTime, String jid,
			String FileUrl, int ds, boolean isMe) {
		holder.avatar.setBackgroundResource(R.mipmap.default_mobile_avatar);
		if(!isMe)
		{
			final String jidforchat = jid;
			holder.avatar.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Activity currentActivity = (Activity) v.getContext();
					Intent detailInfoIntent = new Intent(currentActivity, DetailInfoActivity.class); 
					Uri userNameUri = Uri.parse(jidforchat);
					detailInfoIntent.setData(userNameUri);
					detailInfoIntent.putExtra(DetailInfoActivity.INTENT_EXTRA_USERNAME,
							jidforchat);
					currentActivity.startActivity(detailInfoIntent);
				}
			});
		}
		Log.i("fileurl", FileUrl);
		holder.file.setOnClickListener(new FileButtonListener(FileUrl));
		
		if (ds == ChatConstants.DS_UPLOADING) {
			holder.progressBar.setVisibility(View.VISIBLE);
		//	holder.image.getBackground().setAlpha(20);
		//	holder.image.getDrawable().setAlpha(150);
		} else if (ds == ChatConstants.DS_UPLOAD_FAILED) {
			// 显示发送未成功标记
		//	holder.uploadstatus.setVisibility(View.VISIBLE);
		//	holder.progressBar.setVisibility(View.GONE);
		} else if(ds == ChatConstants.DS_UPLOAD_SUCCESS){
		//	holder.progressBar.setVisibility(View.GONE);
		} else if(ds == ChatConstants.DS_DOWNLOADING){
			
		} else if(ds == ChatConstants.DS_DOWNLOAD_SUCCESS){
		//	holder.progressBar.setVisibility(View.GONE);
		} else if(ds == ChatConstants.DS_DOWNLOAD_FAILED){
			
		}
	}

	private class LvButtonListener implements OnClickListener {
		private String url;

		LvButtonListener(String url) {
			this.url = url;
		}

		public Intent getIntent(String param) {
			Intent intent = new Intent("android.intent.action.VIEW");
			intent.addCategory("android.intent.category.DEFAULT");
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (url.contains(".amr")) {
				
				Log.i("recordURL", url);
				playMusic(url);
				return null;
			} else if (url.contains("content")) {
				intent.setDataAndType(Uri.parse(param), "image/*");
			} else {
				intent.setDataAndType(Uri.fromFile(new File(param)), "image/*");
			}
			return intent;
		}

		@Override
		public void onClick(View arg0) {
			Intent intent = getIntent(url);
			if (intent != null)
				mContext.startActivity(intent);
		}
	}
	
	private class FileButtonListener implements OnClickListener {
		private String url;

		FileButtonListener(String url) {
			this.url = url;
		}
		

		@Override
		public void onClick(View arg0) {
			DownloadFileThread thread = new DownloadFileThread(url, ChatConstants.MEDIA_TYPE_FILE);
			thread.start();
			
		}
	}

	private void playMusic(String name) {
		try {
			if (mMediaPlayer.isPlaying()) {
				mMediaPlayer.stop();
			}
			mMediaPlayer.reset();
			mMediaPlayer.setDataSource(name);
			mMediaPlayer.prepare();
			mMediaPlayer.start();
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {

				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static class ViewHolder {
		ImageView avatar;

		Button msgContent;
		TextView msgTime;
		
		Button audio;// 左边的声音
		TextView audioTime;// 左边的声音时间

		ImageView image;
		// ImageButton image;
		TextView imageTime;// 左边的声音时间
		
		ImageView uploadstatus;

		ProgressBar progressBar;
		
		Button file;
	}
	
	private class DownloadFileThread extends Thread
	{
		private String mUrl;
		private String mMediaType;

		public DownloadFileThread(String url, String mediaType)
		{
			mUrl = url;
			mMediaType = mediaType;
		}

		@Override
		public void run()
		{
			try
			{
				String fileName = mUrl.substring(mUrl.lastIndexOf(File.separator) + 1);
				String dirName = null;
				if (ChatConstants.MEDIA_TYPE_AUDIO.equals(mMediaType))
				{
					dirName = "wanglai_in" + File.separator + "Audio" + File.separator;
				} else if (ChatConstants.MEDIA_TYPE_FILE.equals(mMediaType))
				{
					dirName = "wanglai_in" + File.separator + "Files" + File.separator;
				}

				int result = HttpDownloader.downloadFile(mUrl, dirName, fileName);
				if (result == 0)
				{
					Log.i("filedownload","success");
			//		addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_SUCCESS, mTs, mMsg.getPacketID());
			//		String fromJID = mMsg.getFrom();
			//		mService.newMessage(fromJID, mMediaType);
				} else
				{
					Log.i("filedownload","failed");
			//		addChatMessageToDB(ChatConstants.INCOMING, mMsg, ChatConstants.DS_DOWNLOAD_FAILED, mTs, mMsg.getPacketID());
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

	}
}
