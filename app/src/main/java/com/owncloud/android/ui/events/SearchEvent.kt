/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.events

import android.os.Bundle
import android.os.Parcelable
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SearchType
import kotlinx.parcelize.Parcelize

/**
 * Represents a search or filter request for [com.owncloud.android.ui.fragment.OCFileListFragment].
 *
 * Used to pass search state via fragment arguments and to keep the UI
 * (navigation drawer, empty states) in sync with search behavior.
 *
 * This class bridges {@link SearchRemoteOperation.SearchType} values
 * with UI-level {@link SearchType} used by the file list and drawer highlighting.
 *
 * @property searchQuery the query string for the search (may be empty)
 * @property searchType the search type defining the search behavior
 */
@Parcelize
data class SearchEvent(val searchQuery: String, val searchType: SearchRemoteOperation.SearchType) : Parcelable {
    fun toSearchType(): SearchType? = when (searchType) {
        SearchRemoteOperation.SearchType.FILE_SEARCH -> SearchType.FILE_SEARCH
        SearchRemoteOperation.SearchType.FAVORITE_SEARCH -> SearchType.FAVORITE_SEARCH
        SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH -> SearchType.RECENTLY_MODIFIED_SEARCH
        SearchRemoteOperation.SearchType.SHARED_FILTER -> SearchType.SHARED_FILTER
        else -> null
    }

    /**
     * Creates a bundle for initializing {@link OCFileListFragment} with this search.
     */
    fun getBundle(): Bundle = Bundle().apply {
        putParcelable(OCFileListFragment.SEARCH_EVENT, this@SearchEvent)
        putParcelable(OCFileListFragment.KEY_CURRENT_SEARCH_TYPE, toSearchType())
    }
}
