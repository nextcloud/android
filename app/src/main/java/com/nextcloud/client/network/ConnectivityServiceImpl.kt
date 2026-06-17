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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

@Suppress("TooGenericExceptionCaught", "ReturnCount")
class ConnectivityServiceImpl(
    context: Context,
    private val accountManager: UserAccountManager,
    private val clientFactory: ClientFactory,
    private val requestBuilder: GetRequestBuilder,
    private val walledCheckCache: WalledCheckCache
) : ConnectivityService {

    private val scope = CoroutineScope(Dispatchers.IO)

    private var availabilityCheckJob: Job? = null
    private var notifyJob: Job? = null

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = mutableSetOf<NetworkChangeListener>()

    @Volatile
    private var currentConnectivity: Connectivity = Connectivity.DISCONNECTED

    private val key: ConnectivityKey
        get() = ConnectivityKey.getBy(accountManager)

    override fun addListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        if (listeners.isEmpty()) {
            return
        }

        notifyJob?.cancel()
        notifyJob = scope.launch {
            val available = !isInternetWalled()
            withContext(Dispatchers.Main) {
                listeners.forEach {
                    it.networkAndServerConnectionListener(available)
                }
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            Log_OC.w(TAG, "connection lost")
            updateConnectivity()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            Log_OC.d(TAG, "capability changed")
            updateConnectivity()
        }
    }

    fun interface GetRequestBuilder {
        operator fun invoke(url: String): GetMethod
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        updateConnectivity()
        Log_OC.d(TAG, "connectivity service constructed")
    }

    fun updateConnectivity() {
        val currentKey = key
        val previous = currentConnectivity

        val capabilities = resolveNetworkCapabilities()

        val newConnectivity = if (capabilities == null) {
            Log_OC.w(TAG, "no network capabilities found, connectivity is disconnected")
            Connectivity.DISCONNECTED
        } else {
            val hasTransport = isSupportedTransport(capabilities)
            val hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            Connectivity(
                isConnected = hasTransport || hasInternetCapability,
                isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
                isServerAvailable = previous.isServerAvailable,
                isVPN = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            )
        }

        if (previous != newConnectivity) {
            currentConnectivity = newConnectivity
            walledCheckCache.putConnectivityValue(currentKey, newConnectivity)

            val isStructural = (
                previous.isConnected != newConnectivity.isConnected ||
                    previous.isWifi != newConnectivity.isWifi
                )

            if (isStructural) {
                walledCheckCache.clear(currentKey)
            }
            notifyListeners()
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveNetworkCapabilities(): NetworkCapabilities? {
        connectivityManager.activeNetwork
            ?.let { connectivityManager.getNetworkCapabilities(it) }
            ?.also { return it }

        return connectivityManager.allNetworks
            .mapNotNull { connectivityManager.getNetworkCapabilities(it) }
            .firstOrNull { isSupportedTransport(it) }
    }

    private fun isSupportedTransport(capabilities: NetworkCapabilities) =
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) ||
            (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB)
                )

    override fun isNetworkAndServerAvailable(callback: GenericCallback<Boolean>) {
        availabilityCheckJob?.cancel()
        availabilityCheckJob = scope.launch {
            val available = !isInternetWalled()
            Log_OC.d(TAG, "isNetworkAndServerAvailable: $available")
            withContext(Dispatchers.Main) {
                callback.onComplete(available)
            }
        }
    }

    override fun isConnected() = currentConnectivity.isConnected

    override fun isInternetWalled(): Boolean {
        val currentKey = key
        val cachedValue = walledCheckCache.getValue(currentKey)
        if (cachedValue != null) {
            Log_OC.d(TAG, "cached value is used, isWalled: $cachedValue")
            return cachedValue
        }

        val baseServerAddress = accountManager.user.server.uri.toString()
        if (baseServerAddress.isEmpty()) {
            Log_OC.e(TAG, "no base server address, internet is walled")
            return true
        }

        val resolvedCapabilities = resolveNetworkCapabilities()
        if (resolvedCapabilities == null || !isSupportedTransport(resolvedCapabilities)) {
            Log_OC.e(TAG, "no usable network transport at check time, treating as walled")
            return true
        }

        val get = requestBuilder.invoke(baseServerAddress + CONNECTIVITY_CHECK_ROUTE)
        val client = clientFactory.createPlainClient()

        val isWalled = try {
            val status = get.execute(client)
            (!(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0)).also {
                if (it) Log_OC.w(TAG, "server returned unexpected response, status: $status")
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception during server check", e)
            getWalledValueFromException(e)
        } finally {
            get.releaseConnection()
        }

        walledCheckCache.setValue(currentKey, isWalled)

        Log_OC.d(TAG, "server check, isWalled: $isWalled")
        return isWalled
    }

    override fun getConnectivity() = currentConnectivity

    private fun getWalledValueFromException(e: Exception): Boolean = when (e) {
        is UnknownHostException,
        is ConnectException -> {
            Log_OC.w(TAG, "offline exception (${e::class.simpleName}), treating as walled")
            true
        }

        is SocketTimeoutException -> {
            Log_OC.w(TAG, "timeout during server check, treating as walled")
            true
        }

        is SSLException -> {
            Log_OC.w(TAG, "SSL exception during server check, assuming reachable")
            false
        }

        is IOException -> {
            Log_OC.w(TAG, "I/O exception (${e::class.simpleName}), treating as walled")
            true
        }

        else -> {
            Log_OC.e(TAG, "unexpected exception type (${e::class.simpleName}), using previous state")
            currentConnectivity.isServerAvailable?.let { !it } ?: true
        }
    }

    @Suppress("unused")
    fun unregisterCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    companion object {
        private const val TAG = "ConnectivityServiceImpl"
        private const val CONNECTIVITY_CHECK_ROUTE = "/index.php/204"
    }
}
