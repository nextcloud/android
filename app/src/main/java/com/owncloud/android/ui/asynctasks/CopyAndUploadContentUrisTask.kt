/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.asynctasks

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.model.OCUploadLocalPathData
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.ref.WeakReference

class CopyAndUploadContentUrisTask(
    listener: OnCopyTmpFilesTaskListener?,
    context: Context,
    private val scope: CoroutineScope,
    private val albumName: String?
) {
    companion object {
        private const val TAG = "CopyAndUploadContentUrisTask"
    }

    private var listenerRef = WeakReference(listener)
    private val appContext = context.applicationContext

    fun execute(
        user: User,
        sourceUris: Array<Uri>,
        remotePaths: Array<String>,
        behaviour: Int,
        contentResolver: ContentResolver
    ) {
        val inputStreams: List<InputStream?> = try {
            sourceUris.map { contentResolver.openInputStream(it) }
        } catch (e: FileNotFoundException) {
            Log_OC.e(TAG, "Source file not found", e)
            dispatchResult(ResultCode.LOCAL_FILE_NOT_FOUND)
            return
        } catch (e: SecurityException) {
            Log_OC.e(TAG, "Insufficient permissions to open source URIs", e)
            dispatchResult(ResultCode.FORBIDDEN)
            return
        }

        scope.launch(Dispatchers.IO) {
            val result = performCopy(user, sourceUris, remotePaths, behaviour, contentResolver, inputStreams)
            withContext(Dispatchers.Main) {
                dispatchResult(result)
            }
        }
    }

    private fun performCopy(
        user: User,
        sourceUris: Array<Uri>,
        remotePaths: Array<String>,
        behaviour: Int,
        contentResolver: ContentResolver,
        inputStreams: List<InputStream?>
    ): ResultCode {
        val localPaths = arrayOfNulls<String>(sourceUris.size)
        val resolvedRemotePaths = arrayOfNulls<String>(sourceUris.size)
        var currentTempPath: String? = null

        return try {
            sourceUris.forEachIndexed { index, uri ->
                val remotePath = remotePaths[index]
                val lastModified = queryLastModified(contentResolver, uri)

                currentTempPath = "${FileStorageUtils.getTemporalPath(user.accountName)}$remotePath"
                val cacheFile = File(currentTempPath)

                cacheFile.parentFile?.takeIf { !it.exists() }?.let {
                    Log_OC.d(TAG, "Temp dir creation result: ${it.mkdirs()}")
                }
                Log_OC.d(TAG, "Cache file creation result: ${cacheFile.createNewFile()}")

                inputStreams[index]?.use { input ->
                    FileOutputStream(currentTempPath).use { output ->
                        input.copyTo(output)
                    }
                }

                applyLastModified(cacheFile, lastModified)

                localPaths[index] = currentTempPath
                resolvedRemotePaths[index] = remotePath
                currentTempPath = null
            }

            if (albumName.isNullOrEmpty()) {
                val data = OCUploadLocalPathData.forFile(
                    user,
                    localPaths.requireNoNulls(),
                    resolvedRemotePaths.requireNoNulls(),
                    behaviour
                )
                FileUploadHelper.instance().uploadNewFiles(data)
            } else {
                val data = OCUploadLocalPathData.forAlbum(
                    user,
                    localPaths.requireNoNulls(),
                    resolvedRemotePaths.requireNoNulls(),
                    behaviour
                )
                FileUploadHelper.instance().uploadAndCopyNewFilesForAlbum(data, albumName)
            }

            ResultCode.OK
        } catch (e: FileNotFoundException) {
            Log_OC.e(TAG, "Source file not found during copy", e)
            ResultCode.LOCAL_FILE_NOT_FOUND
        } catch (e: SecurityException) {
            Log_OC.e(TAG, "Insufficient permissions during copy", e)
            ResultCode.FORBIDDEN
        } catch (e: Exception) {
            Log_OC.e(TAG, "Unexpected error during file copy", e)
            currentTempPath?.let { path ->
                val partial = File(path)
                if (partial.exists() && !partial.delete()) {
                    Log_OC.e(TAG, "Failed to delete partial temp file: $path")
                }
            }
            ResultCode.LOCAL_STORAGE_NOT_COPIED
        }
    }

    private fun queryLastModified(contentResolver: ContentResolver, uri: Uri): Long = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use 0L
            val col = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            if (col >= 0) cursor.getLong(col) else 0L
        } ?: 0L
    }.getOrDefault(0L)

    private fun applyLastModified(file: File, lastModified: Long) {
        if (lastModified == 0L) return
        runCatching { file.setLastModified(lastModified) }
            .onFailure { Log_OC.w(TAG, "Could not set lastModified on cache file: ${it.message}") }
    }

    private fun dispatchResult(result: ResultCode) {
        listenerRef.get()?.onTmpFilesCopied(result) ?: run {
            if (result == ResultCode.OK) return
            val messageResId = when (result) {
                ResultCode.LOCAL_FILE_NOT_FOUND -> R.string.uploader_error_message_source_file_not_found
                ResultCode.LOCAL_STORAGE_NOT_COPIED -> R.string.uploader_error_message_source_file_not_copied
                ResultCode.FORBIDDEN -> R.string.uploader_error_message_read_permission_not_granted
                else -> R.string.common_error_unknown
            }
            Toast.makeText(
                appContext,
                appContext.getString(messageResId, appContext.getString(R.string.app_name)),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun setListener(listener: OnCopyTmpFilesTaskListener?) {
        listenerRef = WeakReference(listener)
    }

    fun interface OnCopyTmpFilesTaskListener {
        fun onTmpFilesCopied(result: ResultCode)
    }
}
