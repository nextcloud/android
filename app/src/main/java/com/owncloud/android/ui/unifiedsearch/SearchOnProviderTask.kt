/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.unifiedsearch

import com.nextcloud.android.lib.resources.search.UnifiedSearchRemoteOperation
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.extensions.containsAllTerms
import com.nextcloud.utils.extensions.toSearchTerms
import com.owncloud.android.lib.common.SearchResult
import com.owncloud.android.lib.common.SearchResultEntry
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
        private const val FILES_PROVIDER_ID = "files"
        private const val MAX_SERVER_PAGE_SIZE = 25
    }

    data class Result(val success: Boolean = false, val searchResult: SearchResult = SearchResult())

    override fun invoke(): Result {
        Log_OC.d(TAG, "Run task")
        val terms = query.toSearchTerms()
        val result = if (provider == FILES_PROVIDER_ID && terms.size > 1) {
            searchFilesMatchingAllTerms(terms)
        } else {
            search(query, cursor, limit)
        }

        Log_OC.d(TAG, "Task finished: " + result.success)
        return result
    }

    private fun search(query: String, cursor: Int?, limit: Int): Result {
        val result = UnifiedSearchRemoteOperation(provider, query, cursor, limit).execute(client)
        val searchResult = result.resultData
        return if (result.isSuccess && searchResult != null) {
            Result(success = true, searchResult = searchResult)
        } else {
            Result()
        }
    }

    private fun searchFilesMatchingAllTerms(terms: List<String>): Result {
        val serverQuery = terms.maxBy { it.length }
        val matches = mutableListOf<SearchResultEntry>()
        var nextCursor = cursor
        var page: SearchResult

        do {
            val result = search(serverQuery, nextCursor, MAX_SERVER_PAGE_SIZE)
            if (!result.success) {
                return result
            }

            page = result.searchResult
            matches += page.entries.filter { it.title.containsAllTerms(terms) }
            nextCursor = page.cursor?.toIntOrNull()
        } while (matches.size < limit && page.entries.size == MAX_SERVER_PAGE_SIZE && nextCursor != null)

        return Result(success = true, searchResult = page.copy(entries = matches))
    }
}
