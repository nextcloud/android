/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.unifiedsearch

import com.owncloud.android.lib.common.SearchResultEntry

typealias ProviderID = String

data class UnifiedSearchSection(
    val providerID: ProviderID,
    val name: String,
    val entries: List<SearchResultEntry>,
    val hasMoreResults: Boolean
)

fun List<UnifiedSearchSection>.filterOutHiddenFiles(listOfHiddenFiles: List<String>): List<UnifiedSearchSection> {
    return map { searchSection ->
        val entriesWithoutHiddenFiles = searchSection.entries.filterNot { entry ->
            listOfHiddenFiles.contains(entry.title)
        }

        searchSection.copy(entries = entriesWithoutHiddenFiles)
    }.filter { it.entries.isNotEmpty() }
}
