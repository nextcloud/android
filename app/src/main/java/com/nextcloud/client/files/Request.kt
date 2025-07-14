/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.files

import android.os.Parcel
import android.os.Parcelable
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.PostUploadAction
import com.nextcloud.client.jobs.upload.UploadTrigger
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import java.util.UUID

sealed class Request(val user: User, val file: OCFile, val uuid: UUID, val type: Direction, val test: Boolean) :
    Parcelable

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
class DownloadRequest internal constructor(
    user: User,
    file: OCFile,
    uuid: UUID,
    type: Direction,
    test: Boolean = false
) : Request(user, file, uuid, type, test) {

    constructor(
        user: User,
        file: OCFile
    ) : this(user, file, UUID.randomUUID(), Direction.DOWNLOAD)

    constructor(
        user: User,
        file: OCFile,
        test: Boolean
    ) : this(user, file, UUID.randomUUID(), Direction.DOWNLOAD, test)

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

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DownloadRequest> {
        override fun createFromParcel(parcel: Parcel): DownloadRequest = DownloadRequest(parcel)

        override fun newArray(size: Int): Array<DownloadRequest?> = arrayOfNulls(size)
    }
}

@Suppress("LongParameterList")
class UploadRequest internal constructor(
    user: User,
    file: OCFile,
    val upload: OCUpload,
    uuid: UUID,
    type: Direction,
    test: Boolean
) : Request(user, file, uuid, type, test) {

    constructor(
        user: User,
        upload: OCUpload,
        test: Boolean
    ) : this(
        user,
        OCFile(upload.remotePath).apply {
            storagePath = upload.localPath
            fileLength = upload.fileSize
        },
        upload,
        UUID.randomUUID(),
        Direction.UPLOAD,
        test
    )

    constructor(
        user: User,
        upload: OCUpload
    ) : this(user, upload, false)

    constructor(parcel: Parcel) : this(
        user = parcel.readParcelable<User>(User::class.java.classLoader) as User,
        file = parcel.readParcelable<OCFile>(OCFile::class.java.classLoader) as OCFile,
        upload = parcel.readParcelable<OCUpload>(OCUpload::class.java.classLoader) as OCUpload,
        uuid = parcel.readSerializable() as UUID,
        type = parcel.readSerializable() as Direction,
        test = parcel.readInt() != 0
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(user, flags)
        parcel.writeParcelable(file, flags)
        parcel.writeParcelable(upload, flags)
        parcel.writeSerializable(uuid)
        parcel.writeSerializable(type)
        parcel.writeInt(if (test) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UploadRequest> {
        override fun createFromParcel(parcel: Parcel): UploadRequest = UploadRequest(parcel)

        override fun newArray(size: Int): Array<UploadRequest?> = arrayOfNulls(size)
    }

    /**
     * This class provides a builder pattern with API convenient to be used in Java.
     */
    class Builder(private val user: User, private var source: String, private var destination: String) {
        private var fileSize: Long = 0
        private var nameConflictPolicy = NameCollisionPolicy.ASK_USER
        private var createRemoteFolder = true
        private var trigger = UploadTrigger.USER
        private var requireWifi = false
        private var requireCharging = false
        private var postUploadAction = PostUploadAction.NONE

        fun setPaths(source: String, destination: String): Builder {
            this.source = source
            this.destination = destination
            return this
        }

        fun setFileSize(fileSize: Long): Builder {
            this.fileSize = fileSize
            return this
        }

        fun setNameConflicPolicy(policy: NameCollisionPolicy): Builder {
            this.nameConflictPolicy = policy
            return this
        }

        fun setCreateRemoteFolder(create: Boolean): Builder {
            this.createRemoteFolder = create
            return this
        }

        fun setTrigger(trigger: UploadTrigger): Builder {
            this.trigger = trigger
            return this
        }

        fun setRequireWifi(require: Boolean): Builder {
            this.requireWifi = require
            return this
        }

        fun setRequireCharging(require: Boolean): Builder {
            this.requireCharging = require
            return this
        }

        fun setPostAction(action: PostUploadAction): Builder {
            this.postUploadAction = action
            return this
        }

        fun build(): Request {
            val upload = OCUpload(source, destination, user.accountName)
            upload.fileSize = fileSize
            upload.nameCollisionPolicy = this.nameConflictPolicy
            upload.isCreateRemoteFolder = this.createRemoteFolder
            upload.createdBy = this.trigger.value
            upload.localAction = this.postUploadAction.value
            upload.isUseWifiOnly = this.requireWifi
            upload.isWhileChargingOnly = this.requireCharging
            upload.uploadStatus = UploadsStorageManager.UploadStatus.UPLOAD_IN_PROGRESS
            return UploadRequest(user, upload)
        }
    }
}
