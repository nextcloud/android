package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.ui.fragment.UploadListFragment;

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
        }

        TextView rcView = (TextView) findViewById(R.id.participate_rcView);
        if (rcView != null) {
            rcView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView participateView = (TextView) findViewById(R.id.participate_participateView);
        if (participateView != null) {
            participateView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        TextView contributeView = (TextView) findViewById(R.id.participate_contributeView);
        if (contributeView != null) {
            contributeView.setMovementMethod(LinkMovementMethod.getInstance());
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
