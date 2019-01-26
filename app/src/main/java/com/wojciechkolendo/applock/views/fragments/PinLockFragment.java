package com.wojciechkolendo.applock.views.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import software.rsquared.androidlogger.Logger;
import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.events.FingerprintAuthResponse;
import com.wojciechkolendo.applock.models.LockInfo;
import com.wojciechkolendo.applock.services.LockingAppService;
import com.wojciechkolendo.applock.views.activities.AppListActivity;
import com.wojciechkolendo.applock.views.activities.LockAppActivity;

/**
 * Pin lock fragment.
 */

public class PinLockFragment extends Fragment {

	private TextView fingerprintInfo;
	private ImageView fingerprintIcon;
	private IndicatorDots indicatorDots;

	private PinLockView pinLockView;

	private LockingAppService.ServiceBinder mService;

	private PinLockListener mPinLockListener = new PinLockListener() {
		@Override
		public void onComplete(String pin) {
			LockInfo pinLockInfo = DataSupport.findFirst(LockInfo.class);
			if (pin.equals(pinLockInfo.getLockString())) {
				if (((LockAppActivity) requireActivity()).isLaunchFromHome()) {
					Intent intent = new Intent(getActivity(), AppListActivity.class);
					startActivity(intent);
				} else {
					((LockAppActivity) requireActivity()).getService().addUnlockedApp();
				}
				mService.cancelFingerprint();
				requireActivity().finish();
			} else {
				Logger.error("auth failed!");
				Toast.makeText(getActivity(), R.string.fragment_pin_lock_view_pin_error, Toast.LENGTH_SHORT).show();
				indicatorDots.setPinLength(0);
				pinLockView.resetPinLockView();
			}
		}

		@Override
		public void onEmpty() {
			indicatorDots.setPinLength(0);
		}

		@Override
		public void onPinChange(int pinLength, String intermediatePin) {
			indicatorDots.setPinLength(pinLength);
		}
	};

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		EventBus.getDefault().register(this);

		// get service.
		mService = ((LockAppActivity) requireActivity()).getService();

		View view = inflater.inflate(R.layout.fragment_pin_lock, container, false);

		// If this device has finger print sensor and enrolls one, we will show fingerprint info.
		fingerprintInfo = view.findViewById(R.id.fingerprint_hint);
		fingerprintIcon = view.findViewById(R.id.fingerprint_icon);
		indicatorDots = view.findViewById(R.id.indicator_dots);
		indicatorDots.setPinLength(0);

		if (!mService.hasFingerprintHardware()) {
			fingerprintInfo.setVisibility(View.GONE);
			fingerprintIcon.setVisibility(View.GONE);
		} else if (!mService.isFingerprintEnrolled()) {
			fingerprintIcon.setVisibility(View.GONE);
			fingerprintInfo.setText(R.string.fragment_pattern_view_fingerprint_no_enroll);
			fingerprintInfo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
					startActivity(intent);
				}
			});
		}

		pinLockView = (PinLockView) view.findViewById(R.id.pin_lock_view);
		pinLockView.setPinLockListener(mPinLockListener);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		Logger.debug("onResume");
		if (!mService.hasFingerprintHardware()) {
			fingerprintInfo.setVisibility(View.GONE);
			fingerprintIcon.setVisibility(View.GONE);
		} else if (!mService.isFingerprintEnrolled()) {
			fingerprintIcon.setVisibility(View.GONE);
			fingerprintInfo.setText(R.string.fragment_pattern_view_fingerprint_no_enroll);
			fingerprintInfo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
					startActivity(intent);
				}
			});
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		EventBus.getDefault().unregister(this);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onFingerprintAuthResult(FingerprintAuthResponse response) {
		Logger.debug("Fingerprint auth result: " + response.getResult());
		switch (response.getResult()) {
			case FingerprintAuthResponse.MSG_AUTH_SUCCESS:
				if (((LockAppActivity) getActivity()).isLaunchFromHome()) {
					Intent intent = new Intent(getActivity(), AppListActivity.class);
					startActivity(intent);
				} else {
					((LockAppActivity) getActivity()).getService().addUnlockedApp();
				}
				getActivity().finish();
				break;
			case FingerprintAuthResponse.MSG_AUTH_FAILED:
				fingerprintInfo.setText(R.string.fingerprint_auth_failed);
				break;
			case FingerprintAuthResponse.MSG_AUTH_ERROR:
				fingerprintInfo.setText(R.string.fingerprint_auth_error);
				break;
			case FingerprintAuthResponse.MSG_AUTH_HELP:
				// show failed info for now.
				fingerprintInfo.setText(R.string.fingerprint_auth_failed);
				break;
			default:
				break;
		}
	}
}
