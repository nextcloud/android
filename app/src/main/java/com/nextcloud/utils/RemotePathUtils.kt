/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.owncloud.android.datamodel.OCFile

/**
 * Utility functions for working with remote file paths in Nextcloud.
 *
 * Remote paths follow the convention of using "/" as the separator,
 * with folders ending in "/" and the root being "/".
 */
object RemotePathUtils {

    /**
     * Extracts the filename (last segment) from a remote path.
     *
     * @param remotePath The full remote path.
     * @return The filename, or empty string for root path.
     */
    fun getFilename(remotePath: String): String {
        if (remotePath.isEmpty() || remotePath == OCFile.PATH_SEPARATOR) {
            return ""
        }

        val trimmed = remotePath.trimEnd('/')
        val lastSlash = trimmed.lastIndexOf('/')
        return if (lastSlash >= 0) {
            trimmed.substring(lastSlash + 1)
        } else {
            trimmed
        }
    }

    /**
     * Extracts the parent path from a remote path.
     *
     * @param remotePath The full remote path.
     * @return The parent path ending with "/", or "/" for root-level items.
     */
    fun getParentPath(remotePath: String): String {
        if (remotePath.isEmpty() || remotePath == OCFile.PATH_SEPARATOR) {
            return OCFile.PATH_SEPARATOR
        }

        val trimmed = remotePath.trimEnd('/')
        val lastSlash = trimmed.lastIndexOf('/')
        return if (lastSlash >= 0) {
            trimmed.substring(0, lastSlash + 1)
        } else {
            OCFile.PATH_SEPARATOR
        }
    }

    /**
     * Returns the file extension from a remote path, or empty string if none.
     *
     * @param remotePath The remote path.
     * @return The extension without the leading dot, or empty string.
     */
    fun getExtension(remotePath: String): String {
        val filename = getFilename(remotePath)
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < filename.length - 1) {
            filename.substring(dotIndex + 1)
        } else {
            ""
        }
    }

    /**
     * Computes the depth of a remote path (number of segments).
     * Root "/" has depth 0, "/folder/" has depth 1, "/folder/file.txt" has depth 2.
     *
     * @param remotePath The remote path.
     * @return The depth of the path.
     */
    fun getDepth(remotePath: String): Int {
        if (remotePath.isEmpty() || remotePath == OCFile.PATH_SEPARATOR) {
            return 0
        }

        val trimmed = remotePath.trim('/')
        return if (trimmed.isEmpty()) 0 else trimmed.count { it == '/' } + 1
    }

    /**
     * Joins a parent path and a child name into a proper remote path.
     *
     * @param parentPath The parent directory path.
     * @param childName The name of the child file or folder.
     * @return The joined path.
     */
    fun joinPath(parentPath: String, childName: String): String {
        val normalizedParent = if (parentPath.endsWith("/")) parentPath else "$parentPath/"
        val normalizedChild = childName.trimStart('/')
        return "$normalizedParent$normalizedChild"
    }

    /**
     * Normalizes a remote path by removing duplicate slashes and ensuring
     * it starts with "/".
     *
     * @param remotePath The path to normalize.
     * @return The normalized path.
     */
    fun normalizePath(remotePath: String): String {
        if (remotePath.isBlank()) return OCFile.PATH_SEPARATOR

        val collapsed = remotePath.replace(Regex("/+"), "/")
        return if (collapsed.startsWith("/")) collapsed else "/$collapsed"
    }

    /**
     * Checks whether the given remote path represents a folder (ends with "/").
     *
     * @param remotePath The path to check.
     * @return true if it's a folder path.
     */
    fun isFolderPath(remotePath: String): Boolean = remotePath.endsWith("/")

    /**
     * Returns all ancestor paths of the given remote path, from root to direct parent.
     *
     * @param remotePath The remote path.
     * @return List of ancestor paths, each ending with "/".
     */
    fun getAncestorPaths(remotePath: String): List<String> {
        if (remotePath.isEmpty() || remotePath == OCFile.PATH_SEPARATOR) {
            return emptyList()
        }

        val ancestors = mutableListOf<String>()
        var current = getParentPath(remotePath)

        while (current != OCFile.PATH_SEPARATOR) {
            ancestors.add(0, current)
            current = getParentPath(current)
        }
        ancestors.add(0, OCFile.PATH_SEPARATOR)

        return ancestors
    }
}
