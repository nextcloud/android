/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.file

import com.owncloud.android.utils.FileSortOrder
import third_parties.daveKoeller.AlphanumComparator

sealed interface PlaybackFilesComparator : Comparator<PlaybackFile> {

    object NONE : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = 0
    }

    object FAVORITE : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = AlphanumComparator.compare(a.name, b.name)
    }

    object GALLERY : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = compareValuesBy(b, a) { it.lastModified }
    }

    object SHARED : PlaybackFilesComparator {
        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = compareValuesBy(b, a) { it.lastModified }
    }

    data class Folder(val sortType: FileSortOrder.SortType, val isAscending: Boolean) : PlaybackFilesComparator {
        private val delegate = createDelegate()

        override fun compare(a: PlaybackFile, b: PlaybackFile): Int = delegate.compare(a, b)

        private fun createDelegate(): Comparator<PlaybackFile> {
            val sortTypeComparator: Comparator<PlaybackFile> = when (sortType) {
                FileSortOrder.SortType.ALPHABET -> Comparator { a, b -> AlphanumComparator.compare(a.name, b.name) }
                FileSortOrder.SortType.SIZE -> compareBy { it.contentLength }
                FileSortOrder.SortType.DATE -> compareBy { it.lastModified }
            }
            return compareByDescending(PlaybackFile::isFavorite)
                .thenComparing(if (isAscending) sortTypeComparator else sortTypeComparator.reversed())
        }
    }
}

fun FileSortOrder.toPlaybackFilesComparator(): PlaybackFilesComparator =
    PlaybackFilesComparator.Folder(getType(), isAscending)
