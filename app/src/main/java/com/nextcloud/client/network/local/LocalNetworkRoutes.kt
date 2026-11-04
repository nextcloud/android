/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.RouteInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.owncloud.android.utils.PermissionUtil
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

// Finds servers on the same Wi-Fi as the phone, even when their IP is not a private one like 192.168.x.x
@Singleton
class LocalNetworkRoutes @Inject constructor(context: Context) {
    // Network settings (IPs, routes) of every Wi-Fi, Ethernet etc. network the phone is connected to
    private val linkProperties = ConcurrentHashMap<Network, LinkProperties>()

    // Keeps linkProperties up to date when a network connects, changes or disconnects
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
            linkProperties[network] = properties
        }

        override fun onLost(network: Network) {
            linkProperties.remove(network)
        }
    }

    // Only needed on Android 17+, where the local network is blocked without the permission
    init {
        if (PermissionUtil.isLocalNetworkPermissionRequired(context)) {
            context.getSystemService(ConnectivityManager::class.java)
                ?.registerNetworkCallback(buildLocalNetworkRequest(), callback)
        }
    }

    // IP ranges of the networks the phone is directly on, e.g. 192.168.1.0/24 of the home Wi-Fi
    fun directlyConnectedRoutes(): List<RouteInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) {
            return emptyList()
        }

        return linkProperties.values
            .flatMap { it.routes }
            .filter { it.isDirectlyConnected() }
    }

    // Networks Android counts as local; VPNs are left out by default, cellular is not listed
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun buildLocalNetworkRequest(): NetworkRequest = NetworkRequest.Builder()
        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_USB)
        .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
        .addTransportType(NetworkCapabilities.TRANSPORT_THREAD)
        .build()

    // Route to devices on the same network as the phone, not through the router to the internet
    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun RouteInfo.isDirectlyConnected(): Boolean =
        type == RouteInfo.RTN_UNICAST && !hasGateway() && !isDefaultRoute
}
