/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.databinding.WhatsNewActivityBinding
import com.owncloud.android.ui.adapter.FeaturesViewAdapter
import com.owncloud.android.ui.adapter.FeaturesWebViewAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Activity displaying new features after an update.
 */
class WhatsNewActivity : FragmentActivity(), Injectable {

    @JvmField
    @Inject
    var preferences: AppPreferences? = null

    @JvmField
    @Inject
    var appInfo: AppInfo? = null

    @JvmField
    @Inject
    var onboarding: OnboardingService? = null

    @JvmField
    @Inject
    var viewThemeUtilsFactory: ViewThemeUtils.Factory? = null

    private var viewThemeUtils: ViewThemeUtils? = null

    private lateinit var binding: WhatsNewActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = WhatsNewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewThemeUtils = viewThemeUtilsFactory?.withPrimaryAsBackground()
        viewThemeUtils?.platform?.themeStatusBar(this, ColorRole.PRIMARY)

        val urls = resources.getStringArray(R.array.whatsnew_urls)
        val showWebView = urls.isNotEmpty()

        setupFeatureViewAdapter(showWebView, urls)
        binding.contentPanel.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                controlPanelOnPageSelected(position)
            }
        })
        setupForwardImageButton()
        setupSkipImageButton()
        setupWelcomeText(showWebView)
        updateNextButtonIfNeeded()
        handleOnBackPressed()
    }

    @Suppress("SpreadOperator")
    private fun setupFeatureViewAdapter(showWebView: Boolean, urls: Array<String>) {
        val adapter = if (showWebView) {
            FeaturesWebViewAdapter(this, *urls)
        } else {
            onboarding?.let {
                FeaturesViewAdapter(this, *it.whatsNew)
            }
        }

        adapter?.let {
            binding.progressIndicator.setNumberOfSteps(it.itemCount)
            binding.contentPanel.adapter = it
        }
    }

    private fun setupForwardImageButton() {
        viewThemeUtils?.platform?.colorImageView(binding.forward, ColorRole.ON_PRIMARY)
        binding.forward.setOnClickListener {
            if (binding.progressIndicator.hasNextStep()) {
                binding.contentPanel.setCurrentItem(binding.contentPanel.currentItem + 1, true)
                binding.progressIndicator.animateToStep(binding.contentPanel.currentItem + 1)
            } else {
                onFinish()
                finish()
            }
            updateNextButtonIfNeeded()
        }
        binding.forward.background = null
    }

    private fun setupSkipImageButton() {
        viewThemeUtils?.platform?.colorTextView(binding.skip, ColorRole.ON_PRIMARY)
        binding.skip.setOnClickListener {
            onFinish()
            finish()
        }
    }

    private fun setupWelcomeText(showWebView: Boolean) {
        viewThemeUtils?.platform?.colorTextView(binding.welcomeText, ColorRole.ON_PRIMARY)
        binding.welcomeText.text = if (showWebView) {
            getString(R.string.app_name)
        } else {
            String.format(getString(R.string.whats_new_title), appInfo?.versionName)
        }
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onFinish()
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        )
    }

    private fun updateNextButtonIfNeeded() {
        val hasNextStep = binding.progressIndicator.hasNextStep()
        binding.forward.setImageResource(if (hasNextStep) R.drawable.arrow_right else R.drawable.ic_ok)
        binding.skip.visibility = if (hasNextStep) View.VISIBLE else View.INVISIBLE
    }

    private fun onFinish() {
        preferences?.lastSeenVersionCode = BuildConfig.VERSION_CODE
    }

    private fun controlPanelOnPageSelected(position: Int) {
        binding.progressIndicator.animateToStep(position + 1)
        updateNextButtonIfNeeded()
    }
}
