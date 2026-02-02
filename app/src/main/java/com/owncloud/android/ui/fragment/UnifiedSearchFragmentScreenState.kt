/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import com.owncloud.android.R

sealed class UnifiedSearchFragmentScreenState {

    /**
     * Content is being displayed (search results or current directory items)
     */
    object ShowingContent : UnifiedSearchFragmentScreenState()

    /**
     * Empty state with customizable message
     */
    data class Empty(val titleId: Int, val descriptionId: Int, val iconId: Int) : UnifiedSearchFragmentScreenState() {

        companion object {
            fun startSearch() = Empty(
                titleId = R.string.file_list_empty_unified_search_start_search,
                descriptionId = R.string.file_list_empty_unified_search_start_search_description,
                iconId = R.drawable.ic_search_grey
            )

            fun noResults() = Empty(
                titleId = R.string.file_list_empty_headline_server_search,
                descriptionId = R.string.file_list_empty_unified_search_no_results,
                iconId = R.drawable.ic_search_grey
            )
        }
    }
}
