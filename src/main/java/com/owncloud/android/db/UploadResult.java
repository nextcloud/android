/**
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

package com.owncloud.android.db;

import com.owncloud.android.lib.common.operations.RemoteOperationResult;

public enum UploadResult {
    UNKNOWN(-1),
    UPLOADED(0),
    NETWORK_CONNECTION(1),
    CREDENTIAL_ERROR(2),
    FOLDER_ERROR(3),
    CONFLICT_ERROR(4),
    FILE_ERROR(5),
    PRIVILEDGES_ERROR(6),
    CANCELLED(7),
    FILE_NOT_FOUND(8),
    DELAYED_FOR_WIFI(9),
    SERVICE_INTERRUPTED(10),
    DELAYED_FOR_CHARGING(11),
    MAINTENANCE_MODE(12);

    private final int value;

    UploadResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static UploadResult fromValue(int value) {
        switch (value) {
            case -1:
                return UNKNOWN;
            case 0:
                return UPLOADED;
            case 1:
                return NETWORK_CONNECTION;
            case 2:
                return CREDENTIAL_ERROR;
            case 3:
                return FOLDER_ERROR;
            case 4:
                return CONFLICT_ERROR;
            case 5:
                return FILE_ERROR;
            case 6:
                return PRIVILEDGES_ERROR;
            case 7:
                return CANCELLED;
            case 8:
                return FILE_NOT_FOUND;
            case 9:
                return DELAYED_FOR_WIFI;
            case 10:
                return SERVICE_INTERRUPTED;
            case 11:
                return DELAYED_FOR_CHARGING;
            case 12:
                return MAINTENANCE_MODE;
        }
        return null;
    }

    public static UploadResult fromOperationResult(RemoteOperationResult result) {
        // messy :(
        switch (result.getCode()) {
            case OK:
                return UPLOADED;
            case NO_NETWORK_CONNECTION:
            case HOST_NOT_AVAILABLE:
            case TIMEOUT:
            case WRONG_CONNECTION:
            case INCORRECT_ADDRESS:
            case SSL_ERROR:
            case SSL_RECOVERABLE_PEER_UNVERIFIED:
                return NETWORK_CONNECTION;
            case ACCOUNT_EXCEPTION:
            case UNAUTHORIZED:
                return CREDENTIAL_ERROR;
            case FILE_NOT_FOUND:
                return FOLDER_ERROR;
            case LOCAL_FILE_NOT_FOUND:
                return FILE_NOT_FOUND;
            case CONFLICT:
                return CONFLICT_ERROR;
            case LOCAL_STORAGE_NOT_COPIED:
                return FILE_ERROR;
            case FORBIDDEN:
                return PRIVILEDGES_ERROR;
            case CANCELLED:
                return CANCELLED;
            case DELAYED_FOR_WIFI:
                return DELAYED_FOR_WIFI;
            case DELAYED_FOR_CHARGING:
                return DELAYED_FOR_CHARGING;
            case UNKNOWN_ERROR:
                if (result.getException() instanceof java.io.FileNotFoundException) {
                    return FILE_ERROR;
                }
                return UNKNOWN;
            case MAINTENANCE_MODE:
                return MAINTENANCE_MODE;
            default:
                return UNKNOWN;
        }

    }
}
