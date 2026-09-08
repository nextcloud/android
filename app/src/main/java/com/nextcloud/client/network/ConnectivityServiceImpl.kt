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
import androidx.annotation.VisibleForTesting
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.operations.GetMethod
import com.nextcloud.utils.extensions.hasSupportedTransport
import com.nextcloud.utils.extensions.toConnectivity
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus
import java.util.concurrent.CopyOnWriteArraySet

@Suppress("TooGenericExceptionCaught", "ReturnCount")
class ConnectivityServiceImpl(
    context: Context,
    private val accountManager: UserAccountManager,
    private val clientFactory: ClientFactory,
    private val requestBuilder: GetRequestBuilder,
    private val walledCheckCache: WalledCheckCache
) : ConnectivityService {

    companion object {
        private const val TAG = "ConnectivityServiceImpl"
        private const val CONNECTIVITY_CHECK_ROUTE = "/index.php/204"
        private const val NOTIFY_DEBOUNCE_MS = 300L
    }

    @VisibleForTesting
    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    // region private values
    private val scope by lazy { CoroutineScope(ioDispatcher) }
    private var notifyJob: Job? = null
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val listeners = CopyOnWriteArraySet<NetworkChangeListener>()
    private val stateLock = Any()

    @Volatile
    private var currentConnectivity: Connectivity = Connectivity.DISCONNECTED

    @Volatile
    private var lastNotifiedAvailability: Boolean? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log_OC.d(TAG, "default network available")
            walledCheckCache.clearAll()
            applyConnectivity(connectivityManager.getNetworkCapabilities(network), forceRecheck = true)
        }

        override fun onLost(network: Network) {
            Log_OC.w(TAG, "connection lost")
            applyConnectivity(resolveNetworkCapabilities(), forceRecheck = true)
        }

        /**
         * Fires on a stable network too (signal strength, link bandwidth, validation transitions), so this path must
         * stay allocation- and IPC-free until the derived state really differs.
         */
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            applyConnectivity(networkCapabilities)
        }
    }
    // endregion

    init {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        applyConnectivity(resolveNetworkCapabilities())
    }

    // region overridden methods
    override fun isNetworkAndServerAvailable(onCompleted: (Boolean) -> Unit) {
        scope.launch {
            val available = !isInternetWalled()
            Log_OC.d(TAG, "isNetworkAndServerAvailable: $available")
            withContext(Dispatchers.Main) {
                onCompleted(available)
            }
        }
    }

    override val isConnected: Boolean
        get() = currentConnectivity.isConnected

    override fun isInternetWalled(): Boolean {
        if (!currentConnectivity.isConnected) {
            Log_OC.w(TAG, "no usable network transport, treating as walled")
            return true
        }

        val user = accountManager.user
        val baseServerAddress = user.server.uri.toString()
        if (baseServerAddress.isEmpty()) {
            Log_OC.e(TAG, "no base server address, relying on current connectivity")
            return !currentConnectivity.isConnected
        }

        val currentKey = ConnectivityKey.getBy(user)
        walledCheckCache.getValue(currentKey)?.let {
            Log_OC.d(TAG, "cached value is used, isWalled: $it")
            recordServerAvailability(!it)
            return it
        }

        val isWalled = checkServerReachability(baseServerAddress)
        walledCheckCache.setValue(currentKey, isWalled)
        recordServerAvailability(!isWalled)

        Log_OC.d(TAG, "server check, isWalled: $isWalled")
        return isWalled
    }

    override val connectivity: Connectivity
        get() = currentConnectivity

    override fun addListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
    }
    // endregion

    // region public methods
    fun updateConnectivity() {
        applyConnectivity(resolveNetworkCapabilities())
    }

    @Suppress("unused")
    fun unregisterCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        scope.cancel()
    }

    fun interface GetRequestBuilder {
        operator fun invoke(url: String): GetMethod
    }
    // endregion

    // region private methods
    private fun applyConnectivity(capabilities: NetworkCapabilities?, forceRecheck: Boolean = false) {
        val isStructural: Boolean

        synchronized(stateLock) {
            val previous = currentConnectivity
            val updated = capabilities?.toConnectivity(previous.isServerAvailable) ?: Connectivity.DISCONNECTED

            if (previous.hasSameNetworkStateAs(updated)) {
                if (!forceRecheck) {
                    return
                }
                isStructural = false
            } else {
                isStructural = previous.isConnected != updated.isConnected ||
                    previous.isWifi != updated.isWifi ||
                    previous.isVPN != updated.isVPN
                Log_OC.d(TAG, "connectivity changed to $updated, isStructural: $isStructural")
            }

            currentConnectivity = updated
        }

        if (isStructural) {
            walledCheckCache.clearAll()
        }

        notifyListeners()
    }

    /**
     * [Connectivity.isServerAvailable] is the outcome of the reachability check rather than a property of the link,
     * so it must not take part in deciding whether the network itself changed.
     */
    private fun Connectivity.hasSameNetworkStateAs(other: Connectivity): Boolean = isConnected == other.isConnected &&
        isMetered == other.isMetered &&
        isWifi == other.isWifi &&
        isVPN == other.isVPN

    private fun recordServerAvailability(isAvailable: Boolean) {
        synchronized(stateLock) {
            currentConnectivity = currentConnectivity.copy(isServerAvailable = isAvailable)
        }
    }

    private fun checkServerReachability(baseServerAddress: String): Boolean {
        val get = requestBuilder.invoke(baseServerAddress + CONNECTIVITY_CHECK_ROUTE)
        val client = clientFactory.createPlainClient()

        return try {
            val status = get.execute(client)
            (!(status == HttpStatus.SC_NO_CONTENT && get.getResponseContentLength() <= 0)).also {
                if (it) Log_OC.w(TAG, "server returned unexpected response, status: $status")
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception during server check", e)
            e.toWalledValue(currentConnectivity.isServerAvailable)
        } finally {
            get.releaseConnection()
        }
    }

    private fun notifyListeners() {
        if (listeners.isEmpty()) {
            return
        }

        notifyJob?.cancel()
        notifyJob = scope.launch {
            // A handover delivers onLost, onAvailable and onCapabilitiesChanged in quick succession; suspending here
            // first lets the earlier jobs be cancelled before any of them reaches the server.
            delay(NOTIFY_DEBOUNCE_MS)

            val available = !isInternetWalled()

            if (available == lastNotifiedAvailability) {
                Log_OC.d(TAG, "server availability is still $available, listeners not notified")
                return@launch
            }

            lastNotifiedAvailability = available
            withContext(Dispatchers.Main) {
                listeners.forEach {
                    it.networkAndServerConnectionListener(available)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveNetworkCapabilities(): NetworkCapabilities? {
        connectivityManager.activeNetwork
            ?.let { connectivityManager.getNetworkCapabilities(it) }
            ?.also { return it }

        return connectivityManager.allNetworks
            .mapNotNull { connectivityManager.getNetworkCapabilities(it) }
            .firstOrNull { it.hasSupportedTransport() }
    }
    // endregion
}
