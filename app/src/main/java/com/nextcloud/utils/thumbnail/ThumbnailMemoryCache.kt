/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import android.graphics.Bitmap
import android.util.LruCache

private const val BYTES_PER_KILOBYTE = 1024
private const val HEAP_FRACTION = 8

object ThumbnailMemoryCache {

    private val cache = object : LruCache<String, Bitmap>(maxSizeInKilobytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / BYTES_PER_KILOBYTE
    }

    fun get(key: String): Bitmap? = cache.get(key)?.takeUnless { it.isRecycled }

    fun put(key: String, bitmap: Bitmap) {
        if (bitmap.isRecycled) {
            return
        }

        cache.put(key, bitmap)
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.evictAll()
    }

    private fun maxSizeInKilobytes(): Int =
        (Runtime.getRuntime().maxMemory() / BYTES_PER_KILOBYTE / HEAP_FRACTION).toInt()
}
