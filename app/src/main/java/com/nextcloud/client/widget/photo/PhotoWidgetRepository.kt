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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.media.MediaMetadataRetriever
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val modificationTimestamp: Long,
    val isVideo: Boolean = false,
    val fileRemotePath: String? = null,
    val fileId: Long? = null
)

/**
 * Repository that manages photo widget configurations and image retrieval.
 *
 * Responsibilities:
 * - Save / delete / retrieve per-widget config in SharedPreferences
 * - Query FileDataStorageManager for image files in the selected folder
 * - Pick a random image and return a cached thumbnail Bitmap
 * - Pre-fetch the next candidate for instant loading
 * - Extract video thumbnails with play icon overlay
 */
@Suppress("MagicNumber", "TooManyFunctions")
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
        private const val PREF_FILE_COUNT = "${PREF_PREFIX}file_count_"
        private const val MAX_BITMAP_DIMENSION = 800
        private const val SERVER_REQUEST_DIMENSION = 2048
        private const val READ_TIMEOUT = 40000
        private const val CONNECTION_TIMEOUT = 5000
        private const val CACHE_HISTORY_LIMIT = 10
        private const val OFFLINE_BIAS_PERCENT = 80
        private const val OFFLINE_FALLBACK_ATTEMPTS = 3
        private const val RECENT_FILES_COUNT = 20
        private const val RANDOM_FILES_COUNT = 10
        private const val MAX_VIDEO_CACHE = 2
        private const val PLAY_ICON_SIZE_RATIO = 0.15f
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
            .remove(PREF_FILE_COUNT + widgetId)
            .remove("${PREF_PREFIX}history_$widgetId")
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
     * Uses a "Smart Mix" strategy combining On This Day, Recent, and Random files.
     * Supports both image and video files (videos show as thumbnail + ▶ overlay).
     * Pre-fetches the next candidate for instant loading.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount")
    fun getRandomImageResult(widgetId: Int): PhotoWidgetImageResult? {
        val config = getWidgetConfig(widgetId) ?: return null
        val folderPath = config.folderPath
        val accountName = config.accountName

        val user = userAccountManager.getUser(accountName).orElse(null) ?: return null
        val storageManager = FileDataStorageManager(user, contentResolver)
        val folder = storageManager.getFileByDecryptedRemotePath(folderPath) ?: return null
        val allFiles = storageManager.getAllFilesRecursivelyInsideFolder(folder)

        // Cache invalidation: if file count changed, clear stale cache
        invalidateCacheIfNeeded(widgetId, allFiles)

        // IMPLEMENTATION OF "SMART MIX" STRATEGY
        // 1. "On This Day": Photos from today's date in past years
        val onThisDayFiles = allFiles.filter { isOnThisDay(it.modificationTimestamp) }

        // 2. "Recent": Top 20 newest photos
        val recentFiles = allFiles.sortedByDescending { it.modificationTimestamp }.take(RECENT_FILES_COUNT)

        // 3. "Random": 10 random files from the rest to add variety
        val usedIds = (onThisDayFiles + recentFiles).map { it.remoteId }.toSet()
        val remainingFiles = allFiles.filter { !usedIds.contains(it.remoteId) }
        val randomFiles = remainingFiles.shuffled().take(RANDOM_FILES_COUNT)

        // Combine all candidates — include both images and videos
        val candidatePool = (onThisDayFiles + recentFiles + randomFiles).filter { isMediaFile(it) }

        if (candidatePool.isEmpty()) {
            return null
        }

        // Prioritize images that are already downloaded or cached to avoid network timeouts
        val (offlineFiles, onlineFiles) = candidatePool.partition { file ->
            file.isDown || ThumbnailsCacheManager.containsBitmap(ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId)
        }

        // 80% chance to pick from offline files if available
        var candidateFile = if (offlineFiles.isNotEmpty() && (percentage(OFFLINE_BIAS_PERCENT) || onlineFiles.isEmpty())) {
            offlineFiles.random()
        } else if (onlineFiles.isNotEmpty()) {
            onlineFiles.random()
        } else {
            candidatePool.random()
        }

        val isVideo = isVideoFile(candidateFile)
        var bitmap = if (isVideo) {
            getVideoThumbnail(candidateFile)
        } else {
            getThumbnailForFile(candidateFile, accountName)
        }

        // FAILSAFE: If the selected candidate failed, try offline fallback
        if (bitmap == null && offlineFiles.isNotEmpty()) {
            Log_OC.d(TAG, "Failed to load candidate, trying fallback from ${offlineFiles.size} offline files")
            for (i in 0 until OFFLINE_FALLBACK_ATTEMPTS) {
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

        // Add play icon overlay for video files
        if (isVideo) {
            bitmap = addPlayIconOverlay(bitmap)
        }

        // Update cache history and cleanup old entries
        manageWidgetCache(widgetId, candidateFile, allFiles)

        // Pre-fetch next candidate in background
        prefetchNextCandidate(candidatePool, candidateFile, accountName)

        val geo = candidateFile.geoLocation
        return PhotoWidgetImageResult(
            bitmap = bitmap,
            latitude = geo?.latitude,
            longitude = geo?.longitude,
            modificationTimestamp = candidateFile.modificationTimestamp,
            isVideo = isVideo,
            fileRemotePath = candidateFile.remotePath,
            fileId = candidateFile.localId
        )
    }

    // --------------- Cache invalidation ---------------

    private fun invalidateCacheIfNeeded(widgetId: Int, allFiles: List<OCFile>) {
        val prefKey = PREF_FILE_COUNT + widgetId
        val lastKnownCount = preferences.getInt(prefKey, -1)
        val currentCount = allFiles.size

        if (lastKnownCount != -1 && lastKnownCount != currentCount) {
            Log_OC.d(TAG, "File count changed ($lastKnownCount → $currentCount), clearing stale cache")
            val historyKey = "${PREF_PREFIX}history_$widgetId"
            val historyString = preferences.getString(historyKey, "") ?: ""
            val history = if (historyString.isNotEmpty()) historyString.split(",") else emptyList()

            // Remove cached entries for files that no longer exist
            val existingIds = allFiles.map { it.remoteId }.toSet()
            val staleIds = history.filter { !existingIds.contains(it) }
            for (staleId in staleIds) {
                Log_OC.d(TAG, "Stale cache entry detected (will be evicted by LRU): $staleId")
            }

            // Update history to remove stale entries
            val cleanedHistory = history.filter { existingIds.contains(it) }
            preferences.edit().putString(historyKey, cleanedHistory.joinToString(",")).apply()
        }

        // Save current file count
        preferences.edit().putInt(prefKey, currentCount).apply()
    }

    // --------------- Pre-fetching ---------------

    private fun prefetchNextCandidate(candidates: List<OCFile>, current: OCFile, accountName: String) {
        val nextCandidates = candidates.filter { it.remoteId != current.remoteId }
        if (nextCandidates.isEmpty()) return

        val next = nextCandidates.random()
        // Only pre-fetch if not already cached
        val imageKey = "r" + next.remoteId
        val thumbnailKey = "t" + next.remoteId
        if (ThumbnailsCacheManager.getBitmapFromDiskCache(imageKey) != null ||
            ThumbnailsCacheManager.getBitmapFromDiskCache(thumbnailKey) != null) {
            return
        }

        Log_OC.d(TAG, "Pre-fetching next candidate: ${next.fileName}")
        if (isVideoFile(next)) {
            getVideoThumbnail(next)
        } else {
            getThumbnailForFile(next, accountName)
        }
    }

    // --------------- Video thumbnail support ---------------

    /**
     * Extracts a video frame thumbnail using [MediaMetadataRetriever].
     * Only works for downloaded video files. Max 2 video thumbnails are cached.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun getVideoThumbnail(file: OCFile): Bitmap? {
        if (!file.isDown) {
            Log_OC.d(TAG, "Video not downloaded, cannot extract thumbnail: ${file.fileName}")
            return null
        }

        // Check video cache count
        val videoCacheKey = "video_${file.remoteId}"
        val cached = ThumbnailsCacheManager.getBitmapFromDiskCache(videoCacheKey)
        if (cached != null) return scaleBitmap(cached)

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.storagePath)
            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()

            if (frame != null) {
                // Enforce max 2 video thumbnails in cache
                enforceVideoThumbnailLimit()
                ThumbnailsCacheManager.addBitmapToCache(videoCacheKey, frame)
                return scaleBitmap(frame)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error extracting video thumbnail: ${file.fileName}", e)
        }
        return null
    }

    private fun enforceVideoThumbnailLimit() {
        val videoHistoryKey = "${PREF_PREFIX}video_cache_ids"
        val historyString = preferences.getString(videoHistoryKey, "") ?: ""
        val history = if (historyString.isNotEmpty()) historyString.split(",").toMutableList() else mutableListOf()

        while (history.size >= MAX_VIDEO_CACHE) {
            val oldId = history.removeAt(0)
            Log_OC.d(TAG, "Video thumbnail evicted from tracking: $oldId")
        }

        preferences.edit().putString(videoHistoryKey, history.joinToString(",")).apply()
    }

    /**
     * Draws a semi-transparent play icon (▶) onto the center of the bitmap.
     */
    private fun addPlayIconOverlay(source: Bitmap): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val centerX = result.width / 2f
        val centerY = result.height / 2f
        val iconSize = result.width * PLAY_ICON_SIZE_RATIO

        // Semi-transparent circle background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, iconSize, bgPaint)

        // White triangle play icon
        val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val triangleSize = iconSize * 0.6f
        val path = Path().apply {
            moveTo(centerX - triangleSize * 0.4f, centerY - triangleSize * 0.6f)
            lineTo(centerX - triangleSize * 0.4f, centerY + triangleSize * 0.6f)
            lineTo(centerX + triangleSize * 0.6f, centerY)
            close()
        }
        canvas.drawPath(path, trianglePaint)

        return result
    }

    // --------------- Smart Mix helpers ---------------

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

        // Enforce limit
        while (history.size > CACHE_HISTORY_LIMIT) {
            val oldId = history.removeAt(0)
            val fileToRemove = allFiles.find { it.remoteId == oldId }
            if (fileToRemove != null) {
                Log_OC.d(TAG, "Evicting old widget image from cache: ${fileToRemove.fileName}")
                ThumbnailsCacheManager.removeFromCache(fileToRemove)
            } else {
                Log_OC.d(TAG, "Could not find file object for eviction: $oldId")
            }
        }

        preferences.edit().putString(prefKey, history.joinToString(",")).apply()
    }

    private fun percentage(chance: Int): Boolean {
        return (Math.random() * 100).toInt() < chance
    }

    private fun isMediaFile(file: OCFile): Boolean {
        val mimeType = file.mimeType ?: return false
        return mimeType.startsWith("image/") || mimeType.startsWith("video/")
    }

    private fun isVideoFile(file: OCFile): Boolean {
        val mimeType = file.mimeType ?: return false
        return mimeType.startsWith("video/")
    }

    // --------------- Thumbnail retrieval ---------------

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
            bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.storagePath, SERVER_REQUEST_DIMENSION, SERVER_REQUEST_DIMENSION)
            if (bitmap != null) {
                val keyToCache = imageKey
                ThumbnailsCacheManager.addBitmapToCache(keyToCache, bitmap)
                return scaleBitmap(bitmap)
            }
        }

        // 4. Download from server (High Res)
        return downloadThumbnail(file, imageKey, accountName)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun downloadThumbnail(file: OCFile, cacheKey: String, accountName: String): Bitmap? {
        val user = userAccountManager.getUser(accountName).orElse(null) ?: return null
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(user.toOwnCloudAccount(), MainApp.getAppContext())

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
