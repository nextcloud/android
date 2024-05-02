/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
