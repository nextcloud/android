/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.GetActivitiesRemoteOperation;
import com.owncloud.android.lib.resources.activities.model.RichObject;
import com.owncloud.android.lib.resources.comments.MarkCommentsAsReadRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFileVersionsRemoteOperation;
import com.owncloud.android.lib.resources.files.model.FileVersion;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.operations.CommentFileOperation;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.adapter.ActivityAndVersionListAdapter;
import com.owncloud.android.ui.events.CommentsEvent;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.interfaces.VersionListInterface;
import com.owncloud.android.utils.ThemeUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class FileDetailActivitiesFragment extends Fragment implements
    ActivityListInterface,
    VersionListInterface.View,
    Injectable {

    private static final String TAG = FileDetailActivitiesFragment.class.getSimpleName();

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";
    private static final int END_REACHED = 0;

    private ActivityAndVersionListAdapter adapter;
    private Unbinder unbinder;
    private OwnCloudClient ownCloudClient;
    private NextcloudClient nextcloudClient;

    private OCFile file;
    private Account account;

    private int lastGiven;
    private boolean isLoadingActivities;

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

    @BindView(R.id.commentInputField)
    public TextInputEditText commentInput;

    @BindString(R.string.activities_no_results_headline)
    public String noResultsHeadline;

    @BindString(R.string.activities_no_results_message)
    public String noResultsMessage;

    private boolean restoreFileVersionSupported;
    private FileOperationsHelper operationsHelper;
    private VersionListInterface.CommentCallback callback;

    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;

    public static FileDetailActivitiesFragment newInstance(OCFile file, Account account) {
        FileDetailActivitiesFragment fragment = new FileDetailActivitiesFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        file = getArguments().getParcelable(ARG_FILE);
        account = getArguments().getParcelable(ARG_ACCOUNT);

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelable(FileActivity.EXTRA_FILE);
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }

        View view = inflater.inflate(R.layout.file_details_activities_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);

        setupView();

        onCreateSwipeToRefresh(swipeEmptyListRefreshLayout);
        onCreateSwipeToRefresh(swipeListRefreshLayout);

        fetchAndSetData(-1);

        swipeListRefreshLayout.setOnRefreshListener(() -> onRefreshListLayout(swipeListRefreshLayout));
        swipeEmptyListRefreshLayout.setOnRefreshListener(() -> onRefreshListLayout(swipeEmptyListRefreshLayout));

        callback = new VersionListInterface.CommentCallback() {

            @Override
            public void onSuccess() {
                commentInput.getText().clear();
                fetchAndSetData(-1);
            }

            @Override
            public void onError(int error) {
                Snackbar.make(recyclerView, error, Snackbar.LENGTH_LONG).show();
            }
        };

        commentInput.getBackground().setColorFilter(
                ThemeUtils.primaryAccentColor(getContext()),
                PorterDuff.Mode.SRC_ATOP
        );

        ThemeUtils.themeEditText(getContext(), commentInput, false);

        return view;
    }

    @OnClick(R.id.submitComment)
    public void submitComment() {
        Editable commentField = commentInput.getText();

        if (commentField == null) {
            return;
        }

        String trimmedComment = commentField.toString().trim();

        if (trimmedComment.length() > 0) {
            new SubmitCommentTask(trimmedComment, file.getLocalId(), callback, ownCloudClient).execute();
        }
    }

    private void onRefreshListLayout(SwipeRefreshLayout refreshLayout) {
        setLoadingMessage();
        if (refreshLayout != null && refreshLayout.isRefreshing()) {
            refreshLayout.setRefreshing(false);
        }
        fetchAndSetData(-1);
    }

    private void setLoadingMessage() {
        emptyContentHeadline.setText(R.string.file_list_loading);
        emptyContentMessage.setText("");
        emptyContentIcon.setVisibility(View.GONE);
        emptyContentProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    private void setupView() {
        FileDataStorageManager storageManager = new FileDataStorageManager(account, requireActivity().getContentResolver());
        operationsHelper = ((ComponentsGetter) requireActivity()).getFileOperationsHelper();

        OCCapability capability = storageManager.getCapability(account.name);
        OwnCloudVersion serverVersion = accountManager.getServerVersion(account);
        restoreFileVersionSupported = capability.getFilesVersioning().isTrue() &&
                serverVersion.compareTo(OwnCloudVersion.nextcloud_14) >= 0;

        emptyContentProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryAccentColor(getContext()),
                PorterDuff.Mode.SRC_IN);
        emptyContentIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_activity_light_grey));

        adapter = new ActivityAndVersionListAdapter(getContext(), accountManager, this, this,
                                                    storageManager,
                                                    capability);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

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
                    fetchAndSetData(lastGiven);
                }
            }
        });
    }

    public void reload() {
        fetchAndSetData(-1);
    }

    /**
     * @param lastGiven int; -1 to disable
     */
    private void fetchAndSetData(int lastGiven) {
        final FragmentActivity activity = getActivity();

        if (activity == null) {
            Log_OC.e(this, "Activity is null, aborting!");
            return;
        }

        final SwipeRefreshLayout empty = swipeEmptyListRefreshLayout;
        final SwipeRefreshLayout list = swipeListRefreshLayout;
        final User user = accountManager.getUser();

        if (user.isAnonymous()) {
            activity.runOnUiThread(() -> {
                setEmptyContent(getString(R.string.common_error), getString(R.string.file_detail_activity_error));
                list.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
            });
            return;
        }

        Thread t = new Thread(() -> {
            try {
                ownCloudClient = clientFactory.create(user);
                nextcloudClient = clientFactory.createNextcloudClient(user);

                isLoadingActivities = true;

                GetActivitiesRemoteOperation getRemoteNotificationOperation;

                if (lastGiven > 0) {
                    getRemoteNotificationOperation = new GetActivitiesRemoteOperation(file.getLocalId(), lastGiven);
                } else {
                    getRemoteNotificationOperation = new GetActivitiesRemoteOperation(file.getLocalId());
                }

                Log_OC.d(TAG, "BEFORE getRemoteActivitiesOperation.execute");
                final RemoteOperationResult result = nextcloudClient.execute(getRemoteNotificationOperation);

                ArrayList<Object> versions = null;
                if (restoreFileVersionSupported) {
                    ReadFileVersionsRemoteOperation readFileVersionsOperation = new ReadFileVersionsRemoteOperation(
                        file.getLocalId());

                    RemoteOperationResult result1 = readFileVersionsOperation.execute(ownCloudClient);

                    versions = result1.getData();
                }

                if (result.isSuccess() && result.getData() != null) {
                    final List<Object> data = result.getData();
                    final List<Object> activitiesAndVersions = (ArrayList) data.get(0);

                    this.lastGiven = (int) data.get(1);

                    if (activitiesAndVersions.isEmpty()) {
                        this.lastGiven = END_REACHED;
                    }

                    if (restoreFileVersionSupported && versions != null) {
                        activitiesAndVersions.addAll(versions);
                    }

                    activity.runOnUiThread(() -> {
                        populateList(activitiesAndVersions, lastGiven == -1);
                        if (adapter.getItemCount() == 0) {
                            setEmptyContent(noResultsHeadline, noResultsMessage);
                            list.setVisibility(View.GONE);
                            empty.setVisibility(View.VISIBLE);
                        } else {
                            empty.setVisibility(View.GONE);
                            list.setVisibility(View.VISIBLE);
                        }
                        isLoadingActivities = false;
                    });
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    String logMessage = result.getLogMessage();
                    if (result.getHttpCode() == HttpStatus.SC_NOT_MODIFIED) {
                        logMessage = noResultsMessage;
                    }
                    final String finalLogMessage = logMessage;
                    activity.runOnUiThread(() -> {
                        setErrorContent(finalLogMessage);
                        isLoadingActivities = false;
                    });
                }

                hideRefreshLayoutLoader(activity);
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(TAG, "Error fetching file details activities", e);
            }
        });

        t.start();
    }

    public void markCommentsAsRead() {
        new Thread(() -> {
            if (file.getUnreadCommentsCount() > 0) {
                MarkCommentsAsReadRemoteOperation unreadOperation = new MarkCommentsAsReadRemoteOperation(
                    file.getLocalId());
                RemoteOperationResult remoteOperationResult = unreadOperation.execute(ownCloudClient);

                if (remoteOperationResult.isSuccess()) {
                    EventBus.getDefault().post(new CommentsEvent(file.getRemoteId()));
                }
            }
        }).start();
    }

    private void populateList(List<Object> activities, boolean clear) {
        adapter.setActivityAndVersionItems(activities, nextcloudClient, clear);
    }

    private void setEmptyContent(String headline, String message) {
        if (emptyContentContainer != null && emptyContentMessage != null) {
            emptyContentIcon.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_activity_light_grey));
            emptyContentHeadline.setText(headline);
            emptyContentMessage.setText(message);

            emptyContentMessage.setVisibility(View.VISIBLE);
            emptyContentProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setVisibility(View.VISIBLE);
        }
    }

    private void setErrorContent(String message) {
        if (emptyContentContainer != null && emptyContentMessage != null) {
            emptyContentHeadline.setText(R.string.common_error);
            emptyContentIcon.setImageDrawable(requireContext().getResources().getDrawable(R.drawable.ic_list_empty_error));
            emptyContentMessage.setText(message);

            emptyContentMessage.setVisibility(View.VISIBLE);
            emptyContentProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setVisibility(View.VISIBLE);
        }
    }

    private void hideRefreshLayoutLoader(FragmentActivity activity) {
        activity.runOnUiThread(() -> {
            if (swipeListRefreshLayout != null) {
                swipeListRefreshLayout.setRefreshing(false);
            }
            if (swipeEmptyListRefreshLayout != null) {
                swipeEmptyListRefreshLayout.setRefreshing(false);
            }
            isLoadingActivities = false;
        });
    }

    protected void onCreateSwipeToRefresh(SwipeRefreshLayout refreshLayout) {
        int primaryColor = ThemeUtils.primaryColor(getContext());
        int darkColor = ThemeUtils.primaryDarkColor(getContext());
        int accentColor = ThemeUtils.primaryAccentColor(getContext());

        // Colors in animations
        refreshLayout.setColorSchemeColors(accentColor, primaryColor, darkColor);
    }

    @Override
    public void onActivityClicked(RichObject richObject) {
        // TODO implement activity click
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(FileActivity.EXTRA_FILE, file);
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, account);
    }

    @Override
    public void onRestoreClicked(FileVersion fileVersion) {
        operationsHelper.restoreFileVersion(fileVersion);
    }

    private static class SubmitCommentTask extends AsyncTask<Void, Void, Boolean> {

        private String message;
        private String fileId;
        private VersionListInterface.CommentCallback callback;
        private OwnCloudClient client;

        private SubmitCommentTask(String message, String fileId, VersionListInterface.CommentCallback callback,
                                  OwnCloudClient client) {
            this.message = message;
            this.fileId = fileId;
            this.callback = callback;
            this.client = client;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            CommentFileOperation commentFileOperation = new CommentFileOperation(message, fileId);

            RemoteOperationResult result = commentFileOperation.execute(client);

            return result.isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            if (success) {
                callback.onSuccess();
            } else {
                callback.onError(R.string.error_comment_file);

            }
        }
    }
}
