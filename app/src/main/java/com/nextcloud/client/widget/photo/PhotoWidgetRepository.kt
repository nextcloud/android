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
        private const val MAX_BITMAP_DIMENSION = 512
        private const val READ_TIMEOUT = 40000
        private const val CONNECTION_TIMEOUT = 5000
    }

    // --------------- Configuration persistence ---------------

    fun saveWidgetConfig(widgetId: Int, folderPath: String, accountName: String, intervalMinutes: Long = PhotoWidgetConfig.DEFAULT_INTERVAL_MINUTES) {
        preferences.edit()
            .putString(PREF_FOLDER_PATH + widgetId, folderPath)
            .putString(PREF_ACCOUNT_NAME + widgetId, accountName)
            .putLong(PREF_INTERVAL_MINUTES + widgetId, intervalMinutes)
            .apply()
    }

    fun deleteWidgetConfig(widgetId: Int) {
        preferences.edit()
            .remove(PREF_FOLDER_PATH + widgetId)
            .remove(PREF_ACCOUNT_NAME + widgetId)
            .remove(PREF_INTERVAL_MINUTES + widgetId)
            .apply()
    }

    fun getWidgetConfig(widgetId: Int): PhotoWidgetConfig? {
        val folderPath = preferences.getString(PREF_FOLDER_PATH + widgetId, null) ?: return null
        val accountName = preferences.getString(PREF_ACCOUNT_NAME + widgetId, null) ?: return null
        val interval = preferences.getLong(PREF_INTERVAL_MINUTES + widgetId, PhotoWidgetConfig.DEFAULT_INTERVAL_MINUTES)
        return PhotoWidgetConfig(widgetId, folderPath, accountName, interval)
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
        val user = userAccountManager.getUser(config.accountName).orElse(null) ?: return null

        val storageManager = FileDataStorageManager(user, contentResolver)
        val folder = storageManager.getFileByDecryptedRemotePath(config.folderPath) ?: return null
        val allFiles = storageManager.getAllFilesRecursivelyInsideFolder(folder)

        val imageFiles = allFiles.filter { isImageFile(it) }.shuffled()

        for (file in imageFiles) {
            val bitmap = getThumbnailForFile(file, config.accountName)
            if (bitmap != null) {
                val geo = file.geoLocation
                return PhotoWidgetImageResult(
                    bitmap = bitmap,
                    latitude = geo?.latitude,
                    longitude = geo?.longitude,
                    modificationTimestamp = file.modificationTimestamp
                )
            }
        }
        return null
    }

    @Suppress("MagicNumber")
    private fun isImageFile(file: OCFile): Boolean {
        val mimeType = file.mimeType ?: return false
        return mimeType.startsWith("image/")
    }

    /**
     * Attempts to retrieve a cached thumbnail, or downloads it if missing.
     */
    private fun getThumbnailForFile(file: OCFile, accountName: String): Bitmap? {
        // 1. Try "resized" cache key
        val imageKey = "r" + file.remoteId
        var bitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(imageKey)
        if (bitmap != null) return scaleBitmap(bitmap)

        // 2. Try "thumbnail" cache key
        val thumbnailKey = "t" + file.remoteId
        bitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(thumbnailKey)
        if (bitmap != null) return scaleBitmap(bitmap)

        // 3. If missing, download from server
        if (file.isDown) {
            // If file is downloaded, generate from local storage
            val dimension = ThumbnailsCacheManager.getThumbnailDimension()
            bitmap = BitmapUtils.decodeSampledBitmapFromFile(file.storagePath, dimension, dimension)
            if (bitmap != null) {
                ThumbnailsCacheManager.addBitmapToCache(thumbnailKey, bitmap)
                return scaleBitmap(bitmap)
            }
        }

        // 4. Download from server
        return downloadThumbnail(file, thumbnailKey, accountName)
    }

    private fun downloadThumbnail(file: OCFile, cacheKey: String, accountName: String): Bitmap? {
        val user = userAccountManager.getUser(accountName).orElse(null) ?: return null
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getClientFor(user.toOwnCloudAccount(), MainApp.getAppContext())

        val dimension = ThumbnailsCacheManager.getThumbnailDimension()
        val uri = client.baseUri.toString() + "/index.php/core/preview?fileId=" +
            file.localId + "&x=" + dimension + "&y=" + dimension + "&a=1&mode=cover&forceIcon=0"

        val loopKey = "download_thumb_${file.remoteId}"
        Log_OC.d(TAG, "Downloading widget thumbnail: $uri")

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

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
