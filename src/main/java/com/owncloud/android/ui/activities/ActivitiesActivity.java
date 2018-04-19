package com.owncloud.android.ui.activities;

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

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.models.RichObject;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activities.data.ActivitiesServiceApiImpl;
import com.owncloud.android.ui.activities.data.ActivityRepositories;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.ActivityListAdapter;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ActivitiesActivity extends FileActivity implements ActivityListInterface, ActivitiesContract.View {
    private static final String TAG = ActivitiesActivity.class.getSimpleName();
    private static final String SCREEN_NAME = "Activities";

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

    private ActivitiesContract.ActionListener mActionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        mActionListener = new ActivitiesPresenter(ActivityRepositories.getRepository(new ActivitiesServiceApiImpl()),
                this);

        setContentView(R.layout.activity_list_layout);
        unbinder = ButterKnife.bind(this);

        // setup toolbar
        setupToolbar();

        onCreateSwipeToRefresh(swipeListRefreshLayout);

        // setup drawer
        setupDrawer(R.id.nav_activity);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            ThemeUtils.setColoredTitle(actionBar, getString(R.string.drawer_item_activities));
        }

        swipeListRefreshLayout.setOnRefreshListener(() -> mActionListener.loadActivites(null)
        );

        // Since we use swipe-to-refresh for progress indication we can hide the inherited
        // progressBar, message and headline
        emptyContentProgressBar.setVisibility(View.GONE);
        emptyContentMessage.setVisibility(View.INVISIBLE);
        emptyContentHeadline.setVisibility(View.INVISIBLE);

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
                    mActionListener.loadActivites(nextPageUrl);
                }
            }
        });

        if (getResources().getBoolean(R.bool.bottom_toolbar_enabled)) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            DisplayUtils.setupBottomBar(bottomNavigationView, getResources(), this, -1);
        }

        mActionListener.loadActivites(null);
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

        runOnUiThread(() -> {
            swipeListRefreshLayout.setVisibility(View.GONE);
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

    @Override
    public void showActivites(List<Object> activities, OwnCloudClient client, boolean clear) {
        adapter.setActivityItems(activities, client, clear);
        // Hide the recylerView if list is empty
        if (activities.size() == 0) {
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

    }

    @Override
    public void showActivityDetailUI() {

    }

    @Override
    public void showActivityDetailUIIsNull() {

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
}
