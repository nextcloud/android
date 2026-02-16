/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Axel
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.widget.photo

/**
 * Holds per-widget configuration for the Photo Widget.
 *
 * @param intervalMinutes  Refresh interval in minutes. 0 means manual-only (no auto-refresh).
 */
data class PhotoWidgetConfig(
    val widgetId: Int,
    val folderPath: String,
    val accountName: String,
    val intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES
) {
    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 15L
        val INTERVAL_OPTIONS = longArrayOf(5L, 15L, 30L, 60L, 0L) // 0 = manual
    }
}
