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
package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.owncloud.android.R;
import com.owncloud.android.databinding.CommunityLayoutBinding;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;

/**
 * Activity providing information about ways to participate in the app's development.
 */
public class CommunityActivity extends DrawerActivity {

    private CommunityLayoutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = CommunityLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // setup toolbar
        setupToolbar();

        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_community));

        // setup drawer
        setupDrawer(R.id.nav_community);

        setupContent();
    }

    private void setupContent() {
        binding.communityReleaseCandidateText.setMovementMethod(LinkMovementMethod.getInstance());

        TextView contributeIrcView = binding.communityContributeIrcText;
        contributeIrcView.setMovementMethod(LinkMovementMethod.getInstance());
        contributeIrcView.setText(Html.fromHtml(getString(R.string.community_contribute_irc_text) + " " +
                                                    getString(R.string.community_contribute_irc_text_link,
                                                              ThemeColorUtils.primaryColorToHexString(this),
                                                              getString(R.string.irc_weblink))));

        TextView contributeForumView = binding.communityContributeForumText;
        contributeForumView.setMovementMethod(LinkMovementMethod.getInstance());
        contributeForumView.setText(Html.fromHtml(getString(R.string.community_contribute_forum_text) + " " +
                                                      getString(R.string.community_contribute_forum_text_link,
                                                                ThemeColorUtils.primaryColorToHexString(this),
                                                                getString(R.string.help_link),
                                                                getString(R.string.community_contribute_forum_forum))));

        TextView contributeTranslationView = binding.communityContributeTranslateText;
        contributeTranslationView.setMovementMethod(LinkMovementMethod.getInstance());
        contributeTranslationView.setText(Html.fromHtml(
            getString(R.string.community_contribute_translate_link,
                      ThemeColorUtils.primaryColorToHexString(this),
                      getString(R.string.translation_link),
                      getString(R.string.community_contribute_translate_translate)) + " " +
                getString(R.string.community_contribute_translate_text)));

        TextView contributeGithubView = binding.communityContributeGithubText;
        contributeGithubView.setMovementMethod(LinkMovementMethod.getInstance());
        contributeGithubView.setText(Html.fromHtml(
            getString(R.string.community_contribute_github_text,
                      getString(R.string.community_contribute_github_text_link,
                                ThemeColorUtils.primaryColorToHexString(this),
                                getString(R.string.contributing_link)))));

        MaterialButton reportButton = binding.communityTestingReport;
        ThemeButtonUtils.colorPrimaryButton(reportButton, this);
        reportButton.setOnClickListener(v -> DisplayUtils.startLinkIntent(this, R.string.report_issue_link));

        binding.communityBetaFdroid.setOnClickListener(
            l -> DisplayUtils.startLinkIntent(this, R.string.fdroid_beta_link));

        binding.communityReleaseCandidateFdroid.setOnClickListener(
            l -> DisplayUtils.startLinkIntent(this, R.string.fdroid_link));

        binding.communityReleaseCandidatePlaystore.setOnClickListener(
            l -> DisplayUtils.startLinkIntent(this, R.string.play_store_register_beta));

        binding.communityBetaApk.setOnClickListener(
            l -> DisplayUtils.startLinkIntent(this, R.string.beta_apk_link));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        if (item.getItemId() == android.R.id.home) {
            if (isDrawerOpen()) {
                closeDrawer();
            } else {
                openDrawer();
            }
        } else {
            retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setDrawerMenuItemChecked(R.id.nav_community);
    }
}
