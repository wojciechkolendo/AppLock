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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import software.rsquared.androidlogger.Logger;
import com.wojciechkolendo.applock.R;
import com.wojciechkolendo.applock.events.FingerprintAuthResponse;
import com.wojciechkolendo.applock.models.LockInfo;
import com.wojciechkolendo.applock.services.LockingAppService;
import com.wojciechkolendo.applock.views.activities.AppListActivity;
import com.wojciechkolendo.applock.views.activities.LockAppActivity;
import com.wojciechkolendo.applock.views.custom.Lock9View;

public class PatternLockFragment extends Fragment {

	private TextView fingerprintInfo;
	private ImageView fingerprintIcon;

	private LockingAppService.ServiceBinder mService;

	public PatternLockFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		EventBus.getDefault().register(this);

		// get service.
		mService = ((LockAppActivity) requireActivity()).getService();

		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_pattern_lock, container, false);

		Lock9View patternView = view.findViewById(R.id.patternView);
		patternView.setCallBack(password -> {
			Logger.debug("pattern detected.");
			LockInfo lockInfo = DataSupport.findFirst(LockInfo.class);
			if (password.equals(lockInfo.getLockString())) {
				if (((LockAppActivity) requireActivity()).isLaunchFromHome()) {
					Intent intent = new Intent(getActivity(), AppListActivity.class);
					startActivity(intent);
				} else {
					((LockAppActivity) requireActivity()).getService().addUnlockedApp();
				}
				mService.cancelFingerprint();
				requireActivity().finish();
			} else {
				Toast.makeText(getActivity(), R.string.fragment_pattern_view_pattern_error, Toast.LENGTH_SHORT).show();
			}
		});

		// If this device has finger print sensor and enrolls one, we will show fingerprint info.
		fingerprintInfo = view.findViewById(R.id.fingerprint_hint);
		fingerprintIcon = view.findViewById(R.id.fingerprint_icon);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!mService.hasFingerprintHardware()) {
			Logger.debug("No fingerprint hardware.");
			fingerprintInfo.setVisibility(View.GONE);
			fingerprintIcon.setVisibility(View.GONE);
		} else if (!mService.isFingerprintEnrolled()) {
			Logger.debug("No fingerprint enrolled.");
			fingerprintIcon.setVisibility(View.GONE);
			fingerprintInfo.setText(R.string.fragment_pattern_view_fingerprint_no_enroll);
			fingerprintInfo.setOnClickListener(v -> {
				Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
				startActivity(intent);
			});
		} else {
			Logger.debug("Find hardware and enrolled.");
			fingerprintIcon.setVisibility(View.VISIBLE);
			fingerprintInfo.setText(R.string.fragment_pattern_view_fingerprint);
			fingerprintInfo.setOnClickListener(null);
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
				if (((LockAppActivity) requireActivity()).isLaunchFromHome()) {
					Intent intent = new Intent(getActivity(), AppListActivity.class);
					startActivity(intent);
				} else {
					((LockAppActivity) requireActivity()).getService().addUnlockedApp();
				}
				requireActivity().finish();
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
