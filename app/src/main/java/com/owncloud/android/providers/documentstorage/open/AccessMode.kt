/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.open

import android.os.ParcelFileDescriptor

internal data class AccessMode(val bits: Int) {

    val isReadOnly: Boolean
        get() = bits == ParcelFileDescriptor.MODE_READ_ONLY

    val isWriteOnly: Boolean
        get() = (bits and ParcelFileDescriptor.MODE_WRITE_ONLY) != 0

    val isTruncate: Boolean
        get() = (bits and ParcelFileDescriptor.MODE_TRUNCATE) != 0

    companion object {
        fun parse(mode: String?): AccessMode = AccessMode(ParcelFileDescriptor.parseMode(mode))
    }
}
