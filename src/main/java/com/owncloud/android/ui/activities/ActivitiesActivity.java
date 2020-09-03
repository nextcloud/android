/*
 *   Nextcloud Android client application
 *
 *   Copyright (C) 2018 Edvard Holst
 *   Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.owncloud.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.ViewModelFactory;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.model.Activity;
import com.owncloud.android.lib.resources.activities.model.RichObject;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.ActivityListAdapter;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;

import static com.owncloud.android.ui.activity.FileActivity.EXTRA_ACCOUNT;
import static com.owncloud.android.ui.activity.FileActivity.EXTRA_FILE;

public class ActivitiesActivity extends DrawerActivity implements ActivityListInterface {
    private static final String TAG = ActivitiesActivity.class.getSimpleName();

    @BindView(R.id.empty_list_view)
    public LinearLayout emptyContentContainer;

    @BindView(R.id.swipe_containing_list)
    public SwipeRefreshLayout swipeListRefreshLayout;

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

    private ActivityListAdapter adapter;

    @Inject ClientFactory clientFactory;
    @Inject ViewModelFactory vmFactory;
    @Inject UserAccountManager accountManager;
    private ActivitiesViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_layout);
        setupDrawer(R.id.nav_activity);
        vm = new ViewModelProvider(this, vmFactory).get(ActivitiesViewModel.class);
        ButterKnife.bind(this);
        setupToolbar();
        setupUi();
        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_activities));

        swipeListRefreshLayout.setOnRefreshListener(() -> vm.refresh());

        vm.getActivities().observe(this, this::onActivitiesChanged);
        vm.isLoading().observe(this, this::onIsLoadingChanged);
        vm.getError().observe(this, this::onError);
        vm.getFile().observe(this, this::onLoadedActivity);
        vm.startLoading();
    }

    private void setupUi() {
        ThemeUtils.colorSwipeRefreshLayout(this, swipeListRefreshLayout);
        FileDataStorageManager storageManager = new FileDataStorageManager(getAccount(), getContentResolver());
        adapter = new ActivityListAdapter(this,
                                          getUserAccountManager(),
                                          this,
                                          storageManager,
                                          getCapabilities(),
                                          clientFactory,
                                          false);
        recyclerView.setAdapter(adapter);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = recyclerView.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition();
                if ((totalItemCount - visibleItemCount) <= (firstVisibleItemIndex + 5)) {
                    vm.loadMore();
                }
            }
        });

        emptyContentHeadline.setText(R.string.activities_no_results_headline);
        emptyContentMessage.setText(R.string.activities_no_results_message);
        emptyContentProgressBar.setVisibility(View.GONE);
        emptyContentIcon.setVisibility(View.VISIBLE);
        emptyContentIcon.setImageResource(R.drawable.ic_activity);
        emptyContentHeadline.setVisibility(View.VISIBLE);
        emptyContentMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent i = new Intent(getApplicationContext(), FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    private void onIsLoadingChanged(boolean isLoading) {
        swipeListRefreshLayout.setRefreshing(isLoading);
    }

    private void onActivitiesChanged(List<Activity> activities) {
        adapter.setActivityItems(activities);
        if (activities.isEmpty()) {
            emptyContentContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            emptyContentContainer.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void onError(String error) {
        if (error != null && !error.isEmpty()) {
            DisplayUtils.showSnackMessage(this, error);
            vm.clearError();
        }
    }

    private void onLoadedActivity(OCFile file) {
        if (file != null) {
            Intent showDetailsIntent;
            if (PreviewImageFragment.canBePreviewed(file)) {
                showDetailsIntent = new Intent(getBaseContext(), PreviewImageActivity.class);
            } else {
                showDetailsIntent = new Intent(getBaseContext(), FileDisplayActivity.class);
            }
            showDetailsIntent.putExtra(EXTRA_FILE, file);
            showDetailsIntent.putExtra(EXTRA_ACCOUNT, getAccount());
            startActivity(showDetailsIntent);
        } else {
            DisplayUtils.showSnackMessage(this, R.string.file_not_found);
        }
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
            Log_OC.w(TAG, "Unknown menu item triggered");
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDrawerMenuItemChecked(R.id.nav_activity);
    }

    @Override
    public void onActivityClicked(RichObject richObject) {
        String path = FileUtils.PATH_SEPARATOR + richObject.getPath();
        vm.openFile(path);
    }
}
