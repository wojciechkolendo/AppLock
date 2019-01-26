package com.wojciechkolendo.applock;

import android.app.Application;

import org.litepal.LitePal;

/**
 * @author Wojtek Kolendo
 */
public class AppLockApplication extends Application {

	private static AppLockApplication instance = new AppLockApplication();

	public static AppLockApplication getContext() {
		return instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		LitePal.initialize(this);
	}
}
