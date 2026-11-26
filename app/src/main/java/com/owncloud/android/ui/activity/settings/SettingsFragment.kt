/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.ui.activity.settings.categories.AboutSettingsCategory
import com.owncloud.android.ui.activity.settings.categories.AutoUploadSettingsCategory
import com.owncloud.android.ui.activity.settings.categories.DetailsSettingsCategory
import com.owncloud.android.ui.activity.settings.categories.DevSettingsCategory
import com.owncloud.android.ui.activity.settings.categories.E2ESettingsCategory
import com.owncloud.android.ui.activity.settings.categories.FilesSettingsCategory
import com.owncloud.android.ui.activity.settings.categories.GeneralSettingsCategory
import com.owncloud.android.ui.activity.settings.categories.MoreSettingsCategory
import com.owncloud.android.ui.activity.settings.categories.SettingsCategory
import com.owncloud.android.ui.activity.settings.categories.SyncSettingsCategory
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class SettingsFragment :
    PreferenceFragmentCompat(),
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

    private lateinit var appLockSettings: AppLockSettings
    private lateinit var e2eCategory: E2ESettingsCategory
    private lateinit var moreCategory: MoreSettingsCategory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val user = accountManager.user
        val davX5Launcher = DavX5Launcher(this, user, clientRepository)
        appLockSettings = AppLockSettings(this, preferences, savedInstanceState)
        e2eCategory = E2ESettingsCategory(this, user, arbitraryDataProvider, connectivityService, clientFactory)
        moreCategory = MoreSettingsCategory(this, viewThemeUtils, davX5Launcher)

        val categories: List<SettingsCategory> = listOf(
            GeneralSettingsCategory(this, user, preferences, viewThemeUtils),
            AutoUploadSettingsCategory(this, user, arbitraryDataProvider, viewThemeUtils),
            FilesSettingsCategory(this, preferences, viewThemeUtils),
            DetailsSettingsCategory(this, preferences, viewThemeUtils, appLockSettings),
            SyncSettingsCategory(this, viewThemeUtils),
            moreCategory,
            e2eCategory,
            AboutSettingsCategory(this, viewThemeUtils),
            DevSettingsCategory(this, viewThemeUtils)
        )

        davX5Launcher.loadServerBaseUri()
        categories.forEach(SettingsCategory::setup)
        appLockSettings.showPasscodeDialogIfEnforceAppProtection()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView.fitsSystemWindows = true
        listView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_default))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        appLockSettings.saveState(outState)
    }

    override fun onDestroy() {
        moreCategory.cleanup()

        super.onDestroy()
    }

    fun handleMnemonicRequest(data: Intent?) = e2eCategory.showMnemonic(data)
}
