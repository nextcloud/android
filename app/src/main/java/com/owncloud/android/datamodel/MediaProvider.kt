/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.owncloud.android.MainApp
import com.owncloud.android.utils.PermissionUtil
import java.io.File

/**
 * Media queries to gain access to media lists for the device.
 */
object MediaProvider {
    private const val PATH_SEPARATOR = '/'
    private const val SQL_EQUALS = "="

    private val IMAGES_MEDIA_URI: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private val VIDEOS_MEDIA_URI: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    private val FILE_PROJECTION = arrayOf(MediaStore.MediaColumns.DATA)
    private val IMAGES_FOLDER_PROJECTION = arrayOf(
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    )
    private val VIDEOS_FOLDER_PROJECTION = arrayOf(
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME
    )

    @JvmStatic
    fun getImageFolders(
        contentResolver: ContentResolver,
        itemLimit: Int,
        activity: AppCompatActivity?,
        getWithoutActivity: Boolean
    ): MutableList<MediaFolder> {
        checkPermissions(activity)

        val folderCursor = if (isStorageAccessGranted(activity, getWithoutActivity)) {
            ContentResolverHelper.queryResolver(
                contentResolver,
                IMAGES_MEDIA_URI,
                IMAGES_FOLDER_PROJECTION,
                sortColumn = MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                sortDirection = ContentResolverHelper.SORT_DIRECTION_ASCENDING
            )
        } else {
            null
        }

        val buckets = folderCursor?.use {
            it.readBuckets(MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        } ?: return mutableListOf()

        val dataPath = appDataPath()

        return buckets.mapNotNullTo(mutableListOf()) { (bucketId, folderName) ->
            val selection = bucketSelection(MediaStore.Images.Media.BUCKET_ID, bucketId)
            val fileCursor = ContentResolverHelper.queryResolver(
                contentResolver,
                IMAGES_MEDIA_URI,
                FILE_PROJECTION,
                selection,
                sortColumn = MediaStore.Images.Media.DATE_TAKEN,
                sortDirection = ContentResolverHelper.SORT_DIRECTION_DESCENDING,
                limit = itemLimit
            )

            val filePaths = fileCursor?.use { it.readFilePaths(itemLimit, ::isValidAndExistingFilePath) }.orEmpty()

            buildMediaFolder(MediaFolderType.IMAGE, folderName, filePaths, dataPath) {
                countFiles(contentResolver, IMAGES_MEDIA_URI, selection)
            }
        }
    }

    @JvmStatic
    fun getVideoFolders(
        contentResolver: ContentResolver,
        itemLimit: Int,
        activity: AppCompatActivity?,
        getWithoutActivity: Boolean
    ): MutableList<MediaFolder> {
        checkPermissions(activity)

        val folderCursor = if (isStorageAccessGranted(activity, getWithoutActivity)) {
            contentResolver.query(VIDEOS_MEDIA_URI, VIDEOS_FOLDER_PROJECTION, null, null, null)
        } else {
            null
        }

        val buckets = folderCursor?.use {
            it.readBuckets(MediaStore.Video.Media.BUCKET_ID, MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        } ?: return mutableListOf()

        val dataPath = appDataPath()

        return buckets.mapNotNullTo(mutableListOf()) { (bucketId, folderName) ->
            val selection = bucketSelection(MediaStore.Video.Media.BUCKET_ID, bucketId)
            val fileCursor = ContentResolverHelper.queryResolver(
                contentResolver,
                VIDEOS_MEDIA_URI,
                FILE_PROJECTION,
                selection,
                sortColumn = MediaStore.Video.Media.DATE_TAKEN,
                sortDirection = ContentResolverHelper.SORT_DIRECTION_DESCENDING,
                limit = itemLimit
            )

            val filePaths = fileCursor?.use { it.readFilePaths(itemLimit) }.orEmpty()

            buildMediaFolder(MediaFolderType.VIDEO, folderName, filePaths, dataPath) {
                countFiles(contentResolver, VIDEOS_MEDIA_URI, selection)
            }
        }
    }

    private fun checkPermissions(activity: AppCompatActivity?) {
        if (activity == null || PermissionUtil.checkStoragePermission(activity.applicationContext)) {
            return
        }

        PermissionUtil.requestStoragePermissionIfNeeded(activity)
    }

    private fun isStorageAccessGranted(activity: AppCompatActivity?, getWithoutActivity: Boolean): Boolean =
        getWithoutActivity || (activity != null && PermissionUtil.checkStoragePermission(activity.applicationContext))

    private fun appDataPath(): String = MainApp.getStoragePath() + File.separator + MainApp.getDataFolder()

    private fun bucketSelection(bucketIdColumn: String, bucketId: String?): String =
        bucketIdColumn + SQL_EQUALS + bucketId

    /**
     * Since sdk 29 the media store no longer collapses rows per bucket, so folders have to be distinguished manually.
     * Callers do not rely on the folder order.
     */
    private fun Cursor.readBuckets(idColumn: String, displayNameColumn: String): Map<String?, String?> {
        if (!moveToFirst()) {
            return emptyMap()
        }

        val idIndex = getColumnIndexOrThrow(idColumn)
        val displayNameIndex = getColumnIndexOrThrow(displayNameColumn)
        val buckets = HashMap<String?, String?>()
        do {
            buckets[getString(idIndex)] = getString(displayNameIndex)
        } while (moveToNext())

        return buckets
    }

    private fun Cursor.readFilePaths(itemLimit: Int, isAccepted: (String) -> Boolean = { true }): List<String> {
        if (itemLimit <= 0 || !moveToFirst()) {
            return emptyList()
        }

        val dataIndex = getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        val filePaths = ArrayList<String>(minOf(itemLimit, count))
        var readRows = 0
        do {
            val filePath: String? = getString(dataIndex)
            if (filePath != null && isAccepted(filePath)) {
                filePaths.add(filePath)
            }
            readRows++
            // faulty android implementations may ignore the query limit, so it is enforced here as well
        } while (readRows < itemLimit && moveToNext())

        return filePaths
    }

    private fun buildMediaFolder(
        type: MediaFolderType,
        folderName: String?,
        filePaths: List<String>,
        dataPath: String,
        countFiles: () -> Long
    ): MediaFolder? {
        val absolutePath = filePaths
            .lastOrNull { it.lastIndexOf(PATH_SEPARATOR) > 0 }
            ?.substringBeforeLast(PATH_SEPARATOR)

        // folders within the Nextcloud app itself are not offered for auto upload
        if (absolutePath == null || absolutePath.startsWith(dataPath)) {
            return null
        }

        return MediaFolder().apply {
            this.type = type
            this.folderName = folderName
            this.filePaths = filePaths
            this.absolutePath = absolutePath
            this.numberOfFiles = countFiles()
        }
    }

    private fun countFiles(contentResolver: ContentResolver, uri: Uri, selection: String): Long =
        contentResolver.query(uri, FILE_PROJECTION, selection, null, null)?.use { it.count.toLong() } ?: 0L

    private fun isValidAndExistingFilePath(filePath: String): Boolean =
        filePath.lastIndexOf(PATH_SEPARATOR) > 0 && File(filePath).exists()
}
