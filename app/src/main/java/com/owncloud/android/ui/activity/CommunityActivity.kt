/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Tobias Kaminsky
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
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
