package com.wojciechkolendo.applock.views.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.models.LockTypeUtil;
import com.wojciechkolendo.applock.services.LockingAppService;
import com.wojciechkolendo.applock.views.fragments.SettingsFragment;
import com.wojciechkolendo.applock.views.listeners.SettingsFragmentListener;

public class SettingsActivity extends AppCompatActivity implements SettingsFragmentListener {

	private static final int CODE_REQUEST_ENROLL_ACTIVITY = 500;
	private static final String LOCK_ENROLL_STATUS = "com.wojciechkolendo.applock.LOCK_ENROLL_STATUS";
	private static final String LOCK_ENROLLED = "ENROLLED";
	private SharedPreferences mPrefs;

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
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		initToolbar();

		// bind to service.
		Intent intent = new Intent(this, LockingAppService.class);
		bindService(intent, mConnection, BIND_AUTO_CREATE);

		// get prefs
		mPrefs = getSharedPreferences(LOCK_ENROLL_STATUS, MODE_PRIVATE);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new SettingsFragment()).commit();
		}

	}

	private void initToolbar() {
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case CODE_REQUEST_ENROLL_ACTIVITY:
				if (resultCode == RESULT_OK) {
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putBoolean(LOCK_ENROLLED, true);
					editor.apply();
					finish();
				}
				break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
	}

	private int getSelectedPos(int lockType) {
		if (lockType == LockTypeUtil.TYPE_PATTERN) {
			return 0;
		} else if (lockType == LockTypeUtil.TYPE_PIN) {
			return 1;
		}
		return -1;
	}

	@Override
	public int getCurrentLockType() {
		return getSelectedPos(mService.getLockType());
	}

	@Override
	public void onLockTypePreferenceChanged(int lockType) {
		if (lockType == 0) {
			Intent intent = new Intent(SettingsActivity.this, EnrollPatternActivity.class);
			startActivityForResult(intent, CODE_REQUEST_ENROLL_ACTIVITY);
		} else if (lockType == 1) {
			Intent intent = new Intent(SettingsActivity.this, EnrollPinActivity.class);
			startActivityForResult(intent, CODE_REQUEST_ENROLL_ACTIVITY);
		}
	}
}
