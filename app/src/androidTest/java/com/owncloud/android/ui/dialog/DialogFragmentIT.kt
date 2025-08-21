/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Dialog
import android.content.Intent
import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Looper
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.UiThread
import androidx.fragment.app.DialogFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextcloud.android.common.ui.color.ColorUtil
import com.nextcloud.android.lib.resources.profile.Action
import com.nextcloud.android.lib.resources.profile.HoverCard
import com.nextcloud.client.account.RegisteredUser
import com.nextcloud.client.account.Server
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.documentscan.AppScanOptionalFeature
import com.nextcloud.ui.ChooseAccountDialogFragment.Companion.newInstance
import com.nextcloud.ui.SetOnlineStatusBottomSheet
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.AbstractIT
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.authentication.EnforcedServer
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.Creator
import com.owncloud.android.lib.common.DirectEditing
import com.owncloud.android.lib.common.Editor
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.accounts.AccountTypeUtils
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.LoadingDialog.Companion.newInstance
import com.owncloud.android.ui.dialog.RenameFileDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.Companion.newInstanceForEmptySslError
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment.Companion.newInstance
import com.owncloud.android.ui.fragment.OCFileListBottomSheetActions
import com.owncloud.android.ui.fragment.OCFileListBottomSheetDialog
import com.owncloud.android.ui.fragment.ProfileBottomSheetDialog
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.ScreenshotTest
import com.owncloud.android.utils.theme.CapabilityUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.util.function.Supplier

@Suppress("TooManyFunctions")
class DialogFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.dialog.DialogFragmentIT"
    private val serverUrl = "https://nextcloud.localhost"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun quitLooperIfNeeded() {
        Looper.myLooper()?.quitSafely()
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testRenameFileDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        newInstance(
            OCFile("/Test/"),
            OCFile("/")
        ).run {
            showDialog(this)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testLoadingDialog() {
        newInstance("Wait…").run {
            showDialog(this)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testConfirmationDialogWithOneAction() {
        ConfirmationDialogFragment.newInstance(
            R.string.upload_list_empty_text_auto_upload,
            arrayOf(),
            R.string.filedetails_sync_file,
            R.string.common_ok,
            -1,
            -1,
            -1
        ).run {
            showDialog(this)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testConfirmationDialogWithTwoAction() {
        ConfirmationDialogFragment.newInstance(
            R.string.upload_list_empty_text_auto_upload,
            arrayOf(),
            R.string.filedetails_sync_file,
            R.string.common_ok,
            R.string.common_cancel,
            -1,
            -1
        ).run {
            showDialog(this)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testConfirmationDialogWithThreeAction() {
        ConfirmationDialogFragment.newInstance(
            R.string.upload_list_empty_text_auto_upload,
            arrayOf(),
            R.string.filedetails_sync_file,
            R.string.common_ok,
            R.string.common_cancel,
            R.string.common_confirm,
            -1
        ).run {
            showDialog(this)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testConfirmationDialogWithThreeActionRTL() {
        enableRTL()
        ConfirmationDialogFragment.newInstance(
            R.string.upload_list_empty_text_auto_upload,
            arrayOf(),
            -1,
            R.string.common_ok,
            R.string.common_cancel,
            R.string.common_confirm,
            -1
        ).run {
            showDialog(this)
            resetLocale()
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testRemoveFileDialog() {
        RemoveFilesDialogFragment.newInstance(OCFile("/Test.md")).run {
            showDialog(this)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testRemoveFilesDialog() {
        val toDelete = ArrayList<OCFile>().apply {
            add(OCFile("/Test.md"))
            add(OCFile("/Document.odt"))
        }

        val dialog: RemoveFilesDialogFragment = RemoveFilesDialogFragment.newInstance(toDelete)
        showDialog(dialog)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testRemoveFolderDialog() {
        val dialog = RemoveFilesDialogFragment.newInstance(OCFile("/Folder/"))
        showDialog(dialog)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testRemoveFoldersDialog() {
        val toDelete = ArrayList<OCFile>()
        toDelete.add(OCFile("/Folder/"))
        toDelete.add(OCFile("/Documents/"))

        val dialog: RemoveFilesDialogFragment = RemoveFilesDialogFragment.newInstance(toDelete)
        showDialog(dialog)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testNewFolderDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val sut = CreateFolderDialogFragment.newInstance(OCFile("/"))
        showDialog(sut)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testEnforcedPasswordDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val sut = newInstance(OCFile("/"), true, false)
        showDialog(sut)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testOptionalPasswordDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
        val sut = newInstance(OCFile("/"), true, true)
        showDialog(sut)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testAccountChooserDialog() {
        val intent = Intent(targetContext, FileDisplayActivity::class.java)
        ActivityScenario.launch<FileDisplayActivity>(intent).use { scenario ->
            scenario.onActivity { activity: FileDisplayActivity ->
                EspressoIdlingResource.increment()

                val userAccountManager = activity.userAccountManager
                val accountManager = AccountManager.get(targetContext)
                for (account in accountManager.getAccountsByType(MainApp.getAccountType(targetContext))) {
                    accountManager.removeAccountExplicitly(account)
                }

                val newAccount = Account("test@https://nextcloud.localhost", MainApp.getAccountType(targetContext))
                accountManager.addAccountExplicitly(newAccount, "password", null)
                accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, serverUrl)
                accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, "test")
                accountManager.setAuthToken(
                    newAccount,
                    AccountTypeUtils.getAuthTokenTypePass(newAccount.type),
                    "password"
                )
                val newUser = userAccountManager.getUser(newAccount.name)
                    .orElseThrow(Supplier { RuntimeException() })
                userAccountManager.setCurrentOwnCloudAccount(newAccount.name)

                val newAccount2 = Account("user1@nextcloud.localhost", MainApp.getAccountType(targetContext))
                accountManager.addAccountExplicitly(newAccount2, "password", null)
                accountManager.setUserData(newAccount2, AccountUtils.Constants.KEY_OC_BASE_URL, serverUrl)
                accountManager.setUserData(newAccount2, AccountUtils.Constants.KEY_USER_ID, "user1")
                accountManager.setUserData(newAccount2, AccountUtils.Constants.KEY_OC_VERSION, "20.0.0")
                accountManager.setAuthToken(
                    newAccount2,
                    AccountTypeUtils.getAuthTokenTypePass(newAccount.type),
                    "password"
                )

                val fileDataStorageManager = FileDataStorageManager(
                    newUser,
                    targetContext.contentResolver
                )

                val capability = OCCapability().apply {
                    userStatus = CapabilityBooleanType.TRUE
                    userStatusSupportsEmoji = CapabilityBooleanType.TRUE
                }
                fileDataStorageManager.saveCapabilities(capability)

                EspressoIdlingResource.decrement()

                try {
                    onIdleSync {
                        val sut = newInstance(
                            RegisteredUser(
                                newAccount,
                                OwnCloudAccount(newAccount, targetContext),
                                Server(URI.create(serverUrl), OwnCloudVersion.nextcloud_20)
                            )
                        )
                        showDialog(activity, sut)

                        sut.setStatus(
                            Status(
                                StatusType.DND,
                                "Busy fixing 🐛…",
                                "",
                                -1
                            ),
                            targetContext
                        )
                        screenshot(sut, "dnd")

                        sut.setStatus(
                            Status(
                                StatusType.ONLINE,
                                "",
                                "",
                                -1
                            ),
                            targetContext
                        )
                        screenshot(sut, "online")

                        sut.setStatus(
                            Status(
                                StatusType.ONLINE,
                                "Let's have some fun",
                                "🎉",
                                -1
                            ),
                            targetContext
                        )
                        screenshot(sut, "fun")

                        sut.setStatus(
                            Status(StatusType.OFFLINE, "", "", -1),
                            targetContext
                        )
                        screenshot(sut, "offline")

                        sut.setStatus(
                            Status(StatusType.AWAY, "Vacation", "🌴", -1),
                            targetContext
                        )
                        screenshot(sut, "away")
                    }
                } catch (e: AccountUtils.AccountNotFoundException) {
                    throw java.lang.RuntimeException(e)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    @Throws(AccountUtils.AccountNotFoundException::class)
    fun testAccountChooserDialogWithStatusDisabled() {
        val accountManager = AccountManager.get(targetContext)
        for (account in accountManager.accounts) {
            accountManager.removeAccountExplicitly(account)
        }

        val newAccount = Account("test@https://nextcloud.localhost", MainApp.getAccountType(targetContext))
        accountManager.addAccountExplicitly(newAccount, "password", null)
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_OC_BASE_URL, serverUrl)
        accountManager.setUserData(newAccount, AccountUtils.Constants.KEY_USER_ID, "test")
        accountManager.setAuthToken(newAccount, AccountTypeUtils.getAuthTokenTypePass(newAccount.type), "password")

        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { fda ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val userAccountManager = fda.userAccountManager
                    val newUser = userAccountManager.getUser(newAccount.name).get()
                    val fileDataStorageManager = FileDataStorageManager(
                        newUser,
                        targetContext.contentResolver
                    )

                    val capability = OCCapability().apply {
                        userStatus = CapabilityBooleanType.FALSE
                    }

                    fileDataStorageManager.saveCapabilities(capability)
                    EspressoIdlingResource.decrement()

                    val sut =
                        newInstance(
                            RegisteredUser(
                                newAccount,
                                OwnCloudAccount(newAccount, targetContext),
                                Server(
                                    URI.create(serverUrl),
                                    OwnCloudVersion.nextcloud_20
                                )
                            )
                        )

                    onView(isRoot()).check(matches(isDisplayed()))
                    showDialog(fda, sut)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testBottomSheet() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val action: OCFileListBottomSheetActions = object : OCFileListBottomSheetActions {
            override fun createFolder() = Unit
            override fun uploadFromApp() = Unit
            override fun uploadFiles() = Unit
            override fun newDocument() = Unit
            override fun newSpreadsheet() = Unit
            override fun newPresentation() = Unit
            override fun directCameraUpload() = Unit
            override fun scanDocUpload() = Unit
            override fun showTemplate(creator: Creator?, headline: String?) = Unit
            override fun createRichWorkspace() = Unit
        }

        val info = DeviceInfo()
        val ocFile = OCFile("/test.md").apply {
            remoteId = "00000001"
        }

        val intent = Intent(targetContext, FileDisplayActivity::class.java)

        launchActivity<FileDisplayActivity>(intent).use { scenario ->
            scenario.onActivity { fda ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    // add direct editing info
                    var directEditing = DirectEditing()
                    val creators = directEditing.creators.toMutableMap()
                    val editors = directEditing.editors.toMutableMap()

                    creators.put(
                        "1",
                        Creator(
                            "1",
                            "text",
                            "text file",
                            ".md",
                            "application/octet-stream",
                            false
                        )
                    )
                    creators.put(
                        "2",
                        Creator(
                            "2",
                            "md",
                            "markdown file",
                            ".md",
                            "application/octet-stream",
                            false
                        )
                    )
                    editors.put(
                        "text",
                        Editor(
                            "1",
                            "Text",
                            ArrayList(mutableListOf(MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN)),
                            ArrayList(),
                            false
                        )
                    )

                    directEditing = DirectEditing(editors, creators)
                    val json = Gson().toJson(directEditing)

                    ArbitraryDataProviderImpl(targetContext).storeOrUpdateKeyValue(
                        user.accountName,
                        ArbitraryDataProvider.DIRECT_EDITING,
                        json
                    )

                    // activate templates
                    val capability = fda.capabilities.apply {
                        richDocuments = CapabilityBooleanType.TRUE
                        richDocumentsDirectEditing = CapabilityBooleanType.TRUE
                        richDocumentsTemplatesAvailable = CapabilityBooleanType.TRUE
                        accountName = user.accountName
                    }
                    CapabilityUtils.updateCapability(capability)

                    val appScanOptionalFeature: AppScanOptionalFeature = object : AppScanOptionalFeature() {
                        override fun getScanContract(): ActivityResultContract<Unit, String?> =
                            throw UnsupportedOperationException("Document scan is not available")
                    }

                    val materialSchemesProvider = getMaterialSchemesProvider()
                    val viewThemeUtils = ViewThemeUtils(
                        materialSchemesProvider.getMaterialSchemesForCurrentUser(),
                        ColorUtil(targetContext)
                    )

                    val editorUtils = EditorUtils(ArbitraryDataProviderImpl(targetContext))

                    val sut = OCFileListBottomSheetDialog(
                        fda,
                        action,
                        info,
                        user,
                        ocFile,
                        fda.themeUtils,
                        viewThemeUtils,
                        editorUtils,
                        appScanOptionalFeature
                    )
                    EspressoIdlingResource.decrement()

                    sut.show()
                    sut.behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                    val viewGroup = sut.window?.findViewById<ViewGroup>(android.R.id.content) ?: return@onIdleSync
                    hideCursors(viewGroup)
                    val screenShotName = createName(testClassName + "_" + "testBottomSheet", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut.window?.decorView, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testOnlineStatusBottomSheet() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        // show dialog
        val intent = Intent(targetContext, FileDisplayActivity::class.java)

        launchActivity<FileDisplayActivity>(intent).use { scenario ->
            scenario.onActivity { fda ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val sut = SetOnlineStatusBottomSheet(
                        user,
                        Status(StatusType.DND, "Focus time", "\uD83E\uDD13", -1)
                    )
                    EspressoIdlingResource.decrement()
                    sut.show(fda.supportFragmentManager, "set_online_status")

                    val screenShotName = createName(testClassName + "_" + "testOnlineStatusBottomSheet", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut.view, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testProfileBottomSheet() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        // Fixed values for HoverCard
        val actions: MutableList<Action> = ArrayList()
        actions.add(
            Action(
                "profile",
                "View profile",
                "https://dev.nextcloud.com/core/img/actions/profile.svg",
                "https://dev.nextcloud.com/index.php/u/christine"
            )
        )
        actions.add(
            Action(
                "core",
                "christine.scott@nextcloud.com",
                "https://dev.nextcloud.com/core/img/actions/mail.svg",
                "mailto:christine.scott@nextcloud.com"
            )
        )

        actions.add(
            Action(
                "spreed",
                "Talk to Christine",
                "https://dev.nextcloud.com/apps/spreed/img/app-dark.svg",
                "https://dev.nextcloud.com/apps/spreed/?callUser=christine"
            )
        )

        val hoverCard = HoverCard("christine", "Christine Scott", actions)

        // show dialog
        val intent = Intent(targetContext, FileDisplayActivity::class.java)

        launchActivity<FileDisplayActivity>(intent).use { scenario ->
            scenario.onActivity { fda ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val sut = ProfileBottomSheetDialog(
                        fda,
                        user,
                        hoverCard,
                        fda.viewThemeUtils
                    )
                    EspressoIdlingResource.decrement()
                    sut.show()

                    val screenShotName = createName(testClassName + "_" + "testProfileBottomSheet", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut.window?.decorView, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testSslUntrustedCertDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val certificate = SslCertificate("foo", "bar", "2022/01/10", "2022/01/30")
        val sslError = SslError(SslError.SSL_UNTRUSTED, certificate)

        val handler = mockk<SslErrorHandler>(relaxed = true)

        newInstanceForEmptySslError(sslError, handler).run {
            showDialog(this)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testStoragePermissionDialog() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        newInstance(false).run {
            showDialog(this)
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun testFileActionsBottomSheet() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }

        val ocFile = OCFile("/test.md").apply {
            remoteId = "0001"
        }

        newInstance(ocFile, false).run {
            showDialog(this)
        }
    }

    private fun showDialog(dialog: DialogFragment) {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    onView(isRoot()).check(matches(isDisplayed()))
                    showDialog(sut, dialog)
                }
            }
        }
    }

    private fun showDialog(sut: FileDisplayActivity, dialog: DialogFragment) {
        dialog.show(sut.supportFragmentManager, null)
        onIdleSync {
            val dialogInstance = waitForDialog(dialog)
                ?: throw IllegalStateException("Dialog was not created")

            val viewGroup = dialogInstance.window?.findViewById<ViewGroup>(android.R.id.content) ?: return@onIdleSync
            hideCursors(viewGroup)

            onView(isRoot()).check(matches(isDisplayed()))
            screenshot(dialogInstance.window?.decorView)
        }
    }

    private fun waitForDialog(dialogFragment: DialogFragment, timeoutMs: Long = 5000): Dialog? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val dialog = dialogFragment.dialog
            if (dialog != null) return dialog
            Thread.sleep(100)
        }
        return null
    }

    private fun hideCursors(viewGroup: ViewGroup) {
        for (i in 0..<viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            if (child is ViewGroup) {
                hideCursors(child)
            }

            if (child is TextView) {
                child.isCursorVisible = false
            }
        }
    }

    @Test
    fun testGson() {
        val t = ArrayList<EnforcedServer?>().apply {
            add(EnforcedServer("name", "url"))
            add(EnforcedServer("name2", "url1"))
        }

        val s = Gson().toJson(t)
        val t2 = Gson().fromJson<ArrayList<EnforcedServer>>(
            s,
            object : TypeToken<ArrayList<EnforcedServer?>?>() {
            }.type
        )

        val temp = ArrayList<String?>()
        for (p in t2) {
            temp.add(p.name)
        }
    }
}
