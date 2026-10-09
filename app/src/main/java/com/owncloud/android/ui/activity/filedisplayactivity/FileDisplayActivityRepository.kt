/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity.filedisplayactivity

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult

interface FileDisplayActivityRepository {
    suspend fun fetchRecommendedFiles(ignoreETag: Boolean, folder: OCFile?): ArrayList<OCFile>?

    suspend fun syncFolder(folder: OCFile, ignoreETag: Boolean): RemoteOperationResult<*>

    fun downloadFileIfNotStartedBefore(file: OCFile)

    fun downloadFile(file: OCFile, downloadBehaviour: String, packageName: String, activityName: String)

    fun uploadFiles(localPaths: Array<String>, remotePaths: Array<String>, localBehaviour: Int)

    fun startMetadataSync(remotePath: String, folderAlreadySynced: Boolean)
}
