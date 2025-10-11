/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.unifiedsearch

import android.net.Uri
import androidx.lifecycle.LiveData
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.SearchResultEntry

interface IUnifiedSearchViewModel {
    val browserUri: LiveData<Uri>
    val error: LiveData<String>
    val file: LiveData<OCFile>
    val isLoading: LiveData<Boolean>
    val query: LiveData<String>
    val searchResults: LiveData<List<UnifiedSearchSection>>

    fun initialQuery()
    fun loadMore(provider: ProviderID)
    fun openResult(result: SearchResultEntry)
    fun setQuery(query: String)
    fun openFile(remotePath: String)
    fun getRemoteFile(remotePath: String)
}
