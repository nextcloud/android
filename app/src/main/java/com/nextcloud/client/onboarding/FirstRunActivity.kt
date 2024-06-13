/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.accounts.AccountManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewpager2.widget.ViewPager2
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
class FirstRunActivity : BaseActivity(), Injectable {

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

    private var activityResult: ActivityResultLauncher<Intent>? = null

    private lateinit var binding: FirstRunActivityBinding
    private var defaultViewThemeUtils: ViewThemeUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableAccountHandling = false

        super.onCreate(savedInstanceState)

        applyDefaultTheme()

        binding = FirstRunActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isProviderOrOwnInstallationVisible = resources.getBoolean(R.bool.show_provider_or_own_installation)
        setSlideshowSize(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

        registerActivityResult()
        setupLoginButton()
        setupSignupButton(isProviderOrOwnInstallationVisible)
        setupHostOwnServerTextView(isProviderOrOwnInstallationVisible)
        deleteAccountAtFirstLaunch()
        setupFeaturesViewAdapter()
        handleOnBackPressed()
    }

    private fun applyDefaultTheme() {
        defaultViewThemeUtils = viewThemeUtilsFactory?.withPrimaryAsBackground()
        defaultViewThemeUtils?.platform?.themeStatusBar(this, ColorRole.PRIMARY)
    }

    private fun registerActivityResult() {
        activityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (RESULT_OK == result.resultCode) {
                    val data = result.data
                    val accountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
                    val account = userAccountManager?.getAccountByName(accountName)
                    if (account == null) {
                        DisplayUtils.showSnackMessage(this, R.string.account_creation_failed)
                        return@registerForActivityResult
                    }

                    userAccountManager?.setCurrentOwnCloudAccount(account.name)

                    val i = Intent(this, FileDisplayActivity::class.java)
                    i.action = FileDisplayActivity.RESTART
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                    finish()
                }
            }
    }

    private fun setupLoginButton() {
        defaultViewThemeUtils?.material?.colorMaterialButtonFilledOnPrimary(binding.login)
        binding.login.setOnClickListener {
            if (intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                val authenticatorActivityIntent = getAuthenticatorActivityIntent(false)
                activityResult?.launch(authenticatorActivityIntent)
            } else {
                finish()
            }
        }
    }

    private fun setupSignupButton(isProviderOrOwnInstallationVisible: Boolean) {
        defaultViewThemeUtils?.material?.colorMaterialButtonOutlinedOnPrimary(binding.signup)
        binding.signup.visibility = if (isProviderOrOwnInstallationVisible) View.VISIBLE else View.GONE
        binding.signup.setOnClickListener {
            val authenticatorActivityIntent = getAuthenticatorActivityIntent(true)

            if (intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)) {
                activityResult?.launch(authenticatorActivityIntent)
            } else {
                authenticatorActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(authenticatorActivityIntent)
            }
        }
    }

    private fun getAuthenticatorActivityIntent(extraUseProviderAsWebLogin: Boolean): Intent {
        val intent = Intent(this, AuthenticatorActivity::class.java)
        intent.putExtra(AuthenticatorActivity.EXTRA_USE_PROVIDER_AS_WEBLOGIN, extraUseProviderAsWebLogin)
        return intent
    }

    private fun setupHostOwnServerTextView(isProviderOrOwnInstallationVisible: Boolean) {
        defaultViewThemeUtils?.platform?.colorTextView(binding.hostOwnServer, ColorRole.ON_PRIMARY)
        binding.hostOwnServer.visibility = if (isProviderOrOwnInstallationVisible) View.VISIBLE else View.GONE
        if (isProviderOrOwnInstallationVisible) {
            binding.hostOwnServer.setOnClickListener {
                DisplayUtils.startLinkIntent(
                    this,
                    R.string.url_server_install
                )
            }
        }
    }

    // Sometimes, accounts are not deleted when you uninstall the application so we'll do it now
    private fun deleteAccountAtFirstLaunch() {
        if (onboarding?.isFirstRun == true) {
            userAccountManager?.removeAllAccounts()
        }
    }

    @Suppress("SpreadOperator")
    private fun setupFeaturesViewAdapter() {
        val featuresViewAdapter = FeaturesViewAdapter(this, *firstRun)
        binding.progressIndicator.setNumberOfSteps(featuresViewAdapter.itemCount)
        binding.contentPanel.adapter = featuresViewAdapter
        binding.contentPanel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.progressIndicator.animateToStep(position + 1)
            }
        })
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val isFromAddAccount = intent.getBooleanExtra(EXTRA_ALLOW_CLOSE, false)

                    val destination: Intent = if (isFromAddAccount) {
                        Intent(applicationContext, FileDisplayActivity::class.java)
                    } else {
                        Intent(applicationContext, AuthenticatorActivity::class.java)
                    }

                    if (!isFromAddAccount) {
                        destination.putExtra(EXTRA_EXIT, true)
                    }

                    destination.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(destination)
                    finish()
                }
            }
        )
    }

    private fun setSlideshowSize(isLandscape: Boolean) {
        val isProviderOrOwnInstallationVisible = resources.getBoolean(R.bool.show_provider_or_own_installation)
        binding.buttonLayout.orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        val layoutParams: LinearLayout.LayoutParams = if (isProviderOrOwnInstallationVisible) {
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } else {
            @Suppress("MagicNumber")
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                DisplayUtils.convertDpToPixel(if (isLandscape) 100f else 150f, this)
            )
        }

        binding.bottomLayout.layoutParams = layoutParams
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setSlideshowSize(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    private fun onFinish() {
        preferences?.lastSeenVersionCode = BuildConfig.VERSION_CODE
    }

    override fun onStop() {
        onFinish()
        super.onStop()
    }

    companion object {
        const val EXTRA_ALLOW_CLOSE = "ALLOW_CLOSE"
        const val EXTRA_EXIT = "EXIT"

        val firstRun: Array<FeatureItem>
            get() = arrayOf(
                FeatureItem(R.drawable.logo, R.string.first_run_1_text, R.string.empty, true, false),
                FeatureItem(R.drawable.first_run_files, R.string.first_run_2_text, R.string.empty, true, false),
                FeatureItem(R.drawable.first_run_groupware, R.string.first_run_3_text, R.string.empty, true, false),
                FeatureItem(R.drawable.first_run_talk, R.string.first_run_4_text, R.string.empty, true, false)
            )
    }
}
