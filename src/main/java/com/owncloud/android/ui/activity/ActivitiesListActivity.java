/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Mario Danic
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.GetRemoteActivitiesOperation;
import com.owncloud.android.ui.adapter.ActivityListAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Activity displaying all server side stored activity items.
 */
public class ActivitiesListActivity extends FileActivity {

    private static final String TAG = ActivitiesListActivity.class.getSimpleName();

    @BindView(R.id.empty_list_view)
    public LinearLayout emptyContentContainer;

    @BindView(R.id.empty_list_view_text)
    public TextView emptyContentMessage;

    @BindView(R.id.empty_list_view_headline)
    public TextView emptyContentHeadline;

    @BindView(R.id.empty_list_icon)
    public ImageView emptyContentIcon;

    @BindView(R.id.empty_list_progress)
    public ProgressBar emptyContentProgressBar;

    @BindView(android.R.id.list)
    public RecyclerView recyclerView;

    @BindString(R.string.activities_no_results_headline)
    public String noResultsHeadline;

    @BindString(R.string.activities_no_results_message)
    public String noResultsMessage;

    private ActivityListAdapter adapter;
    private Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_layout);
        unbinder = ButterKnife.bind(this);
        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_activity);
        getSupportActionBar().setTitle(getString(R.string.drawer_item_activities));

        setupContent();
    }

    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    /**
     * sets up the UI elements and loads all activity items.
     */
    private void setupContent() {
        emptyContentIcon.setImageResource(R.drawable.ic_activity_light_grey);
        setLoadingMessage();

        adapter = new ActivityListAdapter(this);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(dividerItemDecoration);

        fetchAndSetData();
    }

    private void fetchAndSetData() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Account account = AccountUtils.getCurrentOwnCloudAccount(ActivitiesListActivity.this);
                RemoteOperation getRemoteNotificationOperation = new GetRemoteActivitiesOperation();
                Log_OC.d(TAG, "BEFORE getRemoteActivitiesOperation.execute");
                final RemoteOperationResult result =
                        getRemoteNotificationOperation.execute(account, ActivitiesListActivity.this);

                if (result.isSuccess() && result.getData() != null) {
                    final ArrayList<Object> activities = result.getData();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (activities.size() > 0) {
                                populateList(activities);
                                emptyContentContainer.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                setEmptyContent(noResultsHeadline, noResultsMessage);
                            }
                        }
                    });
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setEmptyContent(noResultsHeadline, result.getLogMessage());
                        }
                    });
                }
            }
        });

        t.start();
    }

    private void populateList(List<Object> activities) {

        adapter.setActivityItems(activities);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;

        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }

            default:
                retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    private void setLoadingMessage() {
        emptyContentHeadline.setText(R.string.file_list_loading);
        emptyContentMessage.setText("");

        emptyContentIcon.setVisibility(View.GONE);
        emptyContentProgressBar.setVisibility(View.VISIBLE);
    }

    private void setEmptyContent(String headline, String message) {
        if (emptyContentContainer != null && emptyContentMessage != null) {
            emptyContentHeadline.setText(headline);
            emptyContentMessage.setText(message);

            emptyContentProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setVisibility(View.VISIBLE);
        }
    }

}
