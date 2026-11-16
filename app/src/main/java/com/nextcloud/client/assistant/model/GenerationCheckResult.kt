/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.model

import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage

sealed class GenerationCheckResult {
    data class Completed(val message: ChatMessage) : GenerationCheckResult()
    data object Pending : GenerationCheckResult()
    data object Failed : GenerationCheckResult()
}
