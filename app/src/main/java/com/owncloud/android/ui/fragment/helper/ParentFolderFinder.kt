/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.helper

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC

@Suppress("ReturnCount")
class ParentFolderFinder {
    companion object {
        private const val TAG = "ParentFolderFinder"
    }

    /**
     * User tries to move up but parent folder was deleted thus parent of parent
     * will be used as destination else ROOT directory
     */
    fun getParentOnFirstParentRemoved(path: String?, storageManager: FileDataStorageManager?): OCFile? {
        if (storageManager == null) {
            Log_OC.e(TAG, "StorageManager is null")
            return null
        }

        if (path.isNullOrEmpty() || path == OCFile.ROOT_PATH) {
            Log_OC.w(TAG, "Path is null, empty, or already at root. Falling back to ROOT.")
            return storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
        }

        return walkUpByPath(path, storageManager).second
    }

    fun getParent(file: OCFile?, storageManager: FileDataStorageManager?): Pair<Int, OCFile?> {
        if (file == null || file.isRootDirectory) {
            Log_OC.e(TAG, "File is null or already at root")
            return 0 to file
        }

        if (storageManager == null) {
            Log_OC.e(TAG, "StorageManager is null")
            return 0 to file
        }

        // id based lookup
        val parentId = file.parentId
        if (parentId > FileDataStorageManager.ROOT_PARENT_ID && parentId != file.fileId) {
            storageManager.getFileById(parentId)?.let { parentById ->
                if (parentById.isFolder) {
                    return 1 to parentById
                }
            }

            Log_OC.w(TAG, "ID lookup missed for parentId=$parentId, falling back to path walk")
        }

        // path-based walk up
        return walkUpByPath(file.remotePath, storageManager)
    }

    /**
     * Walks the remote path upward one segment at a time until a valid folder
     * is found in the DB, or we reach root.
     */
    private fun walkUpByPath(initialPath: String?, storageManager: FileDataStorageManager): Pair<Int, OCFile?> {
        var path = initialPath
        var moveCount = 0

        while (!path.isNullOrEmpty() && path != OCFile.ROOT_PATH) {
            val parentPath = resolveParentPath(path)
            moveCount++

            if (parentPath == null) {
                Log_OC.w(TAG, "Failed to resolve parent path for $path, fallback to root")
                break
            }

            // Check if this resolved parent exists in DB
            storageManager.getFileByEncryptedRemotePath(parentPath)?.let { candidate ->
                if (candidate.isFolder) {
                    return moveCount to candidate
                }
            }

            if (parentPath == OCFile.ROOT_PATH) {
                Log_OC.w(TAG, "Root path reached but not found in DB loop, forcing fallback")
                break
            }

            // Move up one more level for the next iteration
            path = parentPath
        }

        // fallback to root
        val root = storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
        return moveCount to root
    }

    /**
     * Returns the parent path of `path` with a guaranteed trailing "/",
     * or `null` if `path` is already root or invalid.
     *
     *
     * Correctly handles paths with or without a trailing separator, so both
     * "/Foo/Bar" and "/Foo/Bar/" return "/Foo/".
     */
    private fun resolveParentPath(path: String?): String? {
        if (path.isNullOrEmpty() || path == OCFile.ROOT_PATH) {
            return null
        }

        // Strip trailing "/" before computing parent
        val normalized = if (path.endsWith(OCFile.PATH_SEPARATOR)) {
            path.substring(0, path.length - 1)
        } else {
            path
        }

        val lastSep = normalized.lastIndexOf(OCFile.PATH_SEPARATOR)
        if (lastSep <= 0) {
            Log_OC.w(TAG, "No separator found, or only one at the start")
            return OCFile.ROOT_PATH
        }

        // e.g. "/Foo/Bar" → "/Foo/"
        return normalized.substring(0, lastSep + 1)
    }
}
