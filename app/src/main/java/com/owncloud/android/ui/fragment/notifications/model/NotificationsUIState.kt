/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.notifications.model

import com.owncloud.android.lib.resources.notifications.models.Notification

sealed class NotificationsUIState {
    data object Loading : NotificationsUIState()
    data object Empty : NotificationsUIState()
    data class Loaded(val items: List<Notification>) : NotificationsUIState()
    data class Error(val message: String?) : NotificationsUIState()
}
