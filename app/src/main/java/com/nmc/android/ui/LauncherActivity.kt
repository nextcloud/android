/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivitySplashBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import javax.inject.Inject

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
        if (TextUtils.isEmpty(resources.getString(R.string.splashScreenBold))) {
            binding.splashScreenBold.visibility = View.GONE
        }
        if (TextUtils.isEmpty(resources.getString(R.string.splashScreenNormal))) {
            binding.splashScreenNormal.visibility = View.GONE
        }
    }

    private fun scheduleSplashScreen() {
        Handler(Looper.getMainLooper()).postDelayed({
            if (user.isPresent) {
                startActivity(Intent(this, FileDisplayActivity::class.java))
            }
            finish()
        }, SPLASH_DURATION)
    }

    companion object {
        const val SPLASH_DURATION = 1500L
    }
}
