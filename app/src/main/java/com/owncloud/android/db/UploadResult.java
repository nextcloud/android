/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016-2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2016 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015-2016 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
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
    LOCAL_STORAGE_NOT_COPIED(21),
    QUOTA_EXCEEDED(22),
    SAME_FILE_CONFLICT(23);

    private final int value;

    UploadResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static UploadResult fromValue(int value) {
        return switch (value) {
            case -1 -> UNKNOWN;
            case 0 -> UPLOADED;
            case 1 -> NETWORK_CONNECTION;
            case 2 -> CREDENTIAL_ERROR;
            case 3 -> FOLDER_ERROR;
            case 4 -> CONFLICT_ERROR;
            case 5 -> FILE_ERROR;
            case 6 -> PRIVILEGES_ERROR;
            case 7 -> CANCELLED;
            case 8 -> FILE_NOT_FOUND;
            case 9 -> DELAYED_FOR_WIFI;
            case 10 -> SERVICE_INTERRUPTED;
            case 11 -> DELAYED_FOR_CHARGING;
            case 12 -> MAINTENANCE_MODE;
            case 13 -> LOCK_FAILED;
            case 14 -> DELAYED_IN_POWER_SAVE_MODE;
            case 15 -> SSL_RECOVERABLE_PEER_UNVERIFIED;
            case 16 -> VIRUS_DETECTED;
            case 17 -> LOCAL_STORAGE_FULL;
            case 18 -> OLD_ANDROID_API;
            case 19 -> SYNC_CONFLICT;
            case 20 -> CANNOT_CREATE_FILE;
            case 21 -> LOCAL_STORAGE_NOT_COPIED;
            case 22 -> QUOTA_EXCEEDED;
            case 23 -> SAME_FILE_CONFLICT;
            default -> UNKNOWN;
        };
    }

    public static UploadResult fromOperationResult(RemoteOperationResult result) {
        return switch (result.getCode()) {
            case OK -> UPLOADED;
            case NO_NETWORK_CONNECTION, HOST_NOT_AVAILABLE, TIMEOUT, WRONG_CONNECTION, INCORRECT_ADDRESS, SSL_ERROR ->
                NETWORK_CONNECTION;
            case ACCOUNT_EXCEPTION, UNAUTHORIZED -> CREDENTIAL_ERROR;
            case FILE_NOT_FOUND -> FOLDER_ERROR;
            case LOCAL_FILE_NOT_FOUND -> FILE_NOT_FOUND;
            case CONFLICT -> CONFLICT_ERROR;
            case LOCAL_STORAGE_NOT_COPIED -> LOCAL_STORAGE_NOT_COPIED;
            case LOCAL_STORAGE_FULL -> LOCAL_STORAGE_FULL;
            case OLD_ANDROID_API -> OLD_ANDROID_API;
            case SYNC_CONFLICT -> SYNC_CONFLICT;
            case FORBIDDEN -> PRIVILEGES_ERROR;
            case CANCELLED -> CANCELLED;
            case DELAYED_FOR_WIFI -> DELAYED_FOR_WIFI;
            case DELAYED_FOR_CHARGING -> DELAYED_FOR_CHARGING;
            case DELAYED_IN_POWER_SAVE_MODE -> DELAYED_IN_POWER_SAVE_MODE;
            case MAINTENANCE_MODE -> MAINTENANCE_MODE;
            case SSL_RECOVERABLE_PEER_UNVERIFIED -> SSL_RECOVERABLE_PEER_UNVERIFIED;
            case UNKNOWN_ERROR -> {
                if (result.getException() instanceof java.io.FileNotFoundException) {
                    yield FILE_ERROR;
                }
                yield UNKNOWN;
            }
            case LOCK_FAILED -> LOCK_FAILED;
            case VIRUS_DETECTED -> VIRUS_DETECTED;
            case CANNOT_CREATE_FILE -> CANNOT_CREATE_FILE;
            case QUOTA_EXCEEDED -> QUOTA_EXCEEDED;
            default -> UNKNOWN;
        };
    }
}
