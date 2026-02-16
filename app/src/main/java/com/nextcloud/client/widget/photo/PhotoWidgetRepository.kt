/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Axel
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget.photo

import android.content.ContentResolver
import android.content.SharedPreferences
import android.graphics.Bitmap
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod
import java.io.InputStream
import javax.inject.Inject

/**
 * Holds the result of picking a random image for the widget.
 */
data class PhotoWidgetImageResult(
    val bitmap: Bitmap,
    val latitude: Double?,
    val longitude: Double?,
    val modificationTimestamp: Long
)

/**
 * Repository that manages photo widget configurations and image retrieval.
 *
 * Responsibilities:
 * - Save / delete / retrieve per-widget config in SharedPreferences
 * - Query FileDataStorageManager for image files in the selected folder
 * - Pick a random image and return a cached thumbnail Bitmap
 */
class PhotoWidgetRepository @Inject constructor(
    private val preferences: SharedPreferences,
    private val userAccountManager: UserAccountManager,
    private val contentResolver: ContentResolver
) {

    companion object {
        private const val TAG = "PhotoWidgetRepository"
        private const val PREF_PREFIX = "photo_widget_"
        private const val PREF_FOLDER_PATH = "${PREF_PREFIX}folder_path_"
        private const val PREF_ACCOUNT_NAME = "${PREF_PREFIX}account_name_"
        private const val PREF_INTERVAL_MINUTES = "${PREF_PREFIX}interval_minutes_"
        private const val MAX_BITMAP_DIMENSION = 800 // Increased from 512 for better quality
        private const val SERVER_REQUEST_DIMENSION = 2048 // Request high-res preview from server
        private const val READ_TIMEOUT = 40000
        private const val CONNECTION_TIMEOUT = 5000
    }

    fun getWidgetConfig(widgetId: Int): PhotoWidgetConfig? {
        val folderPath = preferences.getString(PREF_FOLDER_PATH + widgetId, null) ?: return null
        val accountName = preferences.getString(PREF_ACCOUNT_NAME + widgetId, null) ?: return null
        val interval = preferences.getLong(PREF_INTERVAL_MINUTES + widgetId, PhotoWidgetConfig.DEFAULT_INTERVAL_MINUTES)
        return PhotoWidgetConfig(widgetId, folderPath, accountName, interval)
    }

    fun saveWidgetConfig(config: PhotoWidgetConfig) {
        preferences.edit()
            .putString(PREF_FOLDER_PATH + config.widgetId, config.folderPath)
            .putString(PREF_ACCOUNT_NAME + config.widgetId, config.accountName)
            .putLong(PREF_INTERVAL_MINUTES + config.widgetId, config.intervalMinutes)
            .apply()
    }

    fun deleteWidgetConfig(widgetId: Int) {
        preferences.edit()
            .remove(PREF_FOLDER_PATH + widgetId)
            .remove(PREF_ACCOUNT_NAME + widgetId)
            .remove(PREF_INTERVAL_MINUTES + widgetId)
            .apply()
    }

    // --------------- Image retrieval ---------------

    /**
     * Returns a random image bitmap for the given widget, or `null` on any failure.
     */
    fun getRandomImageBitmap(widgetId: Int): Bitmap? {
        return getRandomImageResult(widgetId)?.bitmap
    }

    /**
     * Returns a random image result with bitmap and metadata, or `null` on failure.
     *
     * Shuffles all image files and tries each one until a thumbnail loads successfully.
     * This ensures the widget falls back to cached/local images when the network
     * connection is poor, rather than showing a placeholder.
     */
    fun getRandomImageResult(widgetId: Int): PhotoWidgetImageResult? {
        val config = getWidgetConfig(widgetId) ?: return null
        val folderPath = config.folderPath
        val accountName = config.accountName

        val user = userAccountManager.getUser(accountName).orElse(null) ?: return null
        val storageManager = FileDataStorageManager(user, contentResolver)
        val folder = storageManager.getFileByDecryptedRemotePath(folderPath) ?: return null
        val allFiles = storageManager.getAllFilesRecursivelyInsideFolder(folder)

        // IMPLEMENTATION OF "SMART MIX" STRATEGY
        // 1. "On This Day": Photos from today's date in past years
        val onThisDayFiles = allFiles.filter { isOnThisDay(it.modificationTimestamp) }

        // 2. "Recent": Top 20 newest photos
        val recentFiles = allFiles.sortedByDescending { it.modificationTimestamp }.take(20)

        // 3. "Random": 10 random files from the rest to add variety
        val usedIds = (onThisDayFiles + recentFiles).map { it.remoteId }.toSet()
        val remainingFiles = allFiles.filter { !usedIds.contains(it.remoteId) }
        val randomFiles = remainingFiles.shuffled().take(10)

        // Combine all candidates
        val candidatePool = (onThisDayFiles + recentFiles + randomFiles).filter { isImageFile(it) }

        if (candidatePool.isEmpty()) {
            return null
        }

        // Prioritize images that are already downloaded or cached to avoid network timeouts
        val (offlineFiles, onlineFiles) = candidatePool.partition { file ->
            file.isDown || ThumbnailsCacheManager.containsBitmap(ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId)
        }

        // 80% chance to pick from offline files if available, otherwise fallback to online
        // This keeps the "randomness" but strongly biases towards instant loading
        var candidateFile = if (offlineFiles.isNotEmpty() && (percentage(80) || onlineFiles.isEmpty())) {
            offlineFiles.random()
        } else if (onlineFiles.isNotEmpty()) {
            onlineFiles.random()
        } else {
            candidatePool.random()
        }

        var bitmap = getThumbnailForFile(candidateFile, accountName)

        // FAILSAFE: If the selected candidate failed (e.g. download error or missing file),
        // and we have offline files available, try one of them instead of showing nothing.
        if (bitmap == null && offlineFiles.isNotEmpty()) {
            Log_OC.d(TAG, "Failed to load candidate image (isDown=${candidateFile.isDown}), trying fallback from ${offlineFiles.size} offline files")
            // Try up to 3 random offline files to find a working one
            for (i in 0 until 3) {
                val fallback = offlineFiles.random()
                bitmap = getThumbnailForFile(fallback, accountName)
                if (bitmap != null) {
                    candidateFile = fallback
                    break
                }
            }
        }

        if (bitmap == null) {
            Log_OC.e(TAG, "Failed to load any widget image")
            return null
        }

        // Update cache history and cleanup old entries
        manageWidgetCache(widgetId, candidateFile, allFiles)

        val geo = candidateFile.geoLocation
        return PhotoWidgetImageResult(
            bitmap = bitmap,
            latitude = geo?.latitude,
            longitude = geo?.longitude,
            modificationTimestamp = candidateFile.modificationTimestamp
        )
    }

    private fun isOnThisDay(timestamp: Long): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val todayMonth = calendar.get(java.util.Calendar.MONTH)
        val todayDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        calendar.timeInMillis = timestamp
        val fileMonth = calendar.get(java.util.Calendar.MONTH)
        val fileDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        return (todayMonth == fileMonth && todayDay == fileDay)
    }

    private fun manageWidgetCache(widgetId: Int, newFile: OCFile, allFiles: List<OCFile>) {
        val prefKey = "${PREF_PREFIX}history_$widgetId"
        val historyString = preferences.getString(prefKey, "") ?: ""
        val history = if (historyString.isNotEmpty()) {
            historyString.split(",").toMutableList()
        } else {
            mutableListOf()
        }

        // Add new file ID if not present (move to end if present)
        val newId = newFile.remoteId
        if (history.contains(newId)) {
            history.remove(newId)
        }
        history.add(newId)

        // Enforce limit of 10
        while (history.size > 10) {
            val oldId = history.removeAt(0)
            // Find the file to remove it from cache
            val fileToRemove = allFiles.find { it.remoteId == oldId }
            if (fileToRemove != null) {
                Log_OC.d(TAG, "Evicting old widget image from cache: ${fileToRemove.fileName}")
                ThumbnailsCacheManager.removeFromCache(fileToRemove)
            } else {
                 Log_OC.d(TAG, "Could not find file object for eviction: $oldId")
            }
        }

        // Save updated history
        preferences.edit().putString(prefKey, history.joinToString(",")).apply()
    }

    private fun percentage(chance: Int): Boolean {
        return (Math.random() * 100).toInt() < chance
    }

    @Suppress("MagicNumber")
    private fun isImageFile(file: OCFile): Boolean {
        val mimeType = file.mimeType ?: return false
        return mimeType.startsWith("image/")
    }

    /**
     * Attempts to retrieve a cached thumbnail, or downloads it if missing.
     * Tries "resized" (large) cache first for quality.
     */
    private fun getThumbnailForFile(file: OCFile, accountName: String): Bitmap? {
        // 1. Try "resized" cache key (Best Quality)
        val imageKey = "r" + file.remoteId
        var bitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(imageKey)
        if (bitmap != null) return scaleBitmap(bitmap)

        // 2. Try "thumbnail" cache key (Fallback)
        val thumbnailKey = "t" + file.remoteId
        bitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(thumbnailKey)
        if (bitmap != null) return scaleBitmap(bitmap)

        // 3. If missing, generate from local file
        if (file.isDown) {
            // Generate high-quality local thumbnail
            bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.storagePath, SERVER_REQUEST_DIMENSION, SERVER_REQUEST_DIMENSION)
            if (bitmap != null) {
                // Cache as "resized" for future high-quality use
                val keyToCache = imageKey // Cache as 'r' (resized)
                ThumbnailsCacheManager.addBitmapToCache(keyToCache, bitmap)
                return scaleBitmap(bitmap)
            }
        }

        // 4. Download from server (High Res)
        return downloadThumbnail(file, imageKey, accountName)
    }

    private fun downloadThumbnail(file: OCFile, cacheKey: String, accountName: String): Bitmap? {
        val user = userAccountManager.getUser(accountName).orElse(null) ?: return null
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(user.toOwnCloudAccount(), MainApp.getAppContext())

        // Request high-res preview (2048px)
        val dimension = SERVER_REQUEST_DIMENSION
        val uri = client.baseUri.toString() + "/index.php/core/preview?fileId=" +
            file.localId + "&x=" + dimension + "&y=" + dimension + "&a=1&mode=cover&forceIcon=0"

        Log_OC.d(TAG, "Downloading widget high-res preview: $uri")

        val getMethod = GetMethod(uri)
        getMethod.setRequestHeader("Cookie", "nc_sameSiteCookielax=true;nc_sameSiteCookiestrict=true")
        getMethod.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE)

        try {
            val status = client.executeMethod(getMethod, READ_TIMEOUT, CONNECTION_TIMEOUT)
            if (status == HttpStatus.SC_OK) {
                val inputStream: InputStream = getMethod.responseBodyAsStream
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    ThumbnailsCacheManager.addBitmapToCache(cacheKey, bitmap)
                    return scaleBitmap(bitmap)
                }
            } else {
                client.exhaustResponse(getMethod.responseBodyAsStream)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error downloading widget thumbnail", e)
        } finally {
            getMethod.releaseConnection()
        }

        return null
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_BITMAP_DIMENSION && height <= MAX_BITMAP_DIMENSION) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = MAX_BITMAP_DIMENSION
            newHeight = (MAX_BITMAP_DIMENSION / ratio).toInt()
        } else {
            newHeight = MAX_BITMAP_DIMENSION
            newWidth = (MAX_BITMAP_DIMENSION * ratio).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaledBitmap != bitmap) {
            bitmap.recycle()
        }
        return scaledBitmap
    }
}
