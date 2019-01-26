package com.wojciechkolendo.applock.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import software.rsquared.androidlogger.Logger;

/**
 * @author Wojtek Kolendo
 */
public class OnUserPresentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null && (action.equals(Intent.ACTION_USER_PRESENT))) {
			if (OnBootCompletedReceiver.freshlyBooted) {
				OnBootCompletedReceiver.freshlyBooted = false;
				startSwal(context);
			}
		}
	}

	private void startSwal(Context context) {
		Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.urtica");
		if (intent != null) {
			Logger.debug("Starting SWAL app from AppBlock");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		}
	}
}
