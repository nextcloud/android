/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.Context
import android.content.ContextWrapper
import androidx.work.Configuration
import androidx.work.WorkManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.AppPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class JobsModule {

    @Provides
    @Singleton
    fun workManager(context: Context, factory: BackgroundJobFactory): WorkManager {
        val configuration = Configuration.Builder()
            .setWorkerFactory(factory)
            .build()

        val contextWrapper = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
        }

        WorkManager.initialize(contextWrapper, configuration)
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun backgroundJobManager(
        workManager: WorkManager,
        clock: Clock,
        preferences: AppPreferences
    ): BackgroundJobManager = BackgroundJobManagerImpl(workManager, clock, preferences)
}
