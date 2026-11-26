/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

        var isBackPressed = false
    }
}
