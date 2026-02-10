/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.provider.MediaStore
import androidx.core.net.toUri
import com.nextcloud.utils.extensions.toLocalPath
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

@Suppress("TooGenericExceptionCaught", "MagicNumber", "ReturnCount")
class AutoUploadHelper {
    companion object {
        private const val TAG = "AutoUploadHelper"
        private const val MAX_DEPTH = 100
    }

    fun insertEntries(folder: SyncedFolder, repository: FileSystemRepository) {
        when (folder.type) {
            MediaFolderType.IMAGE -> {
                repository.insertFromUri(MediaStore.Images.Media.INTERNAL_CONTENT_URI, folder)
                repository.insertFromUri(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, folder)
            }

            MediaFolderType.VIDEO -> {
                repository.insertFromUri(MediaStore.Video.Media.INTERNAL_CONTENT_URI, folder)
                repository.insertFromUri(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, folder)
            }

            else -> {
                insertCustomFolderIntoDB(folder, repository)
            }
        }
    }

    /**
     * Attempts to get the file path from a content URI string (e.g., content://media/external/images/media/2281)
     * and checks its type. If the conditions are met, the file is stored for auto-upload.
     * <p>
     * If any attempt fails, the method returns {@code false}.
     *
     * @param syncedFolder The folder marked for auto-upload.
     * @param contentUris  An array of content URI strings collected from
     * {@link ContentObserverWork##checkAndTriggerAutoUpload()}.
     * @return {@code true} if all changed content URIs were successfully stored; {@code false} otherwise.
     */
    fun insertChangedEntries(
        syncedFolder: SyncedFolder,
        contentUris: Array<String>?,
        repository: FileSystemRepository
    ): Boolean {
        contentUris?.forEach { uriString ->
            try {
                val uri = uriString.toUri()
                repository.insertFromUri(uri, syncedFolder, true)
            } catch (e: Exception) {
                Log_OC.e(TAG, "Invalid URI: $uriString", e)
                return false
            }
        }

        Log_OC.d(TAG, "Changed content URIs successfully stored")

        return true
    }

    fun insertCustomFolderIntoDB(folder: SyncedFolder, repository: FileSystemRepository): Int {
        val path = Paths.get(folder.localPath)

        if (!Files.exists(path)) {
            Log_OC.w(TAG, "Folder does not exist: ${folder.localPath}")
            return 0
        }

        if (!Files.isReadable(path)) {
            Log_OC.w(TAG, "Folder is not readable: ${folder.localPath}")
            return 0
        }

        val excludeHidden = folder.isExcludeHidden

        var fileCount = 0
        var skipCount = 0
        var errorCount = 0

        try {
            Files.walkFileTree(
                path,
                setOf(FileVisitOption.FOLLOW_LINKS),
                MAX_DEPTH,
                object : SimpleFileVisitor<Path>() {

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult {
                        if (excludeHidden && dir != path && dir.toFile().isHidden) {
                            Log_OC.d(TAG, "Skipping hidden directory: ${dir.fileName}")
                            skipCount++
                            return FileVisitResult.SKIP_SUBTREE
                        }

                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                        try {
                            val javaFile = file.toFile()
                            val lastModified = attrs?.lastModifiedTime()?.toMillis() ?: javaFile.lastModified()
                            val creationTime = attrs?.creationTime()?.toMillis()
                            val localPath = file.toLocalPath()

                            repository.insertOrReplace(localPath, lastModified, creationTime, folder)

                            fileCount++

                            if (fileCount % 100 == 0) {
                                Log_OC.d(TAG, "Processed $fileCount files so far...")
                            }
                        } catch (e: Exception) {
                            Log_OC.e(TAG, "Error processing file: $file", e)
                            errorCount++
                        }

                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                        when (exc) {
                            is AccessDeniedException -> {
                                Log_OC.w(TAG, "Access denied: $file")
                            }

                            else -> {
                                Log_OC.e(TAG, "Failed to visit file: $file", exc)
                            }
                        }
                        errorCount++
                        return FileVisitResult.CONTINUE
                    }

                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        if (exc != null) {
                            Log_OC.e(TAG, "Error after visiting directory: $dir", exc)
                            errorCount++
                        }
                        return FileVisitResult.CONTINUE
                    }
                }
            )

            Log_OC.d(
                TAG,
                "Scan complete for ${folder.localPath}: " +
                    "$fileCount files processed, $skipCount skipped, $errorCount errors"
            )
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error walking file tree: ${folder.localPath}", e)
        }

        return fileCount
    }
}
