package com.cfryan.wanglai4android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.cfryan.wanglai4android.R;
import com.cfryan.wanglai4android.util.PreferenceConstants;
import com.cfryan.wanglai4android.util.PreferenceUtils;


public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Handler mHandler = new Handler();
        String password = PreferenceUtils.getPrefString(this,
                PreferenceConstants.PASSWORD, "");
        if (!TextUtils.isEmpty(password)) {

            mHandler.postDelayed(gotoMainAct, 3000);

        } else {
            mHandler.postDelayed(gotoLoginAct, 1000);

        }

        setContentView(R.layout.activity_splash);
    }

    Runnable gotoLoginAct = new Runnable() {

        @Override
        public void run() {

            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    };

    Runnable gotoMainAct = new Runnable() {

        @Override
        public void run() {
            startActivity(new Intent(SplashActivity.this, WanglaiMainActivty.class));
            finish();
        }
    };
}
