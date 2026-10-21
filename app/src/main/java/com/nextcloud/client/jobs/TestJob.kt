/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.utils.AppConstants.PROGRESS_PERCENTAGE_MAX
import com.nextcloud.utils.AppConstants.PROGRESS_PERCENTAGE_MIN
import com.nextcloud.utils.AppConstants.ONE_SECOND

class TestJob(appContext: Context, params: WorkerParameters, private val backgroundJobManager: BackgroundJobManager) :
    Worker(appContext, params) {

    companion object {
        private const val PROGRESS_KEY = "progress"
    }

    override fun doWork(): Result {
        backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class))

        for (i in PROGRESS_PERCENTAGE_MIN..PROGRESS_PERCENTAGE_MAX) {
            Thread.sleep(ONE_SECOND.inWholeMilliseconds)
            val progress = Data.Builder()
                .putInt(PROGRESS_KEY, i)
                .build()
            setProgressAsync(progress)
        }

        val result = Result.success()
        backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
        return result
    }
}
