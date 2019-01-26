package com.wojciechkolendo.applock.views.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.wojciechkolendo.applock.BuildConfig;
import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.receivers.OnDeviceAdminReceiver;
import com.wojciechkolendo.applock.views.listeners.SettingsFragmentListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import software.rsquared.androidlogger.Logger;

/**
 * @author Wojtek Kolendo
 */
public class SettingsFragment extends PreferenceFragmentCompat {

	private final int REQUEST_CODE_ENABLE_ADMIN = 4620;

	private ComponentName deviceAdmin;
	private DevicePolicyManager policyManager;

	private SettingsFragmentListener listener;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof SettingsFragmentListener) {
			listener = (SettingsFragmentListener) context;
		} else {
			throw new RuntimeException(context.getClass().getName() + "must implement SettingsFragmentListener");
		}
		policyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		deviceAdmin = new ComponentName(context, OnDeviceAdminReceiver.class);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
		policyManager = null;
		deviceAdmin = null;
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.settings);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		initSecuritySettings();
		initAboutSettings();
	}

	private void initSecuritySettings() {
		ListPreference lockTypePreference = (ListPreference) getPreferenceScreen().findPreference("settings_lock_type");
		SwitchPreference adminPreference = (SwitchPreference) getPreferenceScreen().findPreference("settings_admin");

		adminPreference.setChecked(isAdminActive());
		adminPreference.setOnPreferenceChangeListener((preference, newValue) -> onAdminAppChanged((boolean) newValue));
		lockTypePreference.setOnPreferenceClickListener(preference -> onLockTypeClicked(lockTypePreference));
		lockTypePreference.setOnPreferenceChangeListener((preference, newValue) -> onLockTypeChanged(lockTypePreference, (String) newValue));
	}

	private void initAboutSettings() {
		Preference informationPreference = getPreferenceScreen().findPreference("settings_app_info");
		Preference contactPreference = getPreferenceScreen().findPreference("settings_contact");
		Preference versionPreference = getPreferenceScreen().findPreference("settings_version");

		informationPreference.setOnPreferenceClickListener(preference -> {
			showInformationDialog();
			return true;
		});

		contactPreference.setOnPreferenceClickListener(preference -> {
			Intent intent = new Intent(Intent.ACTION_SENDTO);
			intent.setData(Uri.parse("mailto:"));
			intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.settings_contact_summary)});
			startActivity(Intent.createChooser(intent, getString(R.string.settings_send_mail)));
			return true;
		});

		versionPreference.setSummary(getString(R.string.settings_app_version_summary, BuildConfig.VERSION_NAME));

		versionPreference.setOnPreferenceClickListener(preference -> {
			try {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
			} catch (ActivityNotFoundException e) {
				Logger.error(e);
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
			}
			return true;
		});
	}

	private void showInformationDialog() {
		new AlertDialog.Builder(requireActivity(), R.style.Theme_AppLock_Dialog)
				.setView(R.layout.dialog_about)
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}

	private boolean onLockTypeClicked(ListPreference preference) {
		if (listener != null) {
			int position = listener.getCurrentLockType();
			if (position > -1) {
				Logger.error(preference.getEntryValues()[position].toString());
				preference.setValue(preference.getEntryValues()[position].toString());
			}
		}
		return true;
	}

	private boolean onLockTypeChanged(ListPreference preference, String newValue) {
		if (listener != null) {
			listener.onLockTypePreferenceChanged(preference.findIndexOfValue(newValue));
		}
		return false;
	}

	private boolean onAdminAppChanged(boolean enabled) {
		if (enabled == isAdminActive() || deviceAdmin == null) {
			return false;
		}
		if (enabled) {
			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.settings_admin_explanation));
			startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
			return false;
		} else {
			policyManager.removeActiveAdmin(deviceAdmin);
		}
		return true;
	}

	private boolean isAdminActive() {
		if (policyManager != null && deviceAdmin != null) {
			return policyManager.isAdminActive(deviceAdmin);
		}
		return false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_ENABLE_ADMIN && resultCode == Activity.RESULT_OK) {
			SwitchPreference adminPreference = (SwitchPreference) getPreferenceScreen().findPreference("settings_admin");
			adminPreference.setChecked(true);
		}
	}
}
