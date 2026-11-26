/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.storage.migration.model

import androidx.annotation.StringRes

sealed interface MigrationResult {
    data object Success : MigrationResult

    data class Failure(@StringRes val messageResId: Int) : MigrationResult
}
