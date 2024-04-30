/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.unifiedsearch

import com.owncloud.android.lib.common.SearchResult

data class UnifiedSearchResult(val provider: ProviderID, val success: Boolean, val result: SearchResult)

@Suppress("LongParameterList")
interface IUnifiedSearchRepository {
    fun queryAll(
        query: String,
        onResult: (UnifiedSearchResult) -> Unit,
        onError: (Throwable) -> Unit,
        onFinished: (Boolean) -> Unit
    )

    fun queryProvider(
        query: String,
        provider: ProviderID,
        cursor: Int?,
        onResult: (UnifiedSearchResult) -> Unit,
        onError: (Throwable) -> Unit,
        onFinished: (Boolean) -> Unit
    )
}
