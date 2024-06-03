/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import com.nextcloud.utils.extensions.setHtmlContent
import com.owncloud.android.R
import com.owncloud.android.databinding.CommunityLayoutBinding
import com.owncloud.android.utils.DisplayUtils

/**
 * Activity providing information about ways to participate in the app's development.
 */
open class CommunityActivity : DrawerActivity() {
    lateinit var binding: CommunityLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CommunityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_community))
        setupDrawer(R.id.nav_community)
        binding.communityReleaseCandidateText.movementMethod = LinkMovementMethod.getInstance()
        setupContributeForumView()
        setupContributeTranslationView()
        setupContributeGithubView()
        setupReportButton()
        setOnClickListeners()
    }

    private fun setupContributeForumView() {
        val htmlContent = getString(R.string.community_contribute_forum_text) + " " +
            getString(
                R.string.community_contribute_forum_text_link,
                viewThemeUtils.files
                    .primaryColorToHexString(this),
                getString(R.string.help_link),
                getString(R.string.community_contribute_forum_forum)
            )
        binding.communityContributeForumText.setHtmlContent(htmlContent)
    }

    private fun setupContributeTranslationView() {
        val htmlContent = getString(
            R.string.community_contribute_translate_link,
            viewThemeUtils.files.primaryColorToHexString(this),
            getString(R.string.translation_link),
            getString(R.string.community_contribute_translate_translate)
        ) + " " +
            getString(R.string.community_contribute_translate_text)
        binding.communityContributeTranslateText.setHtmlContent(htmlContent)
    }

    private fun setupContributeGithubView() {
        val htmlContent = getString(
            R.string.community_contribute_github_text,
            getString(
                R.string.community_contribute_github_text_link,
                viewThemeUtils.files.primaryColorToHexString(this),
                getString(R.string.contributing_link)
            )
        )
        binding.communityContributeGithubText.setHtmlContent(htmlContent)
    }

    private fun setupReportButton() {
        val reportButton = binding.communityTestingReport
        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(reportButton)
        reportButton.setOnClickListener {
            DisplayUtils.startLinkIntent(
                this,
                R.string.report_issue_empty_link
            )
        }
    }

    private fun setOnClickListeners() {
        binding.communityBetaFdroid.setOnClickListener {
            DisplayUtils.startLinkIntent(
                this,
                R.string.fdroid_beta_link
            )
        }
        binding.communityReleaseCandidateFdroid.setOnClickListener {
            DisplayUtils.startLinkIntent(
                this,
                R.string.fdroid_link
            )
        }
        binding.communityReleaseCandidatePlaystore.setOnClickListener {
            DisplayUtils.startLinkIntent(
                this,
                R.string.play_store_register_beta
            )
        }
        binding.communityBetaApk.setOnClickListener {
            DisplayUtils.startLinkIntent(
                this,
                R.string.beta_apk_link
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        if (item.itemId == android.R.id.home) {
            if (isDrawerOpen) {
                closeDrawer()
            } else {
                openDrawer()
            }
        } else {
            retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    override fun onResume() {
        super.onResume()
        setDrawerMenuItemChecked(R.id.nav_community)
    }
}
