/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.privacy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.owncloud.android.databinding.ActivityPrivacySettingsBinding
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class PrivacySettingsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: PrivacySettingsViewModel.Factory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private val viewModel by viewModels<PrivacySettingsViewModel> { viewModelFactory }

    private val binding by lazy { ActivityPrivacySettingsBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewThemeUtils.ionos.platform.themeSystemBars(this);

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.switchers.analyticsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onAnalyticsCheckedChange(isChecked)
        }

        viewModel.stateFlow
            .flowWithLifecycle(lifecycle)
            .onEach(::updateState)
            .launchIn(lifecycleScope)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    private fun updateState(state: PrivacySettingsViewModel.State) {
        if (binding.switchers.analyticsSwitch.isChecked != state.isAnalyticsEnabled) {
            binding.switchers.analyticsSwitch.isChecked = state.isAnalyticsEnabled
        }
    }

    companion object {

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, PrivacySettingsActivity::class.java)
        }
    }
}
