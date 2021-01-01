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
import androidx.core.content.getSystemService
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.Clock
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.client.core.ThreadPoolAsyncRunner
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.logger.FileLogHandler
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.logger.LoggerImpl
import com.nextcloud.client.logger.LogsRepository
import com.nextcloud.client.migrations.Migrations
import com.nextcloud.client.migrations.MigrationsDb
import com.nextcloud.client.migrations.MigrationsManager
import com.nextcloud.client.migrations.MigrationsManagerImpl
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.notifications.AppNotificationManager
import com.nextcloud.client.notifications.AppNotificationManagerImpl
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApi
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApiImpl
import com.owncloud.android.ui.activities.data.activities.RemoteActivitiesRepository
import com.owncloud.android.ui.activities.data.files.FilesRepository
import com.owncloud.android.ui.activities.data.files.FilesServiceApiImpl
import com.owncloud.android.ui.activities.data.files.RemoteFilesRepository
import dagger.Module
import dagger.Provides
import org.greenrobot.eventbus.EventBus
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [ComponentsModule::class, VariantComponentsModule::class, BuildTypeComponentsModule::class])
internal class AppModule {
    @Provides
    fun accountManager(application: Application): AccountManager = application.getSystemService()!!

    @Provides
    fun context(application: Application): Context = application

    @Provides
    fun packageManager(application: Application): PackageManager = application.packageManager

    @Provides
    fun contentResolver(context: Context): ContentResolver = context.contentResolver

    @Provides
    fun resources(application: Application): Resources = application.resources

    @Provides
    fun userAccountManager(
        context: Context?,
        accountManager: AccountManager?
    ): UserAccountManager = UserAccountManagerImpl(context, accountManager)

    @Provides
    fun arbitraryDataProvider(context: Context): ArbitraryDataProvider = ArbitraryDataProvider(context.contentResolver)

    @Provides
    fun activitiesServiceApi(accountManager: UserAccountManager?): ActivitiesServiceApi =
        ActivitiesServiceApiImpl(accountManager)

    @Provides
    fun activitiesRepository(api: ActivitiesServiceApi?): ActivitiesRepository = RemoteActivitiesRepository(api!!)

    @Provides
    fun filesRepository(accountManager: UserAccountManager?, clientFactory: ClientFactory?): FilesRepository =
        RemoteFilesRepository(FilesServiceApiImpl(accountManager, clientFactory))

    @Provides
    fun uploadsStorageManager(
        context: Context,
        currentAccountProvider: CurrentAccountProvider?
    ): UploadsStorageManager = UploadsStorageManager(currentAccountProvider, context.contentResolver)

    @Provides
    fun currentAccountProvider(accountManager: UserAccountManager): CurrentAccountProvider = accountManager

    @Provides
    fun deviceInfo(): DeviceInfo = DeviceInfo()

    @Provides
    @Singleton
    fun clock(): Clock = ClockImpl()

    @Provides
    @Singleton
    fun logger(context: Context, clock: Clock?): Logger {
        val logDir = File(context.filesDir, "logs")
        val handler = FileLogHandler(logDir, "log.txt", 1024 * 1024)
        val logger = LoggerImpl(clock!!, handler, Handler(), 1000)
        logger.start()
        return logger
    }

    @Provides
    @Singleton
    fun logsRepository(logger: Logger): LogsRepository = logger as LogsRepository

    @Provides
    @Singleton
    fun uiAsyncRunner(): AsyncRunner = ThreadPoolAsyncRunner(Handler(), 4, "ui")

    @Provides
    @Singleton
    @Named("io")
    fun ioAsyncRunner(): AsyncRunner = ThreadPoolAsyncRunner(Handler(), 8, "io")

    @Provides
    fun notificationManager(context: Context): NotificationManager = context.getSystemService()!!

    @Provides
    fun audioManager(context: Context): AudioManager = context.getSystemService()!!

    @Provides
    @Singleton
    fun eventBus(): EventBus = EventBus.getDefault()

    @Provides
    @Singleton
    fun migrationsDb(application: Application): MigrationsDb =
        MigrationsDb(application.getSharedPreferences("migrations", Context.MODE_PRIVATE))

    @Provides
    @Singleton
    fun migrationsManager(
        migrationsDb: MigrationsDb?,
        appInfo: AppInfo?,
        asyncRunner: AsyncRunner?,
        migrations: Migrations
    ): MigrationsManager =
        MigrationsManagerImpl(appInfo!!, migrationsDb!!, asyncRunner!!, migrations.steps)

    @Provides
    @Singleton
    fun notificationsManager(
        context: Context,
        platformNotificationsManager: NotificationManager?
    ): AppNotificationManager =
        AppNotificationManagerImpl(context, context.resources, platformNotificationsManager!!)

    @Provides
    fun localBroadcastManager(context: Context?): LocalBroadcastManager =
        LocalBroadcastManager.getInstance(context!!)
}
