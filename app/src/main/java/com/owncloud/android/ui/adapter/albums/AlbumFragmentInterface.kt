/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.albums

import com.owncloud.android.lib.resources.albums.PhotoAlbumEntry

interface AlbumFragmentInterface {
    fun onItemClick(album: PhotoAlbumEntry)
}
