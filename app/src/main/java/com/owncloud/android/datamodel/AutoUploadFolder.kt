/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.extensions.calculateScanInterval
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File
import java.io.Serializable

class AutoUploadFolder(
    var id: Long,
    var localPath: String?,
    var remotePath: String?,
    var isWifiOnly: Boolean,
    var isChargingOnly: Boolean,
    var alsoUploadExistingFiles: Boolean,
    var isSubfolderByDate: Boolean,
    var account: String?,
    var uploadAction: Int,
    var nameCollisionPolicyInt: Int,
    enabled: Boolean,
    timestampMs: Long,
    var type: MediaFolderType,
    var isHidden: Boolean,
    var subfolderRule: SubFolderRule,
    var isExcludeHidden: Boolean,
    var lastScanTimestampMs: Long,
    var filePaths: List<String>? = null,
    var folderName: String? = null,
    var numberOfFiles: Long = 0
) : Serializable,
    Cloneable {

    var isEnabled: Boolean = false
        private set

    var enabledTimestampMs: Long = EMPTY_ENABLED_TIMESTAMP_MS
        private set

    val nameCollisionPolicy: NameCollisionPolicy
        get() = NameCollisionPolicy.deserialize(nameCollisionPolicyInt)

    init {
        setEnabled(enabled, timestampMs)
    }

    fun setEnabled(enabled: Boolean, timestampMs: Long) {
        isEnabled = enabled
        enabledTimestampMs = if (enabled) timestampMs else EMPTY_ENABLED_TIMESTAMP_MS
    }

    public override fun clone(): AutoUploadFolder = super.clone() as AutoUploadFolder

    fun isFileInFolderWithCorrectMediaType(file: File, filePath: String?): Boolean {
        val folderPath = localPath
        if (filePath.isNullOrEmpty() || folderPath.isNullOrEmpty()) return false

        val normalizedFolderPath = if (folderPath.endsWith(File.separator)) {
            folderPath
        } else {
            folderPath + File.separator
        }

        return filePath.startsWith(normalizedFolderPath) && hasMatchingMediaType(file)
    }

    fun getTotalScanInterval(
        connectivityService: ConnectivityService,
        powerManagementService: PowerManagementService
    ): Long = lastScanTimestampMs + calculateScanInterval(connectivityService, powerManagementService).first

    private fun hasMatchingMediaType(file: File): Boolean = when (type) {
        MediaFolderType.IMAGE -> MimeTypeUtil.isImage(file)
        MediaFolderType.VIDEO -> MimeTypeUtil.isVideo(file)
        MediaFolderType.CUSTOM -> true
    }

    companion object {
        const val UNPERSISTED_ID = Long.MIN_VALUE
        const val EMPTY_ENABLED_TIMESTAMP_MS = -1L
        const val NOT_SCANNED_YET = -1L
        private const val serialVersionUID = -793476118299906429L
    }
}
