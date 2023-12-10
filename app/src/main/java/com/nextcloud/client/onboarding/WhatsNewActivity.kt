/*
 * Nextcloud Android client application
 *
 * @author Bartosz Przybylski
 * @author Chris Narkiewicz
 * Copyright (C) 2015 Bartosz Przybylski
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2016 Nextcloud.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager
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
class WhatsNewActivity : FragmentActivity(), ViewPager.OnPageChangeListener, Injectable {

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
        binding.contentPanel.addOnPageChangeListener(this)
        setupForwardImageButton()
        setupSkipImageButton()
        setupWelcomeText(showWebView)
        updateNextButtonIfNeeded()
        handleOnBackPressed()
    }

    @Suppress("SpreadOperator")
    private fun setupFeatureViewAdapter(showWebView: Boolean, urls: Array<String>) {
        val adapter = if (showWebView) {
            FeaturesWebViewAdapter(supportFragmentManager, *urls)
        } else {
            onboarding?.let {
                FeaturesViewAdapter(supportFragmentManager, *it.whatsNew)
            }
        }

        adapter?.let {
            binding.progressIndicator.setNumberOfSteps(it.count)
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

    override fun onPageSelected(position: Int) {
        binding.progressIndicator.animateToStep(position + 1)
        updateNextButtonIfNeeded()
    }

    @Suppress("EmptyFunctionBlock")
    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    @Suppress("EmptyFunctionBlock")
    override fun onPageScrollStateChanged(state: Int) {}
}
