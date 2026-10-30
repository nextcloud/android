/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.filedisplayactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LocalIntentReceiver(private val onIntentReceived: (Intent) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) = onIntentReceived(intent)
}
