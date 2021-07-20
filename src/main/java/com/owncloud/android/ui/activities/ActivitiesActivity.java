/*
 *   Nextcloud Android client application
 *
 *   Copyright (C) 2018 Edvard Holst
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

import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ActivityListLayoutBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.model.RichObject;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;
import com.owncloud.android.ui.activities.data.files.FilesRepository;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.ActivityListAdapter;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeLayoutUtils;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.owncloud.android.ui.activity.FileActivity.EXTRA_ACCOUNT;
import static com.owncloud.android.ui.activity.FileActivity.EXTRA_FILE;

/**
 * This Activity presents activities feed.
 */
public class ActivitiesActivity extends DrawerActivity implements ActivityListInterface, ActivitiesContract.View {
    private static final String TAG = ActivitiesActivity.class.getSimpleName();

    private ActivityListLayoutBinding binding;
    private ActivityListAdapter adapter;
    private int lastGiven;
    private boolean isLoadingActivities;
    private ActivitiesContract.ActionListener actionListener;

    @Inject ActivitiesRepository activitiesRepository;
    @Inject FilesRepository filesRepository;
    @Inject ClientFactory clientFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        actionListener = new ActivitiesPresenter(activitiesRepository, filesRepository, this);

        binding = ActivityListLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // setup toolbar
        setupToolbar();

        ThemeLayoutUtils.colorSwipeRefreshLayout(this, binding.swipeContainingList);

        // setup drawer
        setupDrawer(R.id.nav_activity);
        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_activities));

        binding.swipeContainingList.setOnRefreshListener(() -> {
            // We set lastGiven variable to undefined here since when manually refreshing
            // activities data we want to clear the list and reset the pagination.
            lastGiven = ActivitiesContract.ActionListener.UNDEFINED;
            actionListener.loadActivities(lastGiven);
        });
    }

    @VisibleForTesting
    public ActivityListLayoutBinding getBinding() {
        return binding;
    }

    /**
     * sets up the UI elements and loads all activity items.
     */
    private void setupContent() {
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_activity);

        adapter = new ActivityListAdapter(this,
                                          getUserAccountManager(),
                                          this,
                                          clientFactory,
                                          false);
        binding.list.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        binding.list.setLayoutManager(layoutManager);
        binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = recyclerView.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition();

                // synchronize loading state when item count changes
                if (!isLoadingActivities && (totalItemCount - visibleItemCount) <= (firstVisibleItemIndex + 5)
                    && lastGiven > 0) {
                    // Almost reached the end, continue to load new activities
                    actionListener.loadActivities(lastGiven);
                }
            }
        });

        actionListener.loadActivities(ActivitiesContract.ActionListener.UNDEFINED);
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

        actionListener.onResume();

        setDrawerMenuItemChecked(R.id.nav_activity);

        setupContent();
    }

    @Override
    public void onActivityClicked(RichObject richObject) {
        String path = FileUtils.PATH_SEPARATOR + richObject.getPath();
        actionListener.openActivity(path, this);
    }

    @Override
    public void showActivities(List<Object> activities, NextcloudClient client, int lastGiven) {
        boolean clear = false;
        if (this.lastGiven == ActivitiesContract.ActionListener.UNDEFINED) {
            clear = true;
        }
        adapter.setActivityItems(activities, client, clear);
        this.lastGiven = lastGiven;

        // Hide the recyclerView if list is empty
        if (adapter.isEmpty()) {
            showEmptyContent(getString(R.string.activities_no_results_headline), getString(R.string.activities_no_results_message));
            binding.loadingContent.setVisibility(View.GONE);
            binding.list.setVisibility(View.GONE);
        } else {
            binding.emptyList.emptyListView.setVisibility(View.GONE);
            binding.loadingContent.setVisibility(View.GONE);
            binding.list.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showActivitiesLoadError(String error) {
        DisplayUtils.showSnackMessage(this, error);
    }

    @Override
    public void showActivityDetailUI(OCFile ocFile) {
        Intent showDetailsIntent;
        if (PreviewImageFragment.canBePreviewed(ocFile)) {
            showDetailsIntent = new Intent(getBaseContext(), PreviewImageActivity.class);
        } else {
            showDetailsIntent = new Intent(getBaseContext(), FileDisplayActivity.class);
        }
        showDetailsIntent.putExtra(EXTRA_FILE, ocFile);
        showDetailsIntent.putExtra(EXTRA_ACCOUNT, getAccount());
        startActivity(showDetailsIntent);

    }

    @Override
    public void showActivityDetailUIIsNull() {
        DisplayUtils.showSnackMessage(this, R.string.file_not_found);
    }

    @Override
    public void showActivityDetailError(String error) {
        DisplayUtils.showSnackMessage(this, error);
    }

    @Override
    public void showLoadingMessage() {
        binding.emptyList.emptyListView.setVisibility(View.GONE);
    }

    @Override
    public void showEmptyContent(String headline, String message) {
        binding.emptyList.emptyListViewHeadline.setText(headline);
        binding.emptyList.emptyListViewText.setText(message);
        binding.loadingContent.setVisibility(View.GONE);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewHeadline.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setProgressIndicatorState(boolean isActive) {
        isLoadingActivities = isActive;
        if (!adapter.isEmpty()) {
            binding.swipeContainingList.post(() -> binding.swipeContainingList.setRefreshing(isActive));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        actionListener.onStop();
    }
}
