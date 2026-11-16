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
import androidx.media3.datasource.TransferListener
import com.nextcloud.client.player.util.PlayerUtil.getRemoteFileId
import com.nextcloud.common.NextcloudClient
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient
import kotlinx.coroutines.runBlocking
import java.io.IOException

@UnstableApi
class PlaybackDataSource(
    private val createDelegate: (NextcloudClient) -> DataSource,
    private val fileDataStorageManager: FileDataStorageManager,
    private val clientRepository: ClientRepository
) : DataSource {

    private val transferListeners = mutableListOf<TransferListener>()
    private var delegate: DataSource? = null
    private var ownCloudClient: OwnCloudClient? = null

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners.add(transferListener)
        delegate?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val delegate = getOrCreateDelegate()
        val fileId = dataSpec.uri.getRemoteFileId() ?: return delegate.open(dataSpec)
        val file = fileDataStorageManager.getFileByLocalId(fileId)
        return if (file != null && file.isDown) {
            openStoredFile(delegate, dataSpec, file)
        } else {
            openRemoteFile(delegate, dataSpec, fileId)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(delegate) { "Data source is not opened" }.read(buffer, offset, length)

    override fun getUri(): Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders ?: emptyMap()

    override fun close() {
        delegate?.close()
    }

    private fun getOrCreateDelegate(): DataSource {
        delegate?.let { return it }

        val nextcloudClient = runBlocking { clientRepository.getNextcloudClient() }
            ?: throw IOException("Nextcloud client is not available, cannot play media")

        return createDelegate(nextcloudClient).also { created ->
            transferListeners.forEach(created::addTransferListener)
            delegate = created
        }
    }

    private fun openStoredFile(delegate: DataSource, dataSpec: DataSpec, file: OCFile): Long {
        val uri = file.storageUri
        return delegate.open(dataSpec.buildUpon(uri))
    }

    private fun getOrCreateOwnCloudClient(): OwnCloudClient {
        ownCloudClient?.let { return it }

        val client = runBlocking { clientRepository.getOwncloudClient() }
            ?: throw IOException("OwnCloud client is not available, cannot stream")

        return client.also { ownCloudClient = it }
    }

    private fun openRemoteFile(delegate: DataSource, dataSpec: DataSpec, fileId: Long): Long {
        val result = StreamMediaFileOperation(fileId).execute(getOrCreateOwnCloudClient())
        if (!result.isSuccess) {
            throw IOException("Failed to retrieve streaming uri", result.exception)
        }

        val uri = (result.data[0] as? String)?.toUri()
            ?: throw IllegalStateException("url is not valid, cannot stream")
        return delegate.open(dataSpec.buildUpon(uri))
    }

    private fun DataSpec.buildUpon(uri: Uri) = buildUpon().setUri(uri).build()
}
