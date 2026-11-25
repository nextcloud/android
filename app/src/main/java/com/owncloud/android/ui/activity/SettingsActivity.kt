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

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.addCallback
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.postDelayed
import androidx.fragment.app.commit
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.settings.SettingsFragment
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class SettingsActivity :
    AppCompatActivity(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupActionBar()
        registerBackPressedCallback()

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(android.R.id.content, SettingsFragment())
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()

        return true
    }

    @VisibleForTesting
    fun handleMnemonicRequest(data: Intent?) {
        val fragment = supportFragmentManager.findFragmentById(android.R.id.content) as? SettingsFragment ?: return

        fragment.handleMnemonicRequest(data)
    }

    private fun setupActionBar() {
        val actionBar = supportActionBar ?: return

        viewThemeUtils.platform.themeStatusBar(this)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowTitleEnabled(true)

        val backIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_arrow_back, null) ?: return

        viewThemeUtils.androidx.themeActionBar(this, actionBar, getString(R.string.actionbar_settings), backIcon)
    }

    private fun registerBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this) {
            isBackPressed = true
            Handler(Looper.getMainLooper()).postDelayed(BACK_PRESSED_RESET_DELAY.inWholeMilliseconds) {
                Log_OC.d(TAG, "User returned from settings activity, reset onBackPressed flag.")
                isBackPressed = false
            }

            finish()
        }
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
