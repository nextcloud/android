/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ConnectivityService.GenericCallback
import com.nextcloud.operations.GetMethod
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus
import kotlin.jvm.functions.Function1

class ConnectivityServiceImpl(
    context: Context,
    private val accountManager: UserAccountManager,
    private val clientFactory: ClientFactory,
    private val requestBuilder: GetRequestBuilder,
    private val walledCheckCache: WalledCheckCache
) : ConnectivityService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var currentConnectivity = Connectivity.DISCONNECTED

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log_OC.d(TAG, "network available")
            updateConnectivity()
        }

        override fun onLost(network: Network) {
            Log_OC.w(TAG, "connection lost")
            updateConnectivity()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            Log_OC.d(TAG, "capability changed")
            updateConnectivity()
        }
    }

    class GetRequestBuilder : Function1<String, GetMethod> {
        override fun invoke(url: String) = GetMethod(url, false)
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        updateConnectivity()
        Log_OC.d(TAG, "connectivity service constructed")
    }

    fun updateConnectivity() {
        val capabilities = connectivityManager.activeNetwork
            ?.let { connectivityManager.getNetworkCapabilities(it) }

        if (capabilities == null) {
            Log_OC.w(TAG, "no active network or capabilities, connectivity is disconnected")
            currentConnectivity = Connectivity.DISCONNECTED
            return
        }

        currentConnectivity = Connectivity(
            isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                isSupportedTransport(capabilities),
            isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
        )

        walledCheckCache.clear()
    }

    private fun isSupportedTransport(capabilities: NetworkCapabilities) =
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB))

    override fun isNetworkAndServerAvailable(callback: GenericCallback<Boolean?>) {
        scope.launch {
            val available = !isInternetWalled()
            Log_OC.d(TAG, "isNetworkAndServerAvailable: $available")
            withContext(Dispatchers.Main) {
                callback.onComplete(available)
            }
        }
    }

    override fun isConnected() = currentConnectivity.isConnected

    override fun isInternetWalled(): Boolean {
        walledCheckCache.getValue()?.let {
            Log_OC.d(TAG, "isInternetWalled(): cached value is used, isWalled: $it")
            return it
        }

        val baseServerAddress = accountManager.user.server.uri.toString()

        // No connection or no server configured
        if (!currentConnectivity.isConnected || baseServerAddress.isEmpty()) {
            return (!currentConnectivity.isConnected).also {
                walledCheckCache.setValue(it)
                Log_OC.d(TAG, "isInternetWalled(): no connection or server address, isWalled: $it")
            }
        }

        // Skip HTTP probe on metered non-WiFi (e.g. cellular)
        if (!currentConnectivity.isWifi && currentConnectivity.isMetered) {
            return (!currentConnectivity.isConnected).also {
                walledCheckCache.setValue(it)
                Log_OC.d(TAG, "isInternetWalled(): metered non-WiFi, skipping probe, isWalled: $it")
            }
        }

        val get = requestBuilder.invoke(baseServerAddress + CONNECTIVITY_CHECK_ROUTE)
        val client = clientFactory.createPlainClient()

        val isWalled = try {
            val status = get.execute(client)
            (!(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0)).also {
                if (it) Log_OC.w(TAG, "isInternetWalled(): Server returned unexpected response")
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "isInternetWalled(): Exception during server check", e)
            true
        } finally {
            get.releaseConnection()
        }

        walledCheckCache.setValue(isWalled)
        Log_OC.d(TAG, "isInternetWalled(): server check, isWalled: $isWalled")
        return isWalled
    }

    override fun getConnectivity() = currentConnectivity

    fun unregisterCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    companion object {
        private const val TAG = "ConnectivityServiceImpl"
        private const val CONNECTIVITY_CHECK_ROUTE = "/index.php/204"
    }
}
