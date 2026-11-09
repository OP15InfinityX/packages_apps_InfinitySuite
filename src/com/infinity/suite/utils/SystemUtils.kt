package com.infinity.suite.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

import androidx.appcompat.app.AlertDialog

import com.android.settings.R
import com.android.internal.util.infinity.Utils

object SystemUtils {

    @JvmStatic
    fun showSystemUiRestartDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(R.string.systemui_restart_title)
            .setMessage(R.string.systemui_restart_message)
            .setPositiveButton(R.string.systemui_restart_yes) { _, _ ->
                restartSystemUI(context)
            }
            .setNegativeButton(R.string.systemui_restart_not_now, null)
            .show()
    }

    @JvmStatic
    fun restartSystemUI(context: Context) {
        Toast.makeText(
            context,
            R.string.systemui_restart_process,
            Toast.LENGTH_LONG
        ).show()

        Handler(Looper.getMainLooper()).postDelayed({
            Utils.restartSystemUI()
        }, 2000) // 2-second delay
    }
} 