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
package com.owncloud.android.ui.unifiedsearch

import com.nextcloud.android.lib.resources.search.UnifiedSearchRemoteOperation
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.SearchResult
import com.owncloud.android.lib.common.utils.Log_OC

class SearchOnProviderTask(
    private val query: String,
    private val provider: String,
    private val client: NextcloudClient
) : () -> SearchOnProviderTask.Result {
    companion object {
        private const val TAG = "SearchOnProviderTask"
    }

    data class Result(val success: Boolean = false, val searchResult: SearchResult = SearchResult())

    override fun invoke(): Result {
        Log_OC.d(TAG, "Run task")
        val result = UnifiedSearchRemoteOperation(provider, query).execute(client)

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
