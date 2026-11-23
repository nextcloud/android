/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs.upload

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import com.owncloud.android.lib.common.utils.Log_OC

object UploadDiagnostics {
    const val TAG = "UploadDiagnostics"

    @JvmStatic
    fun log(message: String) {
        Log_OC.d(TAG, message)
    }

    fun describeNetwork(context: Context, network: Network?): String {
        if (network == null) {
            return "none"
        }

        val capabilities = context.getSystemService(ConnectivityManager::class.java)?.getNetworkCapabilities(network)

        return "$network $capabilities"
    }
}
