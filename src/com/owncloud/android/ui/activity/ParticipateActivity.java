/**
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   @author Tobias Kaminsky
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.owncloud.android.R;

/**
 * Activity providing information about ways to participate in the app's development.
 */
public class ParticipateActivity extends FileActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.participate_layout);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_participate);
        getSupportActionBar().setTitle(getString(R.string.actionbar_participate));

        setupContent();
    }

    private void setupContent() {
        TextView betaView = (TextView) findViewById(R.id.participate_betaView);
        if (betaView != null) {
            betaView.setMovementMethod(LinkMovementMethod.getInstance());
            betaView.setText(Html.fromHtml(getString(R.string.participate_beta,
                                           getString(R.string.fdroid_beta_link),
                                           getString(R.string.beta_apk_link))));
        }

        TextView rcView = (TextView) findViewById(R.id.participate_rcView);
        if (rcView != null) {
            rcView.setMovementMethod(LinkMovementMethod.getInstance());
            rcView.setText(Html.fromHtml(getString(R.string.participate_release_candidate,
                                         getString(R.string.play_store_register_beta),
                                         getString(R.string.fdroid_link))));
        }

        TextView participateView = (TextView) findViewById(R.id.participate_participateView);
        if (participateView != null) {
            participateView.setMovementMethod(LinkMovementMethod.getInstance());
            participateView.setText(Html.fromHtml(getString(R.string.participate_participate,
                                                  getString(R.string.irc_weblink),
                                                  getString(R.string.help_link))));
        }

        TextView contributeView = (TextView) findViewById(R.id.participate_contributeView);
        if (contributeView != null) {
            contributeView.setMovementMethod(LinkMovementMethod.getInstance());
            contributeView.setText(Html.fromHtml(getString(R.string.participate_contribute,
                                                 getString(R.string.contributing_link),
                                                 getString(R.string.irc_weblink))));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
            }

            default:
                retval = super.onOptionsItemSelected(item);
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
