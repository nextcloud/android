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

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.documentscan.GeneratePDFUseCase
import com.nextcloud.client.documentscan.GeneratePdfFromImagesWork
import com.nextcloud.client.integrations.deck.DeckApi
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.utils.theme.ViewThemeUtils
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import javax.inject.Provider

/**
 * This factory is responsible for creating all background jobs and for injecting worker dependencies.
 *
 * This class is doing too many things and should be split up into smaller factories.
 */
@Suppress("LongParameterList", "TooManyFunctions") // satisfied by DI
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
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val notificationManager: NotificationManager,
    private val eventBus: EventBus,
    private val deckApi: DeckApi,
    private val viewThemeUtils: Provider<ViewThemeUtils>,
    private val localBroadcastManager: Provider<LocalBroadcastManager>,
    private val generatePdfUseCase: GeneratePDFUseCase,
    private val syncedFolderProvider: SyncedFolderProvider
) : WorkerFactory() {

    @SuppressLint("NewApi")
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

        return if (workerClass == ContentObserverWork::class) {
            createContentObserverJob(context, workerParameters)
        } else {
            when (workerClass) {
                ContactsBackupWork::class -> createContactsBackupWork(context, workerParameters)
                ContactsImportWork::class -> createContactsImportWork(context, workerParameters)
                FilesSyncWork::class -> createFilesSyncWork(context, workerParameters)
                OfflineSyncWork::class -> createOfflineSyncWork(context, workerParameters)
                MediaFoldersDetectionWork::class -> createMediaFoldersDetectionWork(context, workerParameters)
                NotificationWork::class -> createNotificationWork(context, workerParameters)
                AccountRemovalWork::class -> createAccountRemovalWork(context, workerParameters)
                CalendarBackupWork::class -> createCalendarBackupWork(context, workerParameters)
                CalendarImportWork::class -> createCalendarImportWork(context, workerParameters)
                FilesExportWork::class -> createFilesExportWork(context, workerParameters)
                FilesUploadWorker::class -> createFilesUploadWorker(context, workerParameters)
                GeneratePdfFromImagesWork::class -> createPDFGenerateWork(context, workerParameters)
                HealthStatusWork::class -> createHealthStatusWork(context, workerParameters)
                TestJob::class -> createTestJob(context, workerParameters)
                else -> null // caller falls back to default factory
            }
        }
    }

    private fun createFilesExportWork(
        context: Context,
        params: WorkerParameters
    ): ListenableWorker {
        return FilesExportWork(
            context,
            accountManager.user,
            contentResolver,
            viewThemeUtils.get(),
            params
        )
    }

    private fun createContentObserverJob(
        context: Context,
        workerParameters: WorkerParameters
    ): ListenableWorker {
        return ContentObserverWork(
            context,
            workerParameters,
            SyncedFolderProvider(contentResolver, preferences, clock),
            powerManagementService,
            backgroundJobManager.get()
        )
    }

    private fun createContactsBackupWork(context: Context, params: WorkerParameters): ContactsBackupWork {
        return ContactsBackupWork(
            context,
            params,
            resources,
            arbitraryDataProvider,
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
            contentResolver = contentResolver,
            userAccountManager = accountManager,
            uploadsStorageManager = uploadsStorageManager,
            connectivityService = connectivityService,
            powerManagementService = powerManagementService,
            syncedFolderProvider = syncedFolderProvider,
            backgroundJobManager = backgroundJobManager.get()
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
            clock,
            viewThemeUtils.get(),
            syncedFolderProvider
        )
    }

    private fun createNotificationWork(context: Context, params: WorkerParameters): NotificationWork {
        return NotificationWork(
            context,
            params,
            notificationManager,
            accountManager,
            deckApi,
            viewThemeUtils.get()
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
            eventBus,
            preferences,
            syncedFolderProvider
        )
    }

    private fun createFilesUploadWorker(context: Context, params: WorkerParameters): FilesUploadWorker {
        return FilesUploadWorker(
            uploadsStorageManager,
            connectivityService,
            powerManagementService,
            accountManager,
            viewThemeUtils.get(),
            localBroadcastManager.get(),
            backgroundJobManager.get(),
            context,
            params
        )
    }

    private fun createPDFGenerateWork(context: Context, params: WorkerParameters): GeneratePdfFromImagesWork {
        return GeneratePdfFromImagesWork(
            appContext = context,
            generatePdfUseCase = generatePdfUseCase,
            viewThemeUtils = viewThemeUtils.get(),
            notificationManager = notificationManager,
            userAccountManager = accountManager,
            logger = logger,
            params = params
        )
    }

    private fun createHealthStatusWork(context: Context, params: WorkerParameters): HealthStatusWork {
        return HealthStatusWork(
            context,
            params,
            accountManager,
            arbitraryDataProvider,
            backgroundJobManager.get()
        )
    }

    private fun createTestJob(context: Context, params: WorkerParameters): TestJob {
        return TestJob(
            context,
            params,
            backgroundJobManager.get()
        )
    }
}
