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

import android.accounts.AccountManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.viewpager.widget.ViewPager
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.databinding.FirstRunActivityBinding
import com.owncloud.android.features.FeatureItem
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.FeaturesViewAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Activity displaying general feature after a fresh install.
 */
class FirstRunActivity : BaseActivity(), ViewPager.OnPageChangeListener, Injectable {
    @JvmField
    @Inject
    var userAccountManager: UserAccountManager? = null

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
    private var binding: FirstRunActivityBinding? = null
    private var defaultViewThemeUtils: ViewThemeUtils? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        enableAccountHandling = false
        super.onCreate(savedInstanceState)
        defaultViewThemeUtils = viewThemeUtilsFactory!!.withPrimaryAsBackground()
        defaultViewThemeUtils!!.platform.themeStatusBar(this, ColorRole.PRIMARY)
        binding = FirstRunActivityBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        val isProviderOrOwnInstallationVisible = resources.getBoolean(R.bool.show_provider_or_own_installation)
        setSlideshowSize(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        defaultViewThemeUtils!!.material.colorMaterialButtonFilledOnPrimary(binding!!.login)
        binding!!.login.setOnClickListener { v: View? ->
            if (intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                val authenticatorActivityIntent = Intent(this, AuthenticatorActivity::class.java)
                authenticatorActivityIntent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, false)
                startActivityForResult(authenticatorActivityIntent, FIRST_RUN_RESULT_CODE)
            } else {
                finish()
            }
        }
        defaultViewThemeUtils!!.material.colorMaterialButtonOutlinedOnPrimary(binding!!.signup)
        binding!!.signup.visibility = if (isProviderOrOwnInstallationVisible) View.VISIBLE else View.GONE
        binding!!.signup.setOnClickListener { v: View? ->
            val authenticatorActivityIntent = Intent(this, AuthenticatorActivity::class.java)
            authenticatorActivityIntent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, true)
            if (intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                startActivityForResult(authenticatorActivityIntent, FIRST_RUN_RESULT_CODE)
            } else {
                authenticatorActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(authenticatorActivityIntent)
            }
        }
        defaultViewThemeUtils!!.platform.colorTextView(binding!!.hostOwnServer, ColorRole.ON_PRIMARY)
        binding!!.hostOwnServer.visibility = if (isProviderOrOwnInstallationVisible) View.VISIBLE else View.GONE
        if (isProviderOrOwnInstallationVisible) {
            binding!!.hostOwnServer.setOnClickListener { v: View? ->
                DisplayUtils.startLinkIntent(
                    this,
                    R.string.url_server_install
                )
            }
        }

        // Sometimes, accounts are not deleted when you uninstall the application so we'll do it now
        if (onboarding!!.isFirstRun) {
            userAccountManager!!.removeAllAccounts()
        }
        val featuresViewAdapter = FeaturesViewAdapter(supportFragmentManager, *firstRun)
        binding!!.progressIndicator.setNumberOfSteps(featuresViewAdapter.count)
        binding!!.contentPanel.adapter = featuresViewAdapter
        binding!!.contentPanel.addOnPageChangeListener(this)
    }

    private fun setSlideshowSize(isLandscape: Boolean) {
        val isProviderOrOwnInstallationVisible = resources.getBoolean(R.bool.show_provider_or_own_installation)
        val layoutParams: LinearLayout.LayoutParams
        binding!!.buttonLayout.orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        layoutParams = if (isProviderOrOwnInstallationVisible) {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                DisplayUtils.convertDpToPixel(if (isLandscape) 100f else 150f, this)
            )
        }
        binding!!.bottomLayout.layoutParams = layoutParams
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setSlideshowSize(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    override fun onBackPressed() {
        onFinish()
        if (intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
            super.onBackPressed()
        } else {
            val intent = Intent(applicationContext, AuthenticatorActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_EXIT, true)
            startActivity(intent)
            finish()
        }
    }

    private fun onFinish() {
        preferences!!.lastSeenVersionCode = BuildConfig.VERSION_CODE
    }

    override fun onStop() {
        onFinish()
        super.onStop()
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        // unused but to be implemented due to abstract parent
    }

    override fun onPageSelected(position: Int) {
        binding!!.progressIndicator.animateToStep(position + 1)
    }

    override fun onPageScrollStateChanged(state: Int) {
        // unused but to be implemented due to abstract parent
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (FIRST_RUN_RESULT_CODE == requestCode && RESULT_OK == resultCode) {
            val accountName = data!!.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            val account = userAccountManager!!.getAccountByName(accountName)
            if (account == null) {
                DisplayUtils.showSnackMessage(this, R.string.account_creation_failed)
                return
            }
            userAccountManager!!.setCurrentOwnCloudAccount(account.name)
            val i = Intent(this, FileDisplayActivity::class.java)
            i.action = FileDisplayActivity.RESTART
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(i)
            finish()
        }
    }

    companion object {
        const val EXTRA_ALLOW_CLOSE = "ALLOW_CLOSE"
        const val EXTRA_EXIT = "EXIT"
        const val FIRST_RUN_RESULT_CODE = 199
        val firstRun: Array<FeatureItem>
            get() = arrayOf(
                FeatureItem(R.drawable.logo, R.string.first_run_1_text, R.string.empty, true, false),
                FeatureItem(R.drawable.first_run_files, R.string.first_run_2_text, R.string.empty, true, false),
                FeatureItem(R.drawable.first_run_groupware, R.string.first_run_3_text, R.string.empty, true, false),
                FeatureItem(R.drawable.first_run_talk, R.string.first_run_4_text, R.string.empty, true, false)
            )
    }
}