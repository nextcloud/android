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
import com.nextcloud.utils.extensions.isConflict
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
import java.io.OutputStream
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

    private var temporalCopy: File? = null

    @Throws(OperationCancelledException::class, IOException::class)
    fun copyToTemporalFileIfNeeded(file: OCFile, originalFile: File, expectedPath: String): RemoteOperationResult<*> {
        if (localBehaviour == FileUploadWorker.LOCAL_BEHAVIOUR_COPY && originalStoragePath != expectedPath) {
            val temporalPath = FileStorageUtils.getInternalTemporalPath(accountName, context) + file.remotePath
            file.storagePath = temporalPath
            Log_OC.d(TAG, "Copying original file to temporal path: $temporalPath")

            val temporalFile = File(temporalPath)
            if (temporalFile.absolutePath != originalStoragePath) {
                temporalCopy = temporalFile
            }

            return copy(originalFile, temporalFile)
        }

        if (cancellationRequested.get()) {
            Log_OC.w(TAG, "Upload cancelled before temporal copy: $originalStoragePath")
            throw OperationCancelledException()
        }

        Log_OC.d(TAG, "Temporal copy not needed, local behaviour: $localBehaviour, path: $originalStoragePath")

        return RemoteOperationResult<Any>(ResultCode.OK)
    }

    fun deleteTemporalCopyUnlessConflict(resultCode: ResultCode) {
        val file = temporalCopy ?: return
        if (resultCode.isConflict()) {
            Log_OC.d(TAG, "Temporal copy kept for conflict resolution: ${file.absolutePath}")
            return
        }

        temporalCopy = null

        try {
            Files.deleteIfExists(file.toPath())
            Log_OC.d(TAG, "Deleted temporal copy: ${file.absolutePath}")
        } catch (e: IOException) {
            Log_OC.w(TAG, "Could not delete temporal copy: ${file.absolutePath}, ${e.message}")
        }
    }

    @Throws(IOException::class)
    fun copy(sourceFile: File, targetFile: File): RemoteOperationResult<*> {
        val targetPath = targetFile.absolutePath
        Log_OC.d(TAG, "Copying $originalStoragePath to $targetPath")

        val usableSpace = FileStorageUtils.usableSpace
        if (usableSpace < sourceFile.length()) {
            Log_OC.e(TAG, "Not enough space to copy ${sourceFile.length()} bytes, usable: $usableSpace bytes")
            return RemoteOperationResult<Any>(ResultCode.LOCAL_STORAGE_FULL)
        }

        if (!createTargetFile(targetFile)) {
            Log_OC.e(TAG, "Cannot create target file: $targetPath")
            return RemoteOperationResult<Any>(ResultCode.CANNOT_CREATE_FILE)
        }

        try {
            if (originalStoragePath == targetPath) {
                Log_OC.d(TAG, "Target is the original file, content copy skipped: $targetPath")
            } else {
                copyContents(sourceFile, targetFile)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Could not copy $originalStoragePath to $targetPath", e)
            return RemoteOperationResult<Any>(ResultCode.LOCAL_STORAGE_NOT_COPIED)
        }

        if (cancellationRequested.get()) {
            Log_OC.w(TAG, "Copy cancelled: $originalStoragePath to $targetPath")
            return RemoteOperationResult<Any>(OperationCancelledException())
        }

        Log_OC.d(TAG, "Copied $originalStoragePath to $targetPath, size: ${targetFile.length()} bytes")

        return RemoteOperationResult<Any>(ResultCode.OK)
    }

    @Throws(IOException::class)
    fun move(sourceFile: File, targetFile: File) {
        if (targetFile == sourceFile) {
            Log_OC.d(TAG, "Source and target are the same, nothing to move: ${sourceFile.absolutePath}")
            return
        }

        val targetFolder = targetFile.parentFile
            ?: throw IOException("Target file has no parent folder: ${targetFile.absolutePath}")
        Files.createDirectories(targetFolder.toPath())

        if (sourceFile.renameTo(targetFile)) {
            Log_OC.d(TAG, "Moved by rename: ${sourceFile.absolutePath} to ${targetFile.absolutePath}")
            return
        }

        Log_OC.d(TAG, "Rename failed, moving by copy: ${sourceFile.absolutePath} to ${targetFile.absolutePath}")
        copyAndDeleteSource(sourceFile.toPath(), targetFile.toPath())

        Log_OC.d(TAG, "Moved by copy: ${sourceFile.absolutePath} to ${targetFile.absolutePath}")
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
            FileOutputStream(targetFile).use { output -> copyUntilCancelled(input, output) }
        }
    }

    private fun copyUntilCancelled(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(COPY_BUFFER_SIZE_BYTES)
        while (!cancellationRequested.get()) {
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) {
                return
            }

            output.write(buffer, 0, bytesRead)
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
            Log_OC.e(TAG, "Could not copy $source to $target, partial target removed", e)
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
