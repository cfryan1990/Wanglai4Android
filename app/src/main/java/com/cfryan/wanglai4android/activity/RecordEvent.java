package com.cfryan.wanglai4android.activity;

import java.io.File;

import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.util.SoundMeter;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RecordEvent
{
	private Activity mRecordEventContext;
	private final int RECORD_STOP = 1;
	private final int RECORD_PROCESSING = 2;
	
	private LinearLayout mRecordLoadingWindow;
	private LinearLayout mRecordProcessingWindow;
	
	private FrameLayout mChatBodyContainer;
	
	private ImageView mRecordCancelBtn;
	private ImageView mRecordCancelImage;
	private SoundMeter mSoundMeter;
	private View mRecordWindow;
	private LinearLayout mRecordCancelWindow;
	private ImageView mRecordVolume;
	private int mRecordFlag = RECORD_STOP;
	private Handler mHandler = new Handler();
	private String voiceName;
	private long mStartRecordTime;
	private long mEndRecordTime;
	private final int POLL_INTERVAL = 300;
	
	public int recordTime;   	//录音时间
	public String audioUrl;		//录音保存路径
	
	
	public RecordEvent(Activity activity, FrameLayout chatBodyContainer)
	{
		mRecordEventContext = activity;
		mChatBodyContainer = chatBodyContainer;
	}
	
	public void init(){
		mSoundMeter = new SoundMeter();
		//在聊天界面的layout里加入录音界面的layout
		mRecordWindow = View.inflate(mRecordEventContext, R.layout.record_window, null);
		mChatBodyContainer.addView(mRecordWindow);
		mRecordWindow.setVisibility(View.GONE);
		
		mRecordVolume = (ImageView) mRecordWindow.findViewById(R.id.record_volume);
		mRecordCancelBtn = (ImageView) mRecordWindow.findViewById(R.id.record_cancel_btn);
		mRecordCancelImage = (ImageView) mRecordWindow.findViewById(R.id.record_cancel_img);

		mRecordCancelWindow = (LinearLayout) mRecordWindow.findViewById(R.id.record_cancel_window);
		mRecordProcessingWindow = (LinearLayout) mRecordWindow.findViewById(R.id.record_processing_hint_window);
		mRecordLoadingWindow = (LinearLayout) mRecordWindow.findViewById(R.id.record_loading_hint_window);
	}
	
	public boolean RecordTouchEvent(Boolean isRecordState, TextView recordBtn, MotionEvent event)
	{
		System.out.println("0");

		if (isRecordState)
		{
			System.out.println("1");

			int[] record_btn_location = new int[2];
			recordBtn.getLocationInWindow(record_btn_location);
			int record_btn_location_Y = record_btn_location[1];
			int record_btn_location_X = record_btn_location[0];
			
			int[] record_cancel_window_location = new int[2];
			mRecordCancelWindow.getLocationInWindow(record_cancel_window_location);
			int record_cancel_window_location_Y = record_cancel_window_location[1];
			int record_cancel_window_location_x = record_cancel_window_location[0];
			
			if (event.getAction() == MotionEvent.ACTION_DOWN && mRecordFlag == RECORD_STOP)
			{
				if (!Environment.getExternalStorageDirectory().exists())
				{
					Toast.makeText(mRecordEventContext , "No SDCard", Toast.LENGTH_LONG).show();
					return false;
				}
				if (event.getY() > record_btn_location_Y && event.getX() > record_btn_location_X)
				{
					// 判断手势按下的位置是否是语音录制按钮的范围内
					System.out.println("2");

					recordBtn.setBackgroundResource(R.drawable.voice_rcd_btn_pressed);
					mRecordWindow.setVisibility(View.VISIBLE);
					mRecordLoadingWindow.setVisibility(View.VISIBLE);
					mRecordProcessingWindow.setVisibility(View.GONE);
					mHandler.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							mRecordLoadingWindow.setVisibility(View.GONE);
							mRecordProcessingWindow.setVisibility(View.VISIBLE);
						}
					}, 300);
					mRecordCancelBtn.setVisibility(View.VISIBLE);
					mRecordCancelWindow.setVisibility(View.GONE);
					mStartRecordTime = SystemClock.currentThreadTimeMillis();
					voiceName = mStartRecordTime + ".amr";
					startRecord(voiceName);
				}
			} else if (event.getAction() == MotionEvent.ACTION_UP && mRecordFlag == RECORD_PROCESSING)
			{
				// 松开手势时执行录制完成
				System.out.println("4");
				recordBtn.setBackgroundResource(R.drawable.voice_rcd_btn_nor);
				if (event.getY() >= record_cancel_window_location_Y
						&& event.getY() <= record_cancel_window_location_Y + mRecordCancelWindow.getHeight()
						&& event.getX() >= record_cancel_window_location_x
						&& event.getX() <= record_cancel_window_location_x + mRecordCancelWindow.getWidth())
				{

					mRecordWindow.setVisibility(View.GONE);
					mRecordCancelBtn.setVisibility(View.VISIBLE);
					mRecordCancelWindow.setVisibility(View.GONE);
					stopRecord();

					File file = new File(Environment.getExternalStorageDirectory() + "/Wanglai/Recordings/" + voiceName);
					if (file.exists())
					{
						file.delete();
					}

				} else
				{
					mRecordProcessingWindow.setVisibility(View.GONE);
					stopRecord();
					mEndRecordTime = SystemClock.currentThreadTimeMillis();
					
					recordTime = (int) ((mEndRecordTime - mStartRecordTime + 50) / (float) 100);
					audioUrl = Environment.getExternalStorageDirectory() + "/Wanglai/Recordings/" + voiceName;
					
					mRecordWindow.setVisibility(View.GONE);
					return true;
				}
			}

			if (event.getY() < record_btn_location_Y)
			{// 手势按下的位置不在语音录制按钮的范围内
				Animation mLitteAnimation = AnimationUtils.loadAnimation(mRecordEventContext, R.anim.cancel_rc);
				Animation mBigAnimation = AnimationUtils.loadAnimation(mRecordEventContext, R.anim.cancel_rc2);
				mRecordCancelBtn.setVisibility(View.GONE);
				mRecordCancelWindow.setVisibility(View.VISIBLE);
				mRecordCancelWindow.setBackgroundResource(R.mipmap.voice_rcd_cancel_bg);
				if (event.getY() >= record_cancel_window_location_Y
						&& event.getY() <= record_cancel_window_location_Y + mRecordCancelWindow.getHeight()
						&& event.getX() >= record_cancel_window_location_x
						&& event.getX() <= record_cancel_window_location_x + mRecordCancelWindow.getWidth())
				{
					mRecordCancelWindow.setBackgroundResource(R.mipmap.voice_rcd_cancel_bg_focused);
					mRecordCancelImage.startAnimation(mLitteAnimation);
					mRecordCancelImage.startAnimation(mBigAnimation);
				}
			} else
			{
				mRecordCancelBtn.setVisibility(View.VISIBLE);
				mRecordCancelWindow.setVisibility(View.GONE);
				mRecordCancelWindow.setBackgroundResource(0);
			}
		}
		return false;
	}
	
	private void startRecord(String name)
	{
		mSoundMeter.start(name);
		mRecordFlag = RECORD_PROCESSING;
		mHandler.postDelayed(mPollTask, POLL_INTERVAL);
	}

	private void stopRecord()
	{
		mHandler.removeCallbacks(mSleepTask);
		mHandler.removeCallbacks(mPollTask);
		mSoundMeter.stop();
		mRecordVolume.setImageResource(R.mipmap.amp1);
		mRecordFlag = RECORD_STOP;
	}
	
	private Runnable mSleepTask = new Runnable()
	{
		@Override
		public void run()
		{
			stopRecord();
		}
	};
	private Runnable mPollTask = new Runnable()
	{
		@Override
		public void run()
		{
			double amp = mSoundMeter.getAmplitude();
			updateDisplay(amp);
			mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		}
	};

	

	/**
	 * 录音音量UI更新
	 * 
	 * @param signalEMA
	 */
	private void updateDisplay(double signalEMA)
	{
		switch ((int) signalEMA)
		{
		case 0:
		case 1:
			mRecordVolume.setImageResource(R.mipmap.amp1);
			break;
		case 2:
		case 3:
			mRecordVolume.setImageResource(R.mipmap.amp2);

			break;
		case 4:
		case 5:
			mRecordVolume.setImageResource(R.mipmap.amp3);
			break;
		case 6:
		case 7:
			mRecordVolume.setImageResource(R.mipmap.amp4);
			break;
		case 8:
		case 9:
			mRecordVolume.setImageResource(R.mipmap.amp5);
			break;
		case 10:
		case 11:
			mRecordVolume.setImageResource(R.mipmap.amp6);
			break;
		default:
			mRecordVolume.setImageResource(R.mipmap.amp7);
			break;
		}
	}
}
