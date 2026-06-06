/*
 * SPDX-FileCopyrightText: crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.infinity.suite.fragments.doze

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import androidx.preference.Preference

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

import com.infinity.suite.utils.SystemUtils
import lineageos.preference.SystemSettingMainSwitchPreference

class EdgeLightSettings : SettingsPreferenceFragment() {

    private var edgeLightEnabled: SystemSettingMainSwitchPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.edge_light_settings)

        edgeLightEnabled = findPreference(KEY_EDGE_LIGHT_ENABLED)
        edgeLightEnabled?.setOnPreferenceChangeListener(this::onEdgeLightToggleChanged)
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.INFINITY

    private fun onEdgeLightToggleChanged(preference: Preference, newValue: Any?): Boolean {
        if (newValue is Boolean && newValue) {
            SystemUtils.showSystemUiRestartDialog(requireContext())
        }
        return true
    }

    companion object {
        private const val KEY_EDGE_LIGHT_ENABLED = "edge_light_enabled"

        @JvmStatic
        fun reset(context: Context) {
            val resolver = context.contentResolver
            Settings.System.putIntForUser(resolver,
                    Settings.System.EDGE_LIGHT_ENABLED, 0, UserHandle.USER_CURRENT)
            Settings.System.putStringForUser(resolver,
                    Settings.System.EDGE_LIGHT_COLOR_MODE, "accent", UserHandle.USER_CURRENT)
            Settings.System.putIntForUser(resolver,
                    Settings.System.EDGE_LIGHT_CUSTOM_COLOR, Color.WHITE, UserHandle.USER_CURRENT)
            Settings.System.putIntForUser(resolver,
                    Settings.System.EDGE_LIGHT_PULSE_COUNT, 1, UserHandle.USER_CURRENT)
            Settings.System.putIntForUser(resolver,
                    Settings.System.EDGE_LIGHT_STROKE_WIDTH, 8, UserHandle.USER_CURRENT)
            Settings.System.putStringForUser(resolver,
                    Settings.System.EDGE_LIGHT_STYLE, "default", UserHandle.USER_CURRENT)
            Settings.System.putStringForUser(resolver,
                    Settings.System.EDGE_LIGHT_ANIMATION_EFFECT, "none", UserHandle.USER_CURRENT)
        }
    }
}
