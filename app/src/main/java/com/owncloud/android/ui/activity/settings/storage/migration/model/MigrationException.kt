/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.storage.migration.model

import androidx.annotation.StringRes

class MigrationException(@StringRes val messageResId: Int) : Exception() {
    companion object {
        private const val serialVersionUID = -4575848188034992066L
    }
}
