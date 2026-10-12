/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

enum class SendShareAction {
    SEND_NOW,
    DOWNLOAD_FIRST,
    DOWNLOAD_UNAVAILABLE;

    companion object {
        fun resolve(isDownloaded: Boolean, hostCanDownload: Boolean): SendShareAction = when {
            isDownloaded -> SEND_NOW
            hostCanDownload -> DOWNLOAD_FIRST
            else -> DOWNLOAD_UNAVAILABLE
        }
    }
}
