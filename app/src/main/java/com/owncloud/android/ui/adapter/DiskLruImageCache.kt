/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import com.jakewharton.disklrucache.DiskLruCache
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class DiskLruImageCache
@Throws(IOException::class)
constructor(
    diskCacheDir: File,
    diskCacheSize: Int,
    private val compressFormat: CompressFormat,
    private val compressQuality: Int
) {
    private val diskCache: DiskLruCache =
        DiskLruCache.open(diskCacheDir, CACHE_VERSION, VALUE_COUNT, diskCacheSize.toLong())

    fun put(key: String, data: Bitmap) {
        var editor: DiskLruCache.Editor? = null
        try {
            editor = diskCache.edit(key.toValidKey()) ?: return
            if (writeBitmap(data, editor)) {
                editor.commit()
            } else {
                editor.abort()
            }
        } catch (_: IOException) {
            editor?.abortQuietly()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun getScaledBitmap(key: String, width: Int, height: Int): Bitmap? = try {
        decodeScaled(key.toValidKey(), width, height)
    } catch (e: Exception) {
        Log_OC.e(TAG, e.message, e)
        null
    }

    fun getBitmap(key: String): Bitmap? = try {
        readEntry(key.toValidKey()) { BitmapFactory.decodeStream(it) }
    } catch (e: IOException) {
        Log_OC.e(TAG, e.message, e)
        null
    }

    fun getDecodedSizeInKB(key: String): Int? = try {
        readBounds(key.toValidKey())
            ?.takeIf { it.outWidth > 0 && it.outHeight > 0 }
            ?.let { it.outWidth * it.outHeight * ARGB_8888_BYTES_PER_PIXEL / BYTES_PER_KB }
    } catch (e: IOException) {
        Log_OC.e(TAG, e.message, e)
        null
    }

    fun containsKey(key: String): Boolean = try {
        diskCache.get(key.toValidKey())?.use { true } == true
    } catch (e: IOException) {
        Log_OC.d(TAG, e.message, e)
        false
    }

    fun clearCache() {
        try {
            diskCache.delete()
        } catch (e: IOException) {
            Log_OC.d(TAG, e.message, e)
        }
    }

    fun removeKey(key: String) {
        val validKey = key.toValidKey()
        try {
            diskCache.remove(validKey)
            Log_OC.d(TAG, "removeKey from cache: $validKey")
        } catch (e: IOException) {
            Log_OC.d(TAG, e.message, e)
        }
    }

    @Throws(IOException::class)
    private fun writeBitmap(bitmap: Bitmap, editor: DiskLruCache.Editor): Boolean =
        BufferedOutputStream(editor.newOutputStream(VALUE_INDEX), IO_BUFFER_SIZE).use {
            bitmap.compress(compressFormat, compressQuality, it)
        }

    @Throws(IOException::class)
    private fun decodeScaled(validKey: String, width: Int, height: Int): Bitmap? {
        val options = readBounds(validKey) ?: return null
        options.inSampleSize = BitmapUtils.calculateSampleFactor(options, width, height)
        options.inJustDecodeBounds = false
        return readEntry(validKey) { BitmapFactory.decodeStream(it, null, options) }
    }

    @Throws(IOException::class)
    private fun readBounds(validKey: String): BitmapFactory.Options? = readEntry(validKey) { stream ->
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeStream(stream, null, this)
        }
    }

    @Throws(IOException::class)
    private fun <T> readEntry(validKey: String, read: (InputStream) -> T): T? = diskCache.get(validKey)?.use {
        BufferedInputStream(it.getInputStream(VALUE_INDEX), IO_BUFFER_SIZE).use(read)
    }

    private fun DiskLruCache.Editor.abortQuietly() {
        try {
            abort()
        } catch (e: IOException) {
            Log_OC.d(TAG, "Error aborting editor", e)
        }
    }

    private fun String.toValidKey(): String = hashCode().toString()

    companion object {
        private const val CACHE_VERSION = 1
        private const val VALUE_COUNT = 1
        private const val VALUE_INDEX = 0
        private const val IO_BUFFER_SIZE = 8 * 1024
        private const val ARGB_8888_BYTES_PER_PIXEL = 4
        private const val BYTES_PER_KB = 1024

        private val TAG = DiskLruImageCache::class.java.simpleName
    }
}
