/*
 *  Copyright (C) 2024-2026 Project Infinity X
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.infinity.suite.fragments.doze.EdgeLightSettings;
import com.infinity.suite.fragments.doze.Utils;
import com.infinity.suite.utils.ImageUtils;

import java.util.List;
import java.util.ArrayList;

@SearchIndexable
public class AmbientCustomizations extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "AmbientCustomizations";

    private static final String KEY_DOZE_ENABLED = "doze_enabled";
    private static final String KEY_DOZE_ALWAYS_ON = "doze_always_on";
    private static final String KEY_DOZE_ALWAYS_ON_SCHEDULE = "always_on_display_schedule";
    private static final String KEY_DOZE_ON_CHARGE = "doze_on_charge";
    private static final String KEY_CUSTOM_AOD_IMAGE = "lockscreen_custom_image";
    private static final String KEY_PULSE_ON_NEW_TRACKS = "pulse_on_new_tracks";
    private static final int CUSTOM_IMAGE_REQUEST_CODE = 1001;

    private SwitchPreferenceCompat mDozeEnabledPreference;
    private SwitchPreferenceCompat mDozeAlwaysOnPreference;
    private SwitchPreferenceCompat mDozeOnChargePreference;
    private SwitchPreferenceCompat mPulseOnNewTracksPreference;

    private Preference mDozeAlwaysOnSchedulePreference;
    private Preference mCustomImagePreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ambient_customization);

        Context context = getContext();
        ContentResolver resolver = context.getContentResolver();

        mDozeEnabledPreference = (SwitchPreferenceCompat) findPreference(KEY_DOZE_ENABLED);
        mDozeEnabledPreference.setChecked(Utils.isDozeEnabled(context));

        mDozeAlwaysOnPreference = (SwitchPreferenceCompat) findPreference(KEY_DOZE_ALWAYS_ON);
        mDozeAlwaysOnSchedulePreference = findPreference(KEY_DOZE_ALWAYS_ON_SCHEDULE);
        mDozeOnChargePreference = (SwitchPreferenceCompat) findPreference(KEY_DOZE_ON_CHARGE);
        mCustomImagePreference = findPreference(KEY_CUSTOM_AOD_IMAGE);
        mPulseOnNewTracksPreference = (SwitchPreferenceCompat) findPreference(KEY_PULSE_ON_NEW_TRACKS);

        // Hides always on toggle if device doesn't support it (based on config_dozeAlwaysOnDisplayAvailable overlay)
        if (!Utils.isDozeAlwaysOnAvailable(context)) {
            getPreferenceScreen().removePreference(mDozeAlwaysOnPreference);
            getPreferenceScreen().removePreference(mDozeAlwaysOnSchedulePreference);
            getPreferenceScreen().removePreference(mDozeOnChargePreference);
            getPreferenceScreen().removePreference(findPreference("custom_aod_image_enabled"));
            if (mCustomImagePreference != null) getPreferenceScreen().removePreference(mCustomImagePreference);
        } else {
            mDozeAlwaysOnPreference.setChecked(Utils.isDozeAlwaysOnEnabled(context));
            mDozeAlwaysOnPreference.setOnPreferenceChangeListener(this);

            boolean dozeOnCharge = Settings.Secure.getIntForUser(resolver,
                    Settings.Secure.DOZE_ON_CHARGE,
                    0, UserHandle.USER_CURRENT) != 0;
            mDozeOnChargePreference.setChecked(dozeOnCharge);

            updateCustomImagePreference();
        }

        updatePreferenceStates();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context context = getContext();
        ContentResolver resolver = context.getContentResolver();

        if (preference == mDozeAlwaysOnPreference) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putIntForUser(resolver, Settings.Secure.DOZE_ALWAYS_ON, 
                 value ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        }
        return false;
    }



    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.DOZE_ENABLED, mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeEnabled) ? 1 : 0,
                UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.DOZE_ALWAYS_ON, mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_dozeAlwaysOnEnabled) ? 1 : 0,
                UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.DOZE_ALWAYS_ON_AUTO_MODE, 0, UserHandle.USER_CURRENT);
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.DOZE_ON_CHARGE, 0, UserHandle.USER_CURRENT);
        EdgeLightSettings.Companion.reset(mContext);
    }

    private void updateCustomImagePreference() {
        if (mCustomImagePreference == null) return;

        int clockStyle = Settings.Secure.getIntForUser(
                getContext().getContentResolver(),
            Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, 0, UserHandle.USER_CURRENT);
        String imagePath = Settings.System.getString(
                getContext().getContentResolver(), "custom_aod_image_uri");

        if (imagePath != null && clockStyle > 0) {
            mCustomImagePreference.setSummary(imagePath);
            mCustomImagePreference.setEnabled(true);
        } else if (clockStyle == 0) {
            mCustomImagePreference.setSummary(
                    getContext().getString(R.string.custom_aod_image_not_supported));
            mCustomImagePreference.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mCustomImagePreference) {
            try {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, CUSTOM_IMAGE_REQUEST_CODE);
            } catch (Exception e) {
                Toast.makeText(getContext(),
                        R.string.quick_settings_header_needs_gallery,
                        Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (requestCode == CUSTOM_IMAGE_REQUEST_CODE
                && resultCode == Activity.RESULT_OK
                && result != null) {
            Uri imgUri = result.getData();
            if (imgUri != null) {
                String savedImagePath = ImageUtils.saveImageToInternalStorage(
                        getContext(), imgUri,
                        "lockscreen_aod_image",
                        "LOCKSCREEN_CUSTOM_AOD_IMAGE");
                if (savedImagePath != null) {
                    Settings.System.putStringForUser(
                            getContext().getContentResolver(),
                            "custom_aod_image_uri",
                            savedImagePath,
                            UserHandle.USER_CURRENT);
                    mCustomImagePreference.setSummary(savedImagePath);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
        if (Utils.isDozeAlwaysOnAvailable(getContext())) {
            updateCustomImagePreference();
        }
    }

    private void updatePreferenceStates() {
        ContentResolver resolver = getContext().getContentResolver();

        if (mPulseOnNewTracksPreference != null) {
            boolean pulseOnNewTracks = Settings.Secure.getIntForUser(resolver,
                    KEY_PULSE_ON_NEW_TRACKS,
                    0, UserHandle.USER_CURRENT) != 0;
            mPulseOnNewTracksPreference.setChecked(pulseOnNewTracks);
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
            new BaseSearchIndexProvider(R.xml.ambient_customization) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    if (!Utils.isDozeAlwaysOnAvailable(context)) {
                        keys.add(KEY_DOZE_ALWAYS_ON);
                        keys.add(KEY_DOZE_ALWAYS_ON_SCHEDULE);
                        keys.add(KEY_DOZE_ON_CHARGE);
                        keys.add("custom_aod_image_enabled");
                        keys.add(KEY_CUSTOM_AOD_IMAGE);
                    }

                    return keys;
                }
            };
}
