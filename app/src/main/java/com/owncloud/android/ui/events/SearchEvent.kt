/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.events

import android.os.Parcelable
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.ui.fragment.SearchType
import kotlinx.parcelize.Parcelize

/**
 * Search event
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
}
