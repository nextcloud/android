/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.parcel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface ConflictDialogType : Parcelable {
    val data: ConflictDialogData

    @Parcelize
    data class Offline(override val data: ConflictDialogData) : ConflictDialogType

    @Parcelize
    data class Normal(override val data: ConflictDialogData) : ConflictDialogType
}

@Parcelize
data class ConflictDialogData(
    val dialogTitle: String?,
    val headline: String?,
    val description: String,
    val localFile: ConflictFileData,
    val serverFile: ConflictFileData
) : Parcelable

@Parcelize
data class ConflictFileData(val title: String, val timestamp: String, val fileSize: String) : Parcelable
