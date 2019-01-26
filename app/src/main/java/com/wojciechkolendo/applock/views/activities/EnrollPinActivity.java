package com.wojciechkolendo.applock.views.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;

import androidx.appcompat.app.AppCompatActivity;
import software.rsquared.androidlogger.Logger;
import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.models.LockTypeUtil;
import com.wojciechkolendo.applock.services.LockingAppService;

public class EnrollPinActivity extends AppCompatActivity {

    private IndicatorDots indicatorDots;

    private PinLockView pinLockView;

    private TextView pinLockInfo;

    private boolean isPinConfirm = false;
    private String pinCode;

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

    private PinLockListener mPinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            Logger.debug("Pin complete: " + pin);
            if (isPinConfirm) {
                if (pinCode.equals(pin)) {
                    // save the lock info here.
                    mService.saveLockInfo(pin, LockTypeUtil.TYPE_PIN);

                    isPinConfirm = false;
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EnrollPinActivity.this,
                            R.string.pin_lock_enroll_info_mismatch, Toast.LENGTH_LONG).show();
                    indicatorDots.setPinLength(0);
                    pinLockView.resetPinLockView();
                    isPinConfirm = false;
                    pinLockInfo.setText(R.string.pin_lock_enroll_step1_info);
                }
            } else {
                pinCode = pin;
                isPinConfirm = true;
                pinLockView.resetPinLockView();
                indicatorDots.setPinLength(0);
                pinLockInfo.setText(R.string.pin_lock_enroll_step2_info);
            }
        }

        @Override
        public void onEmpty() {
            Logger.debug("Pin empty");
            indicatorDots.setPinLength(0);
        }

        @Override
        public void onPinChange(int pinLength, String intermediatePin) {
            Logger.debug("Pin changed, new length " + pinLength + " with intermediate pin " + intermediatePin);
            indicatorDots.setPinLength(pinLength);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_pin);

        // bind to service.
        Intent intent = new Intent(this, LockingAppService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);

        pinLockInfo = findViewById(R.id.pin_lock_info);

        indicatorDots = findViewById(R.id.indicator_dots);
        indicatorDots.setPinLength(0);

        pinLockView = (PinLockView) findViewById(R.id.pin_lock_view);
        pinLockView.setPinLockListener(mPinLockListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }
}
