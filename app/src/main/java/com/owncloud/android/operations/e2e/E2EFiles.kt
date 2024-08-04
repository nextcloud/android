/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File

data class E2EFiles(
    var parentFile: OCFile,
    var temporalFile: File?,
    var originalFile: File,
    var expectedFile: File?,
    var encryptedTempFile: File?
) {
    private val tag = "E2EFiles"

    fun deleteTemporalFile() {
        if (temporalFile?.exists() == true && temporalFile?.delete() == false) {
            Log_OC.e(tag, "Could not delete temporal file " + temporalFile?.absolutePath)
        }
    }

    fun deleteTemporalFileWithOriginalFileComparison() {
        if (originalFile == temporalFile) {
            return
        }

        val isTemporalFileDeleted = temporalFile?.delete()
        Log_OC.d(tag, "isTemporalFileDeleted: $isTemporalFileDeleted")
    }

    fun deleteEncryptedTempFile() {
        if (encryptedTempFile != null) {
            val isTempEncryptedFileDeleted = encryptedTempFile?.delete()
            Log_OC.e(tag, "isTempEncryptedFileDeleted: $isTempEncryptedFileDeleted")
        } else {
            Log_OC.e(tag, "Encrypted temp file cannot be found")
        }
    }
}
