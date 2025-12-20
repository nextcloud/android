/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.lib.resources.assistant.chat.model.ChatMessage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.ExperimentalTime

fun ChatMessage.isHuman(): Boolean = (role == "human")

@OptIn(ExperimentalTime::class)
fun ChatMessage.time(): String {
    val messageDate = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now(ZoneId.systemDefault())

    val pattern = if (messageDate == today) "HH:mm" else "dd.MM.yyyy - HH:mm"

    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    val messageTime = Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()

    return formatter.format(messageTime)
}
