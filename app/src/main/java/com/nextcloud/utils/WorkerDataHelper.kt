/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.client.jobs.ContactsImportWork
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.ByteBuffer

object WorkerDataHelper {

    private const val TAG = "WorkerDataHelper"

    @Suppress("TooGenericExceptionCaught")
    fun writeByteArray(prefix: String, data: ByteArray): String? = try {
        val context = MainApp.getAppContext()
        val filename = prefix + System.currentTimeMillis()
        val file = File(context.cacheDir, "$filename.txt")
        FileUtils.writeByteArrayToFile(file, data)
        file.absolutePath
    } catch (e: Exception) {
        Log_OC.e(TAG, "Exception writeDataLongArrayToCacheDir: $e")
        null
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun readByteArray(path: String?): ByteArray? {
        try {
            if (path.isNullOrEmpty()) {
                return null
            }

            val file = File(path)
            if (!file.exists()) {
                Log_OC.d(TAG, "file not exists")
                return null
            }
            return FileUtils.readFileToByteArray(file)
        } catch (e: Exception) {
            Log_OC.e(ContactsImportWork.Companion.TAG, "Exception readCheckedContractsFromFile: $e")
            return null
        }
    }

    fun cleanup(path: String?) {
        if (path.isNullOrEmpty()) {
            return
        }

        val file = File(path)
        if (!file.exists()) {
            Log_OC.d(TAG, "file not exists")
            return
        }

        file.delete()
    }

    fun IntArray.toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(this.size * Int.SIZE_BYTES)
        val intBuffer = byteBuffer.asIntBuffer()
        intBuffer.put(this)
        return byteBuffer.array()
    }

    fun ByteArray.toIntArray(): IntArray {
        val intBuffer = ByteBuffer.wrap(this).asIntBuffer()
        val intArray = IntArray(this.size / Int.SIZE_BYTES)
        intBuffer.get(intArray)
        return intArray
    }

    fun LongArray.toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(this.size * Long.SIZE_BYTES)
        val buffer = byteBuffer.asLongBuffer()
        buffer.put(this)
        return byteBuffer.array()
    }

    fun ByteArray.toLongArray(): LongArray {
        val buffer = ByteBuffer.wrap(this).asLongBuffer()
        val longArray = LongArray(this.size / Long.SIZE_BYTES)
        buffer.get(longArray)
        return longArray
    }
}
