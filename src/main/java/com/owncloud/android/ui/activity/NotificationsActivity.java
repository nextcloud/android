/**
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
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.GetRemoteNotificationsOperation;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.ui.adapter.NotificationListAdapter;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.IOException;
import java.util.List;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Activity displaying all server side stored activity items.
 */
public class NotificationsActivity extends FileActivity {

    private static final String TAG = NotificationsActivity.class.getSimpleName();
    private static final String SCREEN_NAME = "Notifications";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notifications_layout);
        unbinder = ButterKnife.bind(this);

        // setup toolbar
        setupToolbar();

        swipeEmptyListRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_containing_empty);
        swipeListRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_containing_list);

        // setup drawer
        setupDrawer(R.id.nav_notifications);
        ThemeUtils.setColoredTitle(getSupportActionBar(), getString(R.string.drawer_item_notifications));

        swipeListRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setLoadingMessage();
                fetchAndSetData();
            }
        });

        swipeEmptyListRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                setLoadingMessage();
                fetchAndSetData();

            }
        });

        Context context = getApplicationContext();
        String pushUrl = context.getResources().getString(R.string.push_server_url);

        if (pushUrl.isEmpty()) {
            Snackbar.make(emptyContentContainer, R.string.push_notifications_not_implemented,
                    Snackbar.LENGTH_INDEFINITE).show();
        } else {
            Account account = AccountUtils.getCurrentOwnCloudAccount(context);
            ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());

            boolean usesOldLogin = arbitraryDataProvider.getBooleanValue(account.name,
                    AccountUtils.ACCOUNT_USES_STANDARD_PASSWORD);

            if (usesOldLogin) {
                Snackbar.make(emptyContentContainer, R.string.push_notifications_old_login,
                        Snackbar.LENGTH_INDEFINITE).show();
            } else {
                String pushValue = arbitraryDataProvider.getValue(account.name, PushUtils.KEY_PUSH);

                if (pushValue == null || pushValue.isEmpty()) {
                    Snackbar.make(emptyContentContainer, R.string.push_notifications_temp_error,
                            Snackbar.LENGTH_INDEFINITE).show();
                }
            }
        }

        setupContent();
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
        emptyContentIcon.setImageResource(R.drawable.ic_notification_light_grey);
        emptyContentProgressBar.getIndeterminateDrawable().setColorFilter(ThemeUtils.primaryAccentColor(),
                PorterDuff.Mode.SRC_IN);
        setLoadingMessage();

        adapter = new NotificationListAdapter(this);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                recyclerView.getContext(),
                layoutManager.getOrientation()
        );

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(dividerItemDecoration);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);

        if (getResources().getBoolean(R.bool.bottom_toolbar_enabled)) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            DisplayUtils.setupBottomBar(bottomNavigationView, getResources(), this, -1);
        }

        fetchAndSetData();
    }

    private void populateList(List<Notification> notifications) {
        adapter.setNotificationItems(notifications);
    }

    private void fetchAndSetData() {
        final Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(MainApp.getAppContext());
        final Context context = MainApp.getAppContext();

        Thread t = new Thread(new Runnable() {
            public void run() {
                OwnCloudAccount ocAccount;
                try {
                    ocAccount = new OwnCloudAccount(currentAccount, context);
                    OwnCloudClient mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                            getClientFor(ocAccount, MainApp.getAppContext());
                    mClient.setOwnCloudVersion(AccountUtils.getServerVersion(currentAccount));

                    RemoteOperation getRemoteNotificationOperation = new GetRemoteNotificationsOperation();
                    final RemoteOperationResult result = getRemoteNotificationOperation.execute(mClient);

                    if (result.isSuccess() && result.getNotificationData() != null) {
                        final List<Notification> notifications = result.getNotificationData();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                populateList(notifications);
                                if (notifications.size() > 0) {
                                    swipeEmptyListRefreshLayout.setVisibility(View.GONE);
                                    swipeListRefreshLayout.setVisibility(View.VISIBLE);
                                } else {
                                    setEmptyContent(noResultsHeadline, noResultsMessage);
                                    swipeListRefreshLayout.setVisibility(View.GONE);
                                    swipeEmptyListRefreshLayout.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    } else {
                        Log_OC.d(TAG, result.getLogMessage());
                        // show error
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setEmptyContent(noResultsHeadline, result.getLogMessage());
                            }
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
        });

        t.start();

    }

    private void hideRefreshLayoutLoader() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeListRefreshLayout.setRefreshing(false);
                swipeEmptyListRefreshLayout.setRefreshing(false);
            }
        });
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
    public void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

}
