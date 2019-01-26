package com.wojciechkolendo.applock.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import software.rsquared.androidlogger.Logger;

/**
 * @author Wojtek Kolendo
 */
public class OnDeviceAdminReceiver extends DeviceAdminReceiver {

	@Override
	public void onEnabled(Context context, Intent intent) {
		Logger.debug("Admin permission granted");
		super.onEnabled(context, intent);
	}

	@Override
	public void onDisabled(Context context, Intent intent) {
		Logger.debug("Admin permission disabled");
		super.onDisabled(context, intent);
	}
}
