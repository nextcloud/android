/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.os.Parcel
import android.os.Parcelable

inline fun <reified T : Parcelable> Parcel.readParcelableCompat(classLoader: ClassLoader?): T? {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        readParcelable(classLoader, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        readParcelable(classLoader)
    }
}
