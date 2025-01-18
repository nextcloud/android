/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import java.io.File

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
    private const val SINGLE_FILE = 1
    private val EXCLUDE_PREFIXES = listOf(
        ".thumbnail",
        ".thumbdata",
        ".trashed",
        ".pending",
        ".nomedia"
    )

    /**
     * analyzes a given media folder if its content qualifies for the folder to be handled as a media folder.
     *
     * @param mediaFolder media folder to analyse
     * @return `true` if it qualifies as a media folder else `false`
     */
    fun isQualifyingMediaFolder(mediaFolder: MediaFolder?): Boolean {
        return when {
            mediaFolder == null -> false
            AUTO_QUALIFYING_FOLDER_TYPE_SET.contains(mediaFolder.type) -> true
            !isQualifiedFolder(mediaFolder.absolutePath) -> false
            else -> {
                when {
                    mediaFolder.numberOfFiles < SINGLE_FILE -> false
                    // music album (just one cover-art image)
                    mediaFolder.type == MediaFolderType.IMAGE -> containsQualifiedImages(
                        mediaFolder.filePaths.map { File(it) }
                    )
                    else -> true
                }
            }
        }
    }

    /**
     * analyzes a given synced folder if its content qualifies for the folder to be handled as a media folder.
     *
     * @param syncedFolder synced folder to analyse
     * @return `true` if it qualifies as a media folder else `false`
     */
    fun isQualifyingMediaFolder(syncedFolder: SyncedFolder?): Boolean {
        if (syncedFolder == null) {
            return false
        }

        return isQualifyingMediaFolder(syncedFolder.localPath, syncedFolder.type)
    }

    /**
     * analyzes a given folder based on a path-string and type if its content qualifies for the folder to be handled as
     * a media folder.
     *
     * @param folderPath String representation for a folder
     * @param folderType type of the folder
     * @return `true` if it qualifies as a media folder else `false`
     */
    fun isQualifyingMediaFolder(folderPath: String?, folderType: MediaFolderType): Boolean {
        return when {
            AUTO_QUALIFYING_FOLDER_TYPE_SET.contains(folderType) -> true
            !isQualifiedFolder(folderPath) -> false
            folderPath == null -> false
            else -> {
                val files: List<File> = getFileList(File(folderPath))
                when {
                    // no files
                    files.size < SINGLE_FILE -> false
                    // music album (just one cover-art image)
                    folderType == MediaFolderType.IMAGE -> containsQualifiedImages(files)
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
        return folder.isDirectory && !hasExcludePrefix(folder.name)
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
                    !hasExcludePrefix(fileName)
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
        val files: Array<File> = localFolder.listFiles { pathname: File -> !pathname.isDirectory } ?: return emptyList()
        return files
            .map { Pair(it, it.lastModified()) }
            .sortedBy { it.second }
            .map { it.first }
    }

    /**
     * check if file or folder name has prefix in the list to exclude from auto upload
     *
     * @param name name of file to check
     * @return `true` if file has one of the prefixes in EXCLUDE_FILENAME_PREFIXES
     */
    fun hasExcludePrefix(name: String?): Boolean {
        val hasPrefix = false
        if (name != null) {
            EXCLUDE_PREFIXES.forEach {
                if (name.startsWith(it)) {
                    return true
                }
            }
        }
        return false
    }
}
