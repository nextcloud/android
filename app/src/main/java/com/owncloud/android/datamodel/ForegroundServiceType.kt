/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Enum to specify the type of foreground service.
 * Use this enum when starting a foreground service to indicate its purpose.
 * Note: Foreground service type is not available for older Android versions.
 * This wrapper is designed for compatibility on those versions.
 */
enum class ForegroundServiceType {
    DataSync,
    MediaPlayback;

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getId(): Int {
        return if (this == DataSync) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        }
    }
}
