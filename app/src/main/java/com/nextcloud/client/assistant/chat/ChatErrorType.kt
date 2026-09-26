/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.assistant.chat

sealed class ChatErrorType {
    data object LoadMessages : ChatErrorType()
    data object SendMessage : ChatErrorType()
    data object GenerateResponse : ChatErrorType()
}
