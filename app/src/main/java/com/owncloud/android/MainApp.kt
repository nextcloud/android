/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.StrictMode
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.nextcloud.appReview.InAppReviewHelper
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.di.ActivityInjector
import com.nextcloud.client.di.AppComponent
import com.nextcloud.client.di.DaggerAppComponent
import com.nextcloud.client.errorhandling.ExceptionHandler
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.logger.LegacyLoggerAdapter
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.migrations.AutoUploadEntriesMigration
import com.nextcloud.client.migrations.MigrationsManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.network.NetworkChangeListener
import com.nextcloud.client.network.WalledCheckCache
import com.nextcloud.client.notifications.AppNotificationChannels
import com.nextcloud.client.onboarding.OnboardingService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.DarkMode
import com.nextcloud.client.systembars.SystemBarBackgroundCallbacks
import com.nextcloud.ui.composeActivity.ComposeProcessTextAlias
import com.nextcloud.utils.extensions.registerBroadcastReceiver
import com.nextcloud.utils.extensions.showToast
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.authentication.GlobalPassCodeProtectionCallbacks
import com.owncloud.android.authentication.PassCodeManager
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.ReceiverFlag
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datastorage.StoragePathFixer
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.providers.documentstorage.DocumentsStorageProvider
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import com.owncloud.android.utils.FilesSyncHelper
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.ReceiversHelper
import com.owncloud.android.utils.SecurityUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.conscrypt.Conscrypt
import org.greenrobot.eventbus.EventBus
import java.lang.ref.WeakReference
import java.security.NoSuchAlgorithmException
import java.security.Security
import javax.inject.Inject
import javax.inject.Provider
import javax.net.ssl.SSLContext

open class MainApp :
    Application(),
    HasAndroidInjector,
    NetworkChangeListener {

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject
    lateinit var onboarding: OnboardingService

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var powerManagementService: PowerManagementService

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var appInfo: AppInfo

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var migrationsManager: MigrationsManager

    @Inject
    lateinit var inAppReviewHelper: InAppReviewHelper

    @Inject
    lateinit var passCodeManager: PassCodeManager

    @Inject
    lateinit var walledCheckCache: WalledCheckCache

    @Inject
    lateinit var composeProcessTextAlias: ComposeProcessTextAlias

    @Inject
    lateinit var viewThemeUtilsProvider: Provider<ViewThemeUtils>

    private val restrictionsFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)

    private val restrictionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            setProxyConfig()
        }
    }

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> onAppForeground()
            Lifecycle.Event.ON_STOP -> onAppBackground()
            Lifecycle.Event.ON_RESUME -> onAppResume()
            else -> Unit
        }
    }

    private val isCrashReportingProcess: Boolean
        get() = getProcessName().endsWith(CRASH_REPORTING_PROCESS_SUFFIX)

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initGlobalContext(this)
        initDagger()
        installCrashReporter()
    }

    private fun initDagger() {
        val component = DaggerAppComponent.builder()
            .application(this)
            .build()
        appComponent = component
        component.inject(this)
    }

    private fun installCrashReporter() {
        if (isCrashReportingProcess || appInfo.isDebugBuild) {
            return
        }

        val defaultPlatformHandler = Thread.getDefaultUncaughtExceptionHandler() ?: return
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler(this, defaultPlatformHandler))
    }

    @SuppressFBWarnings("ST")
    override fun onCreate() {
        enableStrictMode()
        val viewThemeUtils = viewThemeUtilsProvider.get()
        setAppTheme(preferences.getDarkThemeMode())
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
        insertConscrypt()
        registerActivityLifecycleCallbacks(ActivityInjector())
        registerActivityLifecycleCallbacks(SystemBarBackgroundCallbacks(viewThemeUtilsProvider))
        inAppReviewHelper.resetAndIncrementAppRestartCounter()
        startMigrations()
        SecurityUtils()
        StoragePathFixer(preferences).fixIfNeeded()
        cancelDownloadJobsIfRequested()
        storagePath = preferences.getStoragePath(applicationContext.filesDir.absolutePath)
        OwnCloudClientManagerFactory.setUserAgent(userAgent)
        initProxy()
        ThumbnailsCacheManager.initDiskCacheAsync()
        enableFileLoggingIfAllowed()
        disableDeathOnFileUriExposure()

        Log_OC.d(TAG, "scheduleContentObserverJob, called")
        backgroundJobManager.scheduleContentObserverJob()

        initSyncOperations(
            this,
            preferences,
            uploadsStorageManager,
            accountManager,
            connectivityService,
            powerManagementService,
            backgroundJobManager,
            clock,
            viewThemeUtils,
            walledCheckCache
        )
        initContactsBackup(accountManager, backgroundJobManager)
        AppNotificationChannels.create(getAppContext())
        scheduleBackgroundJobs()
        registerActivityLifecycleCallbacks(GlobalPassCodeProtectionCallbacks(onboarding, passCodeManager))

        if (!MDMConfig.sendFilesSupport(this)) {
            disableDocumentsStorageProvider()
        }

        connectivityService.addListener(this)
    }

    fun disableDocumentsStorageProvider() {
        packageManager.setComponentEnabledSetting(
            ComponentName(this, DocumentsStorageProvider::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun onAppForeground() {
        Log_OC.d(TAG, "APP IN FOREGROUND")
        composeProcessTextAlias.configure()

        if (!preferences.startAutoUploadOnStart()) {
            return
        }

        FilesSyncHelper.startAutoUploadForEnabledSyncedFolders(
            syncedFolderProvider,
            backgroundJobManager,
            overridePowerSaving = false
        )
        preferences.setLastAutoUploadOnStartTime(System.currentTimeMillis())
    }

    private fun onAppBackground() {
        passCodeManager.canAskPin = true
        Log_OC.d(TAG, "APP IN BACKGROUND")
    }

    private fun onAppResume() {
        setProxyConfig()
        Log_OC.d(TAG, "APP ON RESUME")
    }

    private fun startMigrations() {
        val startedMigrationsCount = migrationsManager.startMigration()
        logger.i(TAG, "Started $startedMigrationsCount migrations")
    }

    private fun initProxy() {
        if (!isClientBrandedPlus) {
            setProxyForNonBrandedPlusClients()
            return
        }

        setProxyConfig()
        registerBroadcastReceiver(restrictionsReceiver, restrictionsFilter, ReceiverFlag.NotExported)
    }

    private fun setProxyForNonBrandedPlusClients() {
        try {
            OwnCloudClientManagerFactory.setProxyHost(resources.getString(R.string.proxy_host))
            OwnCloudClientManagerFactory.setProxyPort(resources.getInteger(R.integer.proxy_port))
        } catch (e: Resources.NotFoundException) {
            Log_OC.d(TAG, "Error caught at setProxyForNonBrandedPlusClients: $e")
        }
    }

    private fun setProxyConfig() {
        if (!isClientBrandedPlus) {
            Log_OC.d(TAG, "Proxy configuration cannot be set. Client is not branded plus.")
            return
        }

        val host = MDMConfig.getHost(this)
        val port = MDMConfig.getPort(this)
        if (host.isEmpty() || port == UNDEFINED_PROXY_PORT) {
            Log_OC.d(TAG, "Proxy configuration cannot be found")
            return
        }

        try {
            OwnCloudClientManagerFactory.setProxyHost(host)
            OwnCloudClientManagerFactory.setProxyPort(port)
            Log_OC.d(TAG, "Proxy configuration successfully set")
        } catch (e: Resources.NotFoundException) {
            Log_OC.e(TAG, "Proxy config cannot able to set due to: \$e")
        }
    }

    private fun insertConscrypt() {
        Security.insertProviderAt(Conscrypt.newProvider(), HIGHEST_SECURITY_PROVIDER_POSITION)

        try {
            val version = Conscrypt.version()
            Log_OC.i(TAG, "Using Conscrypt/${version.major()}$DOT${version.minor()}$DOT${version.patch()} for TLS")
            val engine = SSLContext.getDefault().createSSLEngine()
            Log_OC.i(TAG, "Enabled protocols: ${engine.enabledProtocols.contentToString()} }")
            Log_OC.i(TAG, "Enabled ciphers: ${engine.enabledCipherSuites.contentToString()} }")
        } catch (e: NoSuchAlgorithmException) {
            Log_OC.e(TAG, e.message)
        }
    }

    private fun enableStrictMode() {
        if (!BuildConfig.DEBUG || !BuildConfig.RUNTIME_PERF_ANALYSIS) {
            return
        }

        Log_OC.d(TAG, "Enabling StrictMode")
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }

    private fun enableFileLoggingIfAllowed() {
        if (!MDMConfig.isLogEnabled(this)) {
            return
        }

        Log_OC.setLoggerImplementation(LegacyLoggerAdapter(logger))
        Log_OC.d(DEBUG_TAG, "start logging")
    }

    @Suppress("TooGenericExceptionCaught")
    private fun disableDeathOnFileUriExposure() {
        try {
            StrictMode::class.java.getMethod("disableDeathOnFileUriExposure").invoke(null)
        } catch (e: Exception) {
            Log_OC.d(DEBUG_TAG, "Failed to disable uri exposure")
        }
    }

    private fun cancelDownloadJobsIfRequested() {
        if (!preferences.shouldStopDownloadJobsOnStart()) {
            return
        }

        backgroundJobManager.cancelAllFilesDownloadJobs()
        preferences.setStopDownloadJobsOnStart(false)
    }

    private fun scheduleBackgroundJobs() {
        backgroundJobManager.scheduleMediaFoldersDetectionJob()
        backgroundJobManager.startMediaFoldersDetectionJob()
        backgroundJobManager.schedulePeriodicAutoUpload()
        backgroundJobManager.schedulePeriodicHealthStatus()

        if (preferences.isTwoWaySyncEnabled()) {
            backgroundJobManager.scheduleInternal2WaySync(preferences.getTwoWaySyncInterval())
        }

        backgroundJobManager.startPeriodicallyOfflineOperation()
    }

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun networkAndServerConnectionListener(isNetworkAndServerAvailable: Boolean) {
        if (isNetworkAndServerAvailable) {
            backgroundJobManager.startOfflineOperations()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        connectivityService.removeListener(this)
        ReceiversHelper.shutdown()
    }

    companion object {
        @JvmField
        val OUTDATED_SERVER_VERSION: OwnCloudVersion = NextcloudVersion.nextcloud_30

        @JvmField
        val MINIMUM_SUPPORTED_SERVER_VERSION: OwnCloudVersion = OwnCloudVersion.nextcloud_20

        const val DOT: String = "."

        private val TAG: String = MainApp::class.java.simpleName
        private const val DEBUG_TAG = "Debug"
        private const val CRASH_REPORTING_PROCESS_SUFFIX = ":crash"
        private const val UNDEFINED_PROXY_PORT = -1
        private const val HIGHEST_SECURITY_PROVIDER_POSITION = 1
        private const val NO_PACKAGE_INFO_FLAGS = 0

        private var appContext: WeakReference<Context>? = null
        private var storagePath: String? = null
        private var appComponent: AppComponent? = null

        @JvmStatic
        var isOnlyOnDevice: Boolean = false
            private set

        @JvmStatic
        var isOnlyPersonFiles: Boolean = false
            private set

        private fun initGlobalContext(context: Context) {
            appContext = WeakReference(context)
        }

        @JvmStatic
        fun getAppComponent(): AppComponent = checkNotNull(appComponent) { "Dagger not initialized!" }

        @JvmStatic
        val isClientBranded: Boolean
            get() = getAppContext().resources.getBoolean(R.bool.is_branded_client)

        @JvmStatic
        val isClientBrandedPlus: Boolean
            get() = getAppContext().resources.getBoolean(R.bool.is_branded_plus_client)

        @JvmStatic
        fun initContactsBackup(accountManager: UserAccountManager?, backgroundJobManager: BackgroundJobManager?) {
            if (accountManager == null || backgroundJobManager == null) {
                return
            }

            val arbitraryDataProvider = ArbitraryDataProviderImpl(getAppContext())
            accountManager.allUsers
                .filter {
                    arbitraryDataProvider.getBooleanValue(
                        it,
                        ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP
                    )
                }
                .forEach { backgroundJobManager.schedulePeriodicContactsBackup(it) }
        }

        @JvmStatic
        fun initSyncOperations(
            context: Context,
            preferences: AppPreferences,
            uploadsStorageManager: UploadsStorageManager,
            accountManager: UserAccountManager,
            connectivityService: ConnectivityService,
            powerManagementService: PowerManagementService,
            backgroundJobManager: BackgroundJobManager,
            clock: Clock,
            viewThemeUtils: ViewThemeUtils?,
            walledCheckCache: WalledCheckCache?
        ) {
            val appContext = getAppContext()
            AutoUploadEntriesMigration.updateToAutoUpload(context)
            AutoUploadEntriesMigration.cleanOldEntries(appContext, clock)
            AutoUploadEntriesMigration.updateAutoUploadEntries(appContext, clock)

            if (PermissionUtil.checkStoragePermission(appContext)) {
                AutoUploadEntriesMigration.splitOutAutoUploadEntries(appContext, clock)
            } else {
                preferences.setAutoUploadSplitEntriesEnabled(true)
            }

            if (!preferences.isAutoUploadInitialized()) {
                preferences.setAutoUploadInit(true)
            }

            FilesSyncHelper.restartUploadsIfNeeded(
                uploadsStorageManager,
                accountManager,
                connectivityService,
                powerManagementService
            )

            backgroundJobManager.scheduleOfflineSync()

            ReceiversHelper.registerNetworkChangeReceiver(
                uploadsStorageManager,
                accountManager,
                connectivityService,
                powerManagementService,
                walledCheckCache
            )
            ReceiversHelper.registerPowerChangeReceiver(
                uploadsStorageManager,
                accountManager,
                connectivityService,
                powerManagementService
            )
            ReceiversHelper.registerPowerSaveReceiver(
                uploadsStorageManager,
                accountManager,
                connectivityService,
                powerManagementService
            )
        }

        @JvmStatic
        fun string(@StringRes id: Int): String = getAppContext().getString(id)

        @JvmStatic
        fun string(@StringRes id: Int, args: Any?): String = getAppContext().getString(id, args)

        @JvmStatic
        fun getAppContext(): Context = appContext!!.get()!!

        @JvmStatic
        fun setAppContext(context: Context?) {
            appContext = WeakReference(context)
        }

        @JvmStatic
        fun getStoragePath(): String? = storagePath

        @JvmStatic
        fun setStoragePath(path: String?) {
            storagePath = path
        }

        @JvmStatic
        fun getAccountType(context: Context): String = context.getString(R.string.account_type)

        @JvmStatic
        val authority: String
            get() = string(R.string.authority)

        @JvmStatic
        val authTokenType: String
            get() = string(R.string.authority)

        @JvmStatic
        val dbFile: String
            get() = string(R.string.db_file)

        @JvmStatic
        val dbName: String
            get() = string(R.string.db_name)

        @JvmStatic
        val dataFolder: String
            get() = string(R.string.data_folder)

        @JvmStatic
        fun showOnlyFilesOnDevice(state: Boolean) {
            isOnlyOnDevice = state
        }

        @JvmStatic
        fun showOnlyPersonalFiles(state: Boolean) {
            isOnlyPersonFiles = state
        }

        @JvmStatic
        val menuItemId: Int?
            get() = when {
                isOnlyPersonFiles -> R.id.nav_personal_files
                isOnlyOnDevice -> R.id.nav_on_device
                else -> null
            }

        @JvmStatic
        val userAgent: String
            get() = buildUserAgent(R.string.nextcloud_user_agent)

        @JvmStatic
        fun showMessage(@StringRes messageId: Int) {
            getAppContext().showToast(messageId)
        }

        private fun buildUserAgent(@StringRes agentFormat: Int): String {
            val appString = string(agentFormat)
            val brandedName = string(R.string.name_for_branded_user_agent)
            return appString.format(appVersionName(), brandedName)
        }

        private fun appVersionName(): String? {
            val context = getAppContext()
            return try {
                val packageInfo: PackageInfo? =
                    context.packageManager.getPackageInfo(context.packageName, NO_PACKAGE_INFO_FLAGS)
                if (packageInfo != null) packageInfo.versionName else ""
            } catch (e: PackageManager.NameNotFoundException) {
                Log_OC.e(TAG, "Trying to get packageName", e.cause)
                ""
            }
        }

        @JvmStatic
        fun setAppTheme(mode: DarkMode) {
            val nightMode = when (mode) {
                DarkMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                DarkMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                DarkMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }
}
