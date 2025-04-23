/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.IntentCompat
import java.io.Serializable

fun <T : Serializable?> Intent?.getSerializableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return IntentCompat.getSerializableExtra(this, key, type)
}

fun <T : Parcelable?> Intent?.getParcelableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return IntentCompat.getParcelableExtra(this, key, type)
}
