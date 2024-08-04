/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.test.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
class OtherTestData : Parcelable

data class TestData(val message: String) : Serializable

data class TestDataParcelable(val message: String) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString() ?: "")

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(message)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TestDataParcelable> {
        override fun createFromParcel(parcel: Parcel): TestDataParcelable {
            return TestDataParcelable(parcel)
        }

        override fun newArray(size: Int): Array<TestDataParcelable?> {
            return arrayOfNulls(size)
        }
    }
}
