/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.net.NetworkCapabilities
import android.os.Build
import com.nextcloud.client.network.Connectivity

fun NetworkCapabilities.hasSupportedTransport(): Boolean = supportedTransports.any { hasTransport(it) } ||
    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasTransport(NetworkCapabilities.TRANSPORT_USB))

/**
 * A Nextcloud instance may live on the local network only, so neither [NetworkCapabilities.NET_CAPABILITY_VALIDATED]
 * nor [NetworkCapabilities.NET_CAPABILITY_INTERNET] can be required here: a link with a usable transport is enough,
 * and whether the server answers is decided by the reachability check instead.
 */
fun NetworkCapabilities.toConnectivity(isServerAvailable: Boolean?): Connectivity = Connectivity(
    isConnected = hasSupportedTransport() || hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
    isMetered = !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
    isWifi = hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
    isServerAvailable = isServerAvailable,
    isVPN = hasTransport(NetworkCapabilities.TRANSPORT_VPN)
)

private val supportedTransports = intArrayOf(
    NetworkCapabilities.TRANSPORT_WIFI,
    NetworkCapabilities.TRANSPORT_CELLULAR,
    NetworkCapabilities.TRANSPORT_ETHERNET,
    NetworkCapabilities.TRANSPORT_VPN,
    NetworkCapabilities.TRANSPORT_BLUETOOTH,
    NetworkCapabilities.TRANSPORT_WIFI_AWARE
)
