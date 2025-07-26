/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Daniele Fognini <dfogni@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.owncloud.android.db

/**
 *  Sorts OCUpload by (uploadStatus, uploadingNow, uploadEndTimeStamp, uploadId).
 */
class OCUploadComparator : Comparator<OCUpload?> {
    @Suppress("ReturnCount")
    override fun compare(upload1: OCUpload?, upload2: OCUpload?): Int {
        if (upload1 == null && upload2 == null) {
            return 0
        }
        if (upload1 == null) {
            return -1
        }
        if (upload2 == null) {
            return 1
        }

        val compareUploadStatus = compareUploadStatus(upload1, upload2)
        if (compareUploadStatus != 0) {
            return compareUploadStatus
        }

        val compareUploadingNow = compareUploadingNow(upload1, upload2)
        if (compareUploadingNow != 0) {
            return compareUploadingNow
        }

        val compareUpdateTime = compareUpdateTime(upload1, upload2)
        if (compareUpdateTime != 0) {
            return compareUpdateTime
        }

        val compareUploadId = compareUploadId(upload1, upload2)
        if (compareUploadId != 0) {
            return compareUploadId
        }

        return 0
    }

    private fun compareUploadStatus(upload1: OCUpload, upload2: OCUpload): Int =
        upload1.fixedUploadStatus.compareTo(upload2.fixedUploadStatus)

    private fun compareUploadingNow(upload1: OCUpload, upload2: OCUpload): Int =
        upload2.isFixedUploadingNow.compareTo(upload1.isFixedUploadingNow)

    private fun compareUpdateTime(upload1: OCUpload, upload2: OCUpload): Int =
        upload2.fixedUploadEndTimeStamp.compareTo(upload1.fixedUploadEndTimeStamp)

    private fun compareUploadId(upload1: OCUpload, upload2: OCUpload): Int =
        upload1.fixedUploadId.compareTo(upload2.fixedUploadId)
}
