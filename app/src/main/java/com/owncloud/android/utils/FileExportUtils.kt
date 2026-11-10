/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class FileExportUtils {
    @Throws(IllegalStateException::class)
    fun exportFile(
        fileName: String,
        mimeType: String,
        contentResolver: ContentResolver,
        ocFile: OCFile?,
        file: File?,
        subdirectory: String = ""
    ): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportFileAndroid10AndAbove(
                fileName,
                mimeType,
                contentResolver,
                ocFile,
                file,
                subdirectory
            )
        } else {
            exportFilesBelowAndroid10(
                fileName,
                contentResolver,
                ocFile,
                file,
                subdirectory
            )
        }
        true
    } catch (e: IOException) {
        Log_OC.e(this, "Cannot export file", e)
        false
    } catch (e: SecurityException) {
        Log_OC.e(this, "Cannot access export destination", e)
        false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    internal fun exportFileAndroid10AndAbove(
        fileName: String,
        mimeType: String,
        contentResolver: ContentResolver,
        ocFile: OCFile?,
        file: File?,
        subdirectory: String
    ) {
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$subdirectory")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            ?: throw IOException("Could not create export")
        try {
            writeMediaStoreExport(ocFile, file, contentResolver, uri)
        } catch (e: Exception) {
            runCatching { contentResolver.delete(uri, null, null) }.onFailure {
                Log_OC.e(this, "Could not remove incomplete export", it)
            }
            throw e
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeMediaStoreExport(ocFile: OCFile?, file: File?, contentResolver: ContentResolver, uri: Uri) {
        val stream = contentResolver.openOutputStream(uri, "w") ?: throw IOException("Could not open export")
        copy(ocFile, file, contentResolver, stream)
        val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        if (contentResolver.update(uri, published, null, null) != 1) {
            throw IOException("Could not publish export")
        }
    }

    internal fun exportFilesBelowAndroid10(
        fileName: String,
        contentResolver: ContentResolver,
        ocFile: OCFile?,
        file: File?,
        subdirectory: String
    ) {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            subdirectory
        )
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Could not create export directory")
        }
        var target = File(directory, fileName)
        var count = INITIAL_RENAME_COUNT
        while (!target.createNewFile()) {
            target = File(directory, generateNewName(fileName, count++))
        }
        try {
            copy(ocFile, file, contentResolver, FileOutputStream(target))
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    @Throws(IllegalStateException::class)
    private fun copy(ocFile: OCFile?, file: File?, contentResolver: ContentResolver, outputStream: OutputStream) {
        outputStream.use { fos ->
            val inputStream = when {
                ocFile != null -> contentResolver.openInputStream(ocFile.storageUri)
                file != null -> FileInputStream(file)
                else -> error("ocFile and file both may not be null")
            } ?: throw IOException("Could not open export source")
            inputStream.use { fis ->
                fis.copyTo(fos)
            }
        }
    }

    private fun generateNewName(name: String, count: Int): String {
        val extPos = name.lastIndexOf('.')
        val suffix = " ($count)"

        return if (extPos >= 0) {
            val extension = name.substring(extPos + 1)
            val nameWithoutExtension = name.substring(0, extPos)

            "$nameWithoutExtension$suffix.$extension"
        } else {
            name + suffix
        }
    }

    companion object {
        private const val INITIAL_RENAME_COUNT = 2
    }
}
