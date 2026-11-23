/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.helpers.uriuploader

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.owncloud.android.R
import java.io.File
import java.nio.file.Path

class UriChecker(private val context: Context) {

    private val privateFolders: List<Path> by lazy {
        buildList {
            add(context.dataDir)
            add(context.createDeviceProtectedStorageContext().dataDir)
            addAll(context.getExternalFilesDirs(null).filterNotNull().mapNotNull { it.parentFile })
            addAll(context.externalMediaDirs.filterNotNull())
        }.map { it.canonicalFile.toPath() }
    }

    fun pointsToAppPrivateData(uri: Uri): Boolean = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> isPrivateContentProvider(uri)
        ContentResolver.SCHEME_FILE -> isInPrivateFolder(uri)
        else -> false
    }

    private fun isPrivateContentProvider(uri: Uri): Boolean {
        val authority = uri.authority ?: return true
        if (authority == context.getString(R.string.document_provider_authority)) return false
        val provider = context.packageManager.resolveContentProvider(authority, NO_FLAGS)

        return provider?.packageName == context.packageName
    }

    private fun isInPrivateFolder(uri: Uri): Boolean {
        val path = uri.path ?: return true
        val canonicalPath = File(path).canonicalFile.toPath()

        return privateFolders.any { canonicalPath.startsWith(it) }
    }

    companion object {
        private const val NO_FLAGS = 0
    }
}
