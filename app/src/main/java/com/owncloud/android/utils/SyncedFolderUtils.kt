/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2020 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils

import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import java.io.File
import java.util.Arrays

/**
 * Utility class with methods for processing synced folders.
 */
object SyncedFolderUtils {
    private val DISQUALIFIED_MEDIA_DETECTION_SOURCE = listOf(
        "cover.jpg",
        "cover.jpeg",
        "folder.jpg",
        "folder.jpeg"
    )
    private val DISQUALIFIED_MEDIA_DETECTION_FILE_SET: Set<String> = DISQUALIFIED_MEDIA_DETECTION_SOURCE.toSet()
    private val AUTO_QUALIFYING_FOLDER_TYPE_SET: Set<MediaFolderType> = setOf(MediaFolderType.CUSTOM)
    private const val THUMBNAIL_FOLDER_PREFIX = ".thumbnail"
    private const val THUMBNAIL_DATA_FILE_PREFIX = ".thumbdata"
    private const val SINGLE_FILE = 1

    /**
     * analyzes a given media folder if its content qualifies for the folder to be handled as a media folder.
     *
     * @param mediaFolder media folder to analyse
     * @return `true` if it qualifies as a media folder else `false`
     */
    fun isQualifyingMediaFolder(mediaFolder: MediaFolder?): Boolean {
        return when (mediaFolder) {
            null -> false
            else -> isQualifyingMediaFolder(mediaFolder.absolutePath, mediaFolder.type)
        }
    }

    /**
     * analyzes a given synced folder if its content qualifies for the folder to be handled as a media folder.
     *
     * @param syncedFolder synced folder to analyse
     * @return `true` if it qualifies as a media folder else `false`
     */
    fun isQualifyingMediaFolder(syncedFolder: SyncedFolder?): Boolean {
        return when (syncedFolder) {
            null -> false
            else -> isQualifyingMediaFolder(syncedFolder.localPath, syncedFolder.type)
        }
    }

    /**
     * analyzes a given folder based on a path-string and type if its content qualifies for the folder to be handled as
     * a media folder.
     *
     * @param folderPath String representation for a folder
     * @param folderType type of the folder
     * @return `true` if it qualifies as a media folder else `false`
     */
    fun isQualifyingMediaFolder(folderPath: String, folderType: MediaFolderType): Boolean {
        return when {
            // custom folders are always fine
            AUTO_QUALIFYING_FOLDER_TYPE_SET.contains(folderType) -> true
            // thumbnail folder
            !isQualifiedFolder(folderPath) -> false
            else -> {
                // filter media folders
                val files = getFileList(File(folderPath))
                when {
                    // no files
                    files.size < SINGLE_FILE -> false
                    // music album (just one cover-art image)
                    MediaFolderType.IMAGE == folderType -> containsQualifiedImages(files)
                    else -> true
                }
            }
        }
    }

    /**
     * check if folder is qualified for auto upload.
     *
     * @param folderPath the folder's path string
     * @return code>true if folder qualifies for auto upload else `false`
     */
    @JvmStatic
    fun isQualifiedFolder(folderPath: String?): Boolean {
        if (folderPath == null) {
            return false
        }
        val folder = File(folderPath)
        // check if folder starts with thumbnail prefix
        return folder.isDirectory && !folder.name.startsWith(THUMBNAIL_FOLDER_PREFIX)
    }

    /**
     * check if given list of files contains images that qualify as auto upload relevant files.
     *
     * @param files list of files
     * @return `true` if at least one files qualifies as auto upload relevant else `false`
     */
    private fun containsQualifiedImages(files: List<File>): Boolean {
        return files.any { isFileNameQualifiedForAutoUpload(it.name) && MimeTypeUtil.isImage(it) }
    }

    /**
     * check if given file is auto upload relevant files.
     *
     * @param fileName file name to be checked
     * @return `true` if the file qualifies as auto upload relevant else `false`
     */
    @JvmStatic
    fun isFileNameQualifiedForAutoUpload(fileName: String?): Boolean {
        return when {
            fileName != null -> {
                !DISQUALIFIED_MEDIA_DETECTION_FILE_SET.contains(fileName.lowercase()) &&
                    !fileName.startsWith(THUMBNAIL_DATA_FILE_PREFIX)
            }
            else -> false
        }
    }

    /**
     * return list of files for given folder.
     *
     * @param localFolder folder to scan
     * @return sorted list of folder of given folder
     */
    fun getFileList(localFolder: File): List<File> {
        var files = localFolder.listFiles { pathname: File -> !pathname.isDirectory }
        if (files != null) {
            Arrays.sort(files) { f1: File, f2: File -> f1.lastModified().compareTo(f2.lastModified()) }
        } else {
            files = arrayOf()
        }
        return files.toList()
    }
}
