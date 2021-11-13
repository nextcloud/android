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

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.integrations.deck.DeckApi
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import javax.inject.Provider

/**
 * This factory is responsible for creating all background jobs and for injecting worker dependencies.
 */
@Suppress("LongParameterList") // satisfied by DI
class BackgroundJobFactory @Inject constructor(
    private val logger: Logger,
    private val preferences: AppPreferences,
    private val contentResolver: ContentResolver,
    private val clock: Clock,
    private val powerManagementService: PowerManagementService,
    private val backgroundJobManager: Provider<BackgroundJobManager>,
    private val deviceInfo: DeviceInfo,
    private val accountManager: UserAccountManager,
    private val resources: Resources,
    private val dataProvider: ArbitraryDataProvider,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val notificationManager: NotificationManager,
    private val eventBus: EventBus,
    private val deckApi: DeckApi
) : WorkerFactory() {

    @Suppress("ComplexMethod") // it's just a trivial dispatch
    override fun createWorker(
        context: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        val workerClass = try {
            Class.forName(workerClassName).kotlin
        } catch (ex: ClassNotFoundException) {
            null
        }

        return when (workerClass) {
            ContentObserverWork::class -> createContentObserverJob(context, workerParameters, clock)
            ContactsBackupWork::class -> createContactsBackupWork(context, workerParameters)
            ContactsImportWork::class -> createContactsImportWork(context, workerParameters)
            FilesSyncWork::class -> createFilesSyncWork(context, workerParameters)
            OfflineSyncWork::class -> createOfflineSyncWork(context, workerParameters)
            MediaFoldersDetectionWork::class -> createMediaFoldersDetectionWork(context, workerParameters)
            NotificationWork::class -> createNotificationWork(context, workerParameters)
            AccountRemovalWork::class -> createAccountRemovalWork(context, workerParameters)
            CalendarBackupWork::class -> createCalendarBackupWork(context, workerParameters)
            CalendarImportWork::class -> createCalendarImportWork(context, workerParameters)
            else -> null // caller falls back to default factory
        }
    }

    private fun createContentObserverJob(
        context: Context,
        workerParameters: WorkerParameters,
        clock: Clock
    ): ListenableWorker? {
        val folderResolver = SyncedFolderProvider(contentResolver, preferences, clock)
        @RequiresApi(Build.VERSION_CODES.N)
        if (deviceInfo.apiLevel >= Build.VERSION_CODES.N) {
            return ContentObserverWork(
                context,
                workerParameters,
                folderResolver,
                powerManagementService,
                backgroundJobManager.get()
            )
        } else {
            return null
        }
    }

    private fun createContactsBackupWork(context: Context, params: WorkerParameters): ContactsBackupWork {
        return ContactsBackupWork(
            context,
            params,
            resources,
            dataProvider,
            contentResolver,
            accountManager
        )
    }

    private fun createContactsImportWork(context: Context, params: WorkerParameters): ContactsImportWork {
        return ContactsImportWork(
            context,
            params,
            logger,
            contentResolver
        )
    }

    private fun createCalendarBackupWork(context: Context, params: WorkerParameters): CalendarBackupWork {
        return CalendarBackupWork(
            context,
            params,
            contentResolver,
            accountManager,
            preferences
        )
    }

    private fun createCalendarImportWork(context: Context, params: WorkerParameters): CalendarImportWork {
        return CalendarImportWork(
            context,
            params,
            logger,
            contentResolver
        )
    }

    private fun createFilesSyncWork(context: Context, params: WorkerParameters): FilesSyncWork {
        return FilesSyncWork(
            context = context,
            params = params,
            resources = resources,
            contentResolver = contentResolver,
            userAccountManager = accountManager,
            preferences = preferences,
            uploadsStorageManager = uploadsStorageManager,
            connectivityService = connectivityService,
            powerManagementService = powerManagementService,
            clock = clock
        )
    }

    private fun createOfflineSyncWork(context: Context, params: WorkerParameters): OfflineSyncWork {
        return OfflineSyncWork(
            context = context,
            params = params,
            contentResolver = contentResolver,
            userAccountManager = accountManager,
            connectivityService = connectivityService,
            powerManagementService = powerManagementService
        )
    }

    private fun createMediaFoldersDetectionWork(context: Context, params: WorkerParameters): MediaFoldersDetectionWork {
        return MediaFoldersDetectionWork(
            context,
            params,
            resources,
            contentResolver,
            accountManager,
            preferences,
            clock
        )
    }

    private fun createNotificationWork(context: Context, params: WorkerParameters): NotificationWork {
        return NotificationWork(
            context,
            params,
            notificationManager,
            accountManager,
            deckApi
        )
    }

    private fun createAccountRemovalWork(context: Context, params: WorkerParameters): AccountRemovalWork {
        return AccountRemovalWork(
            context,
            params,
            uploadsStorageManager,
            accountManager,
            backgroundJobManager.get(),
            clock,
            eventBus
        )
    }
}
