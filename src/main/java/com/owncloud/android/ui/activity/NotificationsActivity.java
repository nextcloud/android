/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.jobs.NotificationJob;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
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
import com.owncloud.android.utils.ThemeUtils;

import java.io.IOException;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Activity displaying all server side stored notification items.
 */
public class NotificationsActivity extends FileActivity implements NotificationsContract.View {

    private static final String TAG = NotificationsActivity.class.getSimpleName();

    @BindView(R.id.empty_list_view)
    public LinearLayout emptyContentContainer;

    public SwipeRefreshLayout swipeListRefreshLayout;

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

    @BindString(R.string.notifications_no_results_headline)
    public String noResultsHeadline;

    @BindString(R.string.notifications_no_results_message)
    public String noResultsMessage;

    private Unbinder unbinder;

    private NotificationListAdapter adapter;
    private Snackbar snackbar;
    private OwnCloudClient client;
    private Account currentAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notifications_layout);
        unbinder = ButterKnife.bind(this);

        currentAccount = getAccount();

        // use account from intent (opened via android notification can have a different account than current one)
        if (getIntent() != null && getIntent().getExtras() != null) {
            String account = getIntent().getExtras().getString(NotificationJob.KEY_NOTIFICATION_ACCOUNT);

            if (account != null && (currentAccount == null || !account.equalsIgnoreCase(currentAccount.name))) {
                accountManager.setCurrentOwnCloudAccount(account);
                setAccount(getUserAccountManager().getCurrentAccount());
                currentAccount = getAccount();
            }
        }

        // setup toolbar
        setupToolbar();

        swipeEmptyListRefreshLayout = findViewById(R.id.swipe_containing_empty);
        swipeListRefreshLayout = findViewById(R.id.swipe_containing_list);

        // setup drawer
        setupDrawer(R.id.nav_notifications);
        ThemeUtils.setColoredTitle(getSupportActionBar(), getString(R.string.drawer_item_notifications), this);

        if (currentAccount == null) {
            // show error
            runOnUiThread(() -> setEmptyContent(noResultsHeadline, getString(R.string.account_not_found)));
            return;
        }

        swipeListRefreshLayout.setOnRefreshListener(() -> {
            setLoadingMessage();
            fetchAndSetData();
        });

        swipeEmptyListRefreshLayout.setOnRefreshListener(() -> {
            setLoadingMessage();
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
                snackbar = Snackbar.make(emptyContentContainer, R.string.push_notifications_not_implemented,
                        Snackbar.LENGTH_INDEFINITE);
            } else {
                ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());

                boolean usesOldLogin = arbitraryDataProvider.getBooleanValue(currentAccount.name,
                                                                     UserAccountManager.ACCOUNT_USES_STANDARD_PASSWORD);

                if (usesOldLogin) {
                    snackbar = Snackbar.make(emptyContentContainer, R.string.push_notifications_old_login,
                            Snackbar.LENGTH_INDEFINITE);
                } else {
                    String pushValue = arbitraryDataProvider.getValue(currentAccount.name, PushUtils.KEY_PUSH);

                    if (pushValue == null || pushValue.isEmpty()) {
                        snackbar = Snackbar.make(emptyContentContainer, R.string.push_notifications_temp_error,
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
     * sets up the UI elements and loads all notification items.
     */
    private void setupContent() {
        emptyContentIcon.setImageResource(R.drawable.ic_notification_light_grey);
        emptyContentProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryAccentColor(this),
                PorterDuff.Mode.SRC_IN);
        setLoadingMessage();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);

        fetchAndSetData();
    }

    private void populateList(List<Notification> notifications) {
        adapter.setNotificationItems(notifications);
    }

    private void fetchAndSetData() {
        Thread t = new Thread(() -> {
            if (client == null) {
                try {
                    OwnCloudAccount ocAccount = new OwnCloudAccount(currentAccount, this);
                    client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, this);
                    client.setOwnCloudVersion(accountManager.getServerVersion(currentAccount));
                } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException |
                    IOException | OperationCanceledException | AuthenticatorException e) {
                    Log_OC.e(TAG, "Error initializing client", e);
                }
            }

            if (adapter == null) {
                adapter = new NotificationListAdapter(client, this);
                recyclerView.setAdapter(adapter);
            }

            RemoteOperation getRemoteNotificationOperation = new GetNotificationsRemoteOperation();
            final RemoteOperationResult result = getRemoteNotificationOperation.execute(client);

            if (result.isSuccess() && result.getNotificationData() != null) {
                final List<Notification> notifications = result.getNotificationData();

                runOnUiThread(() -> {
                    populateList(notifications);
                    if (notifications.size() > 0) {
                        swipeEmptyListRefreshLayout.setVisibility(View.GONE);
                        swipeListRefreshLayout.setVisibility(View.VISIBLE);
                    } else {
                        setEmptyContent(noResultsHeadline, noResultsMessage);
                        swipeListRefreshLayout.setVisibility(View.GONE);
                        swipeEmptyListRefreshLayout.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                Log_OC.d(TAG, result.getLogMessage());
                // show error
                runOnUiThread(() -> setEmptyContent(noResultsHeadline, result.getLogMessage()));
            }

            hideRefreshLayoutLoader();
        });

        t.start();
    }

    private void hideRefreshLayoutLoader() {
        runOnUiThread(() -> {
            swipeListRefreshLayout.setRefreshing(false);
            swipeEmptyListRefreshLayout.setRefreshing(false);
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notifications_actions_menu, menu);

        return true;
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

            case R.id.action_empty_notifications:
                new DeleteAllNotificationsTask(client, this).execute();
                break;

            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }

        return retval;
    }

    private void setLoadingMessage() {
        emptyContentHeadline.setText(R.string.notifications_loading_activity);
        emptyContentMessage.setText("");

        emptyContentIcon.setVisibility(View.GONE);
        emptyContentProgressBar.setVisibility(View.VISIBLE);
    }

    private void setEmptyContent(String headline, String message) {
        if (emptyContentContainer != null && emptyContentMessage != null) {
            emptyContentHeadline.setText(headline);
            emptyContentMessage.setText(message);
            emptyContentMessage.setVisibility(View.VISIBLE);

            emptyContentProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setImageResource(R.drawable.ic_notification_light_grey);
            emptyContentIcon.setVisibility(View.VISIBLE);
        }
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
            setEmptyContent(noResultsHeadline, noResultsMessage);
            swipeListRefreshLayout.setVisibility(View.GONE);
            swipeEmptyListRefreshLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRemovedAllNotifications(boolean isSuccess) {
        if (isSuccess) {
            adapter.removeAllNotifications();
            setEmptyContent(noResultsHeadline, noResultsMessage);
            swipeListRefreshLayout.setVisibility(View.GONE);
            swipeEmptyListRefreshLayout.setVisibility(View.VISIBLE);
        } else {
            DisplayUtils.showSnackMessage(this, getString(R.string.clear_notifications_failed));
        }
    }

    @Override
    public void onActionCallback(boolean isSuccess, NotificationListAdapter.NotificationViewHolder holder) {
        if (isSuccess) {
            adapter.removeNotification(holder);
        } else {
            adapter.setButtonEnabled(holder, true);
            DisplayUtils.showSnackMessage(this, getString(R.string.notification_action_failed));
        }
    }
}
