/*
 * Copyright (C) 2024-2026 Lunaris AOSP
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

package com.infinity.suite.fragments

import android.os.Bundle
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.infinity.support.colorpicker.ColorPickerPreference
import com.infinity.suite.utils.SystemUtils

class CutoutProgressSettingsFragment : SettingsPreferenceFragment(),
    Preference.OnPreferenceChangeListener {

    companion object {
        private const val KEY_RING_COLOR_MODE = "cutout_progress_ring_color_mode"
        private const val COLOR_MODE_ACCENT = 0
        private const val COLOR_MODE_CUSTOM = 2

        private const val KEY_RING_COLOR = "cutout_progress_ring_color"
        private const val KEY_ERROR_COLOR = "cutout_progress_error_color"
        private const val KEY_FLASH_COLOR = "cutout_progress_finish_flash_color"
        private const val KEY_BG_COLOR = "cutout_progress_bg_ring_color"
        private const val KEY_MUSIC_COLOR_MODE = "cutout_progress_music_color_mode"
        private const val COLOR_MODE_MUSIC_CUSTOM = 3
        private const val KEY_MUSIC_CUSTOM_COLOR = "cutout_progress_music_custom_color"
        private const val KEY_BATTERY_INDICATOR_ENABLED = "cutout_progress_battery_indicator_enabled"
        private const val KEY_MUSIC_ENABLED = "cutout_progress_music_enabled"

        private const val KEY_FINISH_STYLE = "cutout_progress_finish_style"
        private const val KEY_EASING = "cutout_progress_easing"
        private const val KEY_PERCENT_POSITION = "cutout_progress_percent_position"
        private const val KEY_FILENAME_POSITION = "cutout_progress_filename_position"
        private const val KEY_FILENAME_TRUNCATE = "cutout_progress_filename_truncate"
    }

    private lateinit var ringColorModePref: ListPreference
    private lateinit var musicColorModePref: ListPreference

    private lateinit var ringColorPref: ColorPickerPreference
    private lateinit var errorColorPref: ColorPickerPreference
    private lateinit var flashColorPref: ColorPickerPreference
    private lateinit var bgColorPref: ColorPickerPreference
    private lateinit var musicCustomColorPref: ColorPickerPreference

    private lateinit var finishStylePref: ListPreference
    private lateinit var easingPref: ListPreference
    private lateinit var pctPosPref: ListPreference
    private lateinit var fnamePosPref: ListPreference
    private lateinit var fnameTruncPref: ListPreference
    private lateinit var batteryIndicatorEnabledPref: Preference
    private lateinit var musicEnabledPref: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.cutout_progress_settings)

        ringColorModePref = findPreference(KEY_RING_COLOR_MODE)!!
        musicColorModePref = findPreference(KEY_MUSIC_COLOR_MODE)!!

        ringColorPref = findPreference(KEY_RING_COLOR)!!
        errorColorPref = findPreference(KEY_ERROR_COLOR)!!
        flashColorPref = findPreference(KEY_FLASH_COLOR)!!
        bgColorPref = findPreference(KEY_BG_COLOR)!!
        musicCustomColorPref = findPreference(KEY_MUSIC_CUSTOM_COLOR)!!

        finishStylePref = findPreference(KEY_FINISH_STYLE)!!
        easingPref = findPreference(KEY_EASING)!!
        pctPosPref = findPreference(KEY_PERCENT_POSITION)!!
        fnamePosPref = findPreference(KEY_FILENAME_POSITION)!!
        fnameTruncPref = findPreference(KEY_FILENAME_TRUNCATE)!!
        batteryIndicatorEnabledPref = findPreference(KEY_BATTERY_INDICATOR_ENABLED)!!
        musicEnabledPref = findPreference(KEY_MUSIC_ENABLED)!!

        val storedRingMode = Settings.Secure.getInt(
            requireContext().contentResolver,
            KEY_RING_COLOR_MODE,
            COLOR_MODE_ACCENT
        )
        ringColorModePref.value = storedRingMode.toString()
        updateRingColorVisibility(storedRingMode)

        val storedMusicMode = Settings.Secure.getInt(
            requireContext().contentResolver,
            KEY_MUSIC_COLOR_MODE,
            0
        )
        musicColorModePref.value = storedMusicMode.toString()
        updateMusicCustomColorVisibility(storedMusicMode)

        ringColorModePref.onPreferenceChangeListener = this
        musicColorModePref.onPreferenceChangeListener = this

        ringColorPref.onPreferenceChangeListener = this
        errorColorPref.onPreferenceChangeListener = this
        flashColorPref.onPreferenceChangeListener = this
        bgColorPref.onPreferenceChangeListener = this
        musicCustomColorPref.onPreferenceChangeListener = this

        finishStylePref.onPreferenceChangeListener = this
        easingPref.onPreferenceChangeListener = this
        pctPosPref.onPreferenceChangeListener = this
        fnamePosPref.onPreferenceChangeListener = this
        fnameTruncPref.onPreferenceChangeListener = this
        batteryIndicatorEnabledPref.onPreferenceChangeListener = this
        musicEnabledPref.onPreferenceChangeListener = this

        syncListPreferences()
        initColorPreferences()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        when (preference.key) {
            KEY_RING_COLOR_MODE -> {
                val intValue = (newValue as? String)?.toIntOrNull() ?: return false
                Settings.Secure.putInt(requireContext().contentResolver, preference.key, intValue)
                updateRingColorVisibility(intValue)
                return true
            }
            KEY_MUSIC_COLOR_MODE -> {
                val intValue = (newValue as? String)?.toIntOrNull() ?: return false
                Settings.Secure.putInt(requireContext().contentResolver, preference.key, intValue)
                updateMusicCustomColorVisibility(intValue)
                return true
            }
            KEY_RING_COLOR,
            KEY_ERROR_COLOR,
            KEY_FLASH_COLOR,
            KEY_BG_COLOR,
            KEY_MUSIC_CUSTOM_COLOR -> {
                val intValue = Integer.valueOf(newValue.toString())
                val hex = ColorPickerPreference.convertToARGB(intValue)
                preference.summary = hex
                val intHex = ColorPickerPreference.convertToColorInt(hex)
                Settings.Secure.putInt(
                    requireContext().contentResolver,
                    preference.key,
                    intHex
                )
                return true
            }
            KEY_FINISH_STYLE,
            KEY_EASING,
            KEY_PERCENT_POSITION,
            KEY_FILENAME_POSITION,
            KEY_FILENAME_TRUNCATE -> {
                val intValue = (newValue as? String)?.toIntOrNull() ?: return false
                Settings.Secure.putInt(requireContext().contentResolver, preference.key, intValue)
                return true
            }
            KEY_BATTERY_INDICATOR_ENABLED,
            KEY_MUSIC_ENABLED -> {
                SystemUtils.showSystemUiRestartDialog(requireContext())
                return true
            }
        }
        return false
    }

    private fun updateRingColorVisibility(mode: Int) {
        ringColorPref.isVisible = mode == COLOR_MODE_CUSTOM
    }

    private fun updateMusicCustomColorVisibility(mode: Int) {
        musicCustomColorPref.isVisible = mode == COLOR_MODE_MUSIC_CUSTOM
    }

    private fun syncListPreferences() {
        listOf(
            finishStylePref to 0,
            easingPref to 0,
            pctPosPref to 0,
            fnamePosPref to 4,
            fnameTruncPref to 0
        ).forEach { (pref, default) ->
            val current = Settings.Secure.getInt(
                requireContext().contentResolver,
                pref.key,
                default
            )
            pref.value = current.toString()
        }
    }

    private fun initColorPreferences() {
        listOf(
            ringColorPref to 0xFF2196F3.toInt(),
            errorColorPref to 0xFFF44336.toInt(),
            flashColorPref to 0xFFFFFFFF.toInt(),
            bgColorPref to 0xFF808080.toInt(),
            musicCustomColorPref to 0xFF9C27B0.toInt()
        ).forEach { (pref, default) ->
            val currentColor = Settings.Secure.getInt(
                requireContext().contentResolver,
                pref.key,
                default
            )
            pref.setNewPreviewColor(currentColor)
            pref.summary = ColorPickerPreference.convertToARGB(currentColor)
        }
    }

    override fun getMetricsCategory(): Int = MetricsEvent.INFINITY
}