/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.di.ApplicationScope
import com.nextcloud.client.network.ConnectivityKey
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.network.WalledCheckCache
import com.nextcloud.common.DNSCache
import com.nextcloud.utils.extensions.registerBroadcastReceiver
import com.owncloud.android.datamodel.ReceiverFlag
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ReceiversHelper @Inject constructor(
    private val context: Context,
    private val uploadsStorageManager: UploadsStorageManager,
    private val accountManager: UserAccountManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val walledCheckCache: WalledCheckCache,
    @param:ApplicationScope private val appScope: CoroutineScope
) {

    fun registerReceivers() {
        registerNetworkChangeReceiver()
        registerPowerChangeReceiver()
        registerPowerSaveReceiver()
    }

    private fun registerNetworkChangeReceiver() {
        registerReceiver(CONNECTIVITY_CHANGE_ACTION, WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            appScope.launch { clearNetworkCaches() }

            if (!connectivityService.connectivity.isConnected) return@registerReceiver

            restartUploadsIfNeeded()
        }
    }

    private fun registerPowerChangeReceiver() {
        registerReceiver(Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED) { intent ->
            if (intent.action != Intent.ACTION_POWER_CONNECTED) return@registerReceiver

            restartUploadsIfNeeded()
        }
    }

    private fun registerPowerSaveReceiver() {
        registerReceiver(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
            if (powerManagementService.isPowerSavingEnabled) return@registerReceiver

            restartUploadsIfNeeded()
        }
    }

    private fun registerReceiver(vararg actions: String, onIntent: (Intent) -> Unit) {
        val intentFilter = IntentFilter().apply { actions.forEach(::addAction) }
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = onIntent(intent)
        }
        context.registerBroadcastReceiver(broadcastReceiver, intentFilter, ReceiverFlag.NotExported)
    }

    private fun restartUploadsIfNeeded() {
        FilesSyncHelper.restartUploadsIfNeeded(
            uploadsStorageManager,
            accountManager,
            connectivityService,
            powerManagementService
        )
    }

    private fun clearNetworkCaches() {
        DNSCache.clear()
        walledCheckCache.clear(ConnectivityKey.getBy(accountManager))
        Log_OC.d(TAG, "DNS caches are cleared")
    }

    companion object {
        private val TAG: String = ReceiversHelper::class.java.simpleName

        @Suppress("DEPRECATION")
        private const val CONNECTIVITY_CHANGE_ACTION = ConnectivityManager.CONNECTIVITY_ACTION
    }
}
