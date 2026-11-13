/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.model

sealed interface TeamResourceTarget {
    data class Folder(val remotePath: String) : TeamResourceTarget

    data class File(val fileId: String) : TeamResourceTarget

    data class TalkConversation(val token: String, val webPage: WebPage) : TeamResourceTarget

    data class WebPage(val url: String, val title: String) : TeamResourceTarget
}
