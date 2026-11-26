/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-FileCopyrightText: 2016 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.datastorage

import androidx.annotation.StringRes
import com.owncloud.android.R

class StoragePoint(
    val description: String,
    val path: String,
    val storageType: StorageType,
    val privacyType: PrivacyType
) : Comparable<StoragePoint> {

    enum class StorageType(@get:StringRes val labelResId: Int) {
        INTERNAL(R.string.storage_internal_storage),
        EXTERNAL(R.string.storage_external_storage)
    }

    enum class PrivacyType {
        PRIVATE,
        PUBLIC
    }

    override fun compareTo(other: StoragePoint): Int = path.compareTo(other.path)
}
