/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Unpublished <unpublished@gmx.net>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class SearchType : Parcelable {
    NO_SEARCH,
    LOCAL_SEARCH,
    REGULAR_FILTER,
    FILE_SEARCH,
    FAVORITE_SEARCH,
    GALLERY_SEARCH,
    RECENTLY_MODIFIED_SEARCH,

    // not a real filter, but nevertheless
    SHARED_FILTER,
    GROUPFOLDER
}

@Parcelize
enum class EmptyListState : Parcelable {
    OFFLINE_MODE,
    LOADING,
    ADD_FOLDER,
    ONLY_ON_DEVICE,
    LOCAL_FILE_LIST_EMPTY_FILE,
    LOCAL_FILE_LIST_EMPTY_FOLDER,
    ERROR
}
