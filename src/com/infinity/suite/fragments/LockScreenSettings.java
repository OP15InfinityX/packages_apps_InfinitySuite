/*
 *  Copyright (C) 2024 Project Infinity X
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.infinity.suite.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.infinity.OmniJawsClient;
import com.android.internal.util.infinity.Utils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.infinity.support.colorpicker.ColorPickerPreference;
import com.infinity.suite.fragments.PulseSettings;
import com.infinity.suite.utils.SystemUtils;
import com.infinity.suite.utils.DeviceUtils;

import java.util.List;

import lineageos.providers.LineageSettings;

@SearchIndexable
public class LockScreenSettings extends SettingsPreferenceFragment
            implements Preference.OnPreferenceChangeListener  {

    public static final String TAG = "LockScreenSettings";

    private static final String KEY_RIPPLE_EFFECT = "enable_ripple_effect";
    private static final String KEY_WEATHER = "lockscreen_weather_enabled";
    private static final String UDFPS_CATEGORY = "udfps_category";
    private static final String LOCK_SCREEN_FINGERPRINT_CATEGORY = "lock_screen_fingerprint_category";
    private static final String UDFPS_PACKAGE = "com.infinity.udfps.animations";
    private static final String KEY_CLOCK_COLOR_MODE = "lock_screen_custom_clock_color_mode";
    private static final String KEY_CLOCK_CUSTOM_COLOR = "lock_screen_custom_clock_custom_color";
    private static final String COLOR_MODE_CUSTOM = "custom";

    private PreferenceCategory mFingerprintCategory;
    private Preference mRippleEffect;
    private PreferenceCategory mUdfpsCategory;
    private ListPreference mClockColorMode;
    private ColorPickerPreference mClockCustomColor;

    private SwitchPreferenceCompat mWeather;

    private boolean hasUdfpsSupport(Context context) {
        return Utils.isPackageInstalled(context, UDFPS_PACKAGE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.infinity_suite_lockscreen);

        final PreferenceScreen prefSet = getPreferenceScreen();

        mFingerprintCategory = (PreferenceCategory) findPreference(LOCK_SCREEN_FINGERPRINT_CATEGORY);
        mRippleEffect = (Preference) findPreference(KEY_RIPPLE_EFFECT);

        if (!DeviceUtils.hasFingerprint(getContext())) {
            prefSet.removePreference(mFingerprintCategory);
        }

        if (!DeviceUtils.hasVibrator(getContext()) || !DeviceUtils.hasFingerprint(getContext())) {
            if (mFingerprintCategory != null) {
                Preference p = findPreference("fp_success_vibrate");
                if (p != null) mFingerprintCategory.removePreference(p);
                p = findPreference("fp_error_vibrate");
                if (p != null) mFingerprintCategory.removePreference(p);
            }
        }

        mWeather = (SwitchPreferenceCompat) findPreference(KEY_WEATHER);
        mWeather.setOnPreferenceChangeListener(this);

        mClockCustomColor = findPreference(KEY_CLOCK_CUSTOM_COLOR);
        initClockCustomColorPreference();

        mClockColorMode = findPreference(KEY_CLOCK_COLOR_MODE);
        if (mClockColorMode != null) {
            String colorMode = Settings.Secure.getStringForUser(
                    getContext().getContentResolver(),
                    KEY_CLOCK_COLOR_MODE,
                    UserHandle.USER_CURRENT);
            if (TextUtils.isEmpty(colorMode)) {
                colorMode = mClockColorMode.getValue();
            }
            if (TextUtils.isEmpty(colorMode)) {
                colorMode = "default";
            }
            mClockColorMode.setValue(colorMode);
            mClockColorMode.setOnPreferenceChangeListener(this);
            updateCustomColorPickerVisibility(colorMode);
        }

        updateWeatherSettings();

        mUdfpsCategory = findPreference(UDFPS_CATEGORY);
        if (!hasUdfpsSupport(getContext())) {
            prefSet.removePreference(mUdfpsCategory);
        }
    }

    private void initClockCustomColorPreference() {
        if (mClockCustomColor == null) {
            return;
        }

        int currentColor = Settings.Secure.getIntForUser(
                getContext().getContentResolver(),
                KEY_CLOCK_CUSTOM_COLOR,
                0xFFFFFFFF,
                UserHandle.USER_CURRENT);
        mClockCustomColor.setNewPreviewColor(currentColor);
        mClockCustomColor.setSummary(String.format("#%08x", currentColor));
        mClockCustomColor.setOnPreferenceChangeListener(this);
    }

    private void updateCustomColorPickerVisibility(String colorMode) {
        if (mClockCustomColor != null) {
            mClockCustomColor.setVisible(COLOR_MODE_CUSTOM.equals(colorMode));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mWeather) {
            mWeather.setChecked((Boolean)newValue);
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mClockColorMode) {
            updateCustomColorPickerVisibility((String) newValue);
            return true;
        } else if (preference == mClockCustomColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            Settings.Secure.putIntForUser(
                    getContext().getContentResolver(),
                    KEY_CLOCK_CUSTOM_COLOR,
                    ColorPickerPreference.convertToColorInt(hex),
                    UserHandle.USER_CURRENT);
            return true;
        }

        return false;
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.LOCKSCREEN_BATTERY_INFO, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.ENABLE_RIPPLE_EFFECT, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.FP_ERROR_VIBRATE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.FP_SUCCESS_VIBRATE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.LOCKSCREEN_WEATHER_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.LOCKSCREEN_WEATHER_LOCATION, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.LOCKSCREEN_WEATHER_TEXT, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
            Settings.System.LOCKSCREEN_WEATHER_WIND_INFO, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
            Settings.System.LOCKSCREEN_WEATHER_HUMIDITY_INFO, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.LS_MEDIA_ART_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.LS_MEDIA_ART_FILTER, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.LS_MEDIA_ART_FADE_LEVEL, 40, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.LS_MEDIA_ART_BLUR_LEVEL, 200, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.AMBIENT_MEDIA_ART_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SCREEN_OFF_ANIMATION, 0, UserHandle.USER_CURRENT);
        PulseSettings.reset(mContext);
    }

    private void updateWeatherSettings() {
        if (mWeather == null) return;

        boolean weatherEnabled = OmniJawsClient.get().isOmniJawsEnabled(getContext());
        mWeather.setEnabled(weatherEnabled);
        mWeather.setSummary(weatherEnabled ? R.string.lockscreen_weather_summary :
            R.string.lockscreen_weather_enabled_info);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWeatherSettings();
        if (mClockColorMode != null) {
            updateCustomColorPickerVisibility(mClockColorMode.getValue());
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.INFINITY;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.infinity_suite_lockscreen) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    if (!DeviceUtils.hasFingerprint(context)) {
                        keys.add(LOCK_SCREEN_FINGERPRINT_CATEGORY);
                        keys.add(KEY_RIPPLE_EFFECT);
                    }

                    if (!DeviceUtils.hasVibrator(context) || !DeviceUtils.hasFingerprint(context)) {
                        keys.add("fp_success_vibrate");
                        keys.add("fp_error_vibrate");
                    }

                    LockScreenSettings instance = new LockScreenSettings();
                    if (!instance.hasUdfpsSupport(context)) {
                        keys.add(UDFPS_CATEGORY);
                        keys.add("udfps_settings");
                    }

                    return keys;
                }
            };
}
