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
