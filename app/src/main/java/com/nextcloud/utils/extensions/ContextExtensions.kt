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
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.webkit.WebViewCompat
import com.owncloud.android.R
import com.owncloud.android.datamodel.ReceiverFlag

private const val TAG = "ContextExtensions"

fun Context.isWebViewAvailable(): Boolean = try {
    WebViewCompat.getCurrentWebViewPackage(this) != null
} catch (e: Exception) {
    Log.w(TAG, "WebView provider cannot be resolved: ${e.message}")
    false
}

fun Context.hourPlural(hour: Int): String = resources.getQuantityString(R.plurals.hours, hour, hour)

fun Context.minPlural(min: Int): String = resources.getQuantityString(R.plurals.minutes, min, min)

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerBroadcastReceiver(receiver: BroadcastReceiver?, filter: IntentFilter, flag: ReceiverFlag): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, flag.getId())
    } else {
        registerReceiver(receiver, filter)
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

fun Activity.openMediaPermissions(requestCode: Int) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivityForResult(intent, requestCode)
}

fun Activity.openAllFilesAccessSettings(requestCode: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return
    }

    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
        data = "package:$packageName".toUri()
    }

    startActivityForResult(intent, requestCode)
}
