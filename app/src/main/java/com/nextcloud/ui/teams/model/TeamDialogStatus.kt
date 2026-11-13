/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.model

import androidx.annotation.StringRes

sealed interface TeamDialogStatus {
    data object Editing : TeamDialogStatus

    data object Submitting : TeamDialogStatus

    data class Failed(@StringRes val messageId: Int) : TeamDialogStatus
}
