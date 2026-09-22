/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023-2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.ActivitySplashBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class LauncherActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Mandatory to call this before super method to show system launch screen for api level 31+
        installSplashScreen()

        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)

        setContentView(binding.root)
        updateTitleVisibility()
        scheduleSplashScreen()
    }

    @VisibleForTesting
    fun setSplashTitles(boldText: String, normalText: String) {
        binding.splashScreenBold.visibility = View.VISIBLE
        binding.splashScreenNormal.visibility = View.VISIBLE

        binding.splashScreenBold.text = boldText
        binding.splashScreenNormal.text = normalText
    }

    private fun updateTitleVisibility() {
        if (resources.getString(R.string.splashScreenBold).isEmpty()) {
            binding.splashScreenBold.visibility = View.GONE
        }
        if (resources.getString(R.string.splashScreenNormal).isEmpty()) {
            binding.splashScreenNormal.visibility = View.GONE
        }
    }

    private fun hasBrandedTitle(): Boolean = resources.getString(R.string.splashScreenBold).isNotEmpty() ||
        resources.getString(R.string.splashScreenNormal).isNotEmpty()

    private fun scheduleSplashScreen() {
        lifecycleScope.launch {
            if (hasBrandedTitle()) {
                delay(SPLASH_DURATION)
            }

            openNextScreen()
        }
    }

    private fun openNextScreen() {
        val nextScreen = when {
            !user.isPresent -> AuthenticatorActivity::class.java

            MDMConfig.enforceProtection(this) &&
                appPreferences.lockPreference == SettingsActivity.LOCK_NONE -> SettingsActivity::class.java

            else -> FileDisplayActivity::class.java
        }

        startActivity(Intent(this, nextScreen))
        finish()
    }

    companion object {
        private val SPLASH_DURATION = 1500.milliseconds
    }
}
