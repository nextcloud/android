/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.notifications

import android.app.NotificationManager
import androidx.annotation.StringRes

data class NotificationChannelSpec(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val importance: Int = NotificationManager.IMPORTANCE_LOW
)
