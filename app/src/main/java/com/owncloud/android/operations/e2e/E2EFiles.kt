/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import com.owncloud.android.datamodel.OCFile
import java.io.File

data class E2EFiles(
    var parentFile: OCFile,
    var temporalFile: File?,
    var originalFile: File?,
    var expectedFile: File?,
    var encryptedTempFile: File?
)
