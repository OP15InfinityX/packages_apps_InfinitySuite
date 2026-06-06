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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

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

import com.infinity.suite.fragments.LayoutSettings;
import com.infinity.suite.fragments.QsHeaderImageSettings;
import com.infinity.suite.utils.SystemUtils;
import com.infinity.suite.utils.DeviceUtils;
import com.infinity.support.preferences.CustomSeekBarPreference;
import com.infinity.support.preferences.SystemSettingListPreference;
import com.infinity.support.preferences.SystemSettingSwitchPreference;

import lineageos.providers.LineageSettings;

import java.util.List;
import java.util.ArrayList;

@SearchIndexable
public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "QuickSettings";

    private static final String KEY_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";
    private static final String KEY_BRIGHTNESS_SLIDER_POSITION = "qs_brightness_slider_position";
    private static final String KEY_BRIGHTNESS_SLIDER_HAPTIC = "qs_brightness_slider_haptic";
    private static final String KEY_BRIGHTNESS_SLIDER_STYLE = "qs_brightness_slider_style";
    private static final String KEY_BRIGHTNESS_SLIDER_SHAPE = "qs_brightness_slider_shape";
    private static final String KEY_SHOW_AUTO_BRIGHTNESS = "qs_show_auto_brightness";
    private static final String KEY_QS_COMPACT_MEDIA_PLAYER = "qs_compact_media_player_mode";
    private static final String KEY_MEDIA_WAVEFORM_SEEKBAR = "media_waveform_seekbar";
    private static final String KEY_MEDIA_SQUIGGLE_ANIMATION = "media_squiggle_animation";
    private static final String KEY_SINGLE_QS_TONE = "single_qs_tone_enabled";
    private static final String KEY_QS_TILE_ALTERNATE_COLOR = "qs_tile_alternate_color";
    private static final String KEY_DUAL_TARGET_TILE_STYLE = "dual_target_tile_style";
    private static final String KEY_QS_PANEL_STYLE = "qs_panel_style";
    private static final String KEY_QS_TILE_SHAPE = "qs_tile_shape";
    private static final String KEY_QS_TILE_ICON_SHAPE = "qs_tile_icon_shape";
    private static final String KEY_QS_TILE_LABEL_HIDE = "qs_tile_label_hide";
    private static final String KEY_INFINITY_QS_STYLE = "infinity_revamped_qs";
    private static final String KEY_QS_TILE_GRADIENT = "qs_tile_gradient";
    private static final String KEY_SPLIT_QS_ENABLED = "qs_split_shade";
    private static final String KEY_CUSTOM_GRADIENT_COLOR_MODE = "custom_gradient_color_mode";
    private static final String KEY_CUSTOM_GRADIENT_START_COLOR = "custom_gradient_start_color";
    private static final String KEY_CUSTOM_GRADIENT_END_COLOR = "custom_gradient_end_color";
    private static final String DEFAULT_CUSTOM_GRADIENT_COLOR = "#ff0000";
    private static final String HYPEROS_CUSTOM_GRADIENT_COLOR = "#0a84ff";

    private static final int QS_STYLE_STOCK = 0;
    private static final int QS_STYLE_INFINITY_X = 1;
    private static final int QS_STYLE_HYPEROS = 2;
    private static final int QS_STYLE_OXYGENOS = 3;

    private ListPreference mShowBrightnessSlider;
    private ListPreference mBrightnessSliderPosition;
    private SwitchPreferenceCompat mBrightnessSliderHaptic;
    private SystemSettingSwitchPreference mBrightnessSliderStyle;
    private SystemSettingListPreference mBrightnessSliderShape;
    private SwitchPreferenceCompat mShowAutoBrightness;
    private SwitchPreferenceCompat mQsCompactMediaPlayer;
    private SwitchPreferenceCompat mMediaWaveformSeekbar;
    private SwitchPreferenceCompat mMediaSquiggleAnimation;
    private SwitchPreferenceCompat mSingleQsTone;
    private SwitchPreferenceCompat mQsTileAlternateColor;
    private SwitchPreferenceCompat mDualTargetTileStyle;
    private ListPreference mQsPanelStyle;
    private Preference mQsTileShape;
    private Preference mQsTileIconShape;
    private SwitchPreferenceCompat mQsTileLabelHide;
    private ListPreference mInfinityQsStyle;
    private SystemSettingSwitchPreference mSplitQsEnabled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.infinity_suite_quicksettings);

        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mShowBrightnessSlider = findPreference(KEY_SHOW_BRIGHTNESS_SLIDER);
        mShowBrightnessSlider.setOnPreferenceChangeListener(this);
        boolean showSlider = LineageSettings.Secure.getIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT) > 0;

        mBrightnessSliderPosition = findPreference(KEY_BRIGHTNESS_SLIDER_POSITION);
        mBrightnessSliderPosition.setEnabled(showSlider);

        mBrightnessSliderStyle = findPreference(KEY_BRIGHTNESS_SLIDER_STYLE);
        mBrightnessSliderShape = findPreference(KEY_BRIGHTNESS_SLIDER_SHAPE);
        if (mBrightnessSliderStyle != null) {
            mBrightnessSliderStyle.setOnPreferenceChangeListener(this);
            updateBrightnessSliderStyleDependencies();
        }

        mBrightnessSliderHaptic = findPreference(KEY_BRIGHTNESS_SLIDER_HAPTIC);
        mBrightnessSliderHaptic.setEnabled(showSlider);

        mQsTileAlternateColor = findPreference(KEY_QS_TILE_ALTERNATE_COLOR);
        if (mQsTileAlternateColor != null) {
            mQsTileAlternateColor.setOnPreferenceChangeListener(this);
        }

        mDualTargetTileStyle = findPreference(KEY_DUAL_TARGET_TILE_STYLE);
        if (mDualTargetTileStyle != null) {
            mDualTargetTileStyle.setOnPreferenceChangeListener(this);
        }

                if (!DeviceUtils.hasVibrator(context)) {
                        PreferenceCategory layoutCategory = (PreferenceCategory) findPreference("qs_layout_category");
                        PreferenceCategory brightnessCategory = (PreferenceCategory) findPreference("qs_brightness_slider_category");
                        Preference v = findPreference("qs_tile_haptic");
                        if (v != null && layoutCategory != null) {
                                layoutCategory.removePreference(v);
                        }
                        if (mBrightnessSliderHaptic != null && brightnessCategory != null) {
                                brightnessCategory.removePreference(mBrightnessSliderHaptic);
                        }
                }

        mShowAutoBrightness = findPreference(KEY_SHOW_AUTO_BRIGHTNESS);
        boolean automaticAvailable = context.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);

        if (automaticAvailable) {
            mShowAutoBrightness.setEnabled(showSlider);
        } else {
            prefScreen.removePreference(mShowAutoBrightness);
        }

        updateBrightnessSliderStyleDependencies();

        mQsCompactMediaPlayer = findPreference(KEY_QS_COMPACT_MEDIA_PLAYER);
        mQsCompactMediaPlayer.setOnPreferenceChangeListener(this);

        mMediaWaveformSeekbar = findPreference(KEY_MEDIA_WAVEFORM_SEEKBAR);
        mMediaWaveformSeekbar.setOnPreferenceChangeListener(this);

        mMediaSquiggleAnimation = findPreference(KEY_MEDIA_SQUIGGLE_ANIMATION);
        boolean waveformEnabled = Settings.System.getIntForUser(resolver,
                Settings.System.MEDIA_WAVEFORM_SEEKBAR, 0, UserHandle.USER_CURRENT) == 1;
        mMediaSquiggleAnimation.setEnabled(!waveformEnabled);

        mSingleQsTone = findPreference(KEY_SINGLE_QS_TONE);
        if (mSingleQsTone != null) {
            mSingleQsTone.setOnPreferenceChangeListener(this);
        }

        mQsPanelStyle = findPreference(KEY_QS_PANEL_STYLE);
        mQsPanelStyle.setOnPreferenceChangeListener(this);
        mQsTileShape = findPreference(KEY_QS_TILE_SHAPE);
        mQsTileIconShape = findPreference(KEY_QS_TILE_ICON_SHAPE);
        mQsTileLabelHide = findPreference(KEY_QS_TILE_LABEL_HIDE);
        mInfinityQsStyle = findPreference(KEY_INFINITY_QS_STYLE);
        mSplitQsEnabled = findPreference(KEY_SPLIT_QS_ENABLED);

        if (mInfinityQsStyle != null) {
            mInfinityQsStyle.setOnPreferenceChangeListener(this);
        }

        int panelStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT);
        updatePanelStylePrefs(panelStyle);

        int qsStyle = getQsStyleValueFromSetting();
        if (mInfinityQsStyle != null) {
            mInfinityQsStyle.setValue(String.valueOf(qsStyle));
        }
    }

    private void updatePanelStylePrefs(int panelStyle) {
        boolean isClassic = panelStyle == 1;

        if (mQsTileShape != null) {
            mQsTileShape.setVisible(!isClassic);
        }
        if (mQsTileIconShape != null) {
            mQsTileIconShape.setVisible(isClassic);
        }
        if (mQsTileLabelHide != null) {
            mQsTileLabelHide.setVisible(isClassic);
        }
    }

    private void updateBrightnessSliderStyleDependencies() {
        if (mBrightnessSliderStyle == null) return;

        ContentResolver resolver = getContext().getContentResolver();
        boolean isSliderStyleEnabled = Settings.System.getInt(resolver,
                KEY_BRIGHTNESS_SLIDER_STYLE, 0) == 1;

        if (mBrightnessSliderShape != null) {
            mBrightnessSliderShape.setVisible(!isSliderStyleEnabled);
        }

        if (mShowAutoBrightness != null) {
            boolean automaticAvailable = getContext().getResources().getBoolean(
                    com.android.internal.R.bool.config_automatic_brightness_available);
            if (automaticAvailable) {
                mShowAutoBrightness.setVisible(!isSliderStyleEnabled);
            }
        }
    }

    private void updateBrightnessSliderState(boolean showSlider) {
        boolean enabled = showSlider;

        if (mBrightnessSliderPosition != null) {
            mBrightnessSliderPosition.setEnabled(enabled);
        }
        if (mBrightnessSliderHaptic != null) {
            mBrightnessSliderHaptic.setEnabled(enabled);
        }
        if (mShowAutoBrightness != null) {
            mShowAutoBrightness.setEnabled(enabled);
        }
    }

    private void applyInfinityQsStylePreset(int style) {
        ContentResolver resolver = getContext().getContentResolver();

        boolean usePresetDefaults = style != QS_STYLE_STOCK;
        int panelStyle = usePresetDefaults ? 1 : 0;
        boolean tileLabelHide = usePresetDefaults;
        int showBrightnessSlider = usePresetDefaults ? 0 : 1;
        boolean splitQsEnabled = style == QS_STYLE_HYPEROS;

        if (style == QS_STYLE_INFINITY_X) {
            panelStyle = 0;
            tileLabelHide = false;
        }

        if (style == QS_STYLE_HYPEROS || style == QS_STYLE_OXYGENOS) {
            int hyperOsGradientColor = Color.parseColor(HYPEROS_CUSTOM_GRADIENT_COLOR);
            Settings.System.putIntForUser(resolver,
                    KEY_CUSTOM_GRADIENT_COLOR_MODE, 1, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(resolver,
                    KEY_QS_TILE_GRADIENT, 1, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(resolver,
                    KEY_CUSTOM_GRADIENT_START_COLOR, hyperOsGradientColor, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(resolver,
                    KEY_CUSTOM_GRADIENT_END_COLOR, hyperOsGradientColor, UserHandle.USER_CURRENT);
        } else {
            int defaultGradientColor = Color.parseColor(DEFAULT_CUSTOM_GRADIENT_COLOR);
            Settings.System.putIntForUser(resolver,
                    KEY_CUSTOM_GRADIENT_COLOR_MODE, 0, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(resolver,
                    KEY_QS_TILE_GRADIENT, 0, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(resolver,
                    KEY_CUSTOM_GRADIENT_START_COLOR, defaultGradientColor, UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(resolver,
                    KEY_CUSTOM_GRADIENT_END_COLOR, defaultGradientColor, UserHandle.USER_CURRENT);
        }

        Settings.System.putIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, panelStyle, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_LABEL_HIDE, tileLabelHide ? 1 : 0, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, showBrightnessSlider,
                UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                KEY_SPLIT_QS_ENABLED, splitQsEnabled ? 1 : 0, UserHandle.USER_CURRENT);

        if (mQsPanelStyle != null) {
            mQsPanelStyle.setValue(String.valueOf(panelStyle));
        }
        if (mQsTileLabelHide != null) {
            mQsTileLabelHide.setChecked(tileLabelHide);
        }
        if (mShowBrightnessSlider != null) {
            mShowBrightnessSlider.setValue(String.valueOf(showBrightnessSlider));
        }
        if (mSplitQsEnabled != null) {
            mSplitQsEnabled.setChecked(splitQsEnabled);
        }

        updatePanelStylePrefs(panelStyle);
    }

    private int getQsStyleValueFromSetting() {
        ContentResolver resolver = getContext().getContentResolver();
        int value = Settings.System.getIntForUser(resolver,
                KEY_INFINITY_QS_STYLE, QS_STYLE_STOCK, UserHandle.USER_CURRENT);
        return sanitizeQsStyleValue(value);
    }

    private int getCurrentQsStyle() {
        if (mInfinityQsStyle == null || mInfinityQsStyle.getValue() == null) {
            return getQsStyleValueFromSetting();
        }
        try {
            return sanitizeQsStyleValue(Integer.parseInt(mInfinityQsStyle.getValue()));
        } catch (NumberFormatException e) {
            return QS_STYLE_STOCK;
        }
    }

    private int sanitizeQsStyleValue(int style) {
        if (style < QS_STYLE_STOCK || style > QS_STYLE_OXYGENOS) {
            return QS_STYLE_STOCK;
        }
        return style;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContext().getContentResolver();

        if (preference == mShowBrightnessSlider) {
            int value = Integer.parseInt((String) newValue);
            updateBrightnessSliderState(value > 0);
            updateBrightnessSliderStyleDependencies();
            return true;
        } else if (preference == mInfinityQsStyle) {
            int previousStyle = getCurrentQsStyle();
            int style = sanitizeQsStyleValue(Integer.parseInt((String) newValue));
            if (previousStyle != style) {
                applyInfinityQsStylePreset(style);
            }
            if (previousStyle == QS_STYLE_STOCK && style != QS_STYLE_STOCK) {
                SystemUtils.restartSystemUI(getContext());
            }
            return true;
        } else if (preference == mQsCompactMediaPlayer) {
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mMediaWaveformSeekbar) {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                Settings.Secure.putIntForUser(resolver,
                        Settings.Secure.MEDIA_SQUIGGLE_ANIMATION, 1, UserHandle.USER_CURRENT);
                mMediaSquiggleAnimation.setEnabled(false);
                SystemUtils.showSystemUiRestartDialog(getContext());
            } else {
                mMediaSquiggleAnimation.setEnabled(true);
                SystemUtils.showSystemUiRestartDialog(getContext());
            }
            return true;
        } else if (preference == mSingleQsTone) {
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mQsTileAlternateColor) {
            SystemUtils.showSystemUiRestartDialog(getActivity());
            return true;
        } else if (preference == mDualTargetTileStyle) {
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mBrightnessSliderStyle) {
            updateBrightnessSliderStyleDependencies();
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mQsPanelStyle) {
            int value = Integer.parseInt((String) newValue);
            updatePanelStylePrefs(value);
            return true;
        }
        return false;
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.ENABLE_LOCKSCREEN_QUICK_SETTINGS, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_BRIGHTNESS_SLIDER_HAPTIC, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_BRIGHTNESS_SLIDER_SHAPE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                KEY_BRIGHTNESS_SLIDER_STYLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_BT_SHOW_DIALOG, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_FOOTER_SHOW_SETTINGS, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_FOOTER_SHOW_EDIT, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_FOOTER_SHOW_POWER_MENU, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_SHOW_DATA_USAGE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_HAPTIC, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_SHAPE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.SINGLE_QS_TONE_ENABLED, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_ANIMATION_STYLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_LABEL_HIDE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                KEY_INFINITY_QS_STYLE, QS_STYLE_STOCK, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                KEY_CUSTOM_GRADIENT_COLOR_MODE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                KEY_QS_TILE_GRADIENT, 0, UserHandle.USER_CURRENT);
        int defaultGradientColor = Color.parseColor(DEFAULT_CUSTOM_GRADIENT_COLOR);
        Settings.System.putIntForUser(resolver,
                KEY_CUSTOM_GRADIENT_START_COLOR, defaultGradientColor, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                KEY_CUSTOM_GRADIENT_END_COLOR, defaultGradientColor, UserHandle.USER_CURRENT);
        Settings.System.putStringForUser(resolver,
                Settings.System.QS_TILE_ICON_SHAPE, "circle", UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.QS_BRIGHTNESS_SLIDER_POSITION, 0, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_AUTO_BRIGHTNESS, 1, UserHandle.USER_CURRENT);
        LayoutSettings.reset(mContext);
        QsHeaderImageSettings.reset(mContext);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.INFINITY;
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.infinity_suite_quicksettings) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    final Resources res = context.getResources();

                    boolean automaticAvailable = res.getBoolean(
                            com.android.internal.R.bool.config_automatic_brightness_available);
                    if (!automaticAvailable) {
                        keys.add(KEY_SHOW_AUTO_BRIGHTNESS);
                    }

                    ContentResolver resolver = context.getContentResolver();
                    boolean isSliderStyleEnabled = Settings.System.getInt(resolver,
                            KEY_BRIGHTNESS_SLIDER_STYLE, 0) == 1;
                    if (isSliderStyleEnabled) {
                        keys.add(KEY_BRIGHTNESS_SLIDER_SHAPE);
                        keys.add(KEY_SHOW_AUTO_BRIGHTNESS);
                    }

                                        if (!DeviceUtils.hasVibrator(context)) {
                                                keys.add("qs_tile_haptic");
                                        }

                    return keys;
                }
            };
}
