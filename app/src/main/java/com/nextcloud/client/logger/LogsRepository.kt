/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
