/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.filesRepository

import com.nextcloud.client.database.entity.RecommendedFileEntity
import com.owncloud.android.datamodel.FileDataStorageManager

interface FilesRepository {

    /**
     * Fetches a list of recommended files from the Nextcloud server.
     *
     * This function runs on the IO dispatcher and retrieves recommendations
     * using the Nextcloud client. The results are passed to the provided callback on the main thread.
     *
     * @param onCompleted A callback function that receives the list of recommended files.
     *
     */
    fun fetchRecommendedFiles(
        storageManager: FileDataStorageManager,
        onCompleted: (ArrayList<RecommendedFileEntity>) -> Unit
    )

    fun createRichWorkspace(remotePath: String, onCompleted: (String) -> Unit, onError: () -> Unit)
}
