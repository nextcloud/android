/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network;

import android.os.NetworkOnMainThreadException;

/**
 * This service provides information about current network connectivity
 * and server reachability.
 */
public interface ConnectivityService {
    /**
     * Checks the availability of the server and the device's internet connection.
     * <p>
     * This method performs a network request to verify if the server is accessible and
     * checks if the device has an active internet connection. Due to the network operations involved,
     * this method should be executed on a background thread to avoid blocking the main thread.
     * </p>
     *
     * @return {@code true} if the server is accessible and the device has an internet connection;
     *         {@code false} otherwise.
     *
     * @throws NetworkOnMainThreadException if this function runs on main thread.
     */
    boolean isNetworkAndServerAvailable() throws NetworkOnMainThreadException;

    boolean isConnected();

    /**
     * Check if server is accessible by issuing HTTP status check request.
     * Since this call involves network traffic, it should not be called
     * on a main thread.
     *
     * @return True if server is unreachable, false otherwise
     */
    boolean isInternetWalled();

    /**
     * Get current network connectivity status.
     *
     * @return Network connectivity status in platform-agnostic format
     */
    Connectivity getConnectivity();
}
