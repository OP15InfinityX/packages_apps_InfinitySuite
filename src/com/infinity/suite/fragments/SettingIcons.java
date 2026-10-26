/*
 * Copyright (C) 2024-25 Project Infinity X
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

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.ListPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.infinity.support.preferences.SystemSettingListPreference;
import com.infinity.support.preferences.SystemSettingSwitchPreference;
import com.android.internal.util.android.SystemRestartUtils;

@SearchIndexable
public class SettingIcons extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "SettingIcons";
    private static final String KEY_ICON_STYLE = "icon_style";
    private static final String KEY_ICON_RANDOM_COLORS = "icon_random_colors";
    private static final String KEY_ICON_CORNER_STYLE = "icon_corner_style";
    private static final String KEY_SHOW_ANIMATED = "show_animated_preference";
    private static final String KEY_SHOW_CONTEXTUAL_MESSAGES = "show_contextual_dashboard_messages";

    private Context mContext;
    private SystemSettingListPreference mIconStylePref;
    private SystemSettingSwitchPreference mIconRandomColorsPref;
    private SystemSettingListPreference mIconCornerStylePref;
    private SystemSettingSwitchPreference mShowAnimatedPref;
    private SystemSettingSwitchPreference mShowContextualMessagesPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.infinity_suite_icons);

        mContext = getActivity();
        final PreferenceScreen screen = getPreferenceScreen();

        mIconStylePref = findPreference(KEY_ICON_STYLE);
        if (mIconStylePref != null) {
            mIconStylePref.setOnPreferenceChangeListener(this);
        }

        mIconRandomColorsPref = findPreference(KEY_ICON_RANDOM_COLORS);
        if (mIconRandomColorsPref != null) {
            mIconRandomColorsPref.setOnPreferenceChangeListener(this);
        }

        mIconCornerStylePref = findPreference(KEY_ICON_CORNER_STYLE);
        if (mIconCornerStylePref != null) {
            mIconCornerStylePref.setOnPreferenceChangeListener(this);
        }

        mShowAnimatedPref = findPreference(KEY_SHOW_ANIMATED);
        if (mShowAnimatedPref != null) {
            mShowAnimatedPref.setOnPreferenceChangeListener(this);
        }

        mShowContextualMessagesPref = findPreference(KEY_SHOW_CONTEXTUAL_MESSAGES);
        if (mShowContextualMessagesPref != null) {
            mShowContextualMessagesPref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.INFINITY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        
        if (preference == mIconStylePref) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver, KEY_ICON_STYLE,
                    value, UserHandle.USER_CURRENT);
            updateListPreferenceSummary(mIconStylePref, value);
            SystemRestartUtils.showSettingsRestartDialog(getContext());
            return true;
            
        } else if (preference == mIconRandomColorsPref) {
            boolean enabled = (Boolean) newValue;
            Settings.System.putIntForUser(resolver, KEY_ICON_RANDOM_COLORS,
                    enabled ? 1 : 0, UserHandle.USER_CURRENT);
            SystemRestartUtils.showSettingsRestartDialog(getContext());
            return true;
            
        } else if (preference == mIconCornerStylePref) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver, KEY_ICON_CORNER_STYLE,
                    value, UserHandle.USER_CURRENT);
            updateListPreferenceSummary(mIconCornerStylePref, value);
            SystemRestartUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mShowAnimatedPref) {
            boolean enabled = (Boolean) newValue;
            Settings.System.putIntForUser(resolver, KEY_SHOW_ANIMATED,
                    enabled ? 1 : 0, UserHandle.USER_CURRENT);
            SystemRestartUtils.showSettingsRestartDialog(getContext());
            return true;
        } else if (preference == mShowContextualMessagesPref) {
            boolean enabled = (Boolean) newValue;
            Settings.System.putIntForUser(resolver, KEY_SHOW_CONTEXTUAL_MESSAGES,
                    enabled ? 1 : 0, UserHandle.USER_CURRENT);
            SystemRestartUtils.showSettingsRestartDialog(getContext());
            return true;
        }
        return false;
    }

    private void updateListPreferenceSummary(ListPreference preference, int value) {
        if (preference == null) return;
        
        CharSequence[] entries = preference.getEntries();
        if (entries != null && value >= 0 && value < entries.length) {
            preference.setSummary(entries[value]);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
    }
    
    private void updatePreferenceStates() {
        ContentResolver resolver = getActivity().getContentResolver();
        
    int iconStyle = Settings.System.getIntForUser(resolver,
        KEY_ICON_STYLE, 0, UserHandle.USER_CURRENT);
        if (mIconStylePref != null) {
            mIconStylePref.setValue(String.valueOf(iconStyle));
            updateListPreferenceSummary(mIconStylePref, iconStyle);
        }
        
        boolean randomColorsEnabled = Settings.System.getIntForUser(resolver,
                KEY_ICON_RANDOM_COLORS, 0, UserHandle.USER_CURRENT) == 1;
        if (mIconRandomColorsPref != null) {
            mIconRandomColorsPref.setChecked(randomColorsEnabled);
        }

    boolean showAnimated = Settings.System.getIntForUser(resolver,
        KEY_SHOW_ANIMATED, 1, UserHandle.USER_CURRENT) == 1;
    if (mShowAnimatedPref != null) {
        mShowAnimatedPref.setChecked(showAnimated);
    }

    boolean showContextual = Settings.System.getIntForUser(resolver,
        KEY_SHOW_CONTEXTUAL_MESSAGES, 1, UserHandle.USER_CURRENT) == 1;
    if (mShowContextualMessagesPref != null) {
        mShowContextualMessagesPref.setChecked(showContextual);
    }
        
        int cornerStyle = Settings.System.getIntForUser(resolver,
                KEY_ICON_CORNER_STYLE, 0, UserHandle.USER_CURRENT);
        if (mIconCornerStylePref != null) {
            mIconCornerStylePref.setValue(String.valueOf(cornerStyle));
            updateListPreferenceSummary(mIconCornerStylePref, cornerStyle);
        }
    }
    
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.infinity_suite_icons);
}