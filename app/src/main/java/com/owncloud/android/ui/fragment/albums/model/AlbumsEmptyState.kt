/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.owncloud.android.R

enum class AlbumsEmptyState(@StringRes val headline: Int, @StringRes val message: Int, @DrawableRes val icon: Int) {
    NO_ALBUMS(R.string.empty_albums_title, R.string.empty_albums_message, R.drawable.ic_album),
    LOAD_FAILED(R.string.file_list_error_headline, R.string.file_list_error_description, R.drawable.ic_no_internet)
}