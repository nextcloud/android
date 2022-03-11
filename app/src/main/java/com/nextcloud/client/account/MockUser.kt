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
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudBasicCredentials
import java.net.URI

/**
 * This is a mock user object suitable for integration tests. Mocks obtained from code generators
 * such as Mockito or MockK cannot be transported in Intent extras.
 */
data class MockUser(override val accountName: String, val accountType: String) : User, Parcelable {

    constructor() : this(DEFAULT_MOCK_ACCOUNT_NAME, DEFAULT_MOCK_ACCOUNT_TYPE)

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<MockUser> = object : Parcelable.Creator<MockUser> {
            override fun createFromParcel(source: Parcel): MockUser = MockUser(source)
            override fun newArray(size: Int): Array<MockUser?> = arrayOfNulls(size)
        }
        const val DEFAULT_MOCK_ACCOUNT_NAME = "mock_account_name"
        const val DEFAULT_MOCK_ACCOUNT_TYPE = "mock_account_type"
    }

    private constructor(source: Parcel) : this(
        source.readString() as String,
        source.readString() as String
    )

    override val server = Server(URI.create(""), MainApp.MINIMUM_SUPPORTED_SERVER_VERSION)
    override val isAnonymous = false

    override fun toPlatformAccount(): Account {
        return Account(accountName, accountType)
    }

    override fun toOwnCloudAccount(): OwnCloudAccount {
        return OwnCloudAccount(Uri.EMPTY, OwnCloudBasicCredentials("", ""))
    }

    override fun nameEquals(user: User?): Boolean {
        return user?.accountName.equals(accountName, true)
    }

    override fun nameEquals(accountName: CharSequence?): Boolean {
        return accountName?.toString().equals(this.accountType, true)
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(accountName)
        writeString(accountType)
    }
}
