/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
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
    DataSync, MediaPlayback;

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getId(): Int {
        return if (this == DataSync) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        }
    }
}
