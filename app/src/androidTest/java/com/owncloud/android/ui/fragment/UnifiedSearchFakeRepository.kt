/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment

import com.owncloud.android.lib.common.SearchResult
import com.owncloud.android.lib.common.SearchResultEntry
import com.owncloud.android.ui.unifiedsearch.IUnifiedSearchRepository
import com.owncloud.android.ui.unifiedsearch.ProviderID
import com.owncloud.android.ui.unifiedsearch.UnifiedSearchResult

class UnifiedSearchFakeRepository : IUnifiedSearchRepository {

    override fun queryAll(
        query: String,
        onResult: (UnifiedSearchResult) -> Unit,
        onError: (Throwable) -> Unit,
        onFinished: (Boolean) -> Unit
    ) {
        val result = UnifiedSearchResult(
            provider = "files",
            success = true,
            result = SearchResult(
                "files",
                false,
                listOf(
                    SearchResultEntry(
                        "thumbnailUrl",
                        "Test",
                        "in Files",
                        "http://localhost/nc/index.php/apps/files/?dir=/Files&scrollto=Test",
                        "icon",
                        false
                    ),
                    SearchResultEntry(
                        "thumbnailUrl",
                        "Test1",
                        "in Folder",
                        "http://localhost/nc/index.php/apps/files/?dir=/folder&scrollto=test1.txt",
                        "icon",
                        false
                    )
                )
            )

        )
        onResult(result)
        onFinished(true)
    }

    override fun queryProvider(
        query: String,
        provider: ProviderID,
        cursor: Int?,
        onResult: (UnifiedSearchResult) -> Unit,
        onError: (Throwable) -> Unit,
        onFinished: (Boolean) -> Unit
    ) {
        val result = UnifiedSearchResult(
            provider = provider,
            success = true,
            result = SearchResult(
                provider,
                false,
                listOf(
                    SearchResultEntry(
                        "thumbnailUrl",
                        "Test",
                        "in Files",
                        "http://localhost/nc/index.php/apps/files/?dir=/Files&scrollto=Test",
                        "icon",
                        false
                    ),
                    SearchResultEntry(
                        "thumbnailUrl",
                        "Test1",
                        "in Folder",
                        "http://localhost/nc/index.php/apps/files/?dir=/folder&scrollto=test1.txt",
                        "icon",
                        false
                    )
                )
            )

        )
    }
}
