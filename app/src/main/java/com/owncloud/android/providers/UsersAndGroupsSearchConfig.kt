/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.providers

/**
 * This is a data class that holds the configuration for the user and group searchable.
 * As we cannot access searchable providers in runtime, injecting a singleton into them is the only way to change their
 * config.
 */
data class UsersAndGroupsSearchConfig(var searchOnlyUsers: Boolean = false) {
    fun reset() {
        searchOnlyUsers = false
    }
}
