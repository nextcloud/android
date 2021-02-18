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
package com.nextcloud.client.core

/**
 * Interface allowing cancellation of a running task.
 * Once must be careful when cancelling a non-idempotent task,
 * as cancellation does not guarantee a task termination.
 * One trivial case would be a task finished and cancelled
 * before result delivery.
 *
 * @see [com.nextcloud.client.core.AsyncRunner]
 */
interface Cancellable {

    /**
     * Cancel running task. Task termination is not guaranteed, as some
     * tasks cannot be interrupted, but the result will not be delivered.
     */
    fun cancel()
}
