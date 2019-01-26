package com.wojciechkolendo.applock.utils.fingerprint;

import org.greenrobot.eventbus.EventBus;

import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import com.wojciechkolendo.applock.events.FingerprintAuthResponse;

/**
 * Fingerprint auth result callback.
 */
public class MyAuthCallback extends FingerprintManagerCompat.AuthenticationCallback {

    public MyAuthCallback() {
        super();
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        super.onAuthenticationError(errMsgId, errString);

        EventBus.getDefault().
                post(new FingerprintAuthResponse(FingerprintAuthResponse.MSG_AUTH_ERROR));
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        super.onAuthenticationHelp(helpMsgId, helpString);

        EventBus.getDefault().
                post(new FingerprintAuthResponse(FingerprintAuthResponse.MSG_AUTH_HELP));
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);

        EventBus.getDefault().
                post(new FingerprintAuthResponse(FingerprintAuthResponse.MSG_AUTH_SUCCESS));
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();

        EventBus.getDefault().
                post(new FingerprintAuthResponse(FingerprintAuthResponse.MSG_AUTH_FAILED));
    }
}
