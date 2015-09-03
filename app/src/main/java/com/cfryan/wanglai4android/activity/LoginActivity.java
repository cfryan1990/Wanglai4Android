package com.cfryan.wanglai4android.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.exception.XXAdressMalformedException;
import com.cfryan.wanglai4android.service.IConnectionStatusCallback;
import com.cfryan.wanglai4android.service.XXService;
import com.cfryan.wanglai4android.util.ChangeLog;
import com.cfryan.wanglai4android.util.DialogUtil;
import com.cfryan.wanglai4android.util.L;
import com.cfryan.wanglai4android.util.PreferenceConstants;
import com.cfryan.wanglai4android.util.PreferenceUtils;
import com.cfryan.wanglai4android.util.T;
import com.cfryan.wanglai4android.util.XMPPHelper;

import java.lang.ref.WeakReference;

public class LoginActivity extends Activity implements
        IConnectionStatusCallback, TextWatcher, OnClickListener {
    public static final String LOGIN_ACTION = "com.cfryan.action.LOGIN";
    private static final int LOGIN_OUT_TIME = 0;
    private Button mLoginBtn;
    private Button mRegisterBtn;
    private EditText mAccountEt;
    private EditText mPasswordEt;
    private XXService mXxService;
    private Dialog mLoginDialog;
    private ConnectionOutTimeProcess mLoginOutTimeProcess;
    private String mAccount;
    private String mPassword;
    private myHandler mHandler;

    static class myHandler extends Handler {

        WeakReference<Context> mContext;
        WeakReference<ConnectionOutTimeProcess> mLoginOutTimeProcess;
        WeakReference<Dialog> mLoginDialog;

        myHandler(Context context, ConnectionOutTimeProcess connectionOutTimeProcess, Dialog dialog) {
            mContext = new WeakReference<>(context);
            mLoginOutTimeProcess = new WeakReference<>(connectionOutTimeProcess);
            mLoginDialog = new WeakReference<>(dialog);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case LOGIN_OUT_TIME:
                    if (mLoginOutTimeProcess != null
                            && mLoginOutTimeProcess.get().running)
                        mLoginOutTimeProcess.get().stop();
                    if (mLoginDialog != null && mLoginDialog.get().isShowing())
                        mLoginDialog.get().dismiss();
                    T.showShort(mContext.get(), R.string.timeout_try_again);
                    break;

                default:
                    break;
            }
        }
    }


    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXxService = ((XXService.XXBinder) service).getService();
            mXxService.registerConnectionStatusCallback(LoginActivity.this);
            // 开始连接xmpp服务器
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
        startService(new Intent(LoginActivity.this, XXService.class));
        bindXMPPService();
        setContentView(R.layout.activity_loginpage);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun()) {
            cl.getFullLogDialog().show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindXMPPService();
        if (mLoginOutTimeProcess != null) {
            mLoginOutTimeProcess.stop();
            mLoginOutTimeProcess = null;
        }
    }

    private void initView() {
        mAccountEt = (EditText) findViewById(R.id.et_account_name);
        mPasswordEt = (EditText) findViewById(R.id.et_account_password);
        mLoginBtn = (Button) findViewById(R.id.btn_login);
        mRegisterBtn = (Button) findViewById(R.id.btn_register);
        String account = PreferenceUtils.getPrefString(this,
                PreferenceConstants.ACCOUNT, "");
        String password = PreferenceUtils.getPrefString(this,
                PreferenceConstants.PASSWORD, "");
        if (!TextUtils.isEmpty(account))
            mAccountEt.setText(account);
        if (!TextUtils.isEmpty(password))
            mPasswordEt.setText(password);
        mAccountEt.addTextChangedListener(this);

        mLoginDialog = DialogUtil.getLoginDialog(this);
        mLoginOutTimeProcess = new ConnectionOutTimeProcess();
        mHandler = new myHandler(this, mLoginOutTimeProcess, mLoginDialog);

        mRegisterBtn.setOnClickListener(this);
    }

    public void onLoginClick(View v) {
        mAccount = mAccountEt.getText().toString();
        mAccount = splitAndSaveServer(mAccount);
        mPassword = mPasswordEt.getText().toString();
        if (TextUtils.isEmpty(mAccount)) {
            T.showShort(this, R.string.null_account_prompt);
            return;
        }
        if (TextUtils.isEmpty(mPassword)) {
            T.showShort(this, R.string.password_input_prompt);
            return;
        }
        if (mLoginOutTimeProcess != null && !mLoginOutTimeProcess.running)
            mLoginOutTimeProcess.start();
        if (mLoginDialog != null && !mLoginDialog.isShowing())
            mLoginDialog.show();
        if (mXxService != null) {
            mXxService.Login(mAccount, mPassword);
        }
    }

    private String splitAndSaveServer(String account) {
        if (!account.contains("@")) {
            PreferenceUtils.setPrefString(this, PreferenceConstants.Server, PreferenceConstants.DEFAULT_SERVER);
            return account;
        }

        String[] res = account.split("@");
        String userName = res[0];
        String server = res[1];
        PreferenceUtils.setPrefString(this, PreferenceConstants.Server, server);
        return userName;
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
        Intent mServiceIntent = new Intent(this, XXService.class);
        mServiceIntent.setAction(LOGIN_ACTION);
        bindService(mServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {

        if (s.toString().contains("@")) {
            try {
                XMPPHelper.verifyJabberID(s);
                mLoginBtn.setEnabled(true);
                mAccountEt.setTextColor(getResources().getColor(R.color.font_black));
            } catch (XXAdressMalformedException e) {
                mLoginBtn.setEnabled(false);
                mAccountEt.setTextColor(Color.RED);
            }
        } else {
            mLoginBtn.setEnabled(true);
            mAccountEt.setTextColor(getResources().getColor(R.color.font_black));
        }

    }

    private void save2Preferences() {
        boolean isAutoSavePassword = true;//mAutoSavePasswordCK.isChecked();
        boolean isUseTls = false;//mUseTlsCK.isChecked();
        boolean isSilenceLogin = false;//mSilenceLoginCK.isChecked();
        boolean isHideLogin = false;//mHideLoginCK.isChecked();
        PreferenceUtils.setPrefString(this, PreferenceConstants.ACCOUNT,
                mAccount);// 帐号是一直保存的
        if (isAutoSavePassword)
            PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
                    mPassword);
        else
            PreferenceUtils.setPrefString(this, PreferenceConstants.PASSWORD,
                    "");

        PreferenceUtils.setPrefBoolean(this, PreferenceConstants.REQUIRE_TLS,
                isUseTls);
        PreferenceUtils.setPrefBoolean(this, PreferenceConstants.SCLIENTNOTIFY,
                isSilenceLogin);
        if (isHideLogin)
            PreferenceUtils.setPrefString(this,
                    PreferenceConstants.STATUS_MODE, PreferenceConstants.XA);
        else
            PreferenceUtils.setPrefString(this,
                    PreferenceConstants.STATUS_MODE,
                    PreferenceConstants.AVAILABLE);
    }

    /**
     * 登录超时处理线程
     */
    class ConnectionOutTimeProcess implements Runnable {
        public boolean running = false;
        private long startTime = 0L;
        private Thread thread = null;

        ConnectionOutTimeProcess() {
        }

        @Override
        public void run() {
            while (true) {
                if (!this.running)
                    return;
                if (System.currentTimeMillis() - this.startTime > 20 * 1000L) {
                    //20秒未成功登录则超时，给handler发通知
                    mHandler.sendEmptyMessage(LOGIN_OUT_TIME);
                }
                try {
                    Thread.sleep(10L);
                } catch (Exception localException) {
                }
            }
        }

        public void start() {
            try {
                this.thread = new Thread(this);
                this.running = true;
                this.startTime = System.currentTimeMillis();
                this.thread.start();
            } finally {
            }
        }

        public void stop() {
            try {
                this.running = false;
                this.thread = null;
                this.startTime = 0L;
            } finally {
            }
        }
    }

    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        // TODO Auto-generated method stub
        if (mLoginDialog != null && mLoginDialog.isShowing())
            mLoginDialog.dismiss();
        if (mLoginOutTimeProcess != null && mLoginOutTimeProcess.running) {
            mLoginOutTimeProcess.stop();
            mLoginOutTimeProcess = null;
        }
        if (connectedState == XXService.CONNECTED) {
            save2Preferences();
            startActivity(new Intent(this, WanglaiMainActivty.class));
            finish();
        } else if (connectedState == XXService.DISCONNECTED)
            T.showLong(LoginActivity.this, getString(R.string.request_failed)
                    + reason);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_register:
//			Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
//			startActivity(intent);
                break;

        }

    }
}
