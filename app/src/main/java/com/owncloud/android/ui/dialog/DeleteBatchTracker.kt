/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog

class DeleteBatchTracker(private val onAllDeletesFinished: () -> Unit) {
    private var expectedDeletes: Int = -1
    private var completedDeletes: Int = 0

    fun startBatchDelete(fileCount: Int) {
        expectedDeletes = fileCount
        completedDeletes = 0
    }

    fun onSingleDeleteFinished() {
        if (expectedDeletes < 0) return // batch not active

        completedDeletes++

        if (completedDeletes == expectedDeletes) {
            // Reset so it can handle the next batch
            expectedDeletes = -1
            completedDeletes = 0

            onAllDeletesFinished()
        }
    }
}
