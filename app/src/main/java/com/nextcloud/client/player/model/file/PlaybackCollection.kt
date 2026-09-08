/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.file

import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.ui.fragment.SearchType

enum class PlaybackCollection {
    FOLDER,
    FAVORITES,
    GALLERY,
    SHARED,
    ALBUM
}

fun SearchType?.toPlaybackCollection(): PlaybackCollection = when (this) {
    SearchType.FAVORITE_SEARCH -> PlaybackCollection.FAVORITES
    SearchType.GALLERY_SEARCH -> PlaybackCollection.GALLERY
    SearchType.SHARED_FILTER -> PlaybackCollection.SHARED
    else -> PlaybackCollection.FOLDER
}

fun VirtualFolderType?.toPlaybackCollection(): PlaybackCollection = when (this) {
    VirtualFolderType.FAVORITE -> PlaybackCollection.FAVORITES
    VirtualFolderType.GALLERY -> PlaybackCollection.GALLERY
    VirtualFolderType.ALBUM -> PlaybackCollection.ALBUM
    else -> PlaybackCollection.FOLDER
}
