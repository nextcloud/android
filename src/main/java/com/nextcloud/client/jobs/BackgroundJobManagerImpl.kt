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
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal class BackgroundJobManagerImpl(private val workManager: WorkManager) : BackgroundJobManager {

    companion object {
        const val TAG_CONTENT_SYNC = "content_sync"
        const val MAX_CONTENT_TRIGGER_DELAY_MS = 1500L
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun scheduleContentObserverJob() {
        val constrains = Constraints.Builder()
            .addContentUriTrigger(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Video.Media.INTERNAL_CONTENT_URI, true)
            .addContentUriTrigger(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true)
            .setTriggerContentMaxDelay(MAX_CONTENT_TRIGGER_DELAY_MS, TimeUnit.MILLISECONDS)
            .build()

        val request = OneTimeWorkRequest.Builder(ContentObserverWork::class.java)
            .setConstraints(constrains)
            .addTag(TAG_CONTENT_SYNC)
            .build()

        workManager.enqueueUniqueWork(TAG_CONTENT_SYNC, ExistingWorkPolicy.REPLACE, request)
    }
}
