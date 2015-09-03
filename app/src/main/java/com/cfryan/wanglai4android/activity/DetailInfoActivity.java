package com.cfryan.wanglai4android.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.cfryan.wanglai4android.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class DetailInfoActivity  extends Activity{

	public static final String INTENT_EXTRA_USERNAME =  DetailInfoActivity.class
			.getName() + ".username";
	
	private String mAliasName = null;
	private String mPeerJabberID = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail_info);
		
		TextView tvAliasName = (TextView) findViewById(R.id.tv_detail_info_alias_name);
		TextView tvJidName = (TextView) findViewById(R.id.tv_detail_info_jid);
		ImageView tvAvatar = (ImageView) findViewById(R.id.tv_detail_info_avatar);
		
		mAliasName = (String) getIntent().getCharSequenceExtra(INTENT_EXTRA_USERNAME);
		tvAliasName.setText(mAliasName);
		
		mPeerJabberID = getIntent().getDataString().toLowerCase();
		tvJidName.setText(mAliasName+"("+mPeerJabberID.split("@")[0]+")");
		
		File file = new File("mnt/sdcard/Wanglai/Avatar/" + mPeerJabberID + ".png");
		if (file.exists())
		{
			Bitmap bitmap = getLoacalBitmap("mnt/sdcard/Wanglai/Avatar/" + mPeerJabberID + ".png"); // 从本地取图片(在cdcard中获取)
			// 设置Bitmap
			tvAvatar.setImageBitmap(bitmap);
			// 设置Imageview
		}
		
		Button btnSendMessage = (Button) findViewById(R.id.btn_detail_info_send_msg);
		btnSendMessage.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				startChatActivity(mPeerJabberID, mAliasName);
				finish();
			}
		});
	}
	
	private void startChatActivity(String userJid, String userName)
	{
		Intent chatIntent = new Intent(DetailInfoActivity.this, ChatActivity.class);
		Uri userNameUri = Uri.parse(userJid);
		chatIntent.setData(userNameUri);
		chatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, userName);
		startActivity(chatIntent);
	}
	
	/**
	 * 加载本地图片
	 * 
	 * @param url
	 * @return
	 */
	public static Bitmap getLoacalBitmap(String url)
	{
		try
		{
			FileInputStream fis = new FileInputStream(url);
			return BitmapFactory.decodeStream(fis); // /把流转化为Bitmap图片

		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
	}


}
