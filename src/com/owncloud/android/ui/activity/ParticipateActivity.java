package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.owncloud.android.R;

/**
 * Created by tobi on 03.09.16.
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
        if (item.getItemId() == android.R.id.home) {
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
        }

        return true;
    }
}
