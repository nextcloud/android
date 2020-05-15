/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2019 Chris Narkiewicz
 * Copyright (C) 2019 Nextcloud GmbH
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
import android.os.Parcel
import android.os.Parcelable
import com.owncloud.android.lib.common.OwnCloudAccount

/**
 * This class represents normal user logged into the Nextcloud server.
 */
internal data class RegisteredUser(
    private val account: Account,
    private val ownCloudAccount: OwnCloudAccount,
    override val server: Server
) : User {

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<RegisteredUser> = object : Parcelable.Creator<RegisteredUser> {
            override fun createFromParcel(source: Parcel): RegisteredUser = RegisteredUser(source)
            override fun newArray(size: Int): Array<RegisteredUser?> = arrayOfNulls(size)
        }
    }

    private constructor(source: Parcel) : this(
        source.readParcelable<Account>(Account::class.java.classLoader) as Account,
        source.readParcelable<OwnCloudAccount>(OwnCloudAccount::class.java.classLoader) as OwnCloudAccount,
        source.readParcelable<Server>(Server::class.java.classLoader) as Server
    )

    override val isAnonymous = false

    override val accountName: String get() {
        return account.name
    }

    override fun toPlatformAccount(): Account {
        return account
    }

    override fun toOwnCloudAccount(): OwnCloudAccount {
        return ownCloudAccount
    }

    override fun nameEquals(user: User?): Boolean {
        return nameEquals(user?.accountName)
    }

    override fun nameEquals(accountName: CharSequence?): Boolean {
        return accountName?.toString().equals(this.accountName, true)
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(account, 0)
        writeParcelable(ownCloudAccount, 0)
        writeParcelable(server, 0)
    }
}
