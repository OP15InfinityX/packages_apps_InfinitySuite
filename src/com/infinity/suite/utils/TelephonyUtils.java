package com.infinity.suite.utils;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Helper class which has the same logic as MobileNetworkSettings to display the same
 * network modes and strings as it does.
 */
public class TelephonyUtils {

    private static final String TAG = TelephonyUtils.class.getSimpleName();

    /**
     * Returns whether the device is voice-capable (meaning, it is also a phone).
     */
    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephony =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephony != null && telephony.isVoiceCapable();
    }
}