/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.showToast
import com.nextcloud.utils.extensions.toNextcloudClient
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileExportUtils
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher

@Suppress("LongParameterList", "ReturnCount")
class DownloadFileOperation(
    val user: User,
    val file: OCFile,
    val behaviour: String?,
    val activityName: String?,
    val packageName: String?,
    context: Context?,
    var downloadType: DownloadType?
) : RemoteOperation<Unit>() {

    var etag: String? = ""
        private set

    private val context = WeakReference(context)
    private val dataTransferListeners = ConcurrentHashMap.newKeySet<OnDatatransferProgressListener>()
    private var timestampForModification: Long = 0
    private val cancellationRequested = AtomicBoolean(false)
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    constructor(user: User, file: OCFile, context: Context?) : this(
        user,
        file,
        null,
        null,
        null,
        context,
        DownloadType.DOWNLOAD
    )

    fun isMatching(accountName: String?, fileId: Long) = file.fileId == fileId && user.accountName == accountName

    fun cancelMatchingOperation(accountName: String?, fileId: Long) {
        if (isMatching(accountName, fileId)) cancel()
    }

    val savePath: String
        get() {
            file.storagePath?.let { storagePath ->
                val parentFile = File(storagePath).parentFile
                parentFile?.takeIf { !it.exists() }?.runCatching {
                    Files.createDirectories(toPath())
                }?.onFailure {
                    return FileStorageUtils.getDefaultSavePathFor(user.accountName, file)
                }
                val path = File(storagePath)
                if (path.canWrite() || parentFile?.canWrite() == true) return path.absolutePath
            }
            return FileStorageUtils.getDefaultSavePathFor(user.accountName, file)
        }

    val tmpPath: String get() = FileStorageUtils.getTemporalPath(user.accountName) + file.remotePath
    val tmpFolder: String get() = FileStorageUtils.getTemporalPath(user.accountName)
    val remotePath: String get() = file.remotePath

    val mimeType: String
        get() {
            var mimeType = file.mimeType
            if (mimeType.isNullOrEmpty()) {
                runCatching {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        file.remotePath.substring(file.remotePath.lastIndexOf('.') + 1)
                    )
                }.onFailure {
                    Log_OC.e(TAG, "Trying to find out MIME type of a file without extension: ${file.remotePath}")
                }
            }
            return mimeType ?: "application/octet-stream"
        }

    val size: Long get() = file.fileLength

    fun getModificationTimestamp() =
        if (timestampForModification > 0) timestampForModification else file.modificationTimestamp

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun run(client: OwnCloudClient): RemoteOperationResult<Unit> {
        synchronized(cancellationRequested) {
            if (cancellationRequested.get()) return RemoteOperationResult(OperationCancelledException())
        }

        if (!FileStorageUtils.isValidExtFilename(file.fileName)) {
            mainThreadHandler.post { context.get()?.showToast(R.string.download_download_invalid_local_file_name) }
            return RemoteOperationResult(RemoteOperationResult.ResultCode.INVALID_CHARACTER_IN_NAME)
        }

        val operationContext = context.get()
            ?: return RemoteOperationResult(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)

        val tmpFile = File(tmpPath)
        val (downloadOp, downloadResult) = executeDownload(client, operationContext)

        if (!downloadResult.isSuccess) return downloadResult

        timestampForModification = downloadOp.modificationTimestamp
        etag = downloadOp.etag

        if (file.isEncrypted) {
            handleDecryption(client, operationContext, tmpFile)?.let { return it }
        }

        val result = when (downloadType) {
            DownloadType.DOWNLOAD if !file.isEncrypted -> handleFileMove(tmpFile, downloadResult)
            DownloadType.EXPORT -> handleExport(operationContext, tmpFile, downloadResult)
            else -> downloadResult
        }

        Log_OC.i(TAG, "Download of ${file.remotePath} to $savePath: ${result.logMessage}")
        return result
    }

    private data class DownloadResult(
        val operation: DownloadFileRemoteOperation,
        val result: RemoteOperationResult<Unit>
    )

    @Suppress("UNCHECKED_CAST")
    private fun executeDownload(client: OwnCloudClient, operationContext: Context): DownloadResult {
        val operation = DownloadFileRemoteOperation(file.remotePath, tmpFolder, file.fileLength).also { op ->
            if (downloadType == DownloadType.DOWNLOAD) {
                dataTransferListeners.forEach { op.addProgressListener(it) }
            }
        }
        val result = operation.execute(client.toNextcloudClient(operationContext)) as RemoteOperationResult<Unit>
        return DownloadResult(operation, result)
    }

    private data class EncryptionKeys(val key: String?, val nonce: String?, val authTag: String?)

    private fun extractEncryptionKeys(metadata: Any): EncryptionKeys? = when (metadata) {
        is DecryptedFolderMetadataFile -> metadata.metadata.files[file.encryptedFileName]?.let {
            EncryptionKeys(it.key, it.nonce, it.authenticationTag)
        }

        is DecryptedFolderMetadataFileV1 -> metadata.files[file.encryptedFileName]?.let {
            EncryptionKeys(it.encrypted.key, it.initializationVector, it.authenticationTag)
        }

        else -> null
    }

    @Suppress("DEPRECATION")
    private fun handleDecryption(
        client: OwnCloudClient,
        operationContext: Context,
        tmpFile: File
    ): RemoteOperationResult<Unit>? {
        val fileDataStorageManager = FileDataStorageManager(user, operationContext.contentResolver)
        val parent = fileDataStorageManager.getFileByEncryptedRemotePath(file.parentRemotePath)
        val metadata = EncryptionUtils.downloadFolderMetadata(parent, client, operationContext, user)
            ?: return RemoteOperationResult(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)

        val (keyString, nonceString, authenticationTagString) = extractEncryptionKeys(metadata)
            ?: return RemoteOperationResult(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)

        val key = EncryptionUtils.decodeStringToBase64Bytes(keyString)
        val iv = EncryptionUtils.decodeStringToBase64Bytes(nonceString)

        return runCatching {
            val cipher = EncryptionUtils.getCipher(Cipher.DECRYPT_MODE, key, iv)
            EncryptionUtils.decryptFile(
                cipher,
                tmpFile,
                File(savePath),
                authenticationTagString,
                ArbitraryDataProviderImpl(operationContext),
                user
            )
        }.fold(
            onSuccess = { null },
            onFailure = { RemoteOperationResult(it as? Exception ?: Exception(it)) }
        )
    }

    private fun handleFileMove(tmpFile: File, currentResult: RemoteOperationResult<Unit>): RemoteOperationResult<Unit> {
        val newFile = File(savePath)
        newFile.parentFile?.takeIf { !it.exists() }?.also {
            if (!it.mkdirs()) Log_OC.e(TAG, "Unable to create parent folder ${it.absolutePath}")
        }

        val tempFilePath = tmpFile.toPath()
        val newFilePath = newFile.toPath()

        Log_OC.d(TAG, "trying to move: $tempFilePath to $newFilePath")

        return try {
            Files.move(tempFilePath, newFilePath, StandardCopyOption.REPLACE_EXISTING)
            newFile.setLastModified(file.modificationTimestamp)
                .also { Log_OC.d(TAG, "Last modified set: $it") }
            currentResult
        } catch (e: IOException) {
            Log_OC.e(TAG, "Failed to move file to ${newFile.absolutePath}: ${e.message}")
            RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_MOVED)
        }
    }

    private fun handleExport(
        operationContext: Context,
        tmpFile: File,
        currentResult: RemoteOperationResult<Unit>
    ): RemoteOperationResult<Unit> {
        FileExportUtils().exportFile(file.fileName, file.mimeType, operationContext.contentResolver, null, tmpFile)
        if (!tmpFile.delete()) Log_OC.e(TAG, "Deletion of ${tmpFile.absolutePath} failed!")
        return currentResult
    }

    fun cancel() {
        cancellationRequested.set(true)
    }

    fun addProgressListener(listener: OnDatatransferProgressListener) {
        dataTransferListeners.add(listener)
    }

    fun removeProgressListener(listener: OnDatatransferProgressListener) {
        dataTransferListeners.remove(listener)
    }

    companion object {
        private val TAG = DownloadFileOperation::class.java.simpleName
    }
}
