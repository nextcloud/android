/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license

interface KeyStore {

	operator fun get(keyId: String): String?

	operator fun set(keyId: String, keyValue: String?)
}
