/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2021 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.network

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.core.net.ConnectivityManagerCompat
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.operations.GetMethod
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.httpclient.HttpStatus

internal class ConnectivityServiceImpl(
    private val platformConnectivityManager: ConnectivityManager,
    private val accountManager: UserAccountManager,
    private val clientFactory: ClientFactory,
    private val requestBuilder: GetRequestBuilder,
    private val walledCheckCache: WalledCheckCache
) : ConnectivityService {
    internal class GetRequestBuilder : Function1<String, GetMethod> {
        override operator fun invoke(url: String): GetMethod {
            return GetMethod(url, false)
        }
    }

    override fun isInternetWalled(): Boolean {
        val cachedValue = walledCheckCache.getValue()
        return if (cachedValue != null) {
            cachedValue
        } else {
            val (isConnected, isMetered, isWifi) = getConnectivity()
            val result: Boolean = if (isConnected && isWifi && !isMetered) {
                isInternetWalledOnConnectedNonMeteredWifi()
            } else {
                !isConnected
            }
            walledCheckCache.setValue(result)
            result
        }
    }

    private fun isInternetWalledOnConnectedNonMeteredWifi(): Boolean {
        val baseServerAddress = accountManager.user.server.toString()
        return if (baseServerAddress.isEmpty()) {
            true
        } else {
            val get = requestBuilder.invoke(baseServerAddress + CONNECTIVITY_CHECK_ROUTE)
            val client = clientFactory.createPlainClient()
            val status = get.execute(client)

            // Content-Length is not available when using chunked transfer encoding, so check for -1 as well
            val result = !(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0)
            get.releaseConnection()
            if (result) {
                Log_OC.w(
                    TAG,
                    "isInternetWalled(): Failed to GET $CONNECTIVITY_CHECK_ROUTE, assuming connectivity is impaired"
                )
            }
            result
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun getConnectivity(): Connectivity {
        val networkInfo: NetworkInfo? = try {
            platformConnectivityManager.activeNetworkInfo
        } catch (t: Throwable) {
            null // no network available or no information (permission denied?)
        }
        return if (networkInfo != null) {
            val isConnected = networkInfo.isConnectedOrConnecting
            // more detailed check
            val isMetered: Boolean = isNetworkMetered
            val isWifi = (networkInfo.type == ConnectivityManager.TYPE_WIFI) || hasNonCellularConnectivity()
            Connectivity(isConnected, isMetered, isWifi, null)
        } else {
            Connectivity.DISCONNECTED
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private val isNetworkMetered: Boolean
        get() {
            val network = platformConnectivityManager.activeNetwork
            return try {
                val networkCapabilities = platformConnectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities != null) {
                    !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                } else {
                    ConnectivityManagerCompat.isActiveNetworkMetered(platformConnectivityManager)
                }
            } catch (e: RuntimeException) {
                Log_OC.e(TAG, "Exception when checking network capabilities", e)
                false
            }
        }

    private fun hasNonCellularConnectivity(): Boolean {
        for (networkInfo in platformConnectivityManager.allNetworkInfo) {
            if (networkInfo.isConnectedOrConnecting && (
                    networkInfo.type == ConnectivityManager.TYPE_WIFI ||
                        networkInfo.type == ConnectivityManager.TYPE_ETHERNET
                    )
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "ConnectivityServiceImpl"
        private const val CONNECTIVITY_CHECK_ROUTE = "/index.php/204"
    }
}
