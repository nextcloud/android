/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.RouteInfo
import android.os.Build
import androidx.annotation.RequiresApi

class LocalNetworkRoutes(context: Context) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    // Android only treats Wi-Fi and Ethernet as local network, VPN and cellular are excluded
    @Suppress("DEPRECATION")
    fun directlyConnectedRoutes(): List<RouteInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) {
            return emptyList()
        }

        val manager = connectivityManager ?: return emptyList()
        return manager.allNetworks
            .filter { manager.isLocalNetwork(it) }
            .flatMap { manager.getLinkProperties(it)?.routes.orEmpty() }
            .filter { it.isDirectlyConnected() }
    }

    private fun ConnectivityManager.isLocalNetwork(network: Network): Boolean {
        val capabilities = getNetworkCapabilities(network) ?: return false
        return !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun RouteInfo.isDirectlyConnected(): Boolean =
        type == RouteInfo.RTN_UNICAST && !hasGateway() && !isDefaultRoute
}
