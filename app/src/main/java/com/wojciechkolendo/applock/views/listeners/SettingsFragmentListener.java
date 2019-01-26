package com.wojciechkolendo.applock.views.listeners;

/**
 * @author Wojtek Kolendo
 */
public interface SettingsFragmentListener {

	int getCurrentLockType();

	/**
	 * @param lockType 0 -> Pattern, 1 -> PIN
	 */
	void onLockTypePreferenceChanged(int lockType);

}
