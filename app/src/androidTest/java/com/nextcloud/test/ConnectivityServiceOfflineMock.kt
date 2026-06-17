/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Philipp Hasper <vcs@hasper.info>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.test

import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.network.NetworkChangeListener

/** A mocked connectivity service returning that the device is offline **/
class ConnectivityServiceOfflineMock : ConnectivityService {
    override fun addListener(listener: NetworkChangeListener) = Unit
    override fun removeListener(listener: NetworkChangeListener) = Unit
    override fun isNetworkAndServerAvailable(callback: ConnectivityService.GenericCallback<Boolean>) {
        callback.onComplete(false)
    }
    override fun isConnected(): Boolean = false
    override fun isInternetWalled(): Boolean = false
    override fun getConnectivity(): Connectivity = Connectivity.CONNECTED_WIFI
}
