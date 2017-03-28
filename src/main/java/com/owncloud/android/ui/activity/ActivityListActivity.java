/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2017 Andy Scherzinger
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.GetRemoteActivitiesOperation;
import com.owncloud.android.lib.resources.activities.models.Activity;
import com.owncloud.android.ui.adapter.ActivityListAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Activity displaying all server side stored activity items.
 */
public class ActivityListActivity extends FileActivity {

    private static final String TAG = ActivityListActivity.class.getSimpleName();


    @BindView(android.R.id.list)
    public RecyclerView recyclerView;

    @BindView((android.R.id.empty))
    public TextView empty;

    private ActivityListAdapter adapter;
    private Unbinder unbinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activitiy_list_layout);
        unbinder = ButterKnife.bind(this);
        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_activity);
        getSupportActionBar().setTitle(getString(R.string.drawer_item_activity));

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
        // TODO add all (recycler) view relevant code + data loading + adapter etc.

        adapter = new ActivityListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchAndSetData();
    }

    private void fetchAndSetData() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Account account = AccountUtils.getCurrentOwnCloudAccount(ActivityListActivity.this);
                RemoteOperation getRemoteNotificationOperation = new GetRemoteActivitiesOperation();
                Log_OC.d(TAG, "BEFORE getRemoteActivitiesOperation.execute");
                final RemoteOperationResult result =
                        getRemoteNotificationOperation.execute(account, ActivityListActivity.this);

                if (result.isSuccess() && result.getData() != null) {
                    final ArrayList<Object> activities = result.getData();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (activities.size() > 0) {
                                populateList(activities);
                                empty.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                empty.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            empty.setVisibility(View.VISIBLE);
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
}
