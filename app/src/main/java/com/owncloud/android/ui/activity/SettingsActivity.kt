/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2022-2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017-2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2015-2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 Jose Antonio Barros Ramos <jabarros@solidgear.es>
 * SPDX-FileCopyrightText: 2013 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2011-2015 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceActivity
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.repository.ClientRepository
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.activity.settings.SettingsRequestCode
import com.owncloud.android.ui.activity.settings.about.AboutSettingsCategory
import com.owncloud.android.ui.activity.settings.autoupload.AutoUploadSettingsCategory
import com.owncloud.android.ui.activity.settings.details.DetailsSettingsCategory
import com.owncloud.android.ui.activity.settings.dev.DevSettingsCategory
import com.owncloud.android.ui.activity.settings.e2e.E2ESettings
import com.owncloud.android.ui.activity.settings.files.FilesSettingsCategory
import com.owncloud.android.ui.activity.settings.general.GeneralSettingsCategory
import com.owncloud.android.ui.activity.settings.lock.AppLockSettings
import com.owncloud.android.ui.activity.settings.more.DavX5Launcher
import com.owncloud.android.ui.activity.settings.more.MoreSettingsCategory
import com.owncloud.android.ui.activity.settings.sync.SyncSettingsCategory
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class SettingsActivity :
    PreferenceActivity(),
    Injectable {

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var clientRepository: ClientRepository

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var connectivityService: ConnectivityService

    private val delegate by lazy(LazyThreadSafetyMode.NONE) { AppCompatDelegate.create(this, null) }
    private val scope = MainScope()
    private val user: User by lazy { accountManager.user }

    private val generalCategory by lazy { GeneralSettingsCategory(this, user, preferences, viewThemeUtils) }
    private val syncCategory by lazy { SyncSettingsCategory(this, viewThemeUtils) }
    private val appLockSettings by lazy { AppLockSettings(this, preferences) }
    private val davX5Launcher by lazy { DavX5Launcher(this, user, clientRepository, scope) }
    private val e2eSettings by lazy {
        E2ESettings(this, user, arbitraryDataProvider, connectivityService, clientFactory)
    }

    @Suppress("DEPRECATION")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        delegate.installViewFactory()
        delegate.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        listView.fitsSystemWindows = true
        setupActionBar()
        registerForContextMenu(listView)

        davX5Launcher.loadServerBaseUri()
        setupPreferenceCategories()

        listView.setBackgroundColor(ContextCompat.getColor(this, R.color.bg_default))
        appLockSettings.showPasscodeDialogIfEnforceAppProtection()
    }

    @Suppress("DEPRECATION")
    private fun setupPreferenceCategories() {
        val screen = preferenceScreen

        generalCategory.setup()
        AutoUploadSettingsCategory(this, user, arbitraryDataProvider, viewThemeUtils).setup(screen)
        FilesSettingsCategory(this, preferences, viewThemeUtils).setup()
        DetailsSettingsCategory(this, preferences, viewThemeUtils, appLockSettings).setup(screen)
        syncCategory.setup()
        MoreSettingsCategory(this, viewThemeUtils, e2eSettings, davX5Launcher).setup()
        AboutSettingsCategory(this, viewThemeUtils).setup()
        DevSettingsCategory(this, viewThemeUtils).setup(screen)
    }

    private fun setupActionBar() {
        val actionBar = delegate.supportActionBar ?: return

        viewThemeUtils.platform.themeStatusBar(this)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)

        val backIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_arrow_back, null) ?: return

        viewThemeUtils.androidx.themeActionBar(this, actionBar, getString(R.string.actionbar_settings), backIcon)
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        isBackPressed = true
        super.onBackPressed()

        Handler(Looper.getMainLooper()).postDelayed({
            Log_OC.d(TAG, "User returned from settings activity, reset onBackPressed flag.")
            isBackPressed = false
        }, BACK_PRESSED_RESET_DELAY.inWholeMilliseconds)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SettingsRequestCode.REQUEST_PASSCODE -> appLockSettings.onPasscodeRequestResult(resultCode, data)
            SettingsRequestCode.CONFIRM_PASSCODE -> appLockSettings.onPasscodeConfirmResult(resultCode, data)
            SettingsRequestCode.CONFIRM_DEVICE_CREDENTIALS ->
                appLockSettings.onDeviceCredentialsConfirmResult(resultCode, data)
            SettingsRequestCode.DAVX5_SETUP -> onDavX5SetupResult(resultCode)
            SettingsRequestCode.SHOW_MNEMONIC -> if (resultCode == RESULT_OK) handleMnemonicRequest(data)
            SettingsRequestCode.SETUP_E2E -> onEncryptionSetupResult(data)
            ExtendedSettingsActivityDialog.StorageLocation.resultId -> onStorageLocationResult(data)
            ExtendedSettingsActivityDialog.ThemeSelection.resultId -> onThemeSelectionResult(data)
            ExtendedSettingsActivityDialog.AppPasscode.resultId -> onAppPasscodeResult(data)
            DrawerActivity.REQ_ALL_FILES_ACCESS -> syncCategory.refreshAllFilesAccessPreference()
        }
    }

    @VisibleForTesting
    fun handleMnemonicRequest(data: Intent?) = e2eSettings.showMnemonic(data)

    private fun onDavX5SetupResult(resultCode: Int) {
        if (resultCode != RESULT_OK) return

        SnackbarUtil.show(this, R.string.prefs_calendar_contacts_sync_setup_successful)
    }

    private fun onEncryptionSetupResult(data: Intent?) {
        if (data?.getBooleanExtra(SetupEncryptionDialogFragment.SUCCESS, false) != true) return

        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun onStorageLocationResult(data: Intent?) {
        val newPath = data?.getStringExtra(ExtendedSettingsActivityDialog.StorageLocation.key) ?: return

        generalCategory.onStorageLocationSelected(newPath)
    }

    private fun onThemeSelectionResult(data: Intent?) {
        val selectedTheme = data?.getStringExtra(ExtendedSettingsActivityDialog.ThemeSelection.key) ?: return
        generalCategory.onThemeSelected(selectedTheme)

        recreate()
    }

    private fun onAppPasscodeResult(data: Intent?) {
        val selectedLock = data?.getStringExtra(ExtendedSettingsActivityDialog.AppPasscode.key) ?: return

        appLockSettings.onLockSelected(selectedLock)
    }

    override fun getMenuInflater(): MenuInflater = delegate.menuInflater

    override fun setContentView(@LayoutRes layoutResID: Int) = delegate.setContentView(layoutResID)

    override fun setContentView(view: View?) = delegate.setContentView(view)

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) = delegate.setContentView(view, params)

    override fun addContentView(view: View?, params: ViewGroup.LayoutParams?) = delegate.addContentView(view, params)

    override fun invalidateOptionsMenu() = delegate.invalidateOptionsMenu()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        delegate.onPostCreate(savedInstanceState)
    }

    override fun onPostResume() {
        super.onPostResume()

        delegate.onPostResume()
    }

    override fun onTitleChanged(title: CharSequence?, color: Int) {
        super.onTitleChanged(title, color)

        delegate.setTitle(title)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        delegate.onConfigurationChanged(newConfig)
    }

    override fun onStop() {
        super.onStop()

        delegate.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        delegate.onDestroy()

        scope.cancel()
    }

    companion object {
        private val TAG: String = SettingsActivity::class.java.simpleName
        private val BACK_PRESSED_RESET_DELAY = 2.seconds

        const val PREFERENCE_LOCK = "lock"

        const val LOCK_NONE = "none"
        const val LOCK_PASSCODE = "passcode"
        const val LOCK_DEVICE_CREDENTIALS = "device_credentials"

        const val PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS = "show_media_scan_notifications"

        const val SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI = "SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI"

        var isBackPressed = false
    }
}
