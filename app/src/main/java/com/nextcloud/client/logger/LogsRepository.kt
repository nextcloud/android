/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger

typealias OnLogsLoaded = (entries: List<LogEntry>, totalLogSize: Long) -> Unit

/**
 * This interface provides safe, read only access to application
 * logs stored on a device.
 */
interface LogsRepository {

    /**
     * If true, logger was unable to handle some messages, which means
     * it cannot cope with amount of logged data.
     *
     * This property is thread-safe.
     */
    val lostEntries: Boolean

    /**
     * Asynchronously load available logs. Load can be scheduled on any thread,
     * but the listener will be called on main thread.
     *
     * @param onLoaded: Callback with loaded logs; called on main thread
     */
    fun load(onLoaded: OnLogsLoaded)

    /**
     * Asynchronously delete logs.
     */
    fun deleteAll()
}
