/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel.e2e.v2.decrypted

data class DecryptedFile(
    var filename: String,
    val mimetype: String,
    val nonce: String,
    val authenticationTag: String,
    val key: String
)
