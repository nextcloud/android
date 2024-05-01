/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import java.util.Date
import java.util.UUID

data class JobInfo(
    val id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    val state: String = "",
    val name: String = "",
    val user: String = "",
    val workerClass: String = "",
    val started: Date = Date(0),
    val progress: Int = 0
)

data class LogEntry(
    val started: Date? = null,
    val finished: Date? = null,
    val result: String? = null,
    var workerClass: String = BackgroundJobManagerImpl.NOT_SET_VALUE
)
