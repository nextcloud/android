/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.datasource

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.nextcloud.client.player.model.file.getRemoteFileId
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient
import java.io.IOException

@UnstableApi
class DefaultDataSource(
    private val delegate: DataSource,
    private val fileDataStorageManager: FileDataStorageManager,
    private val ownCloudClient: OwnCloudClient,
    private val streamOperationFactory: StreamMediaFileOperationFactory = DefaultStreamMediaFileOperationFactory()
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
        val streamMediaFileOperation = streamOperationFactory.create(fileId)
        val result = streamMediaFileOperation.execute(ownCloudClient)
        return if (result.isSuccess) {
            val uri = Uri.parse(result.data[0] as String)
            delegate.open(dataSpec.buildUpon(uri))
        } else {
            throw IOException("Failed to retrieve streaming uri", result.exception)
        }
    }

    private fun DataSpec.buildUpon(uri: Uri) = buildUpon().setUri(uri).build()
}

interface StreamMediaFileOperationFactory {
    fun create(fileId: Long): StreamMediaFileOperation
}

class DefaultStreamMediaFileOperationFactory : StreamMediaFileOperationFactory {
    override fun create(fileId: Long) = StreamMediaFileOperation(fileId)
}
