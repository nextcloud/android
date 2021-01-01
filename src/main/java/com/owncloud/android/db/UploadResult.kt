/*
 * ownCloud Android client application
 *
 * @author masensio
 * Copyright (C) 2016 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.db

import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import java.io.FileNotFoundException

enum class UploadResult(val value: Int) {
    UNKNOWN(-1),
    UPLOADED(0),
    NETWORK_CONNECTION(1),
    CREDENTIAL_ERROR(2),
    FOLDER_ERROR(3),
    CONFLICT_ERROR(4),
    FILE_ERROR(5),
    PRIVILEGES_ERROR(6),
    CANCELLED(7),
    FILE_NOT_FOUND(8),
    DELAYED_FOR_WIFI(9),
    SERVICE_INTERRUPTED(10),
    DELAYED_FOR_CHARGING(11),
    MAINTENANCE_MODE(12),
    LOCK_FAILED(13),
    DELAYED_IN_POWER_SAVE_MODE(14),
    SSL_RECOVERABLE_PEER_UNVERIFIED(15),
    VIRUS_DETECTED(16),
    LOCAL_STORAGE_FULL(17),
    OLD_ANDROID_API(18),
    SYNC_CONFLICT(19),
    CANNOT_CREATE_FILE(20),
    LOCAL_STORAGE_NOT_COPIED(21);

    companion object {
        @JvmStatic
        fun fromValue(value: Int): UploadResult = values().find { it.value == value } ?: UNKNOWN

        @JvmStatic
        fun fromOperationResult(result: RemoteOperationResult): UploadResult {
            // messy :(
            return when (result.code) {
                ResultCode.OK -> UPLOADED
                ResultCode.NO_NETWORK_CONNECTION,
                ResultCode.HOST_NOT_AVAILABLE,
                ResultCode.TIMEOUT,
                ResultCode.WRONG_CONNECTION,
                ResultCode.INCORRECT_ADDRESS,
                ResultCode.SSL_ERROR -> NETWORK_CONNECTION

                ResultCode.ACCOUNT_EXCEPTION,

                ResultCode.UNAUTHORIZED -> CREDENTIAL_ERROR

                ResultCode.FILE_NOT_FOUND -> FOLDER_ERROR

                ResultCode.LOCAL_FILE_NOT_FOUND -> FILE_NOT_FOUND

                ResultCode.CONFLICT -> CONFLICT_ERROR

                ResultCode.LOCAL_STORAGE_NOT_COPIED -> LOCAL_STORAGE_NOT_COPIED

                ResultCode.LOCAL_STORAGE_FULL -> LOCAL_STORAGE_FULL

                ResultCode.OLD_ANDROID_API -> OLD_ANDROID_API

                ResultCode.SYNC_CONFLICT -> SYNC_CONFLICT

                ResultCode.FORBIDDEN -> PRIVILEGES_ERROR

                ResultCode.CANCELLED -> CANCELLED

                ResultCode.DELAYED_FOR_WIFI -> DELAYED_FOR_WIFI

                ResultCode.DELAYED_FOR_CHARGING -> DELAYED_FOR_CHARGING

                ResultCode.DELAYED_IN_POWER_SAVE_MODE -> DELAYED_IN_POWER_SAVE_MODE

                ResultCode.MAINTENANCE_MODE -> MAINTENANCE_MODE

                ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED -> SSL_RECOVERABLE_PEER_UNVERIFIED

                ResultCode.UNKNOWN_ERROR -> {
                    if (result.exception is FileNotFoundException) {
                        FILE_ERROR
                    } else UNKNOWN
                }

                ResultCode.LOCK_FAILED -> LOCK_FAILED

                ResultCode.VIRUS_DETECTED -> VIRUS_DETECTED

                ResultCode.CANNOT_CREATE_FILE -> CANNOT_CREATE_FILE

                else -> UNKNOWN
            }
        }
    }
}
