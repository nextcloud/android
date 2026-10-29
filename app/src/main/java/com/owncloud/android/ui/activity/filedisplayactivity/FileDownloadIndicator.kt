/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.filedisplayactivity

import androidx.annotation.DrawableRes
import com.owncloud.android.R

enum class FileDownloadIndicator(@param:DrawableRes val iconId: Int) {
    Downloading(R.drawable.ic_synchronizing),
    Downloaded(R.drawable.ic_synced)
}
