package com.wojciechkolendo.applock.views.activities;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import software.rsquared.androidlogger.Logger;
import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.events.AppListForegroundEvent;
import com.wojciechkolendo.applock.services.LockingAppService;
import com.wojciechkolendo.applock.utils.HomeKeyWatcher;
import com.wojciechkolendo.applock.views.adapters.AppAdapter;

public class AppListActivity extends AppCompatActivity {

	public static final int CODE_REQUEST_PERMISSION = 100;
	public static final int CODE_START_SETTINGS = 101;

	private AppAdapter mProtectedAppAdapter;
	private AppAdapter mUnprotectedAppAdapter;

	private HomeKeyWatcher mHomeKeyWatcher;

	private LockingAppService.ServiceBinder mService;

	private SharedPreferences mPrefs;
	private static final String LOCK_ENROLL_STATUS = "com.wojciechkolendo.applock.LOCK_ENROLL_STATUS";
	private static final String LOCK_ENROLLED = "ENROLLED";

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = (LockingAppService.ServiceBinder) service;

			if (mService != null) {
				final RecyclerView protectedRecyclerView = findViewById(R.id.protected_app_list_view);
				protectedRecyclerView.addItemDecoration(new DividerItemDecoration(AppListActivity.this, DividerItemDecoration.VERTICAL));
				mProtectedAppAdapter = new AppAdapter(AppAdapter.TYPE_PROTECTED, mService.getProtectedAppList(), info -> {
					mService.markToUnprotect(info);
					mUnprotectedAppAdapter.notifyDataSetChanged();
					mProtectedAppAdapter.notifyDataSetChanged();
				});
				protectedRecyclerView.setAdapter(mProtectedAppAdapter);

				RecyclerView unprotectedRecyclerView = findViewById(R.id.unprotected_app_list_view);
				unprotectedRecyclerView.addItemDecoration(new DividerItemDecoration(AppListActivity.this, DividerItemDecoration.VERTICAL));
				mUnprotectedAppAdapter = new AppAdapter(AppAdapter.TYPE_UNPROTECTED, mService.getUnProtectedAppList(), info -> {
					int size = mService.markToProtect(info);
					mUnprotectedAppAdapter.notifyDataSetChanged();
					mProtectedAppAdapter.notifyDataSetChanged();
					protectedRecyclerView.smoothScrollToPosition(size);
				});
				unprotectedRecyclerView.setAdapter(mUnprotectedAppAdapter);
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
		setContentView(R.layout.activity_main);

		// get prefs
		mPrefs = getSharedPreferences(LOCK_ENROLL_STATUS, MODE_PRIVATE);

		// bind to service
		Intent intent = new Intent(AppListActivity.this, LockingAppService.class);
		bindService(intent, mConnection, BIND_AUTO_CREATE);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton doneFab = findViewById(R.id.fab_done);
		doneFab.setOnClickListener(view -> {
			if (mService.isProtectedAppListChanged()) {
				mService.saveProtectList();
				Snackbar.make(view, getString(R.string.snack_info_saved), Snackbar.LENGTH_LONG)
						.show();
			} else {
				Snackbar.make(view, getString(R.string.snack_info_no_change), Snackbar.LENGTH_LONG)
						.show();
			}
		});

		// check if we have PACKAGE_USAGE_STATS permission.
		if (!checkIfGetPermission()) {
			showPermissionRequestDialog();
		}

		// watch for home key press event.
		mHomeKeyWatcher = new HomeKeyWatcher(this);
		mHomeKeyWatcher.setOnHomePressedListener(new HomeKeyWatcher.OnHomePressedListener() {
			@Override
			public void onHomePressed() {
				if (mService.isProtectedAppListChanged()) {
					Toast.makeText(AppListActivity.this,
							R.string.toast_info_config_not_saved, Toast.LENGTH_LONG).show();
				}
			}

			@Override
			public void onHomeLongPressed() {
				// do nothing for now.
			}
		});
		mHomeKeyWatcher.startWatch();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!isLockEnrolled()) {
			Toast.makeText(AppListActivity.this, R.string.first_start_info, Toast.LENGTH_LONG).show();
			Intent intent = new Intent(AppListActivity.this, SettingsActivity.class);
			startActivityForResult(intent, CODE_START_SETTINGS);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		EventBus.getDefault().post(new AppListForegroundEvent(true));
	}

	@Override
	protected void onStop() {
		Logger.debug("onStop");
		super.onStop();
		EventBus.getDefault().post(new AppListForegroundEvent(false));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				Intent intent = new Intent(AppListActivity.this, SettingsActivity.class);
				startActivity(intent);
				break;
			default:
				break;
		}

		return true;
	}

	@Override
	protected void onDestroy() {
		Logger.debug("onDestroy");
		super.onDestroy();
		unbindService(mConnection);
		mHomeKeyWatcher.stopWatch();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case CODE_REQUEST_PERMISSION:
				if (!checkIfGetPermission()) {
					Toast.makeText(AppListActivity.this,
							R.string.toast_info_request_permission_failed, Toast.LENGTH_SHORT).show();
				}
				break;
			case CODE_START_SETTINGS:
				if (!isLockEnrolled()) {
					finish();
				}
				break;
			default:
				break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mService.isProtectedAppListChanged() && (keyCode == KeyEvent.KEYCODE_BACK)) {
			showConfigChangedDialog();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showConfigChangedDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_title_warning)
				.setCancelable(false)
				.setMessage(R.string.dialog_content_config_changed)
				.setPositiveButton(R.string.dialog_action_yes, (dialog, which) -> {
					// User choose to discard changes, so we just quit.
					mService.discardProtectListSettings();
					dialog.dismiss();
					finish();
				})
				.setNegativeButton(R.string.dialog_action_no, (dialog, which) -> dialog.dismiss());
		builder.create().show();
	}

	private void showPermissionRequestDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_title_warning)
				.setCancelable(false)
				.setMessage(R.string.dialog_content_request_permission)
				.setPositiveButton(R.string.dialog_action_yes, (dialog, which) -> {
					Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
					AppListActivity.this.startActivityForResult(intent, CODE_REQUEST_PERMISSION);
				})
				.setNegativeButton(R.string.dialog_action_no, (dialog, which) -> Toast.makeText(AppListActivity.this,
						R.string.toast_info_request_permission_failed, Toast.LENGTH_SHORT).show());
		builder.create().show();
	}

	private boolean checkIfGetPermission() {
		AppOpsManager appOps = (AppOpsManager) this.getSystemService(Context.APP_OPS_SERVICE);
		int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), this.getPackageName());
		return (mode == AppOpsManager.MODE_ALLOWED);
	}

	private boolean isLockEnrolled() {
		return mPrefs.getBoolean(LOCK_ENROLLED, false);
	}
}
