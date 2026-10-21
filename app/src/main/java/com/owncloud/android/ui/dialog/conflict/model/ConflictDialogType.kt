/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.conflict.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface ConflictDialogType : Parcelable {
    val data: ConflictDialogData
    val dialogTitle: String?

    @Parcelize
    data class Offline(override val data: ConflictDialogData) : ConflictDialogType {
        override val dialogTitle: String?
            get() = null
    }

    @Parcelize
    data class Normal(override val dialogTitle: String, override val data: ConflictDialogData) : ConflictDialogType
}
