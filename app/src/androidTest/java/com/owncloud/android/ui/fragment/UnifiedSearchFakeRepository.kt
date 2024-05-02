/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
