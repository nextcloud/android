/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import androidx.annotation.WorkerThread
import com.nextcloud.client.core.Clock
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class E2EEServerKeyCache @Inject constructor(private val clock: Clock) {
    private val entries = mutableMapOf<Pair<String, String>, Pair<Long, RemoteOperationResult<String>>>()

    @WorkerThread
    @Synchronized
    fun get(
        serverUrl: String,
        certificate: String,
        fetch: () -> RemoteOperationResult<String>
    ): RemoteOperationResult<String> {
        val now = clock.millisSinceBoot
        entries.entries.removeAll { now - it.value.first !in 0 until LIFETIME_MILLIS }
        val key = serverUrl to certificate
        entries[key]?.let { return it.second }

        return fetch().also { result ->
            if (result.isSuccess && !result.resultData.isNullOrEmpty()) {
                entries[key] = clock.millisSinceBoot to result
            }
        }
    }

    companion object {
        private val LIFETIME_MILLIS = TimeUnit.MINUTES.toMillis(10)
    }
}
