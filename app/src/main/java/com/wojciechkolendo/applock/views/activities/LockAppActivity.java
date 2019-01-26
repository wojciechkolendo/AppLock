package com.wojciechkolendo.applock.views.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.models.LockTypeUtil;
import com.wojciechkolendo.applock.services.LockingAppService;
import com.wojciechkolendo.applock.views.fragments.PatternLockFragment;
import com.wojciechkolendo.applock.views.fragments.PinLockFragment;

public class LockAppActivity extends AppCompatActivity {

	private boolean isLaunchByUser = false;

	private SharedPreferences mPrefs;
	private static final String LOCK_ENROLL_STATUS = "com.wojciechkolendo.applock.LOCK_ENROLL_STATUS";
	private static final String LOCK_ENROLLED = "ENROLLED";

	private LockingAppService.ServiceBinder mService;
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = (LockingAppService.ServiceBinder) service;
			mService.startFingerprintAuth();

			if (mService.getLockType() > 0) {
				addFragment(mService.getLockType());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fragment_holder);

		// get prefs
		mPrefs = getSharedPreferences(LOCK_ENROLL_STATUS, MODE_PRIVATE);

		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();
			Set<String> catagory = intent.getCategories();
			if (action != null &&
					action.equals(Intent.ACTION_MAIN) && catagory.contains(Intent.CATEGORY_LAUNCHER)) {
				isLaunchByUser = true;
			}
		}

		// start and bind service.
		Intent startServiceIntent = new Intent(LockAppActivity.this, LockingAppService.class);
		startService(startServiceIntent);
		bindService(startServiceIntent, mConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!isLockEnrolled()) {
			Intent intent = new Intent(LockAppActivity.this, AppListActivity.class);
			startActivity(intent);
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mService != null) {
			mService.startFingerprintAuth();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mService != null) {
			mService.cancelFingerprint();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (mService.getLockType() > 0) {
			addFragment(mService.getLockType());
		} else {
			Toast.makeText(LockAppActivity.this, R.string.no_lock_type_enrolled_warning, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (!isLaunchByUser) {
			Intent i = new Intent(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_HOME);
			startActivity(i);
		}
	}

	public LockingAppService.ServiceBinder getService() {
		return mService;
	}

	public boolean isLaunchFromHome() {
		return isLaunchByUser;
	}

	private void addFragment(int type) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		if (type == LockTypeUtil.TYPE_PIN) {
			PinLockFragment fragment = new PinLockFragment();
			transaction.add(R.id.lock_fragment_container, fragment);
		} else if (type == LockTypeUtil.TYPE_PATTERN) {
			PatternLockFragment fragment = new PatternLockFragment();
			transaction.add(R.id.lock_fragment_container, fragment);
		}
		transaction.commit();
	}

	private boolean isLockEnrolled() {
		return mPrefs.getBoolean(LOCK_ENROLLED, false);
	}
}
