/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
data class MockUser(override val accountName: String, val accountType: String) :
    User,
    Parcelable {

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

    override fun toPlatformAccount(): Account = Account(accountName, accountType)

    override fun toOwnCloudAccount(): OwnCloudAccount = OwnCloudAccount(Uri.EMPTY, OwnCloudBasicCredentials("", ""))

    override fun nameEquals(user: User?): Boolean = user?.accountName.equals(accountName, true)

    override fun nameEquals(accountName: CharSequence?): Boolean =
        accountName?.toString().equals(this.accountType, true)

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(accountName)
        writeString(accountType)
    }
}
