/*
 * NextCloud Android client application
 *
 * @copyright Copyright (C) 2019 Daniele Fognini <dfogni@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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

    private fun compareUploadStatus(upload1: OCUpload, upload2: OCUpload): Int {
        return upload1.fixedUploadStatus.compareTo(upload2.fixedUploadStatus)
    }

    private fun compareUploadingNow(upload1: OCUpload, upload2: OCUpload): Int {
        return upload2.isFixedUploadingNow.compareTo(upload1.isFixedUploadingNow)
    }

    private fun compareUpdateTime(upload1: OCUpload, upload2: OCUpload): Int {
        return upload2.fixedUploadEndTimeStamp.compareTo(upload1.fixedUploadEndTimeStamp)
    }

    private fun compareUploadId(upload1: OCUpload, upload2: OCUpload): Int {
        return upload1.fixedUploadId.compareTo(upload2.fixedUploadId)
    }
}
