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

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.ThemeUtils;

/**
 * Activity providing information about ways to participate in the app's development.
 */
public class ParticipateActivity extends FileActivity {

    private static final String TAG = ParticipateActivity.class.getSimpleName();
    private static final String SCREEN_NAME = "Participate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.participate_layout);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_participate);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(ThemeUtils.getColoredTitle(getString(R.string.drawer_participate),
                    ThemeUtils.fontColor(this)));
        }

        setupContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    private void setupContent() {
        TextView rcView = findViewById(R.id.participate_release_candidate_text);
        rcView.setMovementMethod(LinkMovementMethod.getInstance());

        TextView contributeIrcView = findViewById(R.id.participate_contribute_irc_text);
        contributeIrcView.setMovementMethod(LinkMovementMethod.getInstance());
        contributeIrcView.setText(Html.fromHtml(getString(R.string.participate_contribute_irc_text) + " " +
                getString(R.string.participate_contribute_irc_text_link,
                        ThemeUtils.colorToHexString(ThemeUtils.primaryColor(this)),
                        getString(R.string.irc_weblink))));

        TextView contributeForumView = findViewById(R.id.participate_contribute_forum_text);
        contributeForumView.setMovementMethod(LinkMovementMethod.getInstance());
        contributeForumView.setText(Html.fromHtml(getString(R.string.participate_contribute_forum_text) + " " +
                getString(R.string.participate_contribute_forum_text_link,
                        ThemeUtils.colorToHexString(ThemeUtils.primaryColor(this)),
                        getString(R.string.help_link), getString(R.string.participate_contribute_forum_forum))));

        TextView contributeTranslationView = findViewById(R.id.participate_contribute_translate_text);
        contributeTranslationView.setMovementMethod(LinkMovementMethod.getInstance());
        contributeTranslationView.setText(Html.fromHtml(
                getString(R.string.participate_contribute_translate_link,
                        ThemeUtils.colorToHexString(ThemeUtils.primaryColor(this)),
                        getString(R.string.translation_link),
                        getString(R.string.participate_contribute_translate_translate)) + " " +
                        getString(R.string.participate_contribute_translate_text)));

        TextView contributeGithubView = findViewById(R.id.participate_contribute_github_text);
        contributeGithubView.setMovementMethod(LinkMovementMethod.getInstance());
        contributeGithubView.setText(Html.fromHtml(
                getString(R.string.participate_contribute_github_text,
                        getString(R.string.participate_contribute_github_text_link,
                                ThemeUtils.colorToHexString(ThemeUtils.primaryColor(this)),
                                getString(R.string.contributing_link)))));

        AppCompatButton reportButton = findViewById(R.id.participate_testing_report);
        reportButton.getBackground().setColorFilter(ThemeUtils.primaryColor(this), PorterDuff.Mode.SRC_ATOP);
        reportButton.setTextColor(ThemeUtils.fontColor(this));
        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.report_issue_link))));
            }
        });
    }

    public void onGetBetaFDroidClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.fdroid_beta_link))));
    }

    public void onGetRCFDroidClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.fdroid_link))));
    }

    public void onGetRCPlayStoreClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.play_store_register_beta))));
    }

    public void onGetBetaApkClick(View view) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.beta_apk_link))));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                break;
            }

            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent fileDisplayActivity = new Intent(getApplicationContext(),
                FileDisplayActivity.class);
        fileDisplayActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(fileDisplayActivity);
    }
}
