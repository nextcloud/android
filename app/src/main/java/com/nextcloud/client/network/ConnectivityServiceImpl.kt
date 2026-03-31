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
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus
import kotlin.jvm.functions.Function1

@Suppress("TooGenericExceptionCaught", "ReturnCount")
class ConnectivityServiceImpl(
    private val context: Context,
    private val accountManager: UserAccountManager,
    private val clientFactory: ClientFactory,
    private val requestBuilder: GetRequestBuilder,
    private val walledCheckCache: WalledCheckCache
) : ConnectivityService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var availabilityCheckJob: Job? = null
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var currentConnectivity = Connectivity.DISCONNECTED
    private val listeners = mutableSetOf<NetworkChangeListener>()

    override fun addListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        scope.launch {
            val available = !isInternetWalled()
            withContext(Dispatchers.Main) {
                listeners.forEach {
                    Log_OC.d(TAG, "notifying listeners")
                    context.showToast("NOTIFIYING LISTENERS !!!: $available")
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
            walledCheckCache.clear()
            notifyListeners()
            return
        }

        val hasTransport = isSupportedTransport(capabilities)
        val hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        currentConnectivity = Connectivity(
            isConnected = hasTransport || hasInternetCapability,
            isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
            isVPN = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        )

        walledCheckCache.clear()
        notifyListeners()
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
        val cachedValue = walledCheckCache.getValue()
        if (cachedValue != null) {
            Log_OC.d(TAG, "cached value is used, isWalled: $cachedValue")
            return cachedValue
        }

        val baseServerAddress = accountManager.user.server.uri.toString()
        if (baseServerAddress.isEmpty()) {
            Log_OC.e(TAG, "no base server address, internet is walled")
            return true
        }

        val activeCapabilities = connectivityManager.activeNetwork
            ?.let { connectivityManager.getNetworkCapabilities(it) }

        if (activeCapabilities == null) {
            Log_OC.e(TAG, "no active network capabilities at check time, treating as walled")
            return true
        }

        val hasLiveTransport = isSupportedTransport(activeCapabilities)
        if (!hasLiveTransport) {
            Log_OC.e(TAG, "no supported transport at check time, treating as walled")
            return true
        }

        val isMeteredNonWifi = !currentConnectivity.isWifi && currentConnectivity.isMetered
        if (isMeteredNonWifi) {
            Log_OC.w(TAG, "skipping server reachability check, internet is metered and not Wi-Fi")
            return false
        }

        val get = requestBuilder.invoke(baseServerAddress + CONNECTIVITY_CHECK_ROUTE)
        val client = clientFactory.createPlainClient()

        val isWalled = try {
            val status = get.execute(client)
            (!(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0)).also {
                if (it) Log_OC.w(TAG, "server returned unexpected response")
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception during server check", e)
            getWalledValueFromException(e)
        } finally {
            get.releaseConnection()
        }

        walledCheckCache.setValue(isWalled)
        Log_OC.d(TAG, "server check, isWalled: $isWalled")
        return isWalled
    }

    override fun getConnectivity() = currentConnectivity

    private fun getWalledValueFromException(e: Exception): Boolean = when (e) {
        is java.net.UnknownHostException -> {
            Log_OC.w(TAG, "UnknownHostException")
            false
        }

        is javax.net.ssl.SSLException -> {
            Log_OC.w(TAG, "SSLException")
            false
        }

        is java.net.SocketTimeoutException -> {
            Log_OC.w(TAG, "SocketTimeoutException")
            false
        }

        is java.io.IOException -> {
            Log_OC.w(TAG, "IOException")
            false
        }

        else -> {
            Log_OC.w(TAG, "Unknown error, fallback to walled assumption")
            true
        }
    }

    fun unregisterCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    companion object {
        private const val TAG = "ConnectivityServiceImpl"
        private const val CONNECTIVITY_CHECK_ROUTE = "/index.php/204"
    }
}
