/*
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
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.GetRemoteActivitiesOperation;
import com.owncloud.android.lib.resources.activities.models.RichObject;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.adapter.ActivityListAdapter;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Activity displaying all server side stored activity items.
 */
public class ActivitiesListActivity extends FileActivity implements ActivityListInterface {

    private static final String TAG = ActivitiesListActivity.class.getSimpleName();
    private static final String SCREEN_NAME = "Activities";

    @BindView(R.id.empty_list_view)
    public LinearLayout emptyContentContainer;

    @BindView(R.id.swipe_containing_list)
    public SwipeRefreshLayout swipeListRefreshLayout;

    @BindView(R.id.swipe_containing_empty)
    public SwipeRefreshLayout swipeEmptyListRefreshLayout;

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

    @BindView(R.id.bottom_navigation_view)
    public BottomNavigationView bottomNavigationView;

    @BindString(R.string.activities_no_results_headline)
    public String noResultsHeadline;

    @BindString(R.string.activities_no_results_message)
    public String noResultsMessage;

    private ActivityListAdapter adapter;
    private Unbinder unbinder;
    private OwnCloudClient ownCloudClient;
    private AsyncTask<String, Object, OCFile> updateTask;

    private String nextPageUrl;
    private boolean isLoadingActivities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list_layout);
        unbinder = ButterKnife.bind(this);

        // setup toolbar
        setupToolbar();

        onCreateSwipeToRefresh(swipeEmptyListRefreshLayout);
        onCreateSwipeToRefresh(swipeListRefreshLayout);

        // setup drawer
        setupDrawer(R.id.nav_activity);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            ThemeUtils.setColoredTitle(actionBar, getString(R.string.drawer_item_activities));
        }

        swipeListRefreshLayout.setOnRefreshListener(() -> {
                    setLoadingMessage();
                    if (swipeListRefreshLayout != null && swipeListRefreshLayout.isRefreshing()) {
                        swipeListRefreshLayout.setRefreshing(false);
                    }
                    fetchAndSetData(null);
                }
        );

        swipeEmptyListRefreshLayout.setOnRefreshListener(() -> {
                    setLoadingMessage();
                    if (swipeEmptyListRefreshLayout != null && swipeEmptyListRefreshLayout.isRefreshing()) {
                        swipeEmptyListRefreshLayout.setRefreshing(false);
                    }
                    fetchAndSetData(null);
                }
        );
    }

    protected void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {
        int primaryColor = ThemeUtils.primaryColor();
        int darkColor = ThemeUtils.primaryDarkColor();
        int accentColor = ThemeUtils.primaryAccentColor();

        // Colors in animations
        refreshLayout.setColorSchemeColors(accentColor, primaryColor, darkColor);
    }

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
        emptyContentProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryAccentColor(),
                PorterDuff.Mode.SRC_IN);
        setLoadingMessage();

        FileDataStorageManager storageManager = new FileDataStorageManager(getAccount(), getContentResolver());
        adapter = new ActivityListAdapter(this, this, storageManager);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = recyclerView.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemIndex = layoutManager.findFirstVisibleItemPosition();

                // synchronize loading state when item count changes
                if (!isLoadingActivities && (totalItemCount - visibleItemCount) <= (firstVisibleItemIndex + 5)
                        && nextPageUrl != null && !nextPageUrl.isEmpty()) {
                    // Almost reached the end, continue to load new activities
                    fetchAndSetData(nextPageUrl);
                }
            }
        });

        if (getResources().getBoolean(R.bool.bottom_toolbar_enabled)) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            DisplayUtils.setupBottomBar(bottomNavigationView, getResources(), this, -1);
        }

        fetchAndSetData(null);
    }

    /**
     * @param pageUrl String
     */
    private void fetchAndSetData(String pageUrl) {
        final Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
        final Context context = MainApp.getAppContext();

        Thread t = new Thread(() -> {
                OwnCloudAccount ocAccount;
                try {
                    ocAccount = new OwnCloudAccount(currentAccount, context);
                    ownCloudClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, MainApp.getAppContext());
                    ownCloudClient.setOwnCloudVersion(AccountUtils.getServerVersion(currentAccount));
                    isLoadingActivities = true;
                    runOnUiThread(() -> setIndeterminate(isLoadingActivities));

                    GetRemoteActivitiesOperation getRemoteNotificationOperation = new GetRemoteActivitiesOperation();
                    if (pageUrl != null) {
                        getRemoteNotificationOperation.setNextUrl(pageUrl);
                    }

                    Log_OC.d(TAG, "BEFORE getRemoteActivitiesOperation.execute");
                    final RemoteOperationResult result = getRemoteNotificationOperation.execute(ownCloudClient);

                    if (result.isSuccess() && result.getData() != null) {
                        final ArrayList<Object> data = result.getData();
                        final ArrayList<Object> activities = (ArrayList) data.get(0);
                        nextPageUrl = (String) data.get(1);

                        runOnUiThread(() -> {
                            populateList(activities, ownCloudClient, pageUrl == null);
                            if (activities.size() > 0) {
                                swipeEmptyListRefreshLayout.setVisibility(View.GONE);
                                swipeListRefreshLayout.setVisibility(View.VISIBLE);
                            } else {
                                setEmptyContent(noResultsHeadline, noResultsMessage);
                                swipeListRefreshLayout.setVisibility(View.GONE);
                                swipeEmptyListRefreshLayout.setVisibility(View.VISIBLE);
                            }
                            isLoadingActivities = false;
                            setIndeterminate(isLoadingActivities);
                        });
                    } else {
                        Log_OC.d(TAG, result.getLogMessage());
                        // show error
                        String logMessage = result.getLogMessage();
                        if (result.getHttpCode() == 304) {
                            logMessage = noResultsMessage;
                        }
                        final String finalLogMessage = logMessage;
                        runOnUiThread(() -> {
                            setEmptyContent(noResultsHeadline, finalLogMessage);
                            isLoadingActivities = false;
                            setIndeterminate(isLoadingActivities);
                        });
                    }

                    hideRefreshLayoutLoader();
                } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                    Log_OC.e(TAG, "Account not found", e);
                } catch (IOException e) {
                    Log_OC.e(TAG, "IO error", e);
                } catch (OperationCanceledException e) {
                    Log_OC.e(TAG, "Operation has been canceled", e);
                } catch (AuthenticatorException e) {
                    Log_OC.e(TAG, "Authentication Exception", e);
                }
            }
        );

        t.start();
    }

    private void hideRefreshLayoutLoader() {
        runOnUiThread(() -> {
            if (swipeListRefreshLayout != null) {
                swipeListRefreshLayout.setRefreshing(false);
            }
            if (swipeEmptyListRefreshLayout != null) {
                swipeEmptyListRefreshLayout.setRefreshing(false);
            }
            isLoadingActivities = false;
            setIndeterminate(isLoadingActivities);
        });
    }

    private void populateList(List<Object> activities, OwnCloudClient mClient, boolean clear) {
        adapter.setActivityItems(activities, mClient, clear);
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

    @Override
    protected void onResume() {
        super.onResume();

        setupContent();

        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    @Override
    protected void onStop() {
        if (updateTask != null) {
            updateTask.cancel(true);
        }

        super.onStop();
    }

    @Override
    public void onActivityClicked(RichObject richObject) {
        String path = FileUtils.PATH_SEPARATOR + richObject.getPath();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeEmptyListRefreshLayout.setVisibility(View.VISIBLE);
                swipeListRefreshLayout.setVisibility(View.GONE);
                setLoadingMessage();
            }
        });

        updateTask = new AsyncTask<String, Object, OCFile>() {
            @Override
            protected OCFile doInBackground(String... path) {
                OCFile ocFile = null;

                // always update file as it could be an old state saved in database
                ReadRemoteFileOperation operation = new ReadRemoteFileOperation(path[0]);
                RemoteOperationResult resultRemoteFileOp = operation.execute(ownCloudClient);
                if (resultRemoteFileOp.isSuccess()) {
                    OCFile temp = FileStorageUtils.fillOCFile((RemoteFile) resultRemoteFileOp.getData().get(0));

                    ocFile = getStorageManager().saveFileWithParent(temp, getBaseContext());

                    if (ocFile.isFolder()) {
                        // perform folder synchronization
                        RemoteOperation synchFolderOp = new RefreshFolderOperation(ocFile,
                                System.currentTimeMillis(),
                                false,
                                getFileOperationsHelper().isSharedSupported(),
                                true,
                                getStorageManager(),
                                getAccount(),
                                getApplicationContext());
                        synchFolderOp.execute(ownCloudClient);
                    }
                }

                return ocFile;
            }

            @Override
            protected void onPostExecute(OCFile ocFile) {
                if (!isCancelled()) {
                    if (ocFile == null) {
                        Toast.makeText(getBaseContext(), R.string.file_not_found, Toast.LENGTH_LONG).show();

                        swipeEmptyListRefreshLayout.setVisibility(View.GONE);
                        swipeListRefreshLayout.setVisibility(View.VISIBLE);
                        dismissLoadingDialog();

                    } else {
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
                }
            }
        };

        updateTask.execute(path);
    }
}
