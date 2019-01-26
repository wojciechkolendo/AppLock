package com.wojciechkolendo.applock.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.core.os.CancellationSignal;
import software.rsquared.androidlogger.Logger;
import com.wojciechkolendo.applock.BuildConfig;
import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.events.AppListForegroundEvent;
import com.wojciechkolendo.applock.events.FingerprintAuthRequest;
import com.wojciechkolendo.applock.events.FingerprintAuthResponse;
import com.wojciechkolendo.applock.models.AppInfo;
import com.wojciechkolendo.applock.models.LockInfo;
import com.wojciechkolendo.applock.models.ProtectedApplication;
import com.wojciechkolendo.applock.utils.fingerprint.CryptoObjectHelper;
import com.wojciechkolendo.applock.utils.fingerprint.MyAuthCallback;
import com.wojciechkolendo.applock.views.activities.LockAppActivity;

/**
 * Service which handle locking apps
 */

public class LockingAppService extends Service {

	@SuppressWarnings("FieldCanBeLocal")
	private final int FOREGROUND_SERVICE_NOTIFICATION_ID = 500;

	private PackageManager mPm;

	private UsageStatsManager mUsageStatsManager;

	private List<AppInfo> mProtectedAppList = new ArrayList<>();

	private List<AppInfo> mUnprotectedAppList = new ArrayList<>();

	private Set<String> mCheckList = new HashSet<>();

	private String currentLockedApp;

	private List<String> mUnlockedAppList = new ArrayList<>();

	private int lockType = -1;

	//private FingerprintManagerCompat fingerprintManager;
	private MyAuthCallback myAuthCallback = null;
	private CancellationSignal cancellationSignal = null;

	private boolean isAppListInForeground = false;

	private boolean isScreenOn = true;

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Logger.debug("action: " + action);

			if (action != null) {
				switch (action) {
					case Intent.ACTION_SCREEN_OFF:
						mUnlockedAppList.clear();

						isScreenOn = false;

						if (isAppListInForeground) {
							isAppListInForeground = false;
							Intent i = new Intent(Intent.ACTION_MAIN);
							i.addCategory(Intent.CATEGORY_HOME);
							startActivity(i);
						}

						break;
					case Intent.ACTION_SCREEN_ON:
						isScreenOn = true;
						break;
				}
			}
		}
	};

	private ServiceBinder mBinder = new ServiceBinder();

	public class ServiceBinder extends Binder {
		public List<AppInfo> getProtectedAppList() {
			return mProtectedAppList;
		}

		public List<AppInfo> getUnProtectedAppList() {
			return mUnprotectedAppList;
		}

		public int markToProtect(AppInfo appInfo) {
			Logger.debug("mark to protect: " + appInfo);

			mUnprotectedAppList.remove(appInfo);
			mProtectedAppList.add(appInfo);

			return mProtectedAppList.size();
		}

		public void markToUnprotect(AppInfo appInfo) {
			Logger.debug("mark to unprotect: " + appInfo);

			mProtectedAppList.remove(appInfo);
			mUnprotectedAppList.add(appInfo);

			mUnprotectedAppList.size();
		}

		public void saveProtectList() {
			removeAllProtectedApp();
			mCheckList.clear();

			for (AppInfo info : mProtectedAppList) {
				ProtectedApplication app = new ProtectedApplication();
				app.setPackageName(info.getAppPackageName());
				app.save();
				// create new check list
				mCheckList.add(info.getAppPackageName());
			}
		}

		public void discardProtectListSettings() {
			mProtectedAppList.clear();
			mUnprotectedAppList.clear();
			// do init again if user discard settings.
			initAppList();
		}

		public boolean isProtectedAppListChanged() {
			List<ProtectedApplication> protectedList = DataSupport.findAll(ProtectedApplication.class);

			if (protectedList.size() == mProtectedAppList.size()) {
				for (int i = 0; i < mProtectedAppList.size(); i++) {
					ProtectedApplication app = new ProtectedApplication(mProtectedAppList.get(i).getAppPackageName());
					if (!protectedList.contains(app)) {
						return true;
					}
				}
			} else {
				return true;
			}

			return false;
		}

		public void addUnlockedApp() {
			Logger.debug("add unlock app: " + currentLockedApp);
			mUnlockedAppList.add(currentLockedApp);
			currentLockedApp = null;
		}

		// save lock info: lock string and type
		public void saveLockInfo(String lockString, int type) {
			DataSupport.deleteAll(LockInfo.class);

			LockInfo info = new LockInfo();
			info.setLockString(lockString);
			info.setLockType(type);
			info.save();
			lockType = type;
		}

		public int getLockType() {
			if (lockType == -1) {
				LockInfo info = DataSupport.findFirst(LockInfo.class);
				if (info != null) {
					lockType = info.getLockType();
				} else {
					Logger.debug("info is null.");
				}
			}

			return lockType;
		}

		public void startFingerprintAuth() {
			EventBus.getDefault().post(new FingerprintAuthRequest());
		}

		public boolean hasFingerprintHardware() {
			return FingerprintManagerCompat.from(LockingAppService.this).isHardwareDetected();
		}

		public boolean isFingerprintEnrolled() {
			return FingerprintManagerCompat.from(LockingAppService.this).hasEnrolledFingerprints();
		}

		public void cancelFingerprint() {
			if (cancellationSignal != null) {
				// cancel fingerprint auth here.
				cancellationSignal.cancel();
			}
		}

		public String getCurrentAppName() {
			for (AppInfo info : mProtectedAppList) {
				if (info.getAppPackageName().equals(currentLockedApp)) {
					return info.getAppName();
				}
			}

			// otherwise it is ourselves
			return getString(R.string.app_name);
		}

		private void removeAllProtectedApp() {
			DataSupport.deleteAll(ProtectedApplication.class);
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// register event bus.
		EventBus.getDefault().register(this);
		try {
			myAuthCallback = new MyAuthCallback();
		} catch (Exception e) {
			e.printStackTrace();
		}

		mPm = getPackageManager();
		initAppList();
		// create database.
		Connector.getDatabase();
		mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
		mProtectedAppList = mBinder.getProtectedAppList();

		// start working thread.
		AppStartWatchThread mAppStartWatchThread = new AppStartWatchThread();
		mAppStartWatchThread.start();

		// register screen state listener.
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(mReceiver, filter);

		makeForeground();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		Logger.debug("Service bind.");
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// unregister event bus.
		EventBus.getDefault().unregister(this);
		unregisterReceiver(mReceiver);
		Logger.debug("Service died, so no apps can be protected!");
	}

	/*
	 * Fingerprint auth handle function.
	 */
	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onFingerprintAuth(FingerprintAuthRequest req) {
		// start fingerprint auth here.
		FingerprintManagerCompat manager = FingerprintManagerCompat.from(LockingAppService.this);
		if (manager.isHardwareDetected() && manager.hasEnrolledFingerprints()) {
			try {
				CryptoObjectHelper cryptoObjectHelper = new CryptoObjectHelper();
				cancellationSignal = new CancellationSignal();
				Logger.debug("Now we start listen for finger print auth.");
				manager.authenticate(cryptoObjectHelper.buildCryptoObject(), 0,
						cancellationSignal, myAuthCallback, null);
			} catch (Exception e) {
				Logger.debug("Fingerprint exception happens.");
				e.printStackTrace();
				// send this error.
				EventBus.getDefault().
						post(new FingerprintAuthResponse(FingerprintAuthResponse.MSG_AUTH_ERROR));
			}
		}
	}

	/*
	 * App list activity is foreground handle.
	 */
	@Subscribe(threadMode = ThreadMode.BACKGROUND)
	public void onAppListForeground(AppListForegroundEvent event) {
		if (event.isForeground()) {
			isAppListInForeground = true;
			// We quit foreground cause we have foreground activity now.
			stopForeground(true);
		} else {
			// we have to make us foreground now.
			makeForeground();
		}
	}

	private void initAppList() {
		Logger.debug("Init protected and unprotected app list.");
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> homeApps = mPm.queryIntentActivities(intent, 0);

		// sort the results
		Collections.sort(homeApps, new ResolveInfo.DisplayNameComparator(mPm));

		// get protected app list from database.
		List<ProtectedApplication> protectedList = DataSupport.findAll(ProtectedApplication.class);

		Set<String> packageNameSet = new HashSet<>();
		for (ResolveInfo info : homeApps) {
			// skip ourselves
			if (info.activityInfo.packageName.equals(getPackageName())) {
				continue;
			}

			if (!packageNameSet.contains(info.activityInfo.packageName)) {
				packageNameSet.add(info.activityInfo.packageName);
				AppInfo appInfo = new AppInfo();
				appInfo.setAppPackageName(info.activityInfo.packageName);
				appInfo.setAppName((String) info.activityInfo.applicationInfo.loadLabel(mPm));
				appInfo.setAppIcon(info.activityInfo.applicationInfo.loadIcon(mPm));

				if (protectedList.contains(new ProtectedApplication(info.activityInfo.packageName))) {
					mProtectedAppList.add(appInfo);
					// init check list
					mCheckList.add(appInfo.getAppPackageName());
				} else {
					mUnprotectedAppList.add(appInfo);
				}
			}
		}
	}

	private void checkIfNeedProtection() {
		long time = System.currentTimeMillis();
		List<UsageStats> usageStatsList = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 2000, time);
		if (usageStatsList != null && !usageStatsList.isEmpty() && isScreenOn) {
			SortedMap<Long, UsageStats> usageStatsMap = new TreeMap<>();
			for (UsageStats usageStats : usageStatsList) {
				usageStatsMap.put(usageStats.getLastTimeUsed(), usageStats);
			}
			if (!usageStatsMap.isEmpty()) {
				UsageStats usageStats = usageStatsMap.get(usageStatsMap.lastKey());
				if (usageStats != null) {
					String topPackageName = usageStats.getPackageName();
					if (mCheckList.contains(topPackageName) && !mUnlockedAppList.contains(topPackageName)) {
						Logger.debug("protecting: " + topPackageName);
						Intent intent = new Intent(LockingAppService.this, LockAppActivity.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
								Intent.FLAG_ACTIVITY_NO_HISTORY |
								Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
						LockingAppService.this.startActivity(intent);
						currentLockedApp = topPackageName;
					}
				}
			}
		}
	}

	private class AppStartWatchThread extends Thread {
		@SuppressWarnings("InfiniteLoopStatement")
		@Override
		public void run() {
			super.run();
			while (true) {
				try {
					Thread.sleep(500);
					checkIfNeedProtection();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void makeForeground() {
		// start this service in foreground
		Intent intent = new Intent(this, LockAppActivity.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

		NotificationCompat.Builder builder;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder = new NotificationCompat.Builder(this, createNotificationChannel().getId());
		} else {
			builder = new NotificationCompat.Builder(this);
		}

		builder
				.setContentTitle(getString(R.string.service_foreground_notification_title))
				.setContentText(getString(R.string.service_foreground_notification_content))
				.setSmallIcon(R.drawable.lock_white_20dp)
				.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorSecondary))
				.setContentIntent(pi);

		startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, builder.build());
	}

	/**
	 * Creates specified notification channel (for sdk >= Oreo)
	 *
	 * @return NotificationChannel object
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	private NotificationChannel createNotificationChannel() {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationChannel channel = new NotificationChannel(BuildConfig.APPLICATION_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
		notificationManager.createNotificationChannel(channel);
		return channel;
	}
}
