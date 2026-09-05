/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3.datasource

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.nextcloud.client.player.util.PlayerUtil.getRemoteFileId
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient
import java.io.IOException

@UnstableApi
class PlaybackDataSource(
    private val delegate: DataSource,
    private val fileDataStorageManager: FileDataStorageManager,
    private val ownCloudClient: OwnCloudClient
) : DataSource by delegate {

    override fun getResponseHeaders() = delegate.responseHeaders

    override fun open(dataSpec: DataSpec): Long {
        val fileId = dataSpec.uri.getRemoteFileId() ?: return delegate.open(dataSpec)
        val file = fileDataStorageManager.getFileByLocalId(fileId)
        return if (file != null && file.isDown) {
            openStoredFile(dataSpec, file)
        } else {
            openRemoteFile(dataSpec, fileId)
        }
    }

    private fun openStoredFile(dataSpec: DataSpec, file: OCFile): Long {
        val uri = file.storageUri
        return delegate.open(dataSpec.buildUpon(uri))
    }

    private fun openRemoteFile(dataSpec: DataSpec, fileId: Long): Long {
        val result = StreamMediaFileOperation(fileId).execute(ownCloudClient)
        return if (result.isSuccess) {
            val uri = (result.data[0] as? String)?.toUri()
                ?: throw IllegalStateException("url is not valid, cannot stream")
            delegate.open(dataSpec.buildUpon(uri))
        } else {
            throw IOException("Failed to retrieve streaming uri", result.exception)
        }
    }

    private fun DataSpec.buildUpon(uri: Uri) = buildUpon().setUri(uri).build()
}
