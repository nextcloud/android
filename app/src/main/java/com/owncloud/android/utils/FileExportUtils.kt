/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class FileExportUtils {
    @Throws(IllegalStateException::class)
    fun exportFile(fileName: String, mimeType: String, contentResolver: ContentResolver, ocFile: OCFile?, file: File?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportFileAndroid10AndAbove(
                fileName,
                mimeType,
                contentResolver,
                ocFile,
                file
            )
        } else {
            exportFilesBelowAndroid10(
                fileName,
                contentResolver,
                ocFile,
                file
            )
        }
    }

    @SuppressLint("Recycle") // handled inside copy method
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportFileAndroid10AndAbove(
        fileName: String,
        mimeType: String,
        contentResolver: ContentResolver,
        ocFile: OCFile?,
        file: File?
    ) {
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        var uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)

        if (uri == null) {
            var count = INITIAL_RENAME_COUNT
            do {
                val name = generateNewName(fileName, count)
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)

                count++
            } while (uri == null)
        }

        copy(
            ocFile,
            file,
            contentResolver,
            FileOutputStream(contentResolver.openFileDescriptor(uri, "w")?.fileDescriptor)
        )
    }

    private fun exportFilesBelowAndroid10(
        fileName: String,
        contentResolver: ContentResolver,
        ocFile: OCFile?,
        file: File?
    ) {
        try {
            var target = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )

            if (target.exists()) {
                var count = INITIAL_RENAME_COUNT
                do {
                    val name = generateNewName(fileName, count)
                    target = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        name
                    )

                    count++
                } while (target.exists())
            }

            copy(
                ocFile,
                file,
                contentResolver,
                FileOutputStream(target)
            )
        } catch (e: FileNotFoundException) {
            Log_OC.e(this, "File not found", e)
        } catch (e: IOException) {
            Log_OC.e(this, "Cannot write file", e)
        }
    }

    @Throws(IllegalStateException::class)
    private fun copy(ocFile: OCFile?, file: File?, contentResolver: ContentResolver, outputStream: FileOutputStream) {
        outputStream.use { fos ->
            try {
                val inputStream = when {
                    ocFile != null -> contentResolver.openInputStream(ocFile.storageUri)
                    file != null -> FileInputStream(file)
                    else -> error("ocFile and file both may not be null")
                }!!

                inputStream.use { fis ->
                    copyStream(fis, fos)
                }
            } catch (e: IOException) {
                Log_OC.e(this, "Cannot write file", e)
            }
        }
    }

    private fun copyStream(inputStream: InputStream, outputStream: FileOutputStream) {
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        var len: Int
        while (inputStream.read(buffer).also { len = it } != -1) {
            outputStream.write(buffer, 0, len)
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
        private const val COPY_BUFFER_SIZE = 1024
    }
}
