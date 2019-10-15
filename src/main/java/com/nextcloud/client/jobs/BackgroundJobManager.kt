/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.nextcloud.client.jobs

import android.os.Build
import androidx.annotation.RequiresApi

/**
 * This interface allows to control, schedule and monitor all application
 * long-running background tasks, such as periodic checks or synchronization.
 */
interface BackgroundJobManager {

    /**
     * Start content observer job that monitors changes in media folders
     * and launches synchronization when needed.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun scheduleContentObserverJob()
}
