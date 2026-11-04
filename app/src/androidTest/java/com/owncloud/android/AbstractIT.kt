/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.facebook.testing.screenshot.Screenshot
import com.facebook.testing.screenshot.internal.TestNameDetector
import com.nextcloud.android.common.ui.theme.MaterialSchemes
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManagerImpl
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityManagerFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.client.preferences.DarkMode
import com.nextcloud.common.NextcloudClient
import com.nextcloud.test.GrantStoragePermissionRule
import com.nextcloud.test.RandomStringGenerator
import com.nextcloud.utils.PowerManagementFactory
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_BASE_URL
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.FileStorageUtils
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TestRule
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale

abstract class AbstractIT {
    @get:Rule
    val storagePermissionRule: TestRule = GrantStoragePermissionRule.grant()

    protected val connectivityServiceMock: ConnectivityService = ConnectivityManagerFactory.mock

    @JvmField
    protected var currentActivity: Activity? = null

    protected val storageManager = FileDataStorageManager(user, targetContext.contentResolver)

    protected val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)

    protected val allAccounts: Array<Account>
        get() = AccountManager.get(targetContext).accounts

    val randomName: String
        get() = getRandomName(RANDOM_NAME_LENGTH)

    protected val materialSchemesForCurrentUser: MaterialSchemes
        get() = MaterialSchemes.fromColor(R.color.primary)

    @Before
    fun enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true)
    }

    @After
    open fun after() {
        storageManager.removeLocalFiles(user, storageManager)
        storageManager.deleteAllFiles()
    }

    @Throws(AccountUtils.AccountNotFoundException::class)
    protected fun testOnlyOnServer(version: OwnCloudVersion) {
        assumeTrue(getCapability().version.isNewerOrEqual(version))
    }

    @Throws(AccountUtils.AccountNotFoundException::class)
    protected fun getCapability(): OCCapability {
        val client = OwnCloudClientFactory.createNextcloudClient(user, targetContext)
        return GetCapabilitiesRemoteOperation().execute(client).resultData
    }

    @Throws(IOException::class)
    protected fun getFile(filename: String): File = instrumentation.context.assets.open(filename).use { asset ->
        File.createTempFile("file", "file").also { FileUtils.copyInputStreamToFile(asset, it) }
    }

    protected fun waitForIdleSync() {
        instrumentation.waitForIdleSync()
    }

    fun onIdleSync(recipient: Runnable) {
        instrumentation.waitForIdle(recipient)
    }

    protected fun openDrawer(activityRule: IntentsTestRule<*>) {
        val sut = activityRule.launchActivity(null)
        shortSleep()
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        waitForIdleSync()
        screenshot(sut)
    }

    protected fun getCurrentActivity(): Activity? {
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .firstOrNull()
                ?.let { currentActivity = it }
        }
        return currentActivity
    }

    protected fun longSleep() {
        sleepIgnoringInterrupt(LONG_SLEEP_MILLIS)
    }

    protected fun sleep(second: Int) {
        sleepIgnoringInterrupt(MILLIS_PER_SECOND * second)
    }

    fun createFolder(remotePath: String) {
        if (ExistenceCheckRemoteOperation(remotePath, false).execute(client).isSuccess) return

        val result = CreateFolderOperation(remotePath, user, targetContext, storageManager).execute(client)
        assertTrue(result.isSuccess)
    }

    fun uploadFile(file: File, remotePath: String) {
        uploadOCUpload(OCUpload(file.absolutePath, remotePath, account.name))
    }

    open fun uploadOCUpload(ocUpload: OCUpload) {
        executeUpload(ocUpload, NameCollisionPolicy.DEFAULT, FileUploadWorker.LOCAL_BEHAVIOUR_COPY)
    }

    protected fun executeUpload(ocUpload: OCUpload, nameCollisionPolicy: NameCollisionPolicy, localBehaviour: Int) {
        val uploadsStorageManager = UploadsStorageManager(
            UserAccountManagerImpl.fromContext(targetContext),
            targetContext.contentResolver
        )

        val operation = UploadFileOperation(
            uploadsStorageManager,
            connectivityServiceMock,
            PowerManagementFactory.mock,
            user,
            null,
            ocUpload,
            nameCollisionPolicy,
            localBehaviour,
            targetContext,
            false,
            false,
            storageManager
        ).apply {
            addRenameUploadListener { }
            setRemoteFolderToBeCreated()
        }

        val result = operation.execute(client)
        assertTrue(result.logMessage, result.isSuccess)
    }

    protected fun enableRTL() {
        applyLocale(Locale("ar"))
    }

    protected fun resetLocale() {
        applyLocale(Locale("en"))
    }

    @Suppress("DEPRECATION")
    private fun applyLocale(locale: Locale) {
        val resources = instrumentation.targetContext.resources
        val config = resources.configuration.apply { setLocale(locale) }
        resources.updateConfiguration(config, null)
    }

    fun screenshotViaName(activity: Activity?, name: String?) {
        if (!isScreenshotSupported) return
        Screenshot.snapActivity(activity).setName(name).record()
    }

    protected fun screenshotViaName(view: View?, name: String?) {
        if (!isScreenshotSupported) return
        Screenshot.snap(view).setName(name).record()
    }

    @JvmOverloads
    protected fun screenshot(view: View?, prefix: String? = "") {
        if (!isScreenshotSupported) return
        Screenshot.snap(view).setName(createName(prefix)).record()
    }

    protected fun screenshot(sut: Activity?) {
        if (!isScreenshotSupported) return
        Screenshot.snapActivity(sut).setName(createName()).record()
    }

    protected fun screenshot(dialogFragment: DialogFragment, prefix: String?) {
        screenshot(requireNotNull(dialogFragment.requireDialog().window).decorView, prefix)
    }

    fun createName(name: String, prefix: String?): String = buildString {
        append(name)
        if (!prefix.isNullOrEmpty()) append("_").append(prefix)
        if (darkMode.isNotEmpty()) append("_").append(darkMode)
        if (color.isNotEmpty()) append("_").append(color)
    }

    private fun createName(prefix: String? = ""): String =
        createName("${TestNameDetector.getTestClass()}_${TestNameDetector.getTestName()}", prefix)

    fun getRandomName(length: Int): String = RandomStringGenerator.make(length)

    companion object {
        private const val TEST_ACCOUNT_NAME = "test@https://nextcloud.localhost"
        private const val TEST_ACCOUNT_PASSWORD = "password"
        private const val DUMMY_FILE_LINE = "123123123123123123123123123\n"
        private const val RANDOM_NAME_LENGTH = 5
        private const val MILLIS_PER_SECOND = 1000L
        private const val SHORT_SLEEP_MILLIS = 2_000L
        private const val LONG_SLEEP_MILLIS = 20_000L
        private const val DARK = "dark"
        private const val LIGHT = "light"
        private const val DEFAULT_COLOR = "blue"

        private val DUMMY_FILE_LINE_COUNTS = linkedMapOf(
            "empty.txt" to 0,
            "nonEmpty.txt" to 100,
            "chunkedFile.txt" to 500_000
        )

        private val SERVER_COLORS = mapOf(
            "red" to "#7c0000",
            "green" to "#00ff00",
            "white" to "#ffffff",
            "black" to "#000000",
            "lightgreen" to "#aaff00"
        )

        private val instrumentation: Instrumentation
            get() = InstrumentationRegistry.getInstrumentation()

        private val isScreenshotSupported: Boolean
            get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R

        @JvmStatic
        protected lateinit var client: OwnCloudClient

        @JvmStatic
        protected lateinit var nextcloudClient: NextcloudClient

        @JvmStatic
        protected lateinit var account: Account

        @JvmStatic
        protected lateinit var user: User

        @JvmStatic
        protected lateinit var targetContext: Context

        private var darkMode = ""
        private var color = ""

        @BeforeClass
        @JvmStatic
        fun beforeAll() {
            targetContext = instrumentation.targetContext
            removeAccountsOfType(MainApp.getAccountType(targetContext))

            rethrowClientSetupFailure {
                account = createAccount(TEST_ACCOUNT_NAME)
                user = getUser(account)
                client = OwnCloudClientFactory.createOwnCloudClient(account, targetContext)
                nextcloudClient = OwnCloudClientFactory.createNextcloudClient(user, targetContext)
            }

            val arguments = InstrumentationRegistry.getArguments()
            applyServerColorArgument(arguments.getString("COLOR"))
            applyDarkModeArgument(arguments.getString("DARKMODE"))

            if (LIGHT.equals(darkMode, ignoreCase = true) && DEFAULT_COLOR.equals(color, ignoreCase = true)) {
                darkMode = ""
                color = ""
            }
        }

        private fun applyServerColorArgument(colorArgument: String?) {
            if (colorArgument.isNullOrEmpty()) return

            color = colorArgument
            val fileDataStorageManager = FileDataStorageManager(user, targetContext.contentResolver)
            val capability = fileDataStorageManager.getCapability(account.name).apply {
                groupfolders = CapabilityBooleanType.TRUE
                SERVER_COLORS[colorArgument]?.let { serverColor = it }
            }
            fileDataStorageManager.saveCapabilities(capability)
        }

        private fun applyDarkModeArgument(darkModeArgument: String?) {
            if (darkModeArgument == null) return

            if (!DARK.equals(darkModeArgument, ignoreCase = true)) {
                darkMode = LIGHT
                return
            }

            darkMode = DARK
            AppPreferencesImpl.fromContext(targetContext).setDarkThemeMode(DarkMode.DARK)
            MainApp.setAppTheme(DarkMode.DARK)
        }

        @JvmStatic
        protected fun <T> rethrowClientSetupFailure(setup: () -> T): T = try {
            setup()
        } catch (e: OperationCanceledException) {
            throw RuntimeException("Error setting up clients", e)
        } catch (e: IOException) {
            throw RuntimeException("Error setting up clients", e)
        } catch (e: AccountUtils.AccountNotFoundException) {
            throw RuntimeException("Error setting up clients", e)
        } catch (e: AuthenticatorException) {
            throw RuntimeException("Error setting up clients", e)
        }

        @JvmStatic
        protected fun removeAccountsOfType(accountType: String) {
            val platformAccountManager = AccountManager.get(targetContext)
            platformAccountManager.accounts
                .filter { it.type.equals(accountType, ignoreCase = true) }
                .forEach { platformAccountManager.removeAccountExplicitly(it) }
        }

        @JvmStatic
        @Throws(IOException::class)
        protected fun createDummyFiles(): List<File> {
            val tempPath = File(FileStorageUtils.getTemporalPath(account.name))
            if (!tempPath.exists()) {
                assertTrue(tempPath.mkdirs())
            }
            assertTrue(tempPath.exists())

            return DUMMY_FILE_LINE_COUNTS.map { (name, lineCount) -> createFile(name, lineCount) }
        }

        @JvmStatic
        @Throws(IOException::class)
        protected fun getDummyFile(name: String): File {
            val internalTempPath = FileStorageUtils.getInternalTemporalPath(account.name, targetContext)
            val file = File(internalTempPath + File.separator + name)

            if (file.exists()) return file

            if (name.endsWith("/")) {
                file.mkdirs()
                return file
            }

            return createFile(name, DUMMY_FILE_LINE_COUNTS[name] ?: 0)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun createFile(name: String, iteration: Int): File {
            val file = File(FileStorageUtils.getTemporalPath(account.name) + File.separator + name)
            val parent = requireNotNull(file.parentFile)
            if (!parent.exists()) {
                assertTrue(parent.mkdirs())
            }

            file.createNewFile()
            FileWriter(file).use { writer -> repeat(iteration) { writer.write(DUMMY_FILE_LINE) } }

            return file
        }

        @JvmStatic
        protected fun shortSleep() {
            sleepIgnoringInterrupt(SHORT_SLEEP_MILLIS)
        }

        private fun sleepIgnoringInterrupt(millis: Long) {
            try {
                Thread.sleep(millis)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        fun getUserId(user: User): String? =
            AccountManager.get(targetContext).getUserData(user.toPlatformAccount(), KEY_USER_ID)

        @JvmStatic
        protected fun getUser(account: Account): User =
            UserAccountManagerImpl.fromContext(targetContext).getUser(account.name).orElseThrow { IllegalAccessError() }

        @JvmStatic
        protected fun createAccount(name: String): Account {
            val atPos = name.lastIndexOf('@')
            val platformAccount = Account(name, MainApp.getAccountType(targetContext))

            AccountManager.get(targetContext).run {
                addAccountExplicitly(platformAccount, TEST_ACCOUNT_PASSWORD, null)
                setUserData(platformAccount, KEY_OC_BASE_URL, name.substring(atPos + 1))
                setUserData(platformAccount, KEY_USER_ID, name.substring(0, atPos))
            }

            return getRegisteredAccount(name)
        }

        @JvmStatic
        protected fun getRegisteredAccount(name: String): Account {
            val account = UserAccountManagerImpl.fromContext(targetContext).getAccountByName(name)
            if (account.type == targetContext.getString(R.string.anonymous_account_type)) {
                throw RuntimeException("Could not get account with name $name")
            }
            return account
        }

        @JvmStatic
        protected fun removeAccount(account: Account): Boolean =
            AccountManager.get(targetContext).removeAccountExplicitly(account)
    }
}
