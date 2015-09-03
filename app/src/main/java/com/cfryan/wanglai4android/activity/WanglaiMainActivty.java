package com.cfryan.wanglai4android.activity;

import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.application.XXBroadcastReceiver;
import com.cfryan.wanglai4android.fragment.ContactFragment;
import com.cfryan.wanglai4android.fragment.RecentChatFragment;
import com.cfryan.wanglai4android.service.IConnectionStatusCallback;
import com.cfryan.wanglai4android.service.XXService;
//import com.cfryan.wanglai4android.util.ActivityManager;
import com.cfryan.wanglai4android.util.ChangeLog;
import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PreferenceConstants;
import com.cfryan.wanglai4android.util.PreferenceUtils;
import com.cfryan.wanglai4android.util.T;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class WanglaiMainActivty extends Activity implements IConnectionStatusCallback {

    private String linkStatus = "";
    private int mCurrentIndex;
    private XXService mXxService;

    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private RadioGroup radioGroup;
    private TextView mTitle;
    private ImageView mLeftBtn;
    private ProgressBar mTitleProgressBar;

    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXxService = ((XXService.XXBinder) service).getService();
            mXxService.registerConnectionStatusCallback(WanglaiMainActivty.this);
            // 开始连接xmpp服务器
            if (!mXxService.isAuthenticated()) {
                String usr = PreferenceUtils.getPrefString(WanglaiMainActivty.this,
                        PreferenceConstants.ACCOUNT, "");
                String password = PreferenceUtils.getPrefString(
                        WanglaiMainActivty.this, PreferenceConstants.PASSWORD, "");
                mXxService.Login(usr, password);
                //       mXxService.setStatusFromConfig();

            } else {
                /*
                 * mTitleNameView.setText(XMPPHelper
				 * .splitJidAndServer(PreferenceUtils.getPrefString(
				 * MainActivity.this, PreferenceConstants.ACCOUNT, "")));
				 * setStatusImage(true);
				 */
                mXxService.setStatusFromConfig();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXxService.unRegisterConnectionStatusCallback();
            mXxService = null;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(WanglaiMainActivty.this, XXService.class));
        setContentView(R.layout.activity_main_tab);

        mTitle = (TextView) findViewById(R.id.ui_titlebar_txt);
        mLeftBtn = (ImageView) findViewById(R.id.ui_titlebar_back_btn);
        mLeftBtn.setVisibility(View.GONE);

        mTitleProgressBar = (ProgressBar) findViewById(R.id.ivTitleProgress);

        fragmentManager = getFragmentManager();
        radioGroup = (RadioGroup) findViewById(R.id.rg_tab);
        ((RadioButton) radioGroup.findViewById(R.id.RB_0)).setChecked(true);

        transaction = fragmentManager.beginTransaction();

        Fragment fragment = new RecentChatFragment();
        transaction.replace(R.id.content, fragment);
        transaction.commit();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // 此处chenckID为radiogroup中radioButton的R.id
                changeFragmentByIndex(checkedId);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindXMPPService();
//        getContentResolver().registerContentObserver(
//                RosterProvider.CONTENT_URI, true, mRosterObserver);
//        setStatusImage(isConnected());
//        // if (!isConnected())
//        // mTitleNameView.setText(R.string.login_prompt_no);
//        mRosterAdapter.requery();
//        XXBroadcastReceiver.mListeners.add(this);
//        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
//            mNetErrorView.setVisibility(View.VISIBLE);
//        else
//            mNetErrorView.setVisibility(View.GONE);
//        ChangeLog cl = new ChangeLog(this);
//        if (cl != null && cl.firstRun()) {
//            cl.getFullLogDialog().show();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //    getContentResolver().unregisterContentObserver(mRosterObserver);
        unbindXMPPService();
        XXBroadcastReceiver.mListeners.remove(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            L.i(LoginActivity.class, "[SERVICE] Unbind");
        } catch (IllegalArgumentException e) {
            L.e(LoginActivity.class, "Service wasn't bound!");
        }
    }

    private void bindXMPPService() {
        L.i(LoginActivity.class, "[SERVICE] Unbind");
        bindService(new Intent(WanglaiMainActivty.this, XXService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE
                        + Context.BIND_DEBUG_UNBIND);
    }

    private void changeFragmentByIndex(int index) {

        switch (index) {
            case R.id.RB_0:
                transaction = fragmentManager.beginTransaction();
                Fragment fragment1 = new RecentChatFragment();
                transaction.replace(R.id.content, fragment1);
                transaction.commit();
                mTitle.setText(linkStatus.endsWith("") ? getString(R.string.main_tab_message) : linkStatus);
                mCurrentIndex = 0;
                break;

            case R.id.RB_1:
                transaction = fragmentManager.beginTransaction();
                Fragment fragment2 = new ContactFragment();
                transaction.replace(R.id.content, fragment2);
                transaction.commit();
                mTitle.setText(getString(R.string.main_tab_contact));
                mCurrentIndex = 1;
                break;
            case R.id.RB_2:
                transaction = fragmentManager.beginTransaction();
                Fragment fragment3 = new ContactFragment();
                transaction.replace(R.id.content, fragment3);
                transaction.commit();
                mTitle.setText(linkStatus.endsWith("") ? getString(R.string.main_tab_bulletin) : linkStatus);
                mCurrentIndex = 2;
                break;
            case R.id.RB_3:
                transaction = fragmentManager.beginTransaction();
                Fragment fragment4 = new ContactFragment();
                transaction.replace(R.id.content, fragment4);
                transaction.commit();
                mTitle.setText(getString(R.string.main_tab_setting));
                mCurrentIndex = 3;
                break;
        }
    }


    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        switch (connectedState) {
            case XXService.CONNECTED:
                linkStatus = "";
                // mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
                // .getPrefString(MainActivity.this,
                // PreferenceConstants.ACCOUNT, "")));
                mTitleProgressBar.setVisibility(View.GONE);
                // setStatusImage(true);
                if (mCurrentIndex == 0)
                {
                    mTitle.setText(getString(R.string.main_tab_message));
                }else if (mCurrentIndex == 2)
                {
                    mTitle.setText(getString(R.string.main_tab_bulletin));
                }
                break;
            case XXService.CONNECTING:
                linkStatus = getString(R.string.login_prompt_msg);
                mTitleProgressBar.setVisibility(View.VISIBLE);
                if (mCurrentIndex == 0 || mCurrentIndex == 2) {
                    mTitle.setText(linkStatus);
                }
                break;
            case XXService.DISCONNECTED:
                linkStatus = getString(R.string.login_prompt_no);
                mTitleProgressBar.setVisibility(View.GONE);
                if (mCurrentIndex == 0 || mCurrentIndex == 2) {
                    mTitle.setText(linkStatus);
                }
                T.showLong(this, reason);
                break;

            default:
                break;
        }

    }


//	@Override
//	public XXService getService() {
//		// TODO Auto-generated method stub
//		return this.mXxService;
//	}
//
//
//	@Override
//	public WanglaiMainActivty getMainActivity() {
//		// TODO Auto-generated method stub
//		return null;
//	}

}