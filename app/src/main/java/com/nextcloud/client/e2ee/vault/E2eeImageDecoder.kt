/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.graphics.Bitmap

interface E2eeImageDecoder {
    fun decode(bytes: ByteArray, requestedWidth: Int, requestedHeight: Int): Bitmap?
}
