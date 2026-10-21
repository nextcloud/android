/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.scan

import android.content.ContentResolver
import android.net.Uri

sealed interface ScannedPdfUpload {
    data class Embedded(val localPath: String, val remoteFilePath: String) : ScannedPdfUpload

    data class External(val uri: Uri, val remoteFolderPath: String) : ScannedPdfUpload

    data object Invalid : ScannedPdfUpload

    companion object {
        fun from(pdfUri: Uri, remoteFolderPath: String, remoteFileName: String): ScannedPdfUpload {
            if (pdfUri.scheme != ContentResolver.SCHEME_FILE) {
                return External(pdfUri, remoteFolderPath)
            }

            val localPath = pdfUri.path ?: return Invalid
            return Embedded(localPath, remoteFolderPath + remoteFileName)
        }
    }
}
