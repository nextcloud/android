/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.network

import com.owncloud.android.lib.common.utils.Log_OC
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

private const val TAG = "WalledCheckFailure"

fun Exception.toWalledValue(lastKnownAvailability: Boolean?): Boolean = when (this) {
    is UnknownHostException,
    is ConnectException -> {
        Log_OC.w(TAG, "offline exception (${this::class.simpleName}), treating as walled")
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
        Log_OC.w(TAG, "I/O exception (${this::class.simpleName}), treating as walled")
        true
    }

    else -> {
        Log_OC.e(TAG, "unexpected exception type (${this::class.simpleName}), using previous state")
        lastKnownAvailability?.let { !it } ?: true
    }
}
