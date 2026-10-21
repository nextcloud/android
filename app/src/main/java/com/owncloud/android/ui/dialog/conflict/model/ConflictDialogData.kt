/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.conflict.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConflictDialogData(
    val headline: String,
    val description: String,
    val localFile: ConflictFileData,
    val serverFile: ConflictFileData
) : Parcelable
