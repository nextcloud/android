/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.di

import android.accounts.AccountManager
import android.app.Application
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.media.AudioManager
import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Clock
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.client.core.ThreadPoolAsyncRunner
import com.nextcloud.client.database.dao.ArbitraryDataDao
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.jobs.operation.FileOperationHelper
import com.nextcloud.client.logger.FileLogHandler
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.logger.LoggerImpl
import com.nextcloud.client.logger.LogsRepository
import com.nextcloud.client.migrations.Migrations
import com.nextcloud.client.migrations.MigrationsDb
import com.nextcloud.client.migrations.MigrationsManager
import com.nextcloud.client.migrations.MigrationsManagerImpl
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.notifications.AppNotificationManager
import com.nextcloud.client.notifications.AppNotificationManagerImpl
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.Throttler
import com.nextcloud.repository.ClientRepository
import com.nextcloud.repository.RemoteClientRepository
import com.nextcloud.utils.e2ee.E2EEActionResolver
import com.nextcloud.utils.e2ee.E2EEKeyInspector
import com.nextcloud.utils.thumbnail.FolderThumbnailGenerator
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.providers.UsersAndGroupsSearchConfig
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApi
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApiImpl
import com.owncloud.android.ui.activities.data.activities.RemoteActivitiesRepository
import com.owncloud.android.ui.activities.data.files.FilesRepository
import com.owncloud.android.ui.activities.data.files.FilesServiceApiImpl
import com.owncloud.android.ui.activities.data.files.RemoteFilesRepository
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.utils.theme.ViewThemeUtils
import dagger.Module
import dagger.Provides
import org.greenrobot.eventbus.EventBus
import java.io.File
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Module(includes = [ComponentsModule::class, VariantComponentsModule::class, BuildTypeComponentsModule::class])
@Suppress("TooManyFunctions")
internal class AppModule {

    @Provides
    fun accountManager(application: Application): AccountManager =
        application.getSystemService(Context.ACCOUNT_SERVICE) as AccountManager

    @Provides
    fun context(application: Application): Context = application

    @Provides
    fun packageManager(application: Application): PackageManager = application.packageManager

    @Provides
    fun contentResolver(context: Context): ContentResolver = context.contentResolver

    @Provides
    fun resources(application: Application): Resources = application.resources

    @Provides
    fun userAccountManager(context: Context, accountManager: AccountManager): UserAccountManager =
        UserAccountManagerImpl(context, accountManager)

    @Provides
    fun arbitraryDataProvider(dao: ArbitraryDataDao): ArbitraryDataProvider = ArbitraryDataProviderImpl(dao)

    @Provides
    fun syncedFolderProvider(
        contentResolver: ContentResolver,
        appPreferences: AppPreferences,
        clock: Clock
    ): SyncedFolderProvider = SyncedFolderProvider(contentResolver, appPreferences, clock)

    @Provides
    fun activitiesServiceApi(accountManager: UserAccountManager): ActivitiesServiceApi =
        ActivitiesServiceApiImpl(accountManager)

    @Provides
    fun activitiesRepository(api: ActivitiesServiceApi): ActivitiesRepository = RemoteActivitiesRepository(api)

    @Provides
    fun filesRepository(accountManager: UserAccountManager, clientFactory: ClientFactory): FilesRepository =
        RemoteFilesRepository(FilesServiceApiImpl(accountManager, clientFactory))

    @Provides
    fun clientRepository(repository: RemoteClientRepository): ClientRepository = repository

    @Provides
    fun uploadsStorageManager(currentAccountProvider: CurrentAccountProvider, context: Context): UploadsStorageManager =
        UploadsStorageManager(currentAccountProvider, context.contentResolver)

    @Provides
    fun fileDataStorageManager(
        currentAccountProvider: CurrentAccountProvider,
        context: Context
    ): FileDataStorageManager = FileDataStorageManager(currentAccountProvider.user, context.contentResolver)

    @Provides
    fun currentAccountProvider(accountManager: UserAccountManager): CurrentAccountProvider = accountManager

    @Provides
    fun deviceInfo(): DeviceInfo = DeviceInfo()

    @Provides
    @Singleton
    fun clock(): Clock = ClockImpl()

    @Provides
    @Singleton
    fun logger(context: Context, clock: Clock): Logger {
        val logDir = File(context.filesDir, LOG_DIR_NAME)
        val handler = FileLogHandler(logDir, LOG_FILE_NAME, LOG_FILE_MAX_SIZE_BYTES)
        return LoggerImpl(clock, handler, Handler(), LOGGER_QUEUE_CAPACITY).apply { start() }
    }

    @Provides
    @Singleton
    fun logsRepository(logger: Logger): LogsRepository = logger as LogsRepository

    @Provides
    @Singleton
    fun uiAsyncRunner(): AsyncRunner = ThreadPoolAsyncRunner(Handler(), UI_THREAD_POOL_SIZE, UI_RUNNER_TAG)

    @Provides
    @Singleton
    @Named(IO_RUNNER_TAG)
    fun ioAsyncRunner(): AsyncRunner = ThreadPoolAsyncRunner(Handler(), IO_THREAD_POOL_SIZE, IO_RUNNER_TAG)

    @Provides
    fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    fun audioManager(context: Context): AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    @Singleton
    fun eventBus(): EventBus = EventBus.getDefault()

    @Provides
    @Singleton
    fun migrationsDb(application: Application): MigrationsDb =
        MigrationsDb(application.getSharedPreferences(MIGRATIONS_PREFERENCES_NAME, Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun migrationsManager(
        migrationsDb: MigrationsDb,
        appInfo: AppInfo,
        asyncRunner: AsyncRunner,
        migrations: Migrations
    ): MigrationsManager = MigrationsManagerImpl(appInfo, migrationsDb, asyncRunner, migrations.steps)

    @Provides
    @Singleton
    fun notificationsManager(
        context: Context,
        platformNotificationsManager: NotificationManager,
        viewThemeUtilsProvider: Provider<ViewThemeUtils>
    ): AppNotificationManager = AppNotificationManagerImpl(
        context,
        context.resources,
        platformNotificationsManager,
        viewThemeUtilsProvider
    )

    @Provides
    fun localBroadcastManager(context: Context): LocalBroadcastManager = LocalBroadcastManager.getInstance(context)

    @Provides
    fun throttler(clock: Clock): Throttler = Throttler(clock)

    @Provides
    @Singleton
    fun passCodeManager(preferences: AppPreferences, clock: Clock): PassCodeManager =
        PassCodeManager(preferences, clock)

    @Provides
    fun fileOperationHelper(currentAccountProvider: CurrentAccountProvider, context: Context): FileOperationHelper =
        FileOperationHelper(
            currentAccountProvider.user,
            context,
            fileDataStorageManager(currentAccountProvider, context)
        )

    @Provides
    @Singleton
    fun userAndGroupSearchConfig(): UsersAndGroupsSearchConfig = UsersAndGroupsSearchConfig()

    @Provides
    @Singleton
    fun certificateValidator(): CertificateValidator = CertificateValidator()

    @Provides
    fun folderThumbnailGenerator(
        appPreferences: AppPreferences,
        viewThemeUtils: ViewThemeUtils,
        context: Context,
        accountManager: UserAccountManager
    ): FolderThumbnailGenerator = FolderThumbnailGenerator(appPreferences, viewThemeUtils, context, accountManager)

    @Provides
    fun e2eeKeyInspector(
        context: Context,
        fileDataStorageManager: FileDataStorageManager,
        certificateValidator: CertificateValidator,
        arbitraryDataProvider: ArbitraryDataProvider,
        accountManager: UserAccountManager
    ): E2EEKeyInspector = E2EEKeyInspector(
        context,
        fileDataStorageManager,
        certificateValidator,
        arbitraryDataProvider,
        accountManager
    )

    @Provides
    fun e2eeActionResolver(
        fileDataStorageManager: FileDataStorageManager,
        arbitraryDataProvider: ArbitraryDataProvider,
        accountManager: UserAccountManager,
        connectivityService: ConnectivityService,
        e2eeKeyInspector: E2EEKeyInspector
    ): E2EEActionResolver = E2EEActionResolver(
        fileDataStorageManager,
        arbitraryDataProvider,
        accountManager,
        connectivityService,
        e2eeKeyInspector
    )

    companion object {
        private const val LOG_DIR_NAME = "logs"
        private const val LOG_FILE_NAME = "log.txt"
        private const val LOG_FILE_MAX_SIZE_BYTES = 1024L * 1024L
        private const val LOGGER_QUEUE_CAPACITY = 1000
        private const val UI_THREAD_POOL_SIZE = 4
        private const val IO_THREAD_POOL_SIZE = 8
        private const val UI_RUNNER_TAG = "ui"
        private const val IO_RUNNER_TAG = "io"
        private const val MIGRATIONS_PREFERENCES_NAME = "migrations"
    }
}
