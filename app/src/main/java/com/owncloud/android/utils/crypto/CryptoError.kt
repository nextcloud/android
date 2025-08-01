/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.crypto

sealed class CryptoError(message: String) : Exception(message) {
    class EmptyPrivateKey : CryptoError("Private key is empty")
    class InvalidPrivateKeyFormat : CryptoError("Invalid private key format, check IV DELIMITER")
    class SHA1Decryption(val reason: String) : CryptoError("Failed to decrypt private key")
}
