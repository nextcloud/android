/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.network;

/**
 * This service provides information about current network connectivity
 * and server reachability.
 */
public interface ConnectivityService {
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
