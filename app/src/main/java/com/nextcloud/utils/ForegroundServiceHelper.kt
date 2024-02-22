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

package com.nextcloud.utils

import android.app.Notification
import android.app.Service
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.work.ForegroundInfo
import com.owncloud.android.datamodel.ForegroundServiceType

object ForegroundServiceHelper {
    fun startService(
        service: Service,
        id: Int,
        notification: Notification,
        foregroundServiceType: ForegroundServiceType
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                service,
                id,
                notification,
                foregroundServiceType.getId()
            )
        } else {
            service.startForeground(id, notification)
        }
    }

    fun createWorkerForegroundInfo(
        id: Int,
        notification: Notification,
        foregroundServiceType: ForegroundServiceType
    ): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, foregroundServiceType.getId())
        } else {
            ForegroundInfo(id, notification)
        }
    }
}
