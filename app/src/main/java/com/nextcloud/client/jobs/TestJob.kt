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

class TestJob(appContext: Context, params: WorkerParameters, private val backgroundJobManager: BackgroundJobManager) :
    Worker(appContext, params) {

    companion object {
        private const val MAX_PROGRESS = 100
        private const val DELAY_MS = 1000L
        private const val PROGRESS_KEY = "progress"
    }

    override fun doWork(): Result {
        backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class))

        for (i in 0..MAX_PROGRESS) {
            Thread.sleep(DELAY_MS)
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
