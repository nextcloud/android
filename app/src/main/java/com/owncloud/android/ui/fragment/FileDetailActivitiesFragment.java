/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment;

import android.content.ContentResolver;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.nextcloud.utils.extensions.FileExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsActivitiesFragmentBinding;
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
import com.owncloud.android.operations.CommentFileOperation;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.adapter.ActivityAndVersionListAdapter;
import com.owncloud.android.ui.events.CommentsEvent;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.ui.interfaces.VersionListInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.apache.commons.httpclient.HttpStatus;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FileDetailActivitiesFragment extends Fragment implements
    ActivityListInterface,
    DisplayUtils.AvatarGenerationListener,
    VersionListInterface.View,
    Injectable {

    private static final String TAG = FileDetailActivitiesFragment.class.getSimpleName();

    private static final String ARG_FILE = "FILE";
    private static final String ARG_USER = "USER";
    private static final int END_REACHED = 0;

    private ActivityAndVersionListAdapter adapter;
    private OwnCloudClient ownCloudClient;
    private NextcloudClient nextcloudClient;

    private OCFile file;
    private User user;

    private int lastGiven;
    private boolean isLoadingActivities;
    private boolean isDataFetched = false;

    private boolean restoreFileVersionSupported;
    private FileOperationsHelper operationsHelper;
    private VersionListInterface.CommentCallback callback;

    FileDetailsActivitiesFragmentBinding binding;

    @Inject UserAccountManager accountManager;
    @Inject ClientFactory clientFactory;
    @Inject ContentResolver contentResolver;
    @Inject ViewThemeUtils viewThemeUtils;

    public static FileDetailActivitiesFragment newInstance(OCFile file, User user) {
        FileDetailActivitiesFragment fragment = new FileDetailActivitiesFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, file);
        args.putParcelable(ARG_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("arguments are mandatory");
        }
        file = BundleExtensionsKt.getParcelableArgument(arguments, ARG_FILE, OCFile.class);
        user = BundleExtensionsKt.getParcelableArgument(arguments, ARG_USER, User.class);

        if (savedInstanceState != null) {
            file = BundleExtensionsKt.getParcelableArgument(savedInstanceState, ARG_FILE, OCFile.class);
            user = BundleExtensionsKt.getParcelableArgument(savedInstanceState, ARG_USER, User.class);
        }

        binding = FileDetailsActivitiesFragmentBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        setupView();

        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingEmpty);
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList);

        isLoadingActivities = true;
        fetchAndSetData(-1);

        binding.swipeContainingList.setOnRefreshListener(() -> {
            setLoadingMessage();
            binding.swipeContainingList.setRefreshing(true);
            fetchAndSetData(-1);
        });

        binding.swipeContainingEmpty.setOnRefreshListener(() -> {
            setLoadingMessageEmpty();
            fetchAndSetData(-1);
        });

        callback = new VersionListInterface.CommentCallback() {

            @Override
            public void onSuccess() {
                binding.commentInputField.getText().clear();
                fetchAndSetData(-1);
            }

            @Override
            public void onError(int error) {
                Snackbar.make(binding.list, error, Snackbar.LENGTH_LONG).show();
            }
        };

        binding.submitComment.setOnClickListener(v -> submitComment());

        viewThemeUtils.material.colorTextInputLayout(binding.commentInputFieldContainer);

        DisplayUtils.setAvatar(user,
                               this,
                               getResources().getDimension(R.dimen.activity_icon_radius),
                               getResources(),
                               binding.avatar,
                               getContext());

        return view;
    }

    public void submitComment() {
        Editable commentField = binding.commentInputField.getText();

        if (commentField == null) {
            return;
        }

        String trimmedComment = commentField.toString().trim();

        if (!trimmedComment.isEmpty() && nextcloudClient != null && isDataFetched) {
            new SubmitCommentTask(trimmedComment, file.getLocalId(), callback, nextcloudClient).execute();
        }
    }

    private void setLoadingMessage() {
        binding.swipeContainingEmpty.setVisibility(View.GONE);
    }

    @VisibleForTesting
    public void setLoadingMessageEmpty() {
        binding.swipeContainingList.setVisibility(View.GONE);
        binding.emptyList.emptyListView.setVisibility(View.GONE);
        binding.loadingContent.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupView() {
        FileDataStorageManager storageManager = new FileDataStorageManager(user,
                                                                           contentResolver);
        operationsHelper = ((ComponentsGetter) requireActivity()).getFileOperationsHelper();

        OCCapability capability = storageManager.getCapability(user.getAccountName());
        restoreFileVersionSupported = capability.getFilesVersioning().isTrue();

        binding.emptyList.emptyListIcon.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_activity, null));
        binding.emptyList.emptyListView.setVisibility(View.GONE);

        adapter = new ActivityAndVersionListAdapter(getContext(),
                                                    accountManager,
                                                    this,
                                                    this,
                                                    clientFactory,
                                                    viewThemeUtils
        );
        binding.list.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());

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

        final User user = accountManager.getUser();

        if (user.isAnonymous()) {
            activity.runOnUiThread(() -> {
                setEmptyContent(getString(R.string.common_error), getString(R.string.file_detail_activity_error));
            });
            return;
        }
        
        if (!isLoadingActivities) {
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
                RemoteOperationResult result = nextcloudClient.execute(getRemoteNotificationOperation);

                ArrayList<Object> versions = null;
                if (restoreFileVersionSupported) {
                    ReadFileVersionsRemoteOperation readFileVersionsOperation = new ReadFileVersionsRemoteOperation(
                        file.getLocalId());

                    RemoteOperationResult result1 = readFileVersionsOperation.execute(ownCloudClient);

                    if (result1.isSuccess()) {
                        versions = result1.getData();
                    }
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
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            populateList(activitiesAndVersions, lastGiven == -1);
                        }
                    });

                    isDataFetched = true;
                } else {
                    Log_OC.d(TAG, result.getLogMessage());
                    // show error
                    String logMessage = result.getLogMessage();
                    if (result.getHttpCode() == HttpStatus.SC_NOT_MODIFIED) {
                        logMessage = getString(R.string.activities_no_results_message);
                    }
                    final String finalLogMessage = logMessage;
                    activity.runOnUiThread(() -> {
                        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            setErrorContent(finalLogMessage);
                            isLoadingActivities = false;
                        }
                    });

                    isDataFetched = false;
                }

                hideRefreshLayoutLoader(activity);
            } catch (ClientFactory.CreationException e) {
                isDataFetched = false;
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

    @VisibleForTesting
    public void populateList(List<Object> activities, boolean clear) {
        adapter.setActivityAndVersionItems(activities, nextcloudClient, clear);

        if (adapter.getItemCount() == 0) {
            setEmptyContent(
                getString(R.string.activities_no_results_headline),
                getString(R.string.activities_no_results_message)
                           );
        } else {
            binding.swipeContainingList.setVisibility(View.VISIBLE);
            binding.swipeContainingEmpty.setVisibility(View.GONE);
            binding.emptyList.emptyListView.setVisibility(View.GONE);
        }
        isLoadingActivities = false;
    }

    private void setEmptyContent(String headline, String message) {
        setInfoContent(R.drawable.ic_activity, headline, message);
    }

    @VisibleForTesting
    public void setErrorContent(String message) {
        setInfoContent(R.drawable.ic_list_empty_error, getString(R.string.common_error), message);
    }

    private void setInfoContent(@DrawableRes int icon, String headline, String message) {
        binding.emptyList.emptyListIcon.setImageDrawable(ResourcesCompat.getDrawable(requireContext().getResources(),
                                                                                     icon,
                                                                                     null));
        binding.emptyList.emptyListViewHeadline.setText(headline);
        binding.emptyList.emptyListViewText.setText(message);

        binding.swipeContainingList.setVisibility(View.GONE);
        binding.loadingContent.setVisibility(View.GONE);

        binding.emptyList.emptyListViewHeadline.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
        binding.swipeContainingEmpty.setVisibility(View.VISIBLE);
    }

    private void hideRefreshLayoutLoader(FragmentActivity activity) {
        activity.runOnUiThread(() -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                binding.swipeContainingList.setRefreshing(false);
                binding.swipeContainingEmpty.setRefreshing(false);
                binding.emptyList.emptyListView.setVisibility(View.GONE);
                isLoadingActivities = false;
            }
        });
    }

    @Override
    public void onActivityClicked(RichObject richObject) {
        // TODO implement activity click
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        FileExtensionsKt.logFileSize(file, TAG);
        outState.putParcelable(ARG_FILE, file);
        outState.putParcelable(ARG_USER, user);
    }

    @Override
    public void onRestoreClicked(FileVersion fileVersion) {
        operationsHelper.restoreFileVersion(fileVersion);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        binding.avatar.setImageDrawable(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return false;
    }
    
    @VisibleForTesting
    public void disableLoadingActivities() {
        isLoadingActivities = false;
    }

    private static class SubmitCommentTask extends AsyncTask<Void, Void, Boolean> {

        private final String message;
        private final long fileId;
        private final VersionListInterface.CommentCallback callback;
        private final NextcloudClient client;

        private SubmitCommentTask(String message,
                                  long fileId,
                                  VersionListInterface.CommentCallback callback,
                                  NextcloudClient client) {
            this.message = message;
            this.fileId = fileId;
            this.callback = callback;
            this.client = client;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            CommentFileOperation commentFileOperation = new CommentFileOperation(message, fileId);

            RemoteOperationResult<Void> result = commentFileOperation.execute(client);

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
