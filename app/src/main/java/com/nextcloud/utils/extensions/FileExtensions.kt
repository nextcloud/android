/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.DisplayUtils
import java.io.File

fun OCFile?.logFileSize(tag: String) {
    val size = DisplayUtils.bytesToHumanReadable(this?.fileLength ?: -1)
    val rawByte = this?.fileLength ?: -1
    Log_OC.d(tag, "onSaveInstanceState: $size, raw byte $rawByte")
}

fun File?.logFileSize(tag: String) {
    val size = DisplayUtils.bytesToHumanReadable(this?.length() ?: -1)
    val rawByte = this?.length() ?: -1
    Log_OC.d(tag, "onSaveInstanceState: $size, raw byte $rawByte")
}
