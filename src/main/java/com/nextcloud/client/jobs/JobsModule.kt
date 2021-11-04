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

import android.content.Context
import android.content.ContextWrapper
import androidx.work.Configuration
import androidx.work.WorkManager
import com.nextcloud.client.core.Clock
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
            override fun getApplicationContext(): Context {
                return this
            }
        }

        WorkManager.initialize(contextWrapper, configuration)
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun backgroundJobManager(workManager: WorkManager, clock: Clock): BackgroundJobManager {
        return BackgroundJobManagerImpl(workManager, clock)
    }
}
