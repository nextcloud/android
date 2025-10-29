/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import com.nextcloud.utils.extensions.shouldSkipFile
import com.nextcloud.utils.extensions.toLocalPath
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.FilesystemDataProvider
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

    fun insertCustomFolderIntoDB(
        folder: SyncedFolder,
        filesystemDataProvider: FilesystemDataProvider?,
        storageManager: FileDataStorageManager
    ): Int {
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

                            if (folder.shouldSkipFile(javaFile, lastModified)) {
                                skipCount++
                                return FileVisitResult.CONTINUE
                            }

                            val localPath = file.toLocalPath()

                            // Skip existing files on remote
                            if (folder.isExisting) {
                                val fileOnRemote = storageManager.getFileByLocalPath(localPath)
                                if (fileOnRemote != null) {
                                    Log_OC.d(
                                        TAG,
                                        "folder configured as skip existing files on remote" +
                                            "skipping file: ${fileOnRemote.remotePath}"
                                    )
                                    skipCount++
                                    return FileVisitResult.CONTINUE
                                }
                            }

                            filesystemDataProvider?.storeOrUpdateFileValue(
                                localPath,
                                lastModified,
                                javaFile.isDirectory,
                                folder
                            )

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
