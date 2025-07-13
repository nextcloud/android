/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.events

/**
 * Event for set folder as encrypted/decrypted
 */
class EncryptionEvent(val localId: Long, val remoteId: String, val remotePath: String, val shouldBeEncrypted: Boolean)
