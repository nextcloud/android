package com.owncloud.android.ui.unifiedsearch

import com.owncloud.android.lib.common.SearchResultEntry

typealias ProviderID = String

data class UnifiedSearchSection(
    val providerID: ProviderID,
    val name: String,
    val entries: List<SearchResultEntry>,
    val hasMoreResults: Boolean
)
