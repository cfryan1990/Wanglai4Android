package com.cfryan.wanglai4android.activity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jivesoftware.smack.packet.Message;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.FloatMath;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.adapter.ChatAdapter;
import com.cfryan.wanglai4android.adapter.FaceAdapter;
import com.cfryan.wanglai4android.adapter.FacePageAdapter;
import com.cfryan.wanglai4android.application.Wanglai;
import com.cfryan.wanglai4android.db.ChatProvider;
import com.cfryan.wanglai4android.db.RosterProvider;
import com.cfryan.wanglai4android.db.ChatProvider.ChatConstants;
import com.cfryan.wanglai4android.service.IConnectionStatusCallback;
import com.cfryan.wanglai4android.service.XXService;
import com.cfryan.wanglai4android.ui.view.CirclePageIndicator;
import com.cfryan.wanglai4android.ui.xlistview.MsgListView;
import com.cfryan.wanglai4android.ui.xlistview.MsgListView.IXListViewListener;
import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PreferenceConstants;
import com.cfryan.wanglai4android.util.PreferenceUtils;
import com.cfryan.wanglai4android.util.StatusMode;
import com.cfryan.wanglai4android.util.T;
import com.cfryan.wanglai4android.util.XMPPHelper;

public class ChatActivity extends Activity implements OnTouchListener, OnClickListener, IXListViewListener, IConnectionStatusCallback
{
	private boolean isCompressed = true;
	
	private static final int MEDIA_TYPE_IMAGE = 1;
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int TAKE_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	private static final int SEND_FILES_ACTIVITY_REQUEST_CODE = 102;
	private static final int SEND_BUSINESS_CARD_ACTIVITY_REQUEST_CODE = 103;

	private static final int SHOW_NOTHING = 1;
	private static final int SHOW_FACE_DIALOG = 2;
	private static final int SHOW_MEDIA_SELECT_DIALOG = 3;

	private static final int MSG_UPLOAD_BMP_SUCCESS = 0;
	private static final int MSG_UPLOAD_BMP_FAILED = 1;
	private static final int MSG_DOWNLOAD_SUCCESS = 2;
	private static final int MSG_DOWNLOAD_FAILURE = 3;

	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist = 1f;

	private int mWindowState = SHOW_NOTHING;

	private String mImageFileName;
	
	private LocationManager locationManager;
	private String provider;
	private Location location;
	private Address address;

	public static final String INTENT_EXTRA_USERNAME = ChatActivity.class.getName() + ".username";// 昵称对应的key
	private MsgListView mMsgListView;// 对话ListView
	private ViewPager mFaceViewPager;// 表情选择ViewPager
	private int mCurrentPage = 0;// 当前表情页
	private Button mSendMsgBtn;// 发送消息button

	private ImageButton mFaceBtn;// 切换键盘和表情的button
	// private ImageButton mKeyboardBtn;// 切换键盘和表情的button

	private ImageButton mAddMediaBtn; // 发送多媒体button
	private TextView mTitleNameView;// 标题栏
	private EditText mMsgInput;// 消息输入框
	private LinearLayout mFaceSelectWindow;// 表情父容器
	private WindowManager.LayoutParams mWindowNanagerParams;
	private InputMethodManager mInputMethodManager;
	private List<String> mFaceMapKeys;// 表情对应的字符串数组
	private String mPeerJabberID = null;// 当前聊天用户的ID

	private ImageView mRecordSwitcher;
	private TextView mRecordBtn;
	private boolean mIsRecordState = false;

	boolean mIsMediaSelectState = false;

	private RelativeLayout mInputSendContainer;
	// private FrameLayout mSelectFaceOrMediaWindow;
	
	private View mChatBody, mPreviewDialog;
	private ImageView mPreview;
	private Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();
	private CheckBox mIsCompressed;
	private Button mSendImage;
	
	//录音部分
	private RecordEvent mRecordEvent;  //录音对象
	private FrameLayout mChatBodyContainer; //聊天对话界面的布局容器（录音界面需要在此基础上添加上去）;

	private GridView mMediaSelectWindow;
	ArrayList<HashMap<String, Object>> mMediaGridViewData = new ArrayList<HashMap<String, Object>>();

	private static final String[] PROJECTION_FROM = new String[]
	{ ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE, ChatProvider.ChatConstants.DIRECTION, ChatProvider.ChatConstants.JID,
			ChatProvider.ChatConstants.MESSAGE, ChatProvider.ChatConstants.MEDIA_TYPE, ChatProvider.ChatConstants.MEDIA_URL,
			ChatProvider.ChatConstants.MEDIA_SIZE, ChatProvider.ChatConstants.DELIVERY_STATUS };// 查询字段

	private ContentObserver mContactObserver = new ContactObserver();// 联系人数据监听，主要是监听对方在线状态
	private XXService mXxService;// Main服务
	ServiceConnection mServiceConnection = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mXxService = ((XXService.XXBinder) service).getService();
			mXxService.registerConnectionStatusCallback(ChatActivity.this);
			// 如果没有连接上，则重新连接xmpp服务器
			if (!mXxService.isAuthenticated())
			{
				String usr = PreferenceUtils.getPrefString(ChatActivity.this, PreferenceConstants.ACCOUNT, "");
				String password = PreferenceUtils.getPrefString(ChatActivity.this, PreferenceConstants.PASSWORD, "");
				mXxService.Login(usr, password);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mXxService.unRegisterConnectionStatusCallback();
			mXxService = null;
		}

	};

	/**
	 * 解绑服务
	 */
	private void unbindXMPPService()
	{
		try
		{
			unbindService(mServiceConnection);
		} catch (IllegalArgumentException e)
		{
			L.e("Service wasn't bound!");
		}
	}

	/**
	 * 绑定服务
	 */
	private void bindXMPPService()
	{
		Intent mServiceIntent = new Intent(this, XXService.class);
		Uri chatURI = Uri.parse(mPeerJabberID);
		mServiceIntent.setData(chatURI);
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		initData();// 初始化数据
		initView();// 初始化view
		
		//得到聊天对话界面的布局（录音界面需要在此基础上添加上去）
		mChatBodyContainer = (FrameLayout) findViewById(R.id.chat_body);
		mRecordEvent = new RecordEvent(this, mChatBodyContainer); 
		mRecordEvent.init();
		
		setChatWindowAdapter();// 初始化对话数据
		getContentResolver().registerContentObserver(RosterProvider.CONTENT_URI, true, mContactObserver);// 开始监听联系人数据库
		
//		initLocation();
		
	}

	private void initLocation()
	{
		 //获取到LocationManager对象
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //创建一个Criteria对象
        Criteria criteria = new Criteria();
        //设置粗略精确度
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        //设置是否需要返回海拔信息
        criteria.setAltitudeRequired(false);
        //设置是否需要返回方位信息
        criteria.setBearingRequired(false);
        //设置是否允许付费服务
        criteria.setCostAllowed(true);
        //设置电量消耗等级
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        //设置是否需要返回速度信息
        criteria.setSpeedRequired(false);
 
        //根据设置的Criteria对象，获取最符合此标准的provider对象
        String currentProvider = locationManager.getBestProvider(criteria, true);
        Log.d("Location", "currentProvider: " + currentProvider);
        //根据当前provider对象获取最后一次位置信息
        Location currentLocation = locationManager.getLastKnownLocation(currentProvider);
        //如果位置信息为null，则请求更新位置信息
        if(currentLocation == null){
            locationManager.requestLocationUpdates(currentProvider, 0, 0, locationListener);
        }
        //直到获得最后一次位置信息为止，如果未获得最后一次位置信息，则显示默认经纬度
        //每隔10秒获取一次位置信息
        while(true){
            currentLocation = locationManager.getLastKnownLocation(currentProvider);
            if(currentLocation != null){
                Log.d("Location", "Latitude: " + currentLocation.getLatitude());
                Log.d("Location", "location: " + currentLocation.getLongitude());
                break;
            }else{
                Log.d("Location", "Latitude: " + 0);
                Log.d("Location", "location: " + 0);
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                 Log.e("Location", e.getMessage());
            }
        }
        
        //解析地址并显示
        Geocoder geoCoder = new Geocoder(this);
        try {
            int latitude = (int) currentLocation.getLatitude();
            int longitude = (int) currentLocation.getLongitude();
            List<Address> list = geoCoder.getFromLocation(latitude, longitude, 2);
            for(int i=0; i<list.size(); i++){
                Address address = list.get(i); 
                Toast.makeText(this, address.getCountryName() + address.getAdminArea() + address.getFeatureName(), Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this,e.getMessage(), Toast.LENGTH_LONG).show();
        }
		
	}

	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(android.os.Message msg)
		{ // 该方法是在UI主线程中执行
			switch (msg.what)
			{
			case MSG_UPLOAD_BMP_SUCCESS:
				sendImageMessage(mImageFileName, true);
				break;
			case MSG_UPLOAD_BMP_FAILED:
				Toast.makeText(ChatActivity.this, "R.string.error", Toast.LENGTH_LONG).show();
				alert();
				break;
			}
			super.handleMessage(msg);
		};
	};

	@Override
	protected void onResume()
	{
		super.onResume();
		updateContactStatus();// 更新联系人状态
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	// 查询联系人数据库字段
	private static final String[] STATUS_QUERY = new String[]
	{ RosterProvider.RosterConstants.STATUS_MODE, RosterProvider.RosterConstants.STATUS_MESSAGE, };
	private static final String TAG = null;

	private void updateContactStatus()
	{
		Cursor cursor = getContentResolver().query(RosterProvider.CONTENT_URI, STATUS_QUERY, RosterProvider.RosterConstants.JID + " = ?",
				new String[]
				{ mPeerJabberID }, null);
		int MODE_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE);
		int MSG_IDX = cursor.getColumnIndex(RosterProvider.RosterConstants.STATUS_MESSAGE);

		if (cursor.getCount() == 1)
		{
			cursor.moveToFirst();
			int status_mode = cursor.getInt(MODE_IDX);
			String status_message = cursor.getString(MSG_IDX);
			L.d("contact status changed: " + status_mode + " " + status_message);
			mTitleNameView.setText(XMPPHelper.splitJidAndServer(getIntent().getStringExtra(INTENT_EXTRA_USERNAME)));
			int statusId = StatusMode.values()[status_mode].getDrawableId();
//			if (statusId != -1)
//			{// 如果对应离线状态
//				// Drawable icon = getResources().getDrawable(statusId);
//				// mTitleNameView.setCompoundDrawablesWithIntrinsicBounds(icon,
//				// null,
//				// null, null);
//				mTitleStatusView.setImageResource(statusId);
//				mTitleStatusView.setVisibility(View.VISIBLE);
//			} else
//			{
//				mTitleStatusView.setVisibility(View.GONE);
//			}
		}
		cursor.close();
	}

	/**
	 * 联系人数据库变化监听
	 * 
	 */
	private class ContactObserver extends ContentObserver
	{
		public ContactObserver()
		{
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange)
		{
			L.d("ContactObserver.onChange: " + selfChange);
			updateContactStatus();// 联系人状态变化时，刷新界面
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (hasWindowFocus())
			unbindXMPPService();// 解绑服务
		getContentResolver().unregisterContentObserver(mContactObserver);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		// 窗口获取到焦点时绑定服务，失去焦点将解绑
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
	}

	private void initData()
	{
		mPeerJabberID = getIntent().getDataString().toLowerCase();// 获取聊天对象的id
		this.setTitle(mPeerJabberID.split("@")[0]);

		// 将表情map的key保存在数组中
		Set<String> keySet = Wanglai.getInstance().getFaceMap().keySet();
		mFaceMapKeys = new ArrayList<String>();
		mFaceMapKeys.addAll(keySet);
	}

	/**
	 * 附件选择框
	 */
	private void setMediaSelectWindowAdapter()
	{
		ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();

		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("ItemImage", R.drawable.app_panel_pic_icon);
		map.put("ItemText", "图片");
		data.add(map);

		HashMap<String, Object> map1 = new HashMap<String, Object>();
		map1.put("ItemImage", R.drawable.app_panel_video_icon);
		map1.put("ItemText", "拍照");
		data.add(map1);

		HashMap<String, Object> map2 = new HashMap<String, Object>();
		map2.put("ItemImage", R.drawable.app_panel_location_icon);
		map2.put("ItemText", "位置");
		data.add(map2);

		HashMap<String, Object> map3 = new HashMap<String, Object>();
		map3.put("ItemImage", R.drawable.app_panel_add_icon);
		map3.put("ItemText", "文档");
		data.add(map3);

		HashMap<String, Object> map4 = new HashMap<String, Object>();
		map4.put("ItemImage", R.mipmap.app_panel_businesscard_icon);
		map4.put("ItemText", "名片");
		data.add(map4);

		SimpleAdapter adapter = new SimpleAdapter(this, // 没什么解释
				data,// 数据来源
				R.layout.grid_view_item,// night_item的XML实现
				new String[]
				{ "ItemImage", "ItemText" }, new int[]
				{ R.id.ItemImage, R.id.ItemText });
		mMediaSelectWindow.setAdapter(adapter);
	}

	/**
	 * 设置聊天的Adapter
	 */
	private void setChatWindowAdapter()
	{
		String selection = ChatConstants.JID + "='" + mPeerJabberID + "'";// +
																			// "OR"
		Log.i("Jid", mPeerJabberID);
		// 异步查询数据库
		new AsyncQueryHandler(getContentResolver())
		{

			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor)
			{
				// ListAdapter adapter = new ChatWindowAdapter(cursor,
				// PROJECTION_FROM, PROJECTION_TO, mWithJabberID);
				ListAdapter adapter = new ChatAdapter(ChatActivity.this, cursor, PROJECTION_FROM);

				// ListAdapter adapter = new ChatAdapterEx(ChatActivity.this,
				// cursor);

				mMsgListView.setAdapter(adapter);
				mMsgListView.setSelection(adapter.getCount() - 1);
			}

		}.startQuery(0, null, ChatProvider.CONTENT_URI, PROJECTION_FROM, selection, null, null);
		// 同步查询数据库，建议停止使用,如果数据庞大时，导致界面失去响应
		// Cursor cursor = managedQuery(ChatProvider.CONTENT_URI,
		// PROJECTION_FROM,
		// selection, null, null);
		// ListAdapter adapter = new ChatWindowAdapter(cursor, PROJECTION_FROM,
		// PROJECTION_TO, mWithJabberID);
		// mMsgListView.setAdapter(adapter);
		// mMsgListView.setSelection(adapter.getCount() - 1);
	}

	private void initView()
	{
		mChatBody = findViewById(R.id.chat_body);
		mPreviewDialog = findViewById(R.id.preview_dialog);
		mPreview = (ImageView) findViewById(R.id.preview_picture);
		mPreview.setOnTouchListener(this);
		mPreview.setLongClickable(true);
		mIsCompressed = (CheckBox) findViewById(R.id.uncompressed_option);
		mSendImage = (Button) findViewById(R.id.send_image_btn);
		
		mSendImage.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(mIsCompressed.isChecked() == false)
				{
					isCompressed = false;
				}
				sendImageMessage(mImageFileName, isCompressed);
				mChatBody.setVisibility(View.VISIBLE);
				mMediaSelectWindow.setVisibility(View.VISIBLE);
				mPreviewDialog.setVisibility(View.GONE);
			}
		});

		mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		mWindowNanagerParams = getWindow().getAttributes();

		mTitleNameView = (TextView) findViewById(R.id.ui_titlebar_txt);

		mMsgListView = (MsgListView) findViewById(R.id.msg_listView);
		mMsgListView.setOnTouchListener(this);
		mMsgListView.setPullLoadEnable(false);
		mMsgListView.setXListViewListener(this);

		mRecordSwitcher = (ImageView) findViewById(R.id.record_switcher);
		mRecordSwitcher.setOnClickListener(this);

		mInputSendContainer = (RelativeLayout) findViewById(R.id.chat_input_send_container);
		mRecordBtn = (TextView) findViewById(R.id.record_btn);
		mRecordBtn.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1)
			{
				return false;
			}
		});

		mMsgInput = (EditText) findViewById(R.id.chat_msg_input);
		mMsgInput.setOnTouchListener(this);
		mMsgInput.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			@Override
			public void afterTextChanged(Editable s)
			{
				if (s.length() > 0)
				{
					mSendMsgBtn.setVisibility(View.VISIBLE);
					mAddMediaBtn.setVisibility(View.GONE);
				} else
				{
					mSendMsgBtn.setVisibility(View.GONE);
					mAddMediaBtn.setVisibility(View.VISIBLE);
				}
			}
		});

		mFaceBtn = (ImageButton) findViewById(R.id.face_btn);
		mFaceBtn.setOnClickListener(this);

		mSendMsgBtn = (Button) findViewById(R.id.send_msg_btn);
		mSendMsgBtn.setOnClickListener(this);

		mAddMediaBtn = (ImageButton) findViewById(R.id.add_media_btn);
		mAddMediaBtn.setOnClickListener(this);

		mFaceSelectWindow = (LinearLayout) findViewById(R.id.face_select_dialog);
		mFaceViewPager = (ViewPager) findViewById(R.id.face_pager);
		initFacePage();// 初始化表情

		mMediaSelectWindow = (GridView) findViewById(R.id.media_select_dialog);
		setMediaSelectWindowAdapter();
		mMediaSelectWindow.setOnItemClickListener(new ItemClickListener());

	}

	// 按下语音录制按钮时
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{

		if (!Environment.getExternalStorageDirectory().exists())
		{
			Toast.makeText(this, "No SDCard", Toast.LENGTH_LONG).show();
			return false;
		}

		if (!new File(Environment.getExternalStorageDirectory() + "/Wanglai/Recordings/").exists())
		{
			if (!createDir(Environment.getExternalStorageDirectory() + "/Wanglai/Recordings/"))
			{
				System.out.println("创建临时文件失败，不能创建临时文件所在的目录！");
				return false;
			}
		}

		boolean recordresult = mRecordEvent.RecordTouchEvent(mIsRecordState, mRecordBtn, event);
		
		if (recordresult)
		{
			sendAuidoMessage(mRecordEvent.audioUrl, mRecordEvent.recordTime + "");
			mMsgListView.setSelection(mMsgListView.getCount() - 1);
			
			Log.i("audioUrl", mRecordEvent.audioUrl);
			Log.i("audioTime", mRecordEvent.recordTime + "");
		}
		
		return super.onTouchEvent(event);
	}


	// 当AdapterView被单击(触摸屏或者键盘)，则返回的Item单击事件
	class ItemClickListener implements OnItemClickListener
	{
		/** Create a File for saving an image or video */
		private File getOutputMediaFile(int type)
		{
			File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Wanglai");

			if (!mediaStorageDir.exists())
			{
				if (!mediaStorageDir.mkdirs())
				{
					Log.d("Wanglai", "failed to create directory");
					return null;
				}
			}

			// Create a media file name
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
			File mediaFile = null;
			mImageFileName = mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg";
			if (type == MEDIA_TYPE_IMAGE)
			{
				mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
			}

			return mediaFile;
		}

		/** Create a file Uri for saving an image or video */
		private Uri getOutputMediaFileUri(int type)
		{
			return Uri.fromFile(getOutputMediaFile(type));
		}

		@Override
		public void onItemClick(AdapterView<?> view,// The AdapterView where the
													// click happened
				View arg1,// The view within the AdapterView that was clicked
				int position,// The position of the view in the adapter
				long arg3// The row id of the item that was clicked
		)
		{
			HashMap<String, Object> item = (HashMap<String, Object>) view.getItemAtPosition(position);

			String itemText = (String) item.get("ItemText");
			// mMsgInput.setText(itemText);

			int REQUEST_CODE;
			if ("图片".equals(itemText))
			{
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(intent, TAKE_IMAGE_ACTIVITY_REQUEST_CODE);

			} else if ("拍照".equals(itemText))
			{
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				Uri fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

				startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
			}
			else if ("位置".equals(itemText))
			{
				
			}
			else if ("文档".equals(itemText))
			{
//				Intent intent = new Intent(getApplicationContext(), FormFiles.class);
//				startActivityForResult(intent, SEND_FILES_ACTIVITY_REQUEST_CODE);
			}
			else if ("名片".equals(itemText))
			{
//				Intent intent = new Intent(getApplicationContext(), BusinessCardActivity.class);
//				startActivityForResult(intent, SEND_BUSINESS_CARD_ACTIVITY_REQUEST_CODE);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, final int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		BitmapFactory.Options options;
		Bitmap bmp;
		switch (requestCode)
		{
			
		case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
			
			mChatBody.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);
			mPreviewDialog.setVisibility(View.VISIBLE);

			options = new BitmapFactory.Options();
			options.inSampleSize = 5;
			bmp = BitmapFactory.decodeFile(mImageFileName, options);
			mPreview.setImageBitmap(bmp);
			break;
		case TAKE_IMAGE_ACTIVITY_REQUEST_CODE:

			mChatBody.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);
			mPreviewDialog.setVisibility(View.VISIBLE);

			if (resultCode == RESULT_OK)
			{
				if (data != null)
				{
					Uri uri = data.getData();
					mImageFileName = uri.toString();
					Log.i(TAG, "uri = " + uri);
					try
					{
						String[] pojo =
						{ MediaColumns.DATA };

						Cursor cursor = getContentResolver().query(uri, pojo, null, null, null);
						if (cursor != null)
						{
							int colunm_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
							cursor.moveToFirst();
							String path = cursor.getString(colunm_index);
							/***
							 * 这里加这样一个判断主要是为了第三方的软件选择，比如：使用第三方的文件管理器的话，
							 * 你选择的文件就不一定是图片了，这样的话，我们判断文件的后缀名 如果是图片格式的话，那么才可以
							 */
							if (path.endsWith("jpg") || path.endsWith("png"))
							{
								mImageFileName = path;
								options = new BitmapFactory.Options();
								options.inSampleSize = 5;
								bmp = BitmapFactory.decodeFile(mImageFileName, options);
								Log.i("eeeeeeee",mImageFileName);
								mPreview.setImageBitmap(bmp);
							} else
							{
								alert();
							}
						} else
						{
							alert();
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			break;
		case SEND_FILES_ACTIVITY_REQUEST_CODE:
			String filepath = data.getStringExtra("filepath");
			if(filepath.length() > 0)
			{
				Log.i("文件路径",filepath);
				sendFileMessage(filepath);
			}
			break;
		case SEND_BUSINESS_CARD_ACTIVITY_REQUEST_CODE:
			String businesscard = data.getStringExtra("businesscard");
			
			if (mXxService != null)
			{
				Message message = new Message();
				Log.i("JabberID", mPeerJabberID);
				message.setTo(mPeerJabberID);
				message.setBody(businesscard);
				message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_NORMAL);
				mXxService.sendMessage(message, ChatConstants.DS_SENT_OR_READ, false);

				if (!mXxService.isAuthenticated())
					T.showShort(this, "消息已经保存随后发送");
			}
		}
	}

	@Override
	public void onRefresh()
	{
		mMsgListView.stopRefresh();
	}

	@Override
	public void onLoadMore()
	{
		// do nothing
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.face_btn:
			mInputMethodManager.hideSoftInputFromWindow(mMsgInput.getWindowToken(), 0);
			try
			{
				Thread.sleep(80);// 解决此时会黑一下屏幕的问题
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			mMediaSelectWindow.setVisibility(View.GONE);
			mFaceSelectWindow.setVisibility(View.VISIBLE);
			mWindowState = SHOW_FACE_DIALOG;
			break;

		case R.id.send_msg_btn:// 发送消息
			sendTextMessageIfNotNull();
			break;

		case R.id.add_media_btn:
			mInputMethodManager.hideSoftInputFromWindow(mMsgInput.getWindowToken(), 0);
			try
			{
				Thread.sleep(80);// 解决此时会黑一下屏幕的问题
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.VISIBLE);
			mWindowState = SHOW_MEDIA_SELECT_DIALOG;

			break;

		case R.id.record_switcher:
			if (mIsRecordState)
			{
				mRecordSwitcher.setBackgroundResource(R.drawable.chatting_setmode_voice_btn);
				mInputSendContainer.setVisibility(View.VISIBLE);
				mRecordBtn.setVisibility(View.GONE);

			} else
			{
				mRecordSwitcher.setBackgroundResource(R.drawable.chatting_setmode_keyboard_btn);
				mRecordBtn.setVisibility(View.VISIBLE);
				mInputSendContainer.setVisibility(View.GONE);
			}

			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);
			mIsRecordState = !mIsRecordState;
			mWindowState = SHOW_NOTHING;
			break;

		default:
			break;
		}
	}

	private void sendTextMessageIfNotNull()
	{
		if (mMsgInput.getText().length() >= 1)
		{
			if (mXxService != null)
			{
				Message message = new Message();
				Log.i("JabberID", mPeerJabberID);
				message.setTo(mPeerJabberID);
				message.setBody(mMsgInput.getText().toString());
				message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_NORMAL);
				mXxService.sendMessage(message, ChatConstants.DS_SENT_OR_READ, false);

				if (!mXxService.isAuthenticated())
					T.showShort(this, "消息已经保存随后发送");
			}
			mMsgInput.setText(null);
			// mSendMsgBtn.setEnabled(false);
		}
	}

	private void sendAuidoMessage(String audioUrl, String audioTime)
	{
		if (mXxService != null)
		{
			Message message = new Message();
			message.setTo(mPeerJabberID);
			message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_AUDIO);
			message.setProperty("mediaUrl", audioUrl);
			message.setProperty("mediaSize", audioTime);

			Log.i("Message", "Send Audio");

			mXxService.sendMessage(message, ChatConstants.DS_UPLOADING, false);
			if (!mXxService.isAuthenticated())
				T.showShort(this, "消息已经保存随后发送");
		}
	}

	private void sendFileMessage(String fileUrl)
	{
		if (mXxService != null)
		{
			Message message = new Message();
			message.setTo(mPeerJabberID);
			message.setBody("");
			message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_FILE);
			message.setProperty(ChatConstants.MEDIA_URL, fileUrl);
			message.setProperty(ChatConstants.MEDIA_SIZE, "1.5");

			mXxService.sendMessage(message, ChatConstants.DS_UPLOADING, false);
			if (!mXxService.isAuthenticated())
				T.showShort(this, "消息已经保存随后发送");
		}
	}

	private void sendImageMessage(String imageUrl, Boolean compress)
	{
		if (mXxService != null)
		{
			Message message = new Message();
			message.setTo(mPeerJabberID);
			message.setProperty(ChatConstants.MEDIA_TYPE, ChatConstants.MEDIA_TYPE_IMAGE);
			message.setProperty(ChatConstants.MEDIA_URL, imageUrl);
			message.setProperty(ChatConstants.MEDIA_SIZE, "10.5");

			mXxService.sendMessage(message, ChatConstants.DS_UPLOADING, compress);
			if (!mXxService.isAuthenticated())
				T.showShort(this, "消息已经保存随后发送");
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		//预览图片时多点触控实现缩放和移动
		if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP)
			Log.d("Infor", "多点操作");
		switch (event.getActionMasked())
		{
		case MotionEvent.ACTION_DOWN:
			matrix.set(mPreview.getImageMatrix());
			savedMatrix.set(matrix);
			start.set(event.getX(), event.getY());
			Log.d("Infor", "触摸了...");
			mode = DRAG;
			break;
		case MotionEvent.ACTION_POINTER_DOWN: // 多点触控
			oldDist = this.spacing(event);
			if (oldDist > 10f)
			{
				Log.d("Infor", "oldDist" + oldDist);
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
			}
			break;
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG)
			{ // 此实现图片的拖动功能...
				matrix.set(savedMatrix);
				matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
			} else if (mode == ZOOM)
			{// 此实现图片的缩放功能...
				float newDist = spacing(event);
				if (newDist > 10)
				{
					matrix.set(savedMatrix);
					float scale = newDist / oldDist;
					matrix.postScale(scale, scale, mid.x, mid.y);
				}
			}
			break;
		}
		mPreview.setImageMatrix(matrix);

		switch (v.getId())
		{
		case R.id.msg_listView:
			mInputMethodManager.hideSoftInputFromWindow(mMsgInput.getWindowToken(), 0);
			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);

			mWindowState = SHOW_NOTHING;

			break;

		case R.id.chat_msg_input:
			mInputMethodManager.showSoftInput(mMsgInput, 0);
			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);

			if (mMsgInput.getText().length() > 0)
			{
				mSendMsgBtn.setVisibility(View.VISIBLE);
				mAddMediaBtn.setVisibility(View.GONE);
			} else
			{
				mSendMsgBtn.setVisibility(View.GONE);
				mAddMediaBtn.setVisibility(View.VISIBLE);
			}
			// mSendMsgBtn.setVisibility(View.VISIBLE);
			// mAddMediaBtn.setVisibility(View.GONE);

			mWindowState = SHOW_NOTHING;

			break;

		default:
			break;
		}
		return false;
	}

	private void initFacePage()
	{
		// TODO Auto-generated method stub
		List<View> lv = new ArrayList<View>();
		for (int i = 0; i < Wanglai.NUM_PAGE; ++i)
			lv.add(getGridView(i));

		FacePageAdapter adapter = new FacePageAdapter(lv);
		mFaceViewPager.setAdapter(adapter);
		mFaceViewPager.setCurrentItem(mCurrentPage);

		CirclePageIndicator indicator = (CirclePageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(mFaceViewPager);
		adapter.notifyDataSetChanged();
		mFaceSelectWindow.setVisibility(View.GONE);
		indicator.setOnPageChangeListener(new OnPageChangeListener()
		{

			@Override
			public void onPageSelected(int arg0)
			{
				mCurrentPage = arg0;
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2)
			{
				// do nothing
			}

			@Override
			public void onPageScrollStateChanged(int arg0)
			{
				// do nothing
			}
		});

	}

	private GridView getGridView(int i)
	{
		// TODO Auto-generated method stub
		GridView gv = new GridView(this);
		gv.setNumColumns(7);
		gv.setSelector(new ColorDrawable(Color.TRANSPARENT));// 屏蔽GridView默认点击效果
		gv.setBackgroundColor(Color.TRANSPARENT);
		gv.setCacheColorHint(Color.TRANSPARENT);
		gv.setHorizontalSpacing(1);
		gv.setVerticalSpacing(1);
		gv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		gv.setGravity(Gravity.CENTER);
		gv.setAdapter(new FaceAdapter(this, i));
		gv.setOnTouchListener(forbidenScroll());
		gv.setOnItemClickListener(new OnItemClickListener()
		{

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
			{
				// TODO Auto-generated method stub
				if (arg2 == Wanglai.NUM)
				{// 删除键的位置
					int selection = mMsgInput.getSelectionStart();
					String text = mMsgInput.getText().toString();
					if (selection > 0)
					{
						String text2 = text.substring(selection - 1);
						if ("]".equals(text2))
						{
							int start = text.lastIndexOf("[");
							int end = selection;
							mMsgInput.getText().delete(start, end);
							return;
						}
						mMsgInput.getText().delete(selection - 1, selection);
					}
				} else
				{
					int count = mCurrentPage * Wanglai.NUM + arg2;
					// 注释的部分，在EditText中显示字符串
					// String ori = msgEt.getText().toString();
					// int index = msgEt.getSelectionStart();
					// StringBuilder stringBuilder = new StringBuilder(ori);
					// stringBuilder.insert(index, keys.get(count));
					// msgEt.setText(stringBuilder.toString());
					// msgEt.setSelection(index + keys.get(count).length());

					// 下面这部分，在EditText中显示表情
					Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
							(Integer) Wanglai.getInstance().getFaceMap().values().toArray()[count]);
					if (bitmap != null)
					{
						int rawHeigh = bitmap.getHeight();
						int rawWidth = bitmap.getHeight();
						int newHeight = 40;
						int newWidth = 40;
						// 计算缩放因子
						float heightScale = ((float) newHeight) / rawHeigh;
						float widthScale = ((float) newWidth) / rawWidth;
						// 新建立矩阵
						Matrix matrix = new Matrix();
						matrix.postScale(heightScale, widthScale);
						// 设置图片的旋转角度
						// matrix.postRotate(-30);
						// 设置图片的倾斜
						// matrix.postSkew(0.1f, 0.1f);
						// 将图片大小压缩
						// 压缩后图片的宽和高以及kB大小均会变化
						Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, rawWidth, rawHeigh, matrix, true);
						ImageSpan imageSpan = new ImageSpan(ChatActivity.this, newBitmap);
						String emojiStr = mFaceMapKeys.get(count);
						SpannableString spannableString = new SpannableString(emojiStr);
						spannableString.setSpan(imageSpan, emojiStr.indexOf('['), emojiStr.indexOf(']') + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						mMsgInput.append(spannableString);
					} else
					{
						String ori = mMsgInput.getText().toString();
						int index = mMsgInput.getSelectionStart();
						StringBuilder stringBuilder = new StringBuilder(ori);
						stringBuilder.insert(index, mFaceMapKeys.get(count));
						mMsgInput.setText(stringBuilder.toString());
						mMsgInput.setSelection(index + mFaceMapKeys.get(count).length());
					}
				}
			}
		});
		return gv;
	}

	// 防止乱pageview乱滚动
	private OnTouchListener forbidenScroll()
	{
		return new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if (event.getAction() == MotionEvent.ACTION_MOVE)
				{
					Log.i("PapeView", "OnTouch");
					return true;
				}
				return false;
			}
		};
	}

	@Override
	public void connectionStatusChanged(int connectedState, String reason)
	{
		// TODO Auto-generated method stub

	}

	public enum Type
	{
		normal,

		chat,

		groupchat,

		headline,

		error;

		public static Type fromString(String name)
		{
			try
			{
				return Type.valueOf(name);
			} catch (Exception e)
			{
				return normal;
			}
		}
	}

	private void Nav(String url)
	{
		Uri uri = Uri.parse(url);
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{

		if (keyCode == KeyEvent.KEYCODE_BACK && SHOW_NOTHING != mWindowState)
		{
			mFaceSelectWindow.setVisibility(View.GONE);
			mMediaSelectWindow.setVisibility(View.GONE);
			mWindowState = SHOW_NOTHING;

			if (mMsgInput.getText().length() > 0)
			{
				mSendMsgBtn.setVisibility(View.VISIBLE);
				mAddMediaBtn.setVisibility(View.GONE);
			} else
			{
				mSendMsgBtn.setVisibility(View.GONE);
				mAddMediaBtn.setVisibility(View.VISIBLE);
			}

			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	/*
	 * private class DownloadImgThread extends Thread{ private String mImageUrl;
	 * 
	 * public DownloadImgThread(String imageUrl){ mImageUrl = imageUrl; }
	 * 
	 * @Override public void run() { try { Bitmap bmp =
	 * ImageService.getImage(mImageUrl); if(bmp != null){
	 * handler.obtainMessage(MSG_SUCCESS, bmp).sendToTarget(); }else {
	 * handler.obtainMessage(MSG_FAILURE).sendToTarget(); } } catch (Exception
	 * e) { e.printStackTrace(); } } }
	 */

	private void alert()
	{
		Dialog dialog = new AlertDialog.Builder(this).setTitle("提示").setMessage("上传文件失败")
				.setPositiveButton("确定", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						// mImageUrl = null;
					}
				}).create();
		dialog.show();
	}

	public static boolean createDir(String destDirName)
	{
		File dir = new File(destDirName);
		if (dir.exists())
		{
			System.out.println("创建目录" + destDirName + "失败，目标目录已经存在");
			return false;
		}
		if (!destDirName.endsWith(File.separator))
		{
			destDirName = destDirName + File.separator;
		}
		// 创建目录
		if (dir.mkdirs())
		{
			System.out.println("创建目录" + destDirName + "成功！");
			return true;
		} else
		{
			System.out.println("创建目录" + destDirName + "失败！");
			return false;
		}
	}

	private float spacing(MotionEvent event)
	{

		float x = event.getX(0) - event.getX(1);

		float y = event.getY(0) - event.getY(1);

		return FloatMath.sqrt(x * x + y * y);

	}

	private void midPoint(PointF point, MotionEvent event)
	{

		float x = event.getX(0) + event.getX(1);

		float y = event.getY(0) + event.getY(1);

		point.set(x / 2, y / 2);

	}

	
	
	//创建位置监听器
    private LocationListener locationListener = new LocationListener(){
        //位置发生改变时调用
        @Override
        public void onLocationChanged(Location location) {
            Log.d("Location", "onLocationChanged");
            Log.d("Location", "onLocationChanged Latitude" + location.getLatitude());
                 Log.d("Location", "onLocationChanged location" + location.getLongitude());
        }

        //provider失效时调用
        @Override
        public void onProviderDisabled(String provider) {
            Log.d("Location", "onProviderDisabled");
        }

        //provider启用时调用
        @Override
        public void onProviderEnabled(String provider) {
            Log.d("Location", "onProviderEnabled");
        }

        //状态改变时调用
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d("Location", "onStatusChanged");
        }
    };
    
}
