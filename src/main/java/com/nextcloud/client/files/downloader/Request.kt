/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.files.downloader

import android.os.Parcel
import android.os.Parcelable
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile
import java.util.UUID

/**
 * Transfer request. This class should collect all information
 * required to trigger transfer operation.
 *
 * Class is immutable by design, although [OCFile] or other
 * types might not be immutable. Clients should no modify
 * contents of this object.
 *
 * @property user Transfer will be triggered for a given user
 * @property file File to transfer
 * @property uuid Unique request identifier; this identifier can be set in [Transfer]
 * @property dummy if true, this requests a dummy test transfer; no real file transfer will occur
 */
class Request internal constructor(
    val user: User,
    val file: OCFile,
    val uuid: UUID,
    val type: Direction = Direction.DOWNLOAD,
    val test: Boolean = false
) : Parcelable {

    constructor(
        user: User,
        file: OCFile,
        type: Direction = Direction.DOWNLOAD
    ) : this(user, file, UUID.randomUUID(), type)

    constructor(
        user: User,
        file: OCFile,
        type: Direction,
        test: Boolean
    ) : this(user, file, UUID.randomUUID(), type, test)

    constructor(parcel: Parcel) : this(
        user = parcel.readParcelable<User>(User::class.java.classLoader) as User,
        file = parcel.readParcelable<OCFile>(OCFile::class.java.classLoader) as OCFile,
        uuid = parcel.readSerializable() as UUID,
        type = parcel.readSerializable() as Direction,
        test = parcel.readInt() != 0
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(user, flags)
        parcel.writeParcelable(file, flags)
        parcel.writeSerializable(uuid)
        parcel.writeSerializable(type)
        parcel.writeInt(if (test) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Request> {
        override fun createFromParcel(parcel: Parcel): Request {
            return Request(parcel)
        }

        override fun newArray(size: Int): Array<Request?> {
            return arrayOfNulls(size)
        }
    }
}
