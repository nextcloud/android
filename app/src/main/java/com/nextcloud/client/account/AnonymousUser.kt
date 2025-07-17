/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.account

import android.accounts.Account
import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudBasicCredentials
import java.net.URI

/**
 * This object represents anonymous user, ie. user that did not log in the Nextcloud server.
 * It serves as a semantically correct "empty value", allowing simplification of logic
 * in various components requiring user data, such as DB queries.
 */
internal data class AnonymousUser(private val accountType: String) :
    User,
    Parcelable {

    companion object {
        @JvmStatic
        fun fromContext(context: Context): AnonymousUser {
            val type = context.getString(R.string.account_type)
            return AnonymousUser(type)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<AnonymousUser> = object : Parcelable.Creator<AnonymousUser> {
            override fun createFromParcel(source: Parcel): AnonymousUser = AnonymousUser(source)
            override fun newArray(size: Int): Array<AnonymousUser?> = arrayOfNulls(size)
        }
    }

    private constructor(source: Parcel) : this(
        source.readString() as String
    )

    override val accountName: String = "anonymous@nohost"
    override val server = Server(URI.create(""), MainApp.MINIMUM_SUPPORTED_SERVER_VERSION)
    override val isAnonymous = true

    override fun toPlatformAccount(): Account = Account(accountName, accountType)

    override fun toOwnCloudAccount(): OwnCloudAccount = OwnCloudAccount(Uri.EMPTY, OwnCloudBasicCredentials("", ""))

    override fun nameEquals(user: User?): Boolean = user?.accountName.equals(accountName, true)

    override fun nameEquals(accountName: CharSequence?): Boolean =
        accountName?.toString().equals(this.accountType, true)

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(accountType)
    }
}
