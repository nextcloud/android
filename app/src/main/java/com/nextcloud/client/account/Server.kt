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
