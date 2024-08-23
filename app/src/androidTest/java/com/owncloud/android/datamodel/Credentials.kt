/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Öztürk <alper_ozturk@proton.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

data class Credentials(
    val publicKey: String,
    val certificate: String
)