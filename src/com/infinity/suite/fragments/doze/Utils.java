/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.infinity.suite.fragments.doze;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

public final class Utils {

    public static final String DOZE_INTENT = "com.android.systemui.doze.pulse";

    public static boolean isDozeEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.DOZE_ENABLED, context.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeEnabled) ? 1 : 0,
                UserHandle.USER_CURRENT) != 0;
    }

    public static boolean isDozeAlwaysOnAvailable(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeAlwaysOnDisplayAvailable);
    }

    public static boolean isDozeAlwaysOnEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.DOZE_ALWAYS_ON, context.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeAlwaysOnEnabled) ? 1 : 0,
                UserHandle.USER_CURRENT) != 0;
    }
}
