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
package com.nextcloud.client.network

data class Connectivity(
    val isConnected: Boolean = false,
    val isMetered: Boolean = false,
    val isWifi: Boolean = false,
    val isServerAvailable: Boolean? = null
) {
    companion object {
        @JvmField
        val DISCONNECTED = Connectivity()

        @JvmField
        val CONNECTED_WIFI = Connectivity(
            isConnected = true,
            isMetered = false,
            isWifi = true,
            isServerAvailable = true
        )
    }
}
