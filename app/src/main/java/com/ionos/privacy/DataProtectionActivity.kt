/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.privacy

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ionos.utils.context.isDarkMode
import com.ionos.utils.text.convertAnnotatedTextToLinks
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityDataProtectionBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.ExternalSiteWebView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class DataProtectionActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: DataProtectionViewModel.Factory

    private val viewModel by viewModels<DataProtectionViewModel> { viewModelFactory }

    private val binding by lazy { ActivityDataProtectionBinding.inflate(layoutInflater) }

    private val detailPageOnBackPressedCallback by lazy {
        onBackPressedDispatcher.addCallback(this) { viewModel.onDetailPageBackButtonClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val descriptionText = getText(R.string.ionos_data_protection_description).convertAnnotatedTextToLinks(
            linkColor = ContextCompat.getColor(this, R.color.curious_blue),
            linkUnderline = false,
            linkHandler = ::handleLink,
        )

        binding.overviewPage.descriptionTextView.text = descriptionText
        binding.overviewPage.descriptionTextView.movementMethod = LinkMovementMethod.getInstance()

        binding.overviewPage.agreeButton.setOnClickListener { viewModel.onAgreeButtonClick() }
        binding.overviewPage.settingsButton.setOnClickListener { viewModel.onSettingsButtonClick() }
        binding.detailPage.toolbar.setNavigationOnClickListener { viewModel.onDetailPageBackButtonClick() }
        binding.detailPage.saveButton.setOnClickListener { viewModel.onSaveButtonClick() }
        binding.detailPage.switchers.analyticsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onAnalyticsCheckedChange(isChecked)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { rootView, windowInsets ->
            val insetsType = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            val insets = windowInsets.getInsets(insetsType)
            binding.overviewPage.root.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            binding.detailPage.root.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            ViewCompat.onApplyWindowInsets(rootView, windowInsets)
        }

        viewModel.stateFlow
            .flowWithLifecycle(lifecycle)
            .onEach(::updateState)
            .launchIn(lifecycleScope)
    }

    private fun handleLink(type: String) {
        when (type) {
            INFORMATION_LINK -> openPrivacyPolicyScreen()
            REJECT_LINK -> viewModel.onRejectLinkClick()
            else -> throw IllegalArgumentException("Unknown link type: $type")
        }
    }

    private fun openPrivacyPolicyScreen() {
        val externalWebViewIntent = Intent(this, ExternalSiteWebView::class.java)
        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, getString(R.string.privacy))
        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, getString(R.string.privacy_url))
        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
        startActivity(externalWebViewIntent)
    }

    private fun updateState(state: DataProtectionViewModel.State) {
        if (binding.viewSwitcher.displayedChild != state.page.index) {
            binding.viewSwitcher.displayedChild = state.page.index
        }
        if (state.page == DataProtectionViewModel.Page.OVERVIEW) {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            setSystemBarsAppearance(false)
            detailPageOnBackPressedCallback.isEnabled = false
        } else {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
            setSystemBarsAppearance(!isDarkMode())
            detailPageOnBackPressedCallback.isEnabled = true
        }
        if (binding.detailPage.switchers.analyticsSwitch.isChecked != state.isAnalyticsEnabled) {
            binding.detailPage.switchers.analyticsSwitch.isChecked = state.isAnalyticsEnabled
        }
        if (state.isProcessed) {
            intent.getParcelableArgument(TARGET_SCREEN_INTENT_KEY, Intent::class.java)?.let(::startActivity)
            finish()
        }
    }

    private fun setSystemBarsAppearance(isLight: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = isLight
        controller.isAppearanceLightNavigationBars = isLight
    }

    companion object {
        private const val TARGET_SCREEN_INTENT_KEY = "target_screen_intent"
        private const val INFORMATION_LINK = "information_link"
        private const val REJECT_LINK = "reject_link"

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, DataProtectionActivity::class.java)
        }

        @JvmStatic
        fun createIntent(context: Context, targetScreenIntent: Intent): Intent {
            return Intent(context, DataProtectionActivity::class.java)
                .putExtra(TARGET_SCREEN_INTENT_KEY, targetScreenIntent)
        }
    }
}
