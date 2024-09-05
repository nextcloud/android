/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.parcel

import android.os.Parcel
import android.os.Parcelable
import com.nextcloud.utils.extensions.readParcelableCompat

data class ConflictDialogData(
    val folderName: String?,
    val title: String?,
    val description: String,
    val checkboxData: Pair<ConflictFileData, ConflictFileData>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        checkboxData = Pair(
            parcel.readParcelableCompat(ConflictFileData::class.java.classLoader) ?: ConflictFileData("", "", ""),
            parcel.readParcelableCompat(ConflictFileData::class.java.classLoader) ?: ConflictFileData("", "", "")
        )
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(folderName)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeParcelable(checkboxData.first, flags)
        parcel.writeParcelable(checkboxData.second, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ConflictDialogData> {
        override fun createFromParcel(parcel: Parcel): ConflictDialogData = ConflictDialogData(parcel)
        override fun newArray(size: Int): Array<ConflictDialogData?> = arrayOfNulls(size)
    }
}

data class ConflictFileData(
    val title: String,
    val timestamp: String,
    val fileSize: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(timestamp)
        parcel.writeString(fileSize)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ConflictFileData> {
        override fun createFromParcel(parcel: Parcel): ConflictFileData = ConflictFileData(parcel)
        override fun newArray(size: Int): Array<ConflictFileData?> = arrayOfNulls(size)
    }
}
