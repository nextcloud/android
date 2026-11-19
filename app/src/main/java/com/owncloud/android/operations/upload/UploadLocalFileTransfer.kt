/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.content.Context
import androidx.core.net.toUri
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.UriUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean

class UploadLocalFileTransfer(
    private val context: Context,
    private val accountName: String,
    private val originalStoragePath: String,
    private val localBehaviour: Int,
    private val cancellationRequested: AtomicBoolean
) {
    companion object {
        private const val TAG = "UploadLocalFileTransfer"
        private const val COPY_BUFFER_SIZE_BYTES = 64 * 1024
    }

    @Throws(OperationCancelledException::class, IOException::class)
    fun copyToTemporalFileIfNeeded(file: OCFile, originalFile: File, expectedPath: String): RemoteOperationResult<*> {
        if (localBehaviour == FileUploadWorker.LOCAL_BEHAVIOUR_COPY && originalStoragePath != expectedPath) {
            val temporalPath = FileStorageUtils.getInternalTemporalPath(accountName, context) + file.remotePath
            file.storagePath = temporalPath

            return copy(originalFile, File(temporalPath))
        }

        if (cancellationRequested.get()) {
            throw OperationCancelledException()
        }

        return RemoteOperationResult<Any>(ResultCode.OK)
    }

    @Throws(IOException::class)
    fun copy(sourceFile: File, targetFile: File): RemoteOperationResult<*> {
        Log_OC.d(TAG, "Copying local file")

        if (FileStorageUtils.usableSpace < sourceFile.length()) {
            return RemoteOperationResult<Any>(ResultCode.LOCAL_STORAGE_FULL)
        }

        if (!createTargetFile(targetFile)) {
            return RemoteOperationResult<Any>(ResultCode.CANNOT_CREATE_FILE)
        }

        try {
            if (originalStoragePath != targetFile.absolutePath) {
                copyContents(sourceFile, targetFile)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Could not copy $originalStoragePath to ${targetFile.absolutePath}", e)
            return RemoteOperationResult<Any>(ResultCode.LOCAL_STORAGE_NOT_COPIED)
        }

        if (cancellationRequested.get()) {
            return RemoteOperationResult<Any>(OperationCancelledException())
        }

        return RemoteOperationResult<Any>(ResultCode.OK)
    }

    @Throws(IOException::class)
    fun move(sourceFile: File, targetFile: File) {
        if (targetFile == sourceFile) {
            return
        }

        val targetFolder = targetFile.parentFile
            ?: throw IOException("Target file has no parent folder: ${targetFile.absolutePath}")
        Files.createDirectories(targetFolder.toPath())

        if (sourceFile.renameTo(targetFile)) {
            return
        }

        copyAndDeleteSource(sourceFile.toPath(), targetFile.toPath())
    }

    @Throws(IOException::class)
    private fun createTargetFile(targetFile: File): Boolean {
        val parent = targetFile.parentFile ?: return false
        if (!parent.mkdirs() && !parent.isDirectory) {
            return false
        }

        return targetFile.createNewFile() || targetFile.isFile
    }

    private fun copyContents(sourceFile: File, targetFile: File) {
        openSourceStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(COPY_BUFFER_SIZE_BYTES)
                while (!cancellationRequested.get()) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead < 0) {
                        break
                    }

                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    private fun openSourceStream(sourceFile: File): InputStream {
        if (!originalStoragePath.startsWith(UriUtils.URI_CONTENT_SCHEME)) {
            return FileInputStream(sourceFile)
        }

        return context.contentResolver.openInputStream(originalStoragePath.toUri())
            ?: throw FileNotFoundException("Cannot open input stream for $originalStoragePath")
    }

    @Throws(IOException::class)
    private fun copyAndDeleteSource(source: Path, target: Path) {
        Files.createFile(target)
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            Files.deleteIfExists(target)
            throw e
        }

        try {
            Files.delete(source)
        } catch (e: IOException) {
            Log_OC.w(TAG, "Moved file but could not delete source: $source, ${e.message}")
        }
    }
}
