/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.owncloud.android.R
import com.owncloud.android.datamodel.ReceiverFlag

fun Context.hourPlural(hour: Int): String = resources.getQuantityString(R.plurals.hours, hour, hour)

fun Context.minPlural(min: Int): String = resources.getQuantityString(R.plurals.minutes, min, min)

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerBroadcastReceiver(receiver: BroadcastReceiver?, filter: IntentFilter, flag: ReceiverFlag): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, flag.getId())
    } else {
        registerReceiver(receiver, filter)
    }

fun Context.statusBarHeight(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val windowInsets = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .currentWindowMetrics
        .windowInsets
    val insets = windowInsets.getInsets(WindowInsets.Type.statusBars())
    insets.top
} else {
    @Suppress("DEPRECATION")
    val decorView = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay
        .let { display ->
            val decorView = android.view.View(this)
            display.getRealMetrics(android.util.DisplayMetrics())
            decorView
        }
    val windowInsetsCompat = ViewCompat.getRootWindowInsets(decorView)
    windowInsetsCompat?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
}

fun Context.showToast(message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

fun Context.showToast(messageId: Int) = showToast(getString(messageId))

fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
