/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.nextcloud.client.migrations

import com.nextcloud.client.account.UserAccountManager
import javax.inject.Inject

/**
 * This class collects all migration steps and provides API to supply those
 * steps to [MigrationsManager] for execution.
 *
 * Note to maintainers: put all migration routines here and export collection of
 * opaque [Runnable]s via steps property.
 */
class Migrations @Inject constructor(
    private val userAccountManager: UserAccountManager
) {
    /**
     * @param id Step id; id must be unique
     * @param description Human readable migration step description
     * @param function Migration runnable object
     */
    data class Step(val id: Int, val description: String, val function: Runnable)

    /**
     * List of migration steps. Those steps will be loaded and run by [MigrationsManager]
     */
    val steps: List<Step> = listOf(
        Step(0, "migrate user id", Runnable { migrateUserId() })
    ).sortedBy { it.id }

    fun migrateUserId() {
        userAccountManager.migrateUserId()
    }
}
