package com.owncloud.android.ui.fragment

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class SearchType : Parcelable {
    NO_SEARCH,
    REGULAR_FILTER,
    FILE_SEARCH,
    FAVORITE_SEARCH,
    GALLERY_SEARCH,
    RECENTLY_MODIFIED_SEARCH,

    // not a real filter, but nevertheless
    SHARED_FILTER
}
