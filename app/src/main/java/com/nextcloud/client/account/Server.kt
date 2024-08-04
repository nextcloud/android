/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.account

import android.os.Parcel
import android.os.Parcelable
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import java.net.URI

/**
 * This object provides all information necessary to interact
 * with backend server.
 */
data class Server(val uri: URI, val version: OwnCloudVersion) : Parcelable {

    constructor(source: Parcel) : this(
        source.readSerializable() as URI,
        source.readParcelable<Parcelable>(OwnCloudVersion::class.java.classLoader) as OwnCloudVersion
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeSerializable(uri)
        writeParcelable(version, 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Server> = object : Parcelable.Creator<Server> {
            override fun createFromParcel(source: Parcel): Server = Server(source)
            override fun newArray(size: Int): Array<Server?> = arrayOfNulls(size)
        }
    }
}
