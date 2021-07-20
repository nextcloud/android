/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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

package com.nextcloud.client.network;

/**
 * This service provides information about current network connectivity
 * and server reachability.
 */
public interface ConnectivityService {

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
