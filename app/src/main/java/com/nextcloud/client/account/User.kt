/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.account

import android.accounts.Account
import android.os.Parcelable
import com.owncloud.android.lib.common.OwnCloudAccount

interface User :
    Parcelable,
    com.nextcloud.common.User {
    override val accountName: String
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
    override fun toPlatformAccount(): Account

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
