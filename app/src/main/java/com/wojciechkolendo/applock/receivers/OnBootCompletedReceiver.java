package com.wojciechkolendo.applock.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import software.rsquared.androidlogger.Logger;
import com.wojciechkolendo.applock.services.LockingAppService;


public class OnBootCompletedReceiver extends BroadcastReceiver {

	/**
	 * Flag if device was freshly booted used in {@link OnUserPresentReceiver}
	 */
	public static boolean freshlyBooted;

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null && (action.equals(Intent.ACTION_BOOT_COMPLETED))) {
			freshlyBooted = true;
			startService(context);
		}
	}

	private void startService(Context context) {
		Logger.debug("Boot complete, starting service.");
		Intent intent = new Intent(context, LockingAppService.class);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			context.startForegroundService(intent);
		} else {
			context.startService(intent);
		}
	}
}
