/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.file

import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.sort.AlphanumericComparator

sealed interface PlaybackFilesComparator : Comparator<PlaybackFile> {

    object NONE : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = 0
    }

    object FAVORITE : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = AlphanumericComparator.compare(a.name, b.name)
    }

    object ALBUM : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = compareValuesBy(b, a) { it.lastModified }
    }

    object GALLERY : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = compareValuesBy(b, a) { it.lastModified }
    }

    object SHARED : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = compareValuesBy(b, a) { it.lastModified }
    }

    data class Folder(val sortType: FileSortOrder.SortType, val isAscending: Boolean) : PlaybackFilesComparator {
        private val sortTypeComparator: Comparator<PlaybackFile> = when (sortType) {
            FileSortOrder.SortType.ALPHABET -> Comparator { a, b -> AlphanumericComparator.compare(a.name, b.name) }
            FileSortOrder.SortType.SIZE -> compareBy { it.contentLength }
            FileSortOrder.SortType.DATE -> compareBy { it.lastModified }
        }

        private val delegate = compareByDescending(PlaybackFile::isFavorite)
            .thenComparing(if (isAscending) sortTypeComparator else sortTypeComparator.reversed())

        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = delegate.compare(a, b)
    }
}

fun FileSortOrder.toPlaybackFilesComparator(): PlaybackFilesComparator =
    PlaybackFilesComparator.Folder(getType(), isAscending)
