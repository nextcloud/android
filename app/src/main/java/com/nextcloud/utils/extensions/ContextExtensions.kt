/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.owncloud.android.datamodel.ReceiverFlag

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerBroadcastReceiver(receiver: BroadcastReceiver?, filter: IntentFilter, flag: ReceiverFlag): Intent? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, flag.getId())
    } else {
        registerReceiver(receiver, filter)
    }
}

fun Context.showToast(message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

fun Context.showToast(messageId: Int) = showToast(getString(messageId))
