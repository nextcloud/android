/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

interface E2eeVaultSecretCipher {
    fun encrypt(accountName: String, plaintext: ByteArray): E2eeVaultEncryptedPayload

    fun decrypt(accountName: String, payload: E2eeVaultEncryptedPayload): ByteArray

    fun deleteKey(accountName: String)
}
