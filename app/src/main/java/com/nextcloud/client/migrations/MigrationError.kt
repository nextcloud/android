/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.migrations

class MigrationError(val id: Int, message: String, cause: Throwable?) : RuntimeException(message, cause) {
    constructor(id: Int, message: String) : this(id, message, null)
}
