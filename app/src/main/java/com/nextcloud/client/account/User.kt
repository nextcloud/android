/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Chris Narkiewicz
 * Copyright (C) 2020 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.account

import android.accounts.Account
import android.os.Parcelable
import com.owncloud.android.lib.common.OwnCloudAccount

interface User : Parcelable {
    val accountName: String
    val server: Server
    val isAnonymous: Boolean

    /**
     * This is temporary helper method created to facilitate incremental refactoring.
     * Code using legacy platform Account can be partially converted to instantiate User
     * object and use account instance when required.
     *
     * This method calls will allow tracing code awaiting further refactoring.
     *
     * @return Account instance that is associated with this User object.
     */
    @Deprecated("Temporary workaround")
    fun toPlatformAccount(): Account

    /**
     * This is temporary helper method created to facilitate incremental refactoring.
     * Code using legacy ownCloud account can be partially converted to instantiate User
     * object and use account instance when required.
     *
     * This method calls will allow tracing code awaiting further refactoring.
     *
     * @return OwnCloudAccount instance that is associated with this User object.
     */
    @Deprecated("Temporary workaround")
    fun toOwnCloudAccount(): OwnCloudAccount

    /**
     * Compare account names, case insensitive.
     *
     * @return true if account names are same, false otherwise
     */
    fun nameEquals(user: User?): Boolean

    /**
     * Compare account names, case insensitive.
     *
     * @return true if account names are same, false otherwise
     */
    fun nameEquals(accountName: CharSequence?): Boolean
}
