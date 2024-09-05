/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextcloud.client.network.ConnectivityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface NetworkChangeListener {
    fun networkAndServerConnectionListener(isNetworkAndServerAvailable: Boolean)
}

class NetworkChangeReceiver(
    private val listener: NetworkChangeListener,
    private val connectivityService: ConnectivityService
) : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        scope.launch {
            val isNetworkAndServerAvailable = connectivityService.isNetworkAndServerAvailable()

            launch(Dispatchers.Main) {
                listener.networkAndServerConnectionListener(isNetworkAndServerAvailable)
            }
        }
    }
}
