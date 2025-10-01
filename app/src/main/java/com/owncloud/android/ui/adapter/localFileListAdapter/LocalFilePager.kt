/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.localFileListAdapter

import java.io.File

class LocalFilePager(
    private val allFiles: List<File>
) {
    private var currentIndex = 0
    companion object {
        private const val PAGE_SIZE = 50
    }

    fun loadNextPage(): List<File> {
        if (currentIndex >= allFiles.size) return emptyList()

        val from = currentIndex
        val to = (currentIndex + PAGE_SIZE).coerceAtMost(allFiles.size)
        currentIndex = to
        return allFiles.subList(from, to)
    }

    fun reset() {
        currentIndex = 0
    }

    fun hasMore(): Boolean = currentIndex < allFiles.size
}
