package com.wojciechkolendo.applock.views.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.models.LockTypeUtil;
import com.wojciechkolendo.applock.services.LockingAppService;
import com.wojciechkolendo.applock.views.custom.Lock9View;


public class EnrollPatternActivity extends AppCompatActivity {

    private TextView enrollInfo;

    private boolean isPatternConfirm = false;

    private String patternString;

    private LockingAppService.ServiceBinder mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = (LockingAppService.ServiceBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_pattern);

        // bind to service.
        Intent intent = new Intent(this, LockingAppService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);

        Lock9View patternView = findViewById(R.id.patternView);
        patternView.setCallBack(password -> {
            if (isPatternConfirm) {
                isPatternConfirm = false;
                if (patternString.equals(password)) {
                    // save lock info.
                    mService.saveLockInfo(patternString, LockTypeUtil.TYPE_PATTERN);
                    // stop ourselves
                    setResult(RESULT_OK);
                    finish();
                } else {
                    enrollInfo.setText(R.string.pattern_lock_enroll_step1_info);
                    Toast.makeText(EnrollPatternActivity.this,
                            R.string.pattern_lock_enroll_info_mismatch, Toast.LENGTH_LONG).show();
                }
            } else {
                patternString = password;
                isPatternConfirm = true;
                enrollInfo.setText(R.string.pattern_lock_enroll_step2_info);
            }
        });

        enrollInfo = findViewById(R.id.enroll_info);
        View btnCancel = findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }
}
