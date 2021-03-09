/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.jobs.NotificationWork;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.databinding.NotificationsLayoutBinding;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.GetNotificationsRemoteOperation;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.ui.adapter.NotificationListAdapter;
import com.owncloud.android.ui.asynctasks.DeleteAllNotificationsTask;
import com.owncloud.android.ui.notifications.NotificationsContract;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.theme.ThemeLayoutUtils;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * Activity displaying all server side stored notification items.
 */
public class NotificationsActivity extends DrawerActivity implements NotificationsContract.View {

    private static final String TAG = NotificationsActivity.class.getSimpleName();

    private NotificationsLayoutBinding binding;
    private NotificationListAdapter adapter;
    private Snackbar snackbar;
    private OwnCloudClient client;
    private Optional<User> optionalUser;

    @Inject ClientFactory clientFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        binding = NotificationsLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        optionalUser = getUser();

        // use account from intent (opened via android notification can have a different account than current one)
        if (getIntent() != null && getIntent().getExtras() != null) {
            String accountName = getIntent().getExtras().getString(NotificationWork.KEY_NOTIFICATION_ACCOUNT);
            if(accountName != null && optionalUser.isPresent()) {
                User user = optionalUser.get();
                if (user.getAccountName().equalsIgnoreCase(accountName)) {
                    accountManager.setCurrentOwnCloudAccount(accountName);
                    setUser(getUserAccountManager().getUser());
                    optionalUser = getUser();
                }
            }
        }

        // setup toolbar
        setupToolbar();

        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_notifications));

        ThemeLayoutUtils.colorSwipeRefreshLayout(this, binding.swipeContainingList);
        ThemeLayoutUtils.colorSwipeRefreshLayout(this, binding.swipeContainingEmpty);

        // setup drawer
        setupDrawer(R.id.nav_notifications);

        if (!optionalUser.isPresent()) {
            // show error
            runOnUiThread(() -> setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.account_not_found))
                         );
            return;
        }

        binding.swipeContainingList.setOnRefreshListener(() -> {
            setLoadingMessage();
            binding.swipeContainingList.setRefreshing(true);
            fetchAndSetData();
        });

        binding.swipeContainingEmpty.setOnRefreshListener(() -> {
            setLoadingMessageEmpty();
            fetchAndSetData();
        });

        setupPushWarning();
        setupContent();
    }

    private void setupPushWarning() {
        if (snackbar != null) {
            if (!snackbar.isShown()) {
                snackbar.show();
            }
        } else {
            String pushUrl = getResources().getString(R.string.push_server_url);

            if (pushUrl.isEmpty()) {
                snackbar = Snackbar.make(binding.emptyList.emptyListView,
                                         R.string.push_notifications_not_implemented,
                                         Snackbar.LENGTH_INDEFINITE);
            } else {
                final ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());
                final String accountName = optionalUser.isPresent() ? optionalUser.get().getAccountName() : "";
                final boolean usesOldLogin = arbitraryDataProvider.getBooleanValue(accountName,
                                                                     UserAccountManager.ACCOUNT_USES_STANDARD_PASSWORD);

                if (usesOldLogin) {
                    snackbar = Snackbar.make(binding.emptyList.emptyListView,
                                             R.string.push_notifications_old_login,
                                             Snackbar.LENGTH_INDEFINITE);
                } else {
                    String pushValue = arbitraryDataProvider.getValue(accountName, PushUtils.KEY_PUSH);

                    if (pushValue == null || pushValue.isEmpty()) {
                        snackbar = Snackbar.make(binding.emptyList.emptyListView,
                                                 R.string.push_notifications_temp_error,
                                                 Snackbar.LENGTH_INDEFINITE);
                    }
                }
            }

            if (snackbar != null && !snackbar.isShown()) {
                snackbar.show();
            }
        }
    }

    @Override
    public void openDrawer() {
        super.openDrawer();

        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
    }

    @Override
    public void closeDrawer() {
        super.closeDrawer();

        setupPushWarning();
    }

    /**
     * sets up the UI elements and loads all notification items.
     */
    private void setupContent() {
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_notification);
        setLoadingMessageEmpty();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        binding.list.setLayoutManager(layoutManager);

        fetchAndSetData();
    }

    @VisibleForTesting
    public void populateList(List<Notification> notifications) {
        initializeAdapter();
        adapter.setNotificationItems(notifications);
        binding.loadingContent.setVisibility(View.GONE);

        if (notifications.size() > 0) {
            binding.swipeContainingEmpty.setVisibility(View.GONE);
            binding.swipeContainingList.setVisibility(View.VISIBLE);
        } else {
            setEmptyContent(
                getString(R.string.notifications_no_results_headline),
                getString(R.string.notifications_no_results_message)
                           );
            binding.swipeContainingList.setVisibility(View.GONE);
            binding.swipeContainingEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void fetchAndSetData() {
        Thread t = new Thread(() -> {
            initializeAdapter();

            RemoteOperation getRemoteNotificationOperation = new GetNotificationsRemoteOperation();
            final RemoteOperationResult result = getRemoteNotificationOperation.execute(client);

            if (result.isSuccess() && result.getNotificationData() != null) {
                final List<Notification> notifications = result.getNotificationData();

                runOnUiThread(() -> populateList(notifications));
            } else {
                Log_OC.d(TAG, result.getLogMessage());
                // show error
                runOnUiThread(() -> setEmptyContent(getString(R.string.notifications_no_results_headline), result.getLogMessage()));
            }

            hideRefreshLayoutLoader();
        });

        t.start();
    }

    private void initializeClient() {
        if (client == null && optionalUser.isPresent()) {
            try {
                User user = optionalUser.get();
                client = clientFactory.create(user);
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(TAG, "Error initializing client", e);
            }
        }
    }

    private void initializeAdapter() {
        initializeClient();
        if (adapter == null) {
            adapter = new NotificationListAdapter(client, this);
            binding.list.setAdapter(adapter);
        }
    }

    private void hideRefreshLayoutLoader() {
        runOnUiThread(() -> {
            binding.swipeContainingList.setRefreshing(false);
            binding.swipeContainingEmpty.setRefreshing(false);
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_notifications, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;

        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            if (isDrawerOpen()) {
                closeDrawer();
            } else {
                openDrawer();
            }
        } else if (itemId == R.id.action_empty_notifications) {
            new DeleteAllNotificationsTask(client, this).execute();
        } else {
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
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

    @VisibleForTesting
    public void setEmptyContent(String headline, String message) {
        binding.swipeContainingList.setVisibility(View.GONE);
        binding.loadingContent.setVisibility(View.GONE);
        binding.swipeContainingEmpty.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListView.setVisibility(View.VISIBLE);

        binding.emptyList.emptyListViewHeadline.setText(headline);
        binding.emptyList.emptyListViewText.setText(message);
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_notification);

        binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDrawerMenuItemChecked(R.id.nav_notifications);
    }

    @Override
    public void onRemovedNotification(boolean isSuccess) {
        if (!isSuccess) {
            DisplayUtils.showSnackMessage(this, getString(R.string.remove_notification_failed));
            fetchAndSetData();
        }
    }

    @Override
    public void removeNotification(NotificationListAdapter.NotificationViewHolder holder) {
        adapter.removeNotification(holder);

        if (adapter.getItemCount() == 0) {
            setEmptyContent(getString(R.string.notifications_no_results_headline), getString(R.string.notifications_no_results_message));
            binding.swipeContainingList.setVisibility(View.GONE);
            binding.loadingContent.setVisibility(View.GONE);
            binding.swipeContainingEmpty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRemovedAllNotifications(boolean isSuccess) {
        if (isSuccess) {
            adapter.removeAllNotifications();
            setEmptyContent(getString(R.string.notifications_no_results_headline), getString(R.string.notifications_no_results_message));
            binding.loadingContent.setVisibility(View.GONE);
            binding.swipeContainingList.setVisibility(View.GONE);
            binding.swipeContainingEmpty.setVisibility(View.VISIBLE);
        } else {
            DisplayUtils.showSnackMessage(this, getString(R.string.clear_notifications_failed));
        }
    }

    @Override
    public void onActionCallback(boolean isSuccess,
                                 Notification notification,
                                 NotificationListAdapter.NotificationViewHolder holder) {
        if (isSuccess) {
            adapter.removeNotification(holder);
        } else {
            adapter.setButtons(holder, notification);
            DisplayUtils.showSnackMessage(this, getString(R.string.notification_action_failed));
        }
    }
}
