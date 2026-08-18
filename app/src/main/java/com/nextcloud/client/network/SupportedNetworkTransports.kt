/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network

import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.work.Constraints
import androidx.work.NetworkType

object SupportedNetworkTransports {
    private val values: List<Int>
        get() = buildList {
            add(NetworkCapabilities.TRANSPORT_WIFI)
            add(NetworkCapabilities.TRANSPORT_CELLULAR)
            add(NetworkCapabilities.TRANSPORT_ETHERNET)
            add(NetworkCapabilities.TRANSPORT_VPN)
            add(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            add(NetworkCapabilities.TRANSPORT_WIFI_AWARE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(NetworkCapabilities.TRANSPORT_USB)
            }
        }

    fun getConstraints(requiresCharging: Boolean = false): Constraints {
        val networkRequest = NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .apply {
                values.forEach { addTransportType(it) }
            }
            .build()

        return Constraints.Builder()
            .setRequiredNetworkRequest(networkRequest, NetworkType.CONNECTED)
            .setRequiresCharging(requiresCharging)
            .build()
    }

    fun isSupportedTransport(capabilities: NetworkCapabilities) = values.any { capabilities.hasTransport(it) }
}
