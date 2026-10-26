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

package com.infinity.suite

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import com.android.internal.logging.nano.MetricsProto
import com.android.internal.util.infinity.Utils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.WallpaperBlurView
import com.android.settings.core.SubSettingLauncher
import com.google.android.material.card.MaterialCardView

import com.infinity.suite.fragments.QuickSettings
import com.infinity.suite.fragments.MonetSettings
import com.infinity.suite.fragments.StatusBarSettings
import com.infinity.suite.fragments.LockScreenSettings
import com.infinity.suite.fragments.PowerMenuSettings
import com.infinity.suite.fragments.TeamInfo
import com.infinity.suite.fragments.NotificationSettings
import com.infinity.suite.fragments.ThemesSettings
import com.infinity.suite.fragments.NavbarSettings
import com.infinity.suite.fragments.MiscSettings
import com.infinity.suite.fragments.ButtonSettings
import com.infinity.suite.fragments.AmbientCustomizations

class InfinitySuite : SettingsPreferenceFragment(), View.OnClickListener {

    private lateinit var settingCards: List<View>
    private lateinit var lockScreenSettingsCard: WallpaperBlurView
    private lateinit var customizationPickerButton: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.InfinitySuite, container, false)

        val cardIds = listOf(
            R.id.quicksettings_card,
            R.id.monetsettings_card,
            R.id.statusbarsettings_card,
            R.id.powersettings_card,
            R.id.teaminfo_card,
            R.id.notificationsettings_card,
            R.id.theme_card,
            R.id.navigationsettings_card,
            R.id.miscsettings_card,
            R.id.buttonsettings_card,
            R.id.aodsettings_card
        )

        settingCards = cardIds.map { id -> view.findViewById<View>(id).apply { setOnClickListener(this@InfinitySuite) } }

        lockScreenSettingsCard = view.findViewById(R.id.lockscreensettings_card)
        lockScreenSettingsCard.setOnClickListener(this)

        customizationPickerButton = view.findViewById(R.id.customization_picker_button)
        customizationPickerButton.setOnClickListener(this)

        return view
    }

    override fun onClick(view: View) {
        val (fragmentClass, titleRes) = when (view.id) {
            R.id.quicksettings_card -> QuickSettings::class.java.name to R.string.quicksettings_title
            R.id.monetsettings_card -> MonetSettings::class.java.name to R.string.monet_title
            R.id.statusbarsettings_card -> StatusBarSettings::class.java.name to R.string.statusbar_title
            R.id.lockscreensettings_card -> LockScreenSettings::class.java.name to R.string.lockscreen_title
            R.id.powersettings_card -> PowerMenuSettings::class.java.name to R.string.powermenu_title
            R.id.teaminfo_card -> TeamInfo::class.java.name to R.string.team_title
            R.id.notificationsettings_card -> NotificationSettings::class.java.name to R.string.notifications_title
            R.id.theme_card -> ThemesSettings::class.java.name to R.string.themes_title
            R.id.navigationsettings_card -> NavbarSettings::class.java.name to R.string.navbar_title
            R.id.miscsettings_card -> MiscSettings::class.java.name to R.string.misc_title
            R.id.buttonsettings_card -> ButtonSettings::class.java.name to R.string.button_title
            R.id.aodsettings_card -> AmbientCustomizations::class.java.name to R.string.ambient_text_category_title
            R.id.customization_picker_button -> {
                openCustomizationPickerActivity()
                return
            }
            else -> null
        } ?: return

        SubSettingLauncher(requireContext())
            .setDestination(fragmentClass)
            .setTitleRes(titleRes)
            .setSourceMetricsCategory(metricsCategory)
            .launch()
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.INFINITY
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.infinity_suite_title)
    }

    private fun openCustomizationPickerActivity() {
        val isGoogleWpInstalled = Utils.isPackageInstalled(requireContext(), GOOGLE_WP_PKG)
        val packageName = if (isGoogleWpInstalled) GOOGLE_WP_PKG else DEFAULT_WP_PKG
        val activityName = if (isGoogleWpInstalled) GOOGLE_WP_ACTIVITY else DEFAULT_WP_ACTIVITY
        
        val intent = Intent().apply {
            setClassName(packageName, activityName)
        }
        startActivity(intent)
    }

    companion object {
        private const val DEFAULT_WP_PKG = "com.android.wallpaper"
        private const val DEFAULT_WP_ACTIVITY = "com.android.customization.picker.CustomizationPickerActivity"
        private const val GOOGLE_WP_PKG = "com.google.android.apps.wallpaper"
        private const val GOOGLE_WP_ACTIVITY = "com.google.android.apps.wallpaper.picker.CategoryPickerActivity"
        
        fun lockCurrentOrientation(activity: Activity) {
            val currentRotation = activity.windowManager.defaultDisplay.rotation
            val orientation = activity.resources.configuration.orientation
            val frozenRotation = when (currentRotation) {
                Surface.ROTATION_0 -> if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Surface.ROTATION_90 -> if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                else
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Surface.ROTATION_180 -> if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                Surface.ROTATION_270 -> if (orientation == Configuration.ORIENTATION_PORTRAIT)
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            activity.requestedOrientation = frozenRotation
        }
    }
}
