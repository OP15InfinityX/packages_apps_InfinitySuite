/*
 * Copyright (C) 2024 Project Infinity X
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

import static android.os.UserHandle.USER_SYSTEM;

import android.app.ActivityManagerNative;
import android.app.UiModeManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.view.IWindowManager;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Locale;
import android.text.TextUtils;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.Utils;
import com.android.internal.util.android.SystemRestartUtils;

import com.infinity.support.colorpicker.ColorPickerPreference;
import com.infinity.support.preferences.SystemSettingListPreference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SearchIndexable
public class ThemesSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "ThemesSettings";
    private static final String KEY_EMOJI_STYLE = "emoji_style";
    private static final String KEY_SYSTEM_ANIMATION_STYLE = "system_animation_style";
    private static final String PROP_ACTIVITY_ANIM_PERF_OVERRIDE =
            "persist.sys.activity_anim_perf_override";
    private static final String PROP_EMOJI_STYLE = "persist.sys.ax_emoji_style";
    private static final String DEFAULT_EMOJI_STYLE = "android";

    private Context mContext;
    private SystemSettingListPreference mEmojiStylePref;
    private SystemSettingListPreference mSystemAnimationStylePref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.infinity_suite_themes);

        mContext = getActivity();

        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen screen = getPreferenceScreen();

        mEmojiStylePref = findPreference(KEY_EMOJI_STYLE);
        if (mEmojiStylePref != null) {
            mEmojiStylePref.setValue(SystemProperties.get(PROP_EMOJI_STYLE, DEFAULT_EMOJI_STYLE));
            mEmojiStylePref.setOnPreferenceChangeListener(this);
        }

        mSystemAnimationStylePref = findPreference(KEY_SYSTEM_ANIMATION_STYLE);
        if (mSystemAnimationStylePref != null) {
            updateSystemAnimationStyleAvailability();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.INFINITY;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSystemAnimationStyleAvailability();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEmojiStylePref) {
            SystemProperties.set(PROP_EMOJI_STYLE, (String) newValue);
            SystemRestartUtils.showSystemRestartDialog(getContext());
            return true;
        }
        return false;
    }

    private void updateSystemAnimationStyleAvailability() {
        if (mSystemAnimationStylePref == null) {
            return;
        }

        final boolean isPerfOverrideEnabled = SystemProperties.getBoolean(
                PROP_ACTIVITY_ANIM_PERF_OVERRIDE, false);
        mSystemAnimationStylePref.setEnabled(!isPerfOverrideEnabled);
    }
    
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.infinity_suite_themes);
}
