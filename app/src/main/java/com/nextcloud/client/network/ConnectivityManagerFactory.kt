/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.network

object ConnectivityManagerFactory {
    val mock: ConnectivityService = object : ConnectivityService {
        override fun isNetworkAndServerAvailable(onCompleted: (Boolean) -> Unit) {
            onCompleted(true)
        }
        override fun addListener(listener: NetworkChangeListener) = Unit
        override fun removeListener(listener: NetworkChangeListener) = Unit
        override val isConnected = true
        override fun isInternetWalled(): Boolean = false
        override val connectivity = Connectivity.CONNECTED_WIFI
    }

    val metered: ConnectivityService = object : ConnectivityService {
        override fun isNetworkAndServerAvailable(onCompleted: (Boolean) -> Unit) {
            onCompleted(true)
        }
        override fun addListener(listener: NetworkChangeListener) = Unit
        override fun removeListener(listener: NetworkChangeListener) = Unit
        override val isConnected = false
        override fun isInternetWalled(): Boolean = false
        override val connectivity = Connectivity(true, true, true, true, false)
    }

    val wifi: ConnectivityService = object : ConnectivityService {
        override fun isNetworkAndServerAvailable(onCompleted: (Boolean) -> Unit) {
            onCompleted(true)
        }
        override fun addListener(listener: NetworkChangeListener) = Unit
        override fun removeListener(listener: NetworkChangeListener) = Unit
        override val isConnected = false
        override fun isInternetWalled(): Boolean = false
        override val connectivity = Connectivity(true, false, false, true, false)
    }
}
