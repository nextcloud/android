/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

data class EncryptedFiledrop(
    val encrypted: String,
    val initializationVector: String,
    val authenticationTag: String,
    val encryptedKey: String,
    val encryptedTag: String,
    val encryptedInitializationVector: String
)
