/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.community

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nextcloud.utils.extensions.setHtmlContent
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentCommunityBinding
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.utils.DisplayUtils

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!

    private val drawerActivity get() = activity as? DrawerActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawerActivity?.run {
            setupToolbar()
            updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_community))
        }
        binding.communityReleaseCandidateText.movementMethod = LinkMovementMethod.getInstance()
        drawerActivity?.let { setupViews(it) }
        setOnClickListeners()
    }

    private fun setupViews(activity: DrawerActivity) {
        val primaryColor = activity.viewThemeUtils.files.primaryColorToHexString(requireContext())
        val helpLink = getString(R.string.help_link)
        val forum = getString(R.string.community_contribute_forum_forum)
        val translationLink = getString(R.string.translation_link)
        val translate = getString(R.string.community_contribute_translate_translate)
        val contributingLink = getString(R.string.contributing_link)

        listOf(
            binding.communityContributeForumText to
                "${getString(R.string.community_contribute_forum_text)} " +
                getString(
                    R.string.community_contribute_forum_text_link,
                    primaryColor,
                    helpLink,
                    forum
                ),

            binding.communityContributeTranslateText to
                getString(
                    R.string.community_contribute_translate_link,
                    primaryColor,
                    translationLink,
                    translate
                ) + " " + getString(R.string.community_contribute_translate_text),

            binding.communityContributeGithubText to
                getString(
                    R.string.community_contribute_github_text,
                    getString(
                        R.string.community_contribute_github_text_link,
                        primaryColor,
                        contributingLink
                    )
                )
        ).forEach { (view, content) ->
            view.setHtmlContent(content)
        }

        activity.viewThemeUtils.material.colorMaterialButtonPrimaryFilled(binding.communityTestingReport)
        binding.communityTestingReport.setOnClickListener {
            DisplayUtils.startLinkIntent(requireActivity(), R.string.report_issue_empty_link)
        }
    }

    private fun setOnClickListeners() {
        val activity = requireActivity()

        listOf(
            binding.communityBetaFdroid to R.string.fdroid_beta_link,
            binding.communityReleaseCandidateFdroid to R.string.fdroid_link,
            binding.communityReleaseCandidatePlaystore to R.string.play_store_register_beta,
            binding.communityBetaApk to R.string.beta_apk_link
        ).forEach { (view, linkRes) ->
            view.setOnClickListener {
                DisplayUtils.startLinkIntent(activity, linkRes)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
