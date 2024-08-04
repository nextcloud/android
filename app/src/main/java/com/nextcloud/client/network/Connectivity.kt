/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
