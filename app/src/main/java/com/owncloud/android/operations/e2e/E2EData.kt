/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFile

data class E2EData(
    val key: ByteArray,
    val iv: ByteArray,
    val encryptedFile: EncryptedFile,
    val encryptedFileName: String
)
