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

import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
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
import com.owncloud.android.utils.ThemeUtils;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.owncloud.android.ui.activity.FileActivity.EXTRA_ACCOUNT;
import static com.owncloud.android.ui.activity.FileActivity.EXTRA_FILE;

public class ActivitiesActivity extends DrawerActivity implements ActivityListInterface, ActivitiesContract.View {
    private static final String TAG = ActivitiesActivity.class.getSimpleName();
    private static final int UNDEFINED = -1;

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
    private int lastGiven;

    private boolean isLoadingActivities;

    private ActivitiesContract.ActionListener mActionListener;
    @Inject ActivitiesRepository activitiesRepository;
    @Inject FilesRepository filesRepository;
    @Inject ClientFactory clientFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        mActionListener = new ActivitiesPresenter(activitiesRepository, filesRepository, this);

        setContentView(R.layout.activity_list_layout);
        unbinder = ButterKnife.bind(this);

        // setup toolbar
        setupToolbar();

        ThemeUtils.colorSwipeRefreshLayout(this, swipeListRefreshLayout);

        // setup drawer
        setupDrawer(R.id.nav_activity);
        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_activities));
        setDrawerIndicatorEnabled(false);
        swipeListRefreshLayout.setOnRefreshListener(() -> {
            // We set lastGiven variable to undefined here since when manually refreshing
            // activities data we want to clear the list and reset the pagination.
            lastGiven = UNDEFINED;
            mActionListener.loadActivities(lastGiven);
        });

        // Since we use swipe-to-refresh for progress indication we can hide the inherited
        // progressBar, message and headline
        emptyContentProgressBar.setVisibility(View.GONE);
        emptyContentMessage.setVisibility(View.INVISIBLE);
        emptyContentHeadline.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    /**
     * sets up the UI elements and loads all activity items.
     */
    private void setupContent() {
        emptyContentIcon.setImageResource(R.drawable.ic_activity);
        emptyContentProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryAccentColor(this),
                                                                          PorterDuff.Mode.SRC_IN);

        FileDataStorageManager storageManager = new FileDataStorageManager(getAccount(), getContentResolver());
        adapter = new ActivityListAdapter(this,
                                          getUserAccountManager(),
                                          this,
                                          storageManager,
                                          getCapabilities(),
                                          clientFactory,
                                          false);
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
                    && lastGiven > 0) {
                    // Almost reached the end, continue to load new activities
                    mActionListener.loadActivities(lastGiven);
                }
            }
        });

        mActionListener.loadActivities(UNDEFINED);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;

        switch (item.getItemId()) {
            case android.R.id.home:
//                finish();
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
    public void showActivities(List<Object> activities, NextcloudClient client, int lastGiven) {
        boolean clear = false;
        if (this.lastGiven == UNDEFINED) {
            clear = true;
        }
        adapter.setActivityItems(activities, client, clear);
        this.lastGiven = lastGiven;

        // Hide the recyclerView if list is empty
        if (adapter.isEmpty()) {
            showEmptyContent(noResultsHeadline, noResultsMessage);
            recyclerView.setVisibility(View.INVISIBLE);
        } else {
            emptyContentMessage.setVisibility(View.INVISIBLE);
            emptyContentHeadline.setVisibility(View.INVISIBLE);

            recyclerView.setVisibility(View.VISIBLE);
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
            emptyContentHeadline.setVisibility(View.VISIBLE);
            emptyContentMessage.setVisibility(View.VISIBLE);

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
