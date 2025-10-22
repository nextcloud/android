/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network;


import android.net.ConnectivityManager;
import android.net.Network;

import com.nextcloud.client.account.Server;
import com.nextcloud.client.account.UserAccountManager;

import androidx.annotation.NonNull;

/**
 * This service provides information about current network connectivity
 * and server reachability.
 */
public interface ConnectivityService {
    /**
     * Asynchronously checks whether both the device's local network connection
     * and the Nextcloud server are available.
     *
     * <p>This method executes its logic on a background thread and posts the result
     * back to the main thread through the provided {@link GenericCallback}.</p>
     *
     * <p>The check is based on {@link #isInternetWalled()} — if the Internet is not
     * walled (i.e., the server is reachable and not restricted by a captive portal),
     * this method reports {@code true}. Otherwise, it reports {@code false}.</p>
     *
     * @param callback a callback that receives {@code true} when the network and
     *                 Nextcloud server are reachable, or {@code false} otherwise.
     */
    void isNetworkAndServerAvailable(@NonNull GenericCallback<Boolean> callback);

    /**
     * Checks whether the device currently has an active, validated Internet connection
     * via a recognized transport type.
     *
     * <p>This method queries the Android {@link ConnectivityManager} to determine
     * whether there is an active {@link Network} with Internet capability and an
     * acceptable transport such as Wi-Fi, Cellular, Ethernet, VPN, or Bluetooth.</p>
     *
     * <p>For Android 12 (API 31) and newer, USB network transport is also considered valid.</p>
     *
     * <p>Note: This only confirms that the Android system has validated Internet access,
     * not necessarily that the Nextcloud server itself is reachable.</p>
     *
     * @return {@code true} if the device is connected to the Internet through a supported
     *         transport type; {@code false} otherwise.
     */
    boolean isConnected();

    /**
     * Determines whether the device's current Internet connection is "walled" — that is,
     * restricted by a captive portal or other form of network access control that prevents
     * full connectivity to the Nextcloud server.
     *
     * <p>This method does <strong>not</strong> test general Internet reachability (e.g. Google or DNS),
     * but rather focuses on the ability to access the configured Nextcloud server directly.
     * In other words, it checks whether the server can be reached without network interference
     * such as a hotel's captive portal, Wi-Fi login page, or similar restrictions.</p>
     *
     * <p>The implementation performs the following steps:</p>
     * <ul>
     *     <li>Uses cached results from {@link WalledCheckCache} when available to avoid
     *         redundant network calls.</li>
     *     <li>Retrieves the active {@link Server} from {@link UserAccountManager}.</li>
     *     <li>If connected via a non-metered Wi-Fi network, issues a lightweight
     *         HTTP {@code GET} request to the server’s <code>/index.php/204</code> endpoint
     *         (which should respond with HTTP 204 No Content when connectivity is healthy).</li>
     *     <li>If the response differs from the expected 204 No Content, the connection is
     *         assumed to be behind a captive portal or otherwise restricted.</li>
     *     <li>If no active network or server is detected, the method assumes the Internet
     *         is walled.</li>
     * </ul>
     *
     * <p>Results are cached for subsequent checks to minimize unnecessary HTTP requests.</p>
     *
     * @return {@code true} if the Internet appears to be walled (e.g. captive portal or
     *         restricted access); {@code false} if the Nextcloud server is reachable and
     *         the network allows normal Internet access.
     */
    boolean isInternetWalled();

    /**
     * Returns a {@link Connectivity} object that represents the current network state.
     *
     * <p>This includes whether the device is connected, whether the network is metered,
     * and whether it uses Wi-Fi or Ethernet transport. It uses
     * {@link #isConnected()} to verify active Internet capability</p>
     *
     * <p>If no active network is found, {@link Connectivity#DISCONNECTED} is returned.</p>
     *
     * @return a {@link Connectivity} instance describing the current network connection.
     */
    Connectivity getConnectivity();

    /**
     * Callback interface for asynchronous results.
     *
     * @param <T> The type of result returned by the callback.
     */
    interface GenericCallback<T> {
        void onComplete(T result);
    }
}
