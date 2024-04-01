/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.Serializable

@Suppress("TopLevelPropertyNaming")
private const val tag = "BundleExtension"

fun <T : Serializable?> Bundle?.getSerializableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getSerializable(key, type)
        } else {
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            if (type.isInstance(this.getSerializable(key))) {
                this.getSerializable(key) as T
            } else {
                null
            }
        }
    } catch (e: ClassCastException) {
        Log_OC.e(tag, e.localizedMessage)
        null
    }
}

fun <T : Parcelable?> Bundle?.getParcelableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getParcelable(key, type)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelable(key)
        }
    } catch (e: ClassCastException) {
        Log_OC.e(tag, e.localizedMessage)
        e.printStackTrace()
        null
    }
}
