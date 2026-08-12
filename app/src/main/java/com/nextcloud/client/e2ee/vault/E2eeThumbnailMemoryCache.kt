/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.graphics.Bitmap
import androidx.collection.LruCache
import com.owncloud.android.datamodel.OCFile

object E2eeThumbnailMemoryCache : E2eeVaultSessionLockListener {
    private val cache = object : LruCache<String, Bitmap>(maxCacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / BYTES_IN_KIB).coerceAtLeast(1)
    }
    private val failedUntilByKey = mutableMapOf<String, Long>()

    @Synchronized
    fun get(accountName: String, file: OCFile): Bitmap? =
        cache.get(cacheKey(accountName, file))?.takeUnless { it.isRecycled }

    @Synchronized
    fun put(accountName: String, file: OCFile, bitmap: Bitmap): Bitmap {
        val key = cacheKey(accountName, file)
        failedUntilByKey.remove(key)
        cache.put(key, bitmap)
        return bitmap
    }

    @Synchronized
    fun hasRecentFailure(accountName: String, file: OCFile): Boolean {
        val key = cacheKey(accountName, file)
        val failedUntil = failedUntilByKey[key] ?: return false
        val failureStillActive = System.currentTimeMillis() <= failedUntil

        if (!failureStillActive) {
            failedUntilByKey.remove(key)
        }

        return failureStillActive
    }

    @Synchronized
    fun putFailure(accountName: String, file: OCFile) {
        failedUntilByKey[cacheKey(accountName, file)] = System.currentTimeMillis() + FAILURE_CACHE_DURATION_MILLIS
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
        failedUntilByKey.clear()
    }

    @Synchronized
    override fun onVaultLocked(key: E2eeVaultSessionKey) {
        val accountPrefix = accountPrefix(key.accountName)
        cache.snapshot().keys
            .filter { it.startsWith(accountPrefix) }
            .forEach { cache.remove(it) }
        failedUntilByKey.keys
            .filter { it.startsWith(accountPrefix) }
            .forEach { failedUntilByKey.remove(it) }
    }

    @Synchronized
    override fun onAllVaultsLocked() {
        clear()
    }

    private fun cacheKey(accountName: String, file: OCFile): String = buildString {
        append(accountPrefix(accountName))
        append(file.remoteId ?: file.remotePath)
        append(KEY_SEPARATOR)
        append(file.etag ?: file.etagOnServer)
        append(KEY_SEPARATOR)
        append(file.fileLength)
        append(KEY_SEPARATOR)
        append(file.modificationTimestamp)
    }

    private fun accountPrefix(accountName: String): String = accountName + KEY_SEPARATOR

    private fun maxCacheSizeKb(): Int = (Runtime.getRuntime().maxMemory() / MEMORY_CACHE_DIVISOR / BYTES_IN_KIB).toInt()
        .coerceAtLeast(MIN_CACHE_SIZE_KB)

    private const val BYTES_IN_KIB = 1024
    private const val FAILURE_CACHE_DURATION_MILLIS = 30_000L
    private const val KEY_SEPARATOR = ":"
    private const val MEMORY_CACHE_DIVISOR = 16
    private const val MIN_CACHE_SIZE_KB = 1024
}
