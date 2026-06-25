/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network

/**
 * This service provides information about current network connectivity
 * and server reachability.
 */
interface ConnectivityService {
    fun addListener(listener: NetworkChangeListener)
    fun removeListener(listener: NetworkChangeListener)

    /**
     * Asynchronously checks whether both the device's network connection
     * and the Nextcloud server are available.
     *
     *
     * This method executes its logic on a background thread and posts the result
     * back to the main thread.
     *
     * The check is based on [.isInternetWalled] — if the Internet is not
     * walled (i.e., the server is reachable and not restricted by a captive portal),
     * this method reports `true`. Otherwise, it reports `false`.
     *
     * returns `true` when the network and
     * Nextcloud server are reachable, or `false` otherwise.
     */
    fun isNetworkAndServerAvailable(onCompleted: (Boolean) -> Unit)

    /**
     * Checks whether the device currently has an active connection using a recognized
     * transport type.
     *
     *
     * This method reflects the current connectivity state as exposed by this service
     * and indicates whether a supported transport such as Wi-Fi, Cellular, Ethernet,
     * VPN, or Bluetooth is active.
     *
     *
     * For Android 12 (API 31) and newer, USB network transport is also considered supported.
     *
     *
     * Note: This does **not** guarantee that the active network has
     * Android Internet capability, has been system-validated for Internet access, or
     * that the Nextcloud server itself is reachable.
     *
     * @return `true` if the device is connected through a supported transport
     * type; `false` otherwise.
     */
    val isConnected: Boolean

    /**
     * Determines whether the device's current Internet connection is "walled" — that is,
     * restricted by a captive portal or other form of network access control that prevents
     * full connectivity to the Nextcloud server.
     *
     *
     * This method does **not** test general Internet reachability (e.g. Google or DNS),
     * but rather focuses on the ability to access the configured Nextcloud server directly.
     * In other words, it checks whether the server can be reached without network interference
     * such as a hotel's captive portal, Wi-Fi login page, or similar restrictions.
     *
     *
     * Results are cached for subsequent checks to minimize unnecessary HTTP requests.
     *
     * @return `true` if the Internet appears to be walled (e.g. captive portal or
     * restricted access); `false` if the Nextcloud server is reachable and
     * the network allows normal Internet access.
     */
    fun isInternetWalled(): Boolean

    /**
     * Returns a [Connectivity] object that represents the current network state.
     *
     *
     * This includes whether the device is connected, whether the network is metered,
     * and whether it uses Wi-Fi or Ethernet transport. It uses
     * [.isConnected] to verify active Internet capability
     *
     *
     * If no active network is found, [Connectivity.DISCONNECTED] is returned.
     *
     * @return a [Connectivity] instance describing the current network connection.
     */
    val connectivity: Connectivity
}
