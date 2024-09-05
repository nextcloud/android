/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.di;

import android.accounts.AccountManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Handler;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.client.appinfo.AppInfo;
import com.nextcloud.client.core.AsyncRunner;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.core.ClockImpl;
import com.nextcloud.client.core.ThreadPoolAsyncRunner;
import com.nextcloud.client.database.dao.ArbitraryDataDao;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.jobs.operation.FileOperationHelper;
import com.nextcloud.client.logger.FileLogHandler;
import com.nextcloud.client.logger.Logger;
import com.nextcloud.client.logger.LoggerImpl;
import com.nextcloud.client.logger.LogsRepository;
import com.nextcloud.client.migrations.Migrations;
import com.nextcloud.client.migrations.MigrationsDb;
import com.nextcloud.client.migrations.MigrationsManager;
import com.nextcloud.client.migrations.MigrationsManagerImpl;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.notifications.AppNotificationManager;
import com.nextcloud.client.notifications.AppNotificationManagerImpl;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.utils.Throttler;
import com.owncloud.android.providers.UsersAndGroupsSearchConfig;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApi;
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApiImpl;
import com.owncloud.android.ui.activities.data.activities.RemoteActivitiesRepository;
import com.owncloud.android.ui.activities.data.files.FilesRepository;
import com.owncloud.android.ui.activities.data.files.FilesServiceApiImpl;
import com.owncloud.android.ui.activities.data.files.RemoteFilesRepository;
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import dagger.Module;
import dagger.Provides;

@Module(includes = {ComponentsModule.class, VariantComponentsModule.class, BuildTypeComponentsModule.class})
class AppModule {

    @Provides
    AccountManager accountManager(Application application) {
        return (AccountManager) application.getSystemService(Context.ACCOUNT_SERVICE);
    }

    @Provides
    Context context(Application application) {
        return application;
    }

    @Provides
    PackageManager packageManager(Application application) {
        return application.getPackageManager();
    }

    @Provides
    ContentResolver contentResolver(Context context) {
        return context.getContentResolver();
    }

    @Provides
    Resources resources(Application application) {
        return application.getResources();
    }

    @Provides
    UserAccountManager userAccountManager(
        Context context,
        AccountManager accountManager) {
        return new UserAccountManagerImpl(context, accountManager);
    }

    @Provides
    ArbitraryDataProvider arbitraryDataProvider(ArbitraryDataDao dao) {
        return new ArbitraryDataProviderImpl(dao);
    }

    @Provides
    SyncedFolderProvider syncedFolderProvider(ContentResolver contentResolver,
                                              AppPreferences appPreferences,
                                              Clock clock) {
        return new SyncedFolderProvider(contentResolver, appPreferences, clock);
    }

    @Provides
    ActivitiesServiceApi activitiesServiceApi(UserAccountManager accountManager) {
        return new ActivitiesServiceApiImpl(accountManager);
    }

    @Provides
    ActivitiesRepository activitiesRepository(ActivitiesServiceApi api) {
        return new RemoteActivitiesRepository(api);
    }

    @Provides
    FilesRepository filesRepository(UserAccountManager accountManager, ClientFactory clientFactory) {
        return new RemoteFilesRepository(new FilesServiceApiImpl(accountManager, clientFactory));
    }

    @Provides
    UploadsStorageManager uploadsStorageManager(CurrentAccountProvider currentAccountProvider,
                                                Context context) {
        return new UploadsStorageManager(currentAccountProvider, context.getContentResolver());
    }

    @Provides
    FileDataStorageManager fileDataStorageManager(CurrentAccountProvider currentAccountProvider,
                                                  Context context) {
        return new FileDataStorageManager(currentAccountProvider.getUser(), context.getContentResolver());
    }

    @Provides
    CurrentAccountProvider currentAccountProvider(UserAccountManager accountManager) {
        return accountManager;
    }

    @Provides
    DeviceInfo deviceInfo() {
        return new DeviceInfo();
    }

    @Provides
    @Singleton
    Clock clock() {
        return new ClockImpl();
    }

    @Provides
    @Singleton
    Logger logger(Context context, Clock clock) {
        File logDir = new File(context.getFilesDir(), "logs");
        FileLogHandler handler = new FileLogHandler(logDir, "log.txt", 1024 * 1024);
        LoggerImpl logger = new LoggerImpl(clock, handler, new Handler(), 1000);
        logger.start();
        return logger;
    }

    @Provides
    @Singleton
    LogsRepository logsRepository(Logger logger) {
        return (LogsRepository) logger;
    }

    @Provides
    @Singleton
    AsyncRunner uiAsyncRunner() {
        Handler uiHandler = new Handler();
        return new ThreadPoolAsyncRunner(uiHandler, 4, "ui");
    }

    @Provides
    @Singleton
    @Named("io")
    AsyncRunner ioAsyncRunner() {
        Handler uiHandler = new Handler();
        return new ThreadPoolAsyncRunner(uiHandler, 8, "io");
    }

    @Provides
    NotificationManager notificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    AudioManager audioManager(Context context) {
        return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Provides
    @Singleton
    EventBus eventBus() {
        return EventBus.getDefault();
    }

    @Provides
    @Singleton
    MigrationsDb migrationsDb(Application application) {
        SharedPreferences store = application.getSharedPreferences("migrations", Context.MODE_PRIVATE);
        return new MigrationsDb(store);
    }

    @Provides
    @Singleton
    MigrationsManager migrationsManager(MigrationsDb migrationsDb,
                                        AppInfo appInfo,
                                        AsyncRunner asyncRunner,
                                        Migrations migrations) {
        return new MigrationsManagerImpl(appInfo, migrationsDb, asyncRunner, migrations.getSteps());
    }

    @Provides
    @Singleton
    AppNotificationManager notificationsManager(Context context,
                                                NotificationManager platformNotificationsManager,
                                                Provider<ViewThemeUtils> viewThemeUtilsProvider) {
        return new AppNotificationManagerImpl(context,
                                              context.getResources(),
                                              platformNotificationsManager,
                                              viewThemeUtilsProvider.get());
    }

    @Provides
    LocalBroadcastManager localBroadcastManager(Context context) {
        return LocalBroadcastManager.getInstance(context);
    }

    @Provides
    Throttler throttler(Clock clock) {
        return new Throttler(clock);
    }

    @Provides
    @Singleton
    PassCodeManager passCodeManager(AppPreferences preferences, Clock clock) {
        return new PassCodeManager(preferences, clock);
    }

    @Provides
    FileOperationHelper fileOperationHelper(CurrentAccountProvider currentAccountProvider, Context context) {
        return new FileOperationHelper(currentAccountProvider.getUser(), context, fileDataStorageManager(currentAccountProvider, context));
    }

    @Provides
    @Singleton
    UsersAndGroupsSearchConfig userAndGroupSearchConfig() {
        return new UsersAndGroupsSearchConfig();
    }


    @Provides
    @Singleton
    CertificateValidator certificateValidator() {
        return new CertificateValidator();
    }
}
