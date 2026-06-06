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

package com.infinity.suite.fragments;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.search.BaseSearchIndexProvider;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.List;

import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.infinity.Utils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.infinity.suite.fragments.UdfpsAnimation;
import com.infinity.suite.fragments.UdfpsIconPicker;


@SearchIndexable
public class UdfpsSettings extends SettingsPreferenceFragment {

    private static final String KEY_UDFPS_ICONS = "udfps_icon_picker";
    private static final String KEY_UDFPS_ANIMATIONS = "udfps_recognizing_animation_preview";

    private Preference mUdfpsIcons;
    private Preference mUdfpsAnimations;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.udfps_settings);

        final PreferenceScreen prefSet = getPreferenceScreen();
        Resources resources = getResources();

        final boolean udfpsIconsPkgInstalled = Utils.isPackageInstalled(getContext(),
                "com.infinity.udfps.icons");
        final boolean udfpsAnimsPkgInstalled = Utils.isPackageInstalled(getContext(),
                "com.infinity.udfps.animations");
        mUdfpsIcons = findPreference(KEY_UDFPS_ICONS);
        mUdfpsAnimations = findPreference(KEY_UDFPS_ANIMATIONS);
        if (!udfpsIconsPkgInstalled) {
            prefSet.removePreference(mUdfpsIcons);
        }
        if (!udfpsAnimsPkgInstalled) {
            prefSet.removePreference(mUdfpsAnimations);
        }
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        UdfpsAnimation.reset(mContext);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.INFINITY;
    }
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.udfps_settings) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    final boolean udfpsIconsPkgInstalled = Utils.isPackageInstalled(context,
                            "com.infinity.udfps.icons");
                    final boolean udfpsAnimsPkgInstalled = Utils.isPackageInstalled(context,
                            "com.infinity.udfps.animations");
                    if (!udfpsIconsPkgInstalled) {
                        keys.add(KEY_UDFPS_ICONS);
                    }
                    if (!udfpsAnimsPkgInstalled) {
                        keys.add(KEY_UDFPS_ANIMATIONS);
                    }

                    return keys;
                }
            };

}