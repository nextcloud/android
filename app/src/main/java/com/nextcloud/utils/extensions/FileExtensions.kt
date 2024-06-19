/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File

fun OCFile?.logFileSize(tag: String) = Log_OC.d(tag, "onSaveInstanceState: ${this?.fileLength}")

fun File?.logFileSize(tag: String) = Log_OC.d(tag, "onSaveInstanceState: ${this?.length()}")
