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
package com.nextcloud.client.jobs

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import com.nextcloud.client.account.User

/**
 * This interface allows to control, schedule and monitor all application
 * long-running background tasks, such as periodic checks or synchronization.
 */
interface BackgroundJobManager {

    /**
     * Information about all application background jobs.
     */
    val jobs: LiveData<List<JobInfo>>

    /**
     * Start content observer job that monitors changes in media folders
     * and launches synchronization when needed.
     *
     * This call is idempotent - there will be only one scheduled job
     * regardless of number of calls.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun scheduleContentObserverJob()

    /**
     * Schedule periodic contacts backups job. Operating system will
     * decide when to start the job.
     *
     * This call is idempotent - there can be only one scheduled job
     * at any given time.
     *
     * @param user User for which job will be scheduled.
     */
    fun schedulePeriodicContactsBackup(user: User)

    /**
     * Cancel periodic contacts backup. Existing tasks might finish, but no new
     * invocations will occur.
     */
    fun cancelPeriodicContactsBackup(user: User)

    /**
     * Immediately start single contacts backup job.
     * This job will launch independently from periodic contacts backup.
     *
     * @return Job info with current status, or null if job does not exist anymore
     */
    fun startImmediateContactsBackup(user: User): LiveData<JobInfo?>

    fun scheduleTestJob()
    fun cancelTestJob()

    fun pruneJobs()
    fun cancelAllJobs()
}
