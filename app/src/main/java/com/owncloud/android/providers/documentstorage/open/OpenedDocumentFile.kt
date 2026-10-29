/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.open

import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal data class OpenedDocumentFile(val localFile: File, val descriptorFile: File) {

    private val isCopy: Boolean
        get() = descriptorFile != localFile

    @Throws(IOException::class)
    fun moveCopyToLocalFile() {
        if (!isCopy) {
            return
        }

        Files.move(descriptorFile.toPath(), localFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    fun deleteCopy() {
        if (isCopy && !descriptorFile.delete()) {
            Log_OC.w(TAG, "Copy could not be deleted: ${descriptorFile.absolutePath}")
        }
    }

    companion object {
        private val TAG = OpenedDocumentFile::class.java.simpleName
    }
}
