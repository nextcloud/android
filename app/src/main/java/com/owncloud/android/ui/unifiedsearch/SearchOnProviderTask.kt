/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.unifiedsearch

import com.nextcloud.android.lib.resources.search.UnifiedSearchRemoteOperation
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.SearchResult
import com.owncloud.android.lib.common.utils.Log_OC

class SearchOnProviderTask(
    private val query: String,
    private val provider: String,
    private val client: NextcloudClient,
    private val cursor: Int? = null,
    private val limit: Int = 5
) : () -> SearchOnProviderTask.Result {
    companion object {
        private const val TAG = "SearchOnProviderTask"
    }

    data class Result(val success: Boolean = false, val searchResult: SearchResult = SearchResult())

    override fun invoke(): Result {
        Log_OC.d(TAG, "Run task")
        val result = UnifiedSearchRemoteOperation(provider, query, cursor, limit).execute(client)

        Log_OC.d(TAG, "Task finished: " + result.isSuccess)
        return if (result.isSuccess && result.resultData != null) {
            Result(
                success = true,
                searchResult = result.resultData as SearchResult
            )
        } else {
            Result()
        }
    }
}
