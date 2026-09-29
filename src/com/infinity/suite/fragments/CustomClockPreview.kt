/*
 * Copyright (C) 2023-2024 The risingOS Android Project
 * Copyright (C) 2024-26 Project Infinity X 
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

import android.content.res.Configuration
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

import com.android.internal.util.infinity.ThemeUtils
import com.infinity.suite.utils.SystemUtils
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class CustomClockPreview : SettingsPreferenceFragment() {

    companion object {
        private const val TAG = "LockClockPreview"
        private const val PREF_FIRST_TIME = "first_time_clock_face_access"

        private val mCenterClocks = intArrayOf(
            4, 5, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34,
            35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
            51, 52, 53, 54
        )

        private val CLOCK_LAYOUTS = intArrayOf(
            R.layout.keyguard_clock_default,
            R.layout.keyguard_clock_empty,
            R.layout.keyguard_clock_oos,
            R.layout.keyguard_clock_oos2,
            R.layout.keyguard_clock_center,
            R.layout.keyguard_clock_ios,
            R.layout.keyguard_clock_ios2,
            R.layout.keyguard_clock_ios3,
            R.layout.keyguard_clock_ios4,
            R.layout.keyguard_clock_ios5,
            R.layout.keyguard_clock_ios6,
            R.layout.keyguard_clock_ios7,
            R.layout.keyguard_clock_ios8,
            R.layout.keyguard_clock_ios9,
            R.layout.keyguard_clock_ios10,
            R.layout.keyguard_clock_ios11,
            R.layout.keyguard_clock_ios12,
            R.layout.keyguard_clock_ios13,
            R.layout.keyguard_clock_ios14,
            R.layout.keyguard_clock_ios15,
            R.layout.keyguard_clock_ios16,
            R.layout.keyguard_clock_ios17,
            R.layout.keyguard_clock_ios18,
            R.layout.keyguard_clock_ios19,
            R.layout.keyguard_clock_miui,
            R.layout.keyguard_clock_miui2,
            R.layout.keyguard_clock_cos1,
            R.layout.keyguard_clock_cos2,
            R.layout.keyguard_clock_oppo,
            R.layout.keyguard_clock_simple,
            R.layout.keyguard_clock_ide,
            R.layout.keyguard_clock_moto,
            R.layout.keyguard_clock_stylish,
            R.layout.keyguard_clock_stylish2,
            R.layout.keyguard_clock_stylish3,
            R.layout.keyguard_clock_stylish4,
            R.layout.keyguard_clock_stylish5,
            R.layout.keyguard_clock_stylish6,
            R.layout.keyguard_clock_stylish7,
            R.layout.keyguard_clock_stylish8,
            R.layout.keyguard_clock_stylish9,
            R.layout.keyguard_clock_stylish10,
            R.layout.keyguard_clock_word,
            R.layout.keyguard_clock_life,
            R.layout.keyguard_clock_a9,
            R.layout.keyguard_clock_nos1,
            R.layout.keyguard_clock_nos2,
            R.layout.keyguard_clock_space_age,
            R.layout.keyguard_clock_polyline,
            R.layout.keyguard_clock_num,
            R.layout.keyguard_clock_accent,
            R.layout.keyguard_clock_analog,
            R.layout.keyguard_clock_block,
            R.layout.keyguard_clock_bubble,
            R.layout.keyguard_anci_clock_outline,
            R.layout.keyguard_anci_clock_ovalium,
            R.layout.keyguard_anci_clock_rectangle,
            R.layout.keyguard_anci_clock_wallet,
            R.layout.keyguard_anci_clockdate_clavicula,
            R.layout.keyguard_anci_clockdate_kln,
            R.layout.keyguard_anci_clockdate_miring,
            R.layout.keyguard_anci_clockdate_scapula,
            R.layout.keyguard_anci_clockdate_sternum,
            R.layout.keyguard_sparkCircle,
            R.layout.keyguard_sparkList,
            R.layout.keyguard_clock_big1,
            R.layout.keyguard_clock_big2,
            R.layout.keyguard_clock_big3,
            R.layout.keyguard_clock_big4,
            R.layout.keyguard_clock_sweet,
            R.layout.keyguard_clock_pixel,
            R.layout.keyguard_clock_samurai,
            R.layout.keyguard_clock_gateway,
            R.layout.keyguard_clock_tall,
            R.layout.keyguard_clock_gobold,
            R.layout.keyguard_clock_gobold2,
            R.layout.keyguard_clock_delirium,
            R.layout.keyguard_clock_deliriumdual,
            R.layout.keyguard_clock_skewrom,
            R.layout.keyguard_clock_skewrom2,
            R.layout.keyguard_clock_taller,
            R.layout.keyguard_clock_taller2,
            R.layout.keyguard_clock_taller3,
            R.layout.keyguard_clock_modak,
            R.layout.keyguard_clock_galada,
            R.layout.keyguard_clock_neu,
            R.layout.keyguard_clock_neu2,
            R.layout.keyguard_clock_neu3,
            R.layout.keyguard_clock_badeen,
        )
    }

    private var viewPager: ViewPager? = null
    private lateinit var pagerAdapter: ClockPagerAdapter
    private var recyclerView: RecyclerView? = null
    private lateinit var recyclerAdapter: ClockRecyclerAdapter
    private var fastScrollCheckbox: CheckBox? = null
    private var applyFab: ExtendedFloatingActionButton? = null
    private var highlightGuide: View? = null
    private var clockNameTextView: TextView? = null

    private var mClockPosition = 0

    private lateinit var mThemeUtils: ThemeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.title = activity?.getString(R.string.custom_clock_style)
        mThemeUtils = ThemeUtils(activity!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.lockscreen_clock_preview, container, false)
        clockNameTextView = rootView.findViewById(R.id.clock_name)

        viewPager = rootView.findViewById(R.id.view_pager)
        pagerAdapter = ClockPagerAdapter()
        viewPager?.adapter = pagerAdapter
        mClockPosition = Settings.Secure.getIntForUser(
            requireContext().contentResolver,
            Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, 0, UserHandle.USER_CURRENT
        )
        if (mClockPosition < 0 || mClockPosition >= CLOCK_LAYOUTS.size) {
            mClockPosition = 0
            Settings.Secure.putIntForUser(
                requireContext().contentResolver,
                Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, 0, UserHandle.USER_CURRENT
            )
        }
        viewPager?.currentItem = mClockPosition

        recyclerView = rootView.findViewById(R.id.recycler_view)
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView?.layoutManager = layoutManager
        recyclerAdapter = ClockRecyclerAdapter()
        recyclerView?.adapter = recyclerAdapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(rcView: RecyclerView, newState: Int) {
                if (fastScrollCheckbox?.isChecked == true && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = rcView.layoutManager as? LinearLayoutManager ?: return
                    val snapView = snapHelper.findSnapView(lm)
                    if (snapView != null) {
                        val position = lm.getPosition(snapView)
                        if (position != RecyclerView.NO_POSITION && position != mClockPosition) {
                            mClockPosition = position
                            rcView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            updateClockName(position)
                        }
                    }
                }
            }

            override fun onScrolled(rcView: RecyclerView, dx: Int, dy: Int) {
                if (fastScrollCheckbox?.isChecked == true) {
                    val lm = rcView.layoutManager as? LinearLayoutManager ?: return
                    val snapView = snapHelper.findSnapView(lm)
                    if (snapView != null) {
                        val position = lm.getPosition(snapView)
                        if (position != RecyclerView.NO_POSITION) {
                            updateClockName(position)
                        }
                    }
                }
            }
        })

        fastScrollCheckbox = rootView.findViewById(R.id.fast_scroll_checkbox)
        fastScrollCheckbox?.setOnCheckedChangeListener { _, isChecked ->
            Settings.Secure.putIntForUser(
                requireContext().contentResolver,
                "lock_screen_custom_clock_fast_scroll", if (isChecked) 1 else 0,
                UserHandle.USER_CURRENT
            )
            if (isChecked) {
                viewPager?.visibility = View.GONE
                recyclerView?.visibility = View.VISIBLE
                layoutManager.scrollToPosition(mClockPosition)
            } else {
                recyclerView?.visibility = View.GONE
                viewPager?.visibility = View.VISIBLE
                viewPager?.currentItem = mClockPosition
            }
        }

        val isFastScrollEnabled = Settings.Secure.getIntForUser(
            requireContext().contentResolver,
            "lock_screen_custom_clock_fast_scroll", 0, UserHandle.USER_CURRENT
        ) == 1
        fastScrollCheckbox?.isChecked = isFastScrollEnabled

        // Initialize layout manager's position to keep in sync
        layoutManager.scrollToPosition(mClockPosition)

        applyFab = rootView.findViewById(R.id.apply_extended_fab)
        applyFab?.setOnClickListener {
            Settings.Secure.putIntForUser(
                requireContext().contentResolver,
                Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, mClockPosition,
                UserHandle.USER_CURRENT
            )
            Settings.Secure.putIntForUser(
                requireContext().contentResolver,
                Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE, 0,
                UserHandle.USER_CURRENT
            )
            Settings.Secure.putIntForUser(
                requireContext().contentResolver,
                Settings.Secure.LOCKSCREEN_SMARTSPACE_ENABLED, 0,
                UserHandle.USER_CURRENT
            )
            updateClockOverlays(mClockPosition)
            SystemUtils.restartSystemUI(requireContext())
        }

        highlightGuide = rootView.findViewById(R.id.highlight_guide)
        if (isFirstTime()) {
            highlightGuide?.visibility = View.VISIBLE
            highlightGuide?.setOnClickListener {
                highlightGuide?.visibility = View.GONE
                disableHighlight()
            }
        } else {
            highlightGuide?.visibility = View.GONE
        }

        viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                mClockPosition = position
                if (viewPager != null) {
                    viewPager?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                updateClockName(position)
            }
        })
        return rootView
    }

    private fun updateClockName(position: Int) {
        val clockNames = arrayOf(
            "Default Clock",
            "No Clock",
            "OnePlus Clock",
            "OnePlus Clock 2",
            "iOS Clock Legacy",
            "iOS Clock",
            "iOS Clock 2",
            "iOS Clock 3",
            "iOS Clock 4",
            "iOS Clock 5",
            "iOS Clock 6",
            "iOS Clock 7",
            "iOS Clock 8",
            "iOS Clock 9",
            "iOS Clock 10",
            "iOS Clock 11",
            "iOS Clock 12",
            "iOS Clock 13",
            "iOS Clock 14",
            "iOS Clock 15",
            "iOS Clock 16",
            "iOS Clock 17",
            "iOS Clock 18",
            "iOS Clock 19",
            "MIUI Clock",
            "HyperOS Clock",
            "ColorOS Clock 1",
            "ColorOS Clock 2",
            "ColorOS Big Clock",
            "Simple Clock",
            "IDE Clock",
            "Moto Clock",
            "Stylish Clock",
            "Stylish Clock 2",
            "Stylish Clock 3",
            "Stylish Clock 4",
            "Stylish Clock 5",
            "Stylish Clock 6",
            "Stylish Clock 7",
            "Stylish Clock 8",
            "Stylish Clock 9",
            "Stylish Clock 10",
            "Text Clock",
            "LifeStyle Clock",
            "Android 9 Vibe",
            "NothingOS 1 Clock",
            "NothingOS 2 Clock",
            "NOS4 Spaceage Clock",
            "NOS4 Polyline Clock",
            "Stacked Clock",
            "X Factor",
            "Simple Analog",
            "Block",
            "Bubble",
            "Outline",
            "Ovalium",
            "Rectangle",
            "Wallet",
            "Clavicula",
            "KLN",
            "Miring",
            "Scapula",
            "Sternum",
            "Circle",
            "List",
            "Big Clock 1",
            "Big Clock 2",
            "Big Clock 3",
            "Big Clock 4",
            "Sweet",
            "Pixel",
            "Samurai",
            "Gateway",
            "Tall Clock",
            "GoBold monet",
            "GoBold",
            "Delirium",
            "Delirium Dual",
            "Skewrom",
            "Skewrom 2",
            "Taller",
            "Taller 2",
            "Taller 3",
            "Modak",
            "Galada",
            "Neu",
            "Neu2",
            "Neu3",
            "Badeen"
        )
        if (clockNameTextView != null && position >= 0 && position < clockNames.size) {
            clockNameTextView?.text = clockNames[position]
        }
    }

    private fun updateClockOverlays(clockStyle: Int) {
        mThemeUtils.setOverlayEnabled(
            "android.theme.customization.hideclock",
            if (clockStyle != 0) "com.android.systemui.clocks.hideclock" else "android",
            "android"
        )
        mThemeUtils.setOverlayEnabled(
            "android.theme.customization.smartspace",
            if (clockStyle != 0) "com.android.systemui.hide.smartspace" else "com.android.systemui",
            "com.android.systemui"
        )
        mThemeUtils.setOverlayEnabled(
            "android.theme.customization.smartspace_offset",
            if (clockStyle != 0 && isCenterClock(clockStyle))
                "com.android.systemui.smartspace_offset.smartspace"
            else
                "com.android.systemui",
            "com.android.systemui"
        )
    }

    private fun isCenterClock(clockStyle: Int): Boolean {
        for (centerClock in mCenterClocks) {
            if (centerClock == clockStyle) {
                return true
            }
        }
        return false
    }

    private fun shouldScaleDown(position: Int): Boolean {
        val layoutId = CLOCK_LAYOUTS[position]
        return layoutId == R.layout.keyguard_clock_stylish
                || layoutId == R.layout.keyguard_clock_stylish2 || layoutId == R.layout.keyguard_clock_stylish3
                || layoutId == R.layout.keyguard_clock_stylish4 || layoutId == R.layout.keyguard_clock_stylish5
                || layoutId == R.layout.keyguard_clock_stylish6 || layoutId == R.layout.keyguard_clock_stylish7
                || layoutId == R.layout.keyguard_clock_stylish8 || layoutId == R.layout.keyguard_clock_stylish9
                || layoutId == R.layout.keyguard_clock_stylish10
    }

    private fun isFirstTime(): Boolean {
        return Settings.System.getIntForUser(
            requireContext().contentResolver, PREF_FIRST_TIME, 1, UserHandle.USER_CURRENT
        ) != 0
    }

    private fun disableHighlight() {
        Settings.System.putIntForUser(requireContext().contentResolver, PREF_FIRST_TIME, 0, UserHandle.USER_CURRENT)
    }

    private inner class ClockPagerAdapter : PagerAdapter() {
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(requireActivity())
            val layout = inflater.inflate(CLOCK_LAYOUTS[position], container, false)

            val bottomPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                150f,
                resources.displayMetrics
            ).toInt()
            layout.setPadding(
                layout.paddingLeft,
                layout.paddingTop,
                layout.paddingRight,
                bottomPadding
            )

            if (shouldScaleDown(position)) {
                layout.scaleX = 0.5f
                layout.scaleY = 0.5f
            }

            container.addView(layout)
            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getCount(): Int = CLOCK_LAYOUTS.size

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }
    }

    private inner class ClockRecyclerAdapter : RecyclerView.Adapter<ClockRecyclerAdapter.ViewHolder>() {

        inner class ViewHolder(val container: FrameLayout) : RecyclerView.ViewHolder(container)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val container = FrameLayout(parent.context)
            container.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT
            )
            return ViewHolder(container)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val container = holder.container
            container.removeAllViews()

            val inflater = LayoutInflater.from(container.context)
            val clockView = inflater.inflate(CLOCK_LAYOUTS[position], container, false)

            val bottomPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                150f,
                resources.displayMetrics
            ).toInt()
            clockView.setPadding(
                clockView.paddingLeft,
                clockView.paddingTop,
                clockView.paddingRight,
                bottomPadding
            )

            if (shouldScaleDown(position)) {
                clockView.scaleX = 0.5f
                clockView.scaleY = 0.5f
            } else {
                clockView.scaleX = 1.0f
                clockView.scaleY = 1.0f
            }

            container.addView(clockView)
        }

        override fun getItemCount(): Int = CLOCK_LAYOUTS.size
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateClockName(mClockPosition)
    }

    override fun onResume() {
        super.onResume()
        updateClockName(mClockPosition)
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN
    }
}
