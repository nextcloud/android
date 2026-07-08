/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import java.time.*
import java.time.temporal.Temporal

fun Temporal?.toInstant(): Instant = when (this) {
    is Instant -> this
    is ZonedDateTime -> toInstant()
    is OffsetDateTime -> toInstant()
    is LocalDateTime -> atZone(ZoneOffset.UTC).toInstant()
    is LocalDate -> atStartOfDay(ZoneOffset.UTC).toInstant()
    else -> throw IllegalArgumentException("Unsupported temporal type")
}

fun Temporal?.toEpochMilli(): Long = toInstant().toEpochMilli()
