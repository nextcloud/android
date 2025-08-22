/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.filesRepository

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

interface FilesRepository {

    /**
     * Fetches a list of recommended files from the Nextcloud server.
     *
     * This function runs on the IO dispatcher and retrieves recommendations
     * using the Nextcloud client. The results are passed to the provided callback on the main thread.
     *
     */
    suspend fun fetchRecommendedFiles(
        ignoreETag: Boolean,
        storageManager: FileDataStorageManager
    ): ArrayList<OCFile>

    fun createRichWorkspace(remotePath: String, onCompleted: (String) -> Unit, onError: () -> Unit)
}
