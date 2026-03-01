/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import java.io.Serializable

inline fun <reified T : Parcelable> Parcel?.readParcelableCompat(classLoader: ClassLoader?): T? {
    if (this == null) {
        return null
    }

    return ParcelCompat.readParcelable(this, classLoader, T::class.java)
}

inline fun <reified T : Serializable> Parcel?.readSerializableCompat(classLoader: ClassLoader?): T? {
    if (this == null) {
        return null
    }

    return ParcelCompat.readSerializable(this, classLoader, T::class.java) as T?
}
