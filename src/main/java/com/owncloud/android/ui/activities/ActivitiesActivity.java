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
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.model.RichObject;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;
import com.owncloud.android.ui.activities.data.files.FilesRepository;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.ActivityListAdapter;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.ThemeUtils;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ActivitiesActivity extends FileActivity implements ActivityListInterface, ActivitiesContract.View {
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

    @BindString(R.string.activities_no_results_headline)
    public String noResultsHeadline;

    @BindString(R.string.activities_no_results_message)
    public String noResultsMessage;

    private ActivityListAdapter adapter;
    private Unbinder unbinder;
    private String nextPageUrl;

    private boolean isLoadingActivities;

    private ActivitiesContract.ActionListener mActionListener;
    @Inject ActivitiesRepository activitiesRepository;
    @Inject FilesRepository filesRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        mActionListener = new ActivitiesPresenter(activitiesRepository, filesRepository, this);

        setContentView(R.layout.activity_list_layout);
        unbinder = ButterKnife.bind(this);

        // setup toolbar
        setupToolbar();

        onCreateSwipeToRefresh(swipeListRefreshLayout);

        // setup drawer
        setupDrawer(R.id.nav_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ThemeUtils.setColoredTitle(actionBar, getString(R.string.drawer_item_activities), this);
        }

        swipeListRefreshLayout.setOnRefreshListener(() -> {
            // We set the nextPageUrl variable to null here since when manually refreshing
            // activities data we want to clear the list and reset the pagination.
            nextPageUrl = null;
            mActionListener.loadActivities(nextPageUrl);
        });

        // Since we use swipe-to-refresh for progress indication we can hide the inherited
        // progressBar, message and headline
        emptyContentProgressBar.setVisibility(View.GONE);
        emptyContentMessage.setVisibility(View.INVISIBLE);
        emptyContentHeadline.setVisibility(View.INVISIBLE);

    }

    protected void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {
        int primaryColor = ThemeUtils.primaryColor(this);
        int darkColor = ThemeUtils.primaryDarkColor(this);
        int accentColor = ThemeUtils.primaryAccentColor(this);

        // Colors in animations
        refreshLayout.setColorSchemeColors(accentColor, primaryColor, darkColor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent i = new Intent(getApplicationContext(), FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    /**
     * sets up the UI elements and loads all activity items.
     */
    private void setupContent() {
        emptyContentIcon.setImageResource(R.drawable.ic_activity_light_grey);
        emptyContentProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryAccentColor(this),
                PorterDuff.Mode.SRC_IN);

        FileDataStorageManager storageManager = new FileDataStorageManager(getAccount(), getContentResolver());
        adapter = new ActivityListAdapter(this, getUserAccountManager(), this, storageManager, getCapabilities(), false);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = recyclerView.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition();

                // synchronize loading state when item count changes
                if (!isLoadingActivities && (totalItemCount - visibleItemCount) <= (firstVisibleItemIndex + 5)
                        && nextPageUrl != null && !nextPageUrl.isEmpty()) {
                    // Almost reached the end, continue to load new activities
                    mActionListener.loadActivities(nextPageUrl);
                }
            }
        });

        mActionListener.loadActivities(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;

        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                break;
            default:
                Log_OC.w(TAG, "Unknown menu item triggered");
                retval = super.onOptionsItemSelected(item);
                break;
        }

        return retval;
    }


    @Override
    protected void onResume() {
        super.onResume();

        mActionListener.onResume();

        setDrawerMenuItemChecked(R.id.nav_activity);

        setupContent();
    }

    @Override
    public void onActivityClicked(RichObject richObject) {
        String path = FileUtils.PATH_SEPARATOR + richObject.getPath();
        mActionListener.openActivity(path, this);
    }

    @Override
    public void showActivities(List<Object> activities, OwnCloudClient client, String nextPageUrl) {
        boolean clear = false;
        if (this.nextPageUrl == null) {
            clear = true;
        }
        adapter.setActivityItems(activities, client, clear);
        this.nextPageUrl = nextPageUrl;
        // Hide the recyclerView if list is empty
        if (activities.isEmpty()) {
            recyclerView.setVisibility(View.INVISIBLE);

            emptyContentMessage.setText(noResultsMessage);
            emptyContentHeadline.setText(noResultsHeadline);
            emptyContentMessage.setVisibility(View.VISIBLE);
            emptyContentHeadline.setVisibility(View.VISIBLE);
        } else {
            emptyContentMessage.setVisibility(View.INVISIBLE);
            emptyContentHeadline.setVisibility(View.INVISIBLE);

            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showActivitiesLoadError(String error) {
        Toast.makeText(getBaseContext(), error, Toast.LENGTH_LONG).show();
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
        Toast.makeText(getBaseContext(), R.string.file_not_found, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showActivityDetailError(String error) {
        Toast.makeText(getBaseContext(), error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showLoadingMessage() {
        emptyContentHeadline.setText(R.string.file_list_loading);
        emptyContentMessage.setText("");

        emptyContentIcon.setVisibility(View.GONE);
        emptyContentProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void showEmptyContent(String headline, String message) {
        if (emptyContentContainer != null && emptyContentMessage != null) {
            emptyContentHeadline.setText(headline);
            emptyContentMessage.setText(message);

            emptyContentProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setProgressIndicatorState(boolean isActive) {
        isLoadingActivities = isActive;
        swipeListRefreshLayout.post(() -> swipeListRefreshLayout.setRefreshing(isActive));

    }

    @Override
    protected void onStop() {
        super.onStop();

        mActionListener.onStop();
    }
}
