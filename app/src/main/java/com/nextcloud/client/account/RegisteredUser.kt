/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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

    override fun toPlatformAccount(): Account = account

    override fun toOwnCloudAccount(): OwnCloudAccount = ownCloudAccount

    override fun nameEquals(user: User?): Boolean = nameEquals(user?.accountName)

    override fun nameEquals(accountName: CharSequence?): Boolean =
        accountName?.toString().equals(this.accountName, true)

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeParcelable(account, 0)
        writeParcelable(ownCloudAccount, 0)
        writeParcelable(server, 0)
    }
}
