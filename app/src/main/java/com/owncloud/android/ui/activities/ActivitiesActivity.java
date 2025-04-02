/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
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

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.owncloud.android.ui.activity.FileActivity.EXTRA_FILE;
import static com.owncloud.android.ui.activity.FileActivity.EXTRA_USER;

/**
 * This Activity presents activities feed.
 */
public class ActivitiesActivity extends DrawerActivity implements ActivityListInterface, ActivitiesContract.View {
    private static final String TAG = ActivitiesActivity.class.getSimpleName();

    ActivityListLayoutBinding binding;
    private ActivityListAdapter adapter;
    private long lastGiven;
    private boolean isLoadingActivities;
    private ActivitiesContract.ActionListener actionListener;
    private Snackbar snackbar;

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

        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList);

        // setup drawer
        setupDrawer();
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
                                          false,
                                          viewThemeUtils);
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
        setupContent();
    }

    @Override
    public void onActivityClicked(RichObject richObject) {
        String path = FileUtils.PATH_SEPARATOR + richObject.getPath();
        actionListener.openActivity(path, this);
    }

    @Override
    public void showActivities(List<Object> activities, NextcloudClient client, long lastGiven) {
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
        snackbar = DisplayUtils.showSnackMessage(this, error);
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
        showDetailsIntent.putExtra(EXTRA_USER, getUser().orElseThrow(RuntimeException::new));
        startActivity(showDetailsIntent);

    }

    @Override
    public void showActivityDetailUIIsNull() {
        snackbar = DisplayUtils.showSnackMessage(this, R.string.file_not_found);
    }

    @Override
    public void showActivityDetailError(String error) {
        snackbar = DisplayUtils.showSnackMessage(this, error);
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

    @VisibleForTesting
    public void dismissSnackbar() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
            snackbar = null;
        }
    }
}
