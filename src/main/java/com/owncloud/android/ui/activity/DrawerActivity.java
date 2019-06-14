/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2016 Nextcloud
 * Copyright (C) 2016 ownCloud Inc.
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

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.material.navigation.NavigationView;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ExternalLinksProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.ExternalLink;
import com.owncloud.android.lib.common.ExternalLinkType;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.Quota;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.ExternalLinksOperation;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.status.CapabilityBooleanType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.operations.GetCapabilitiesOperation;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.ui.activities.ActivitiesActivity;
import com.owncloud.android.ui.events.AccountRemovedEvent;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.DummyDrawerEvent;
import com.owncloud.android.ui.events.MenuItemClickEvent;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.trashbin.TrashbinActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.DrawerMenuUtil;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.ThemeUtils;
import com.owncloud.android.utils.svg.MenuSimpleTarget;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * Base class to handle setup of the drawer implementation including user switching and avatar fetching and fallback
 * generation.
 */
public abstract class DrawerActivity extends ToolbarActivity
    implements DisplayUtils.AvatarGenerationListener, Injectable {

    private static final String TAG = DrawerActivity.class.getSimpleName();
    private static final String KEY_IS_ACCOUNT_CHOOSER_ACTIVE = "IS_ACCOUNT_CHOOSER_ACTIVE";
    private static final String KEY_CHECKED_MENU_ITEM = "CHECKED_MENU_ITEM";
    private static final int ACTION_MANAGE_ACCOUNTS = 101;
    private static final int MENU_ORDER_ACCOUNT = 1;
    private static final int MENU_ORDER_ACCOUNT_FUNCTION = 2;
    private static final int MENU_ORDER_EXTERNAL_LINKS = 3;
    private static final int MENU_ITEM_EXTERNAL_LINK = 111;
    /**
     * menu account avatar radius.
     */
    private float mMenuAccountAvatarRadiusDimension;

    /**
     * current account avatar radius.
     */
    private float mCurrentAccountAvatarRadiusDimension;

    /**
     * other accounts avatar radius.
     */
    private float mOtherAccountAvatarRadiusDimension;

    /**
     * Reference to the drawer layout.
     */
    protected DrawerLayout mDrawerLayout;

    /**
     * Reference to the drawer toggle.
     */
    protected ActionBarDrawerToggle mDrawerToggle;

    /**
     * Reference to the navigation view.
     */
    private NavigationView mNavigationView;

    /**
     * Reference to the account chooser toggle.
     */
    private ImageView mAccountChooserToggle;

    /**
     * Reference to the middle account avatar.
     */
    private ImageView mAccountMiddleAccountAvatar;

    /**
     * Reference to the end account avatar.
     */
    private ImageView mAccountEndAccountAvatar;

    /**
     * Flag to signal if the account chooser is active.
     */
    private boolean mIsAccountChooserActive;

    /**
     * Id of the checked menu item.
     */
    private int mCheckedMenuItem = Menu.NONE;

    /**
     * accounts for the (max) three displayed accounts in the drawer header.
     */
    private Account[] mAvatars = new Account[3];

    /**
     * container layout of the quota view.
     */
    private LinearLayout mQuotaView;

    /**
     * progress bar of the quota view.
     */
    private ProgressBar mQuotaProgressBar;

    /**
     * text view of the quota view.
     */
    private TextView mQuotaTextPercentage;
    private TextView mQuotaTextLink;

    /**
     * runnable that will be executed after the drawer has been closed.
     */
    private Runnable pendingRunnable;

    private ExternalLinksProvider externalLinksProvider;
    private ArbitraryDataProvider arbitraryDataProvider;

    @Inject
    AppPreferences preferences;

    /**
     * Initializes the drawer, its content and highlights the menu item with the given id.
     * This method needs to be called after the content view has been set.
     *
     * @param menuItemId the menu item to be checked/highlighted
     */
    protected void setupDrawer(int menuItemId) {
        setupDrawer();
        setDrawerMenuItemChecked(menuItemId);
    }

    /**
     * Initializes the drawer and its content.
     * This method needs to be called after the content view has been set.
     */
    protected void setupDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout);

        mNavigationView = findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            setupDrawerHeader();

            setupDrawerMenu(mNavigationView);

            setupQuotaElement();
        }

        setupDrawerToggle();

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * initializes and sets up the drawer toggle.
     */
    private void setupDrawerToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                // standard behavior of drawer is to switch to the standard menu on closing
                if (mIsAccountChooserActive) {
                    toggleAccountList();
                }
                supportInvalidateOptionsMenu();
                mDrawerToggle.setDrawerIndicatorEnabled(isDrawerIndicatorAvailable());

                if (pendingRunnable != null) {
                    new Handler().post(pendingRunnable);
                    pendingRunnable = null;
                }

                closeDrawer();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                supportInvalidateOptionsMenu();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerToggle.getDrawerArrowDrawable().setColor(ThemeUtils.fontColor(this));
    }

    /**
     * initializes and sets up the drawer header.
     */
    private void setupDrawerHeader() {
        mIsAccountChooserActive = false;
        mAccountMiddleAccountAvatar = (ImageView) findNavigationViewChildById(R.id.drawer_account_middle);
        mAccountEndAccountAvatar = (ImageView) findNavigationViewChildById(R.id.drawer_account_end);

        mAccountChooserToggle = (ImageView) findNavigationViewChildById(R.id.drawer_account_chooser_toggle);
        mAccountChooserToggle.setColorFilter(ThemeUtils.fontColor(this));

        if (getResources().getBoolean(R.bool.allow_profile_click)) {
            mAccountChooserToggle.setImageResource(R.drawable.ic_down);

            findNavigationViewChildById(R.id.drawer_active_user)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            toggleAccountList();
                        }
                    });
        } else {
            mAccountChooserToggle.setVisibility(View.GONE);
        }
    }

    /**
     * setup quota elements of the drawer.
     */
    private void setupQuotaElement() {
        mQuotaView = (LinearLayout) findQuotaViewById(R.id.drawer_quota);
        mQuotaProgressBar = (ProgressBar) findQuotaViewById(R.id.drawer_quota_ProgressBar);
        mQuotaTextPercentage = (TextView) findQuotaViewById(R.id.drawer_quota_percentage);
        mQuotaTextLink = (TextView) findQuotaViewById(R.id.drawer_quota_link);
        ThemeUtils.colorProgressBar(mQuotaProgressBar, ThemeUtils.primaryColor(this));
    }

    /**
     * setup drawer content, basically setting the item selected listener.
     *
     * @param navigationView the drawers navigation view
     */
    protected void setupDrawerMenu(NavigationView navigationView) {
        navigationView.setItemIconTintList(null);

        // setup actions for drawer menu items
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
                        mDrawerLayout.closeDrawers();
                        // pending runnable will be executed after the drawer has been closed
                        pendingRunnable = new Runnable() {
                            @Override
                            public void run() {
                                selectNavigationItem(menuItem);
                            }
                        };
                        return true;
                    }
                });

        // handle correct state
        if (mIsAccountChooserActive) {
            navigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, true);
        } else {
            navigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
        }

        Account account = accountManager.getCurrentAccount();
        filterDrawerMenu(navigationView.getMenu(), account);
    }

    private void filterDrawerMenu(Menu menu, Account account) {
        OCCapability capability = null;
        if (account != null) {
            FileDataStorageManager storageManager = new FileDataStorageManager(account, getContentResolver());
            capability = storageManager.getCapability(account.name);
        }

        boolean hasSearchSupport = accountManager.getServerVersion(account).isSearchSupported();

        DrawerMenuUtil.filterForBottomToolbarMenuItems(menu, getResources());
        DrawerMenuUtil.filterSearchMenuItems(menu, account, getResources(), hasSearchSupport);
        DrawerMenuUtil.filterTrashbinMenuItem(menu, account, capability, accountManager);
        DrawerMenuUtil.filterActivityMenuItem(menu, capability);

        DrawerMenuUtil.setupHomeMenuItem(menu, getResources());

        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_participate,
                !getResources().getBoolean(R.bool.participate_enabled));
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_shared, !getResources().getBoolean(R.bool.shared_enabled));
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_contacts, !getResources().getBoolean(R.bool.contacts_backup)
                || !getResources().getBoolean(R.bool.show_drawer_contacts_backup));

        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_synced_folders,
                getResources().getBoolean(R.bool.syncedFolder_light));
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_logout, !getResources().getBoolean(R.bool.show_drawer_logout));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MenuItemClickEvent event) {
        unsetAllDrawerMenuItems();

        switch (event.menuItem.getItemId()) {
            case R.id.nav_bar_files:
                showFiles(false);
                break;
            case R.id.nav_bar_settings:
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DummyDrawerEvent event) {
        unsetAllDrawerMenuItems();
    }


    private void selectNavigationItem(final MenuItem menuItem) {

        setDrawerMenuItemChecked(menuItem.getItemId());

        if (menuItem.getGroupId() == R.id.drawer_menu_accounts) {
            switch (menuItem.getItemId()) {
                case R.id.drawer_menu_account_add:
                    boolean isProviderOrOwnInstallationVisible = getResources()
                        .getBoolean(R.bool.show_provider_or_own_installation);

                    if (isProviderOrOwnInstallationVisible) {
                        Intent firstRunIntent = new Intent(getApplicationContext(), FirstRunActivity.class);
                        firstRunIntent.putExtra(FirstRunActivity.EXTRA_ALLOW_CLOSE, true);
                        startActivity(firstRunIntent);
                    } else {
                        createAccount(false);
                    }
                    break;

                case R.id.drawer_menu_account_manage:
                    Intent manageAccountsIntent = new Intent(getApplicationContext(), ManageAccountsActivity.class);
                    startActivityForResult(manageAccountsIntent, ACTION_MANAGE_ACCOUNTS);
                    break;

                default:
                    accountClicked(menuItem.getItemId());
                    break;
            }

            return;
        }

        switch (menuItem.getItemId()) {
            case R.id.nav_all_files:
                showFiles(false);
                EventBus.getDefault().post(new ChangeMenuEvent());
                break;
            case R.id.nav_favorites:
                handleSearchEvents(new SearchEvent("", SearchRemoteOperation.SearchType.FAVORITE_SEARCH,
                        SearchEvent.UnsetType.NO_UNSET), menuItem.getItemId());
                break;
            case R.id.nav_photos:
                handleSearchEvents(new SearchEvent("image/%", SearchRemoteOperation.SearchType.CONTENT_TYPE_SEARCH,
                        SearchEvent.UnsetType.NO_UNSET), menuItem.getItemId());
                break;
            case R.id.nav_on_device:
                EventBus.getDefault().post(new ChangeMenuEvent());
                showFiles(true);
                break;
            case R.id.nav_uploads:
                Intent uploadListIntent = new Intent(getApplicationContext(), UploadListActivity.class);
                uploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(uploadListIntent);
                break;
            case R.id.nav_trashbin:
                Intent trashbinIntent = new Intent(getApplicationContext(), TrashbinActivity.class);
                trashbinIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(trashbinIntent);
                break;
            case R.id.nav_activity:
                Intent activityIntent = new Intent(getApplicationContext(), ActivitiesActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(activityIntent);
                break;
            case R.id.nav_notifications:
                Intent notificationsIntent = new Intent(getApplicationContext(), NotificationsActivity.class);
                startActivity(notificationsIntent);
                break;
            case R.id.nav_synced_folders:
                Intent syncedFoldersIntent = new Intent(getApplicationContext(), SyncedFoldersActivity.class);
                startActivity(syncedFoldersIntent);
                break;
            case R.id.nav_contacts:
                Intent contactsIntent = new Intent(getApplicationContext(), ContactsPreferenceActivity.class);
                startActivity(contactsIntent);
                break;
            case R.id.nav_settings:
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            case R.id.nav_participate:
                Intent participateIntent = new Intent(getApplicationContext(), ParticipateActivity.class);
                startActivity(participateIntent);
                break;
            case R.id.nav_logout:
                mCheckedMenuItem = -1;
                menuItem.setChecked(false);
                UserInfoActivity.openAccountRemovalConfirmationDialog(getAccount(), getSupportFragmentManager(), true);
                break;
            case R.id.nav_recently_added:
                handleSearchEvents(new SearchEvent("%", SearchRemoteOperation.SearchType.CONTENT_TYPE_SEARCH,
                        SearchEvent.UnsetType.UNSET_BOTTOM_NAV_BAR), menuItem.getItemId());
                break;
            case R.id.nav_recently_modified:
                handleSearchEvents(new SearchEvent("", SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH,
                        SearchEvent.UnsetType.UNSET_BOTTOM_NAV_BAR), menuItem.getItemId());
                break;
            case R.id.nav_shared:
                handleSearchEvents(new SearchEvent("", SearchRemoteOperation.SearchType.SHARED_SEARCH,
                        SearchEvent.UnsetType.UNSET_BOTTOM_NAV_BAR), menuItem.getItemId());
                break;
            case R.id.nav_videos:
                handleSearchEvents(new SearchEvent("video/%", SearchRemoteOperation.SearchType.CONTENT_TYPE_SEARCH,
                        SearchEvent.UnsetType.UNSET_BOTTOM_NAV_BAR), menuItem.getItemId());
                break;
            default:
                if (menuItem.getItemId() >= MENU_ITEM_EXTERNAL_LINK &&
                    menuItem.getItemId() <= MENU_ITEM_EXTERNAL_LINK + 100) {
                    // external link clicked
                    externalLinkClicked(menuItem);
                } else {
                    Log_OC.i(TAG, "Unknown drawer menu item clicked: " + menuItem.getTitle());
                }
                break;
        }
    }

    private void handleSearchEvents(SearchEvent searchEvent, int menuItemId) {
        if (this instanceof FileDisplayActivity) {
            EventBus.getDefault().post(searchEvent);
        } else {
            Intent intent = new Intent(getApplicationContext(), FileDisplayActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(Intent.ACTION_SEARCH);
            intent.putExtra(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent));
            intent.putExtra(FileDisplayActivity.DRAWER_MENU_ID, menuItemId);
            startActivity(intent);
        }
    }

    /**
     * show the file list to the user.
     *
     * @param onDeviceOnly flag to decide if all files or only the ones on the device should be shown
     */
    public abstract void showFiles(boolean onDeviceOnly);


    /**
     * sets the new/current account and restarts. In case the given account equals the actual/current account the
     * call will be ignored.
     *
     * @param hashCode HashCode of account to be set
     */
    private void accountClicked(int hashCode) {
        final Account currentAccount = accountManager.getCurrentAccount();
        if (currentAccount != null && currentAccount.hashCode() != hashCode &&
            accountManager.setCurrentOwnCloudAccount(hashCode)) {
            fetchExternalLinks(true);
            restart();
        }
    }

    private void externalLinkClicked(MenuItem menuItem){
        for (ExternalLink link : externalLinksProvider.getExternalLink(ExternalLinkType.LINK)) {
            if (menuItem.getTitle().toString().equalsIgnoreCase(link.name)) {
                if (link.redirect) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.url));
                    DisplayUtils.startIntentIfAppAvailable(intent, this, R.string.no_browser_available);
                } else {
                    Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, link.name);
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, link.url);
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, true);
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, menuItem.getItemId());
                    startActivity(externalWebViewIntent);
                }
            }
        }
    }

    /**
     * click method for mini avatars in drawer header.
     *
     * @param view the clicked ImageView
     */
    public void onAccountDrawerClick(View view) {
        accountClicked(Integer.parseInt(view.getContentDescription().toString()));
    }

    /**
     * checks if the drawer exists and is opened.
     *
     * @return <code>true</code> if the drawer is open, else <code>false</code>
     */
    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    /**
     * closes the drawer.
     */
    public void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /**
     * opens the drawer.
     */
    public void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);

            updateExternalLinksInDrawer();
            updateQuotaLink();
        }
    }

    /**
     * Enable or disable interaction with all drawers.
     *
     * @param lockMode The new lock mode for the given drawer. One of {@link DrawerLayout#LOCK_MODE_UNLOCKED},
     *                 {@link DrawerLayout#LOCK_MODE_LOCKED_CLOSED} or {@link DrawerLayout#LOCK_MODE_LOCKED_OPEN}.
     */
    public void setDrawerLockMode(int lockMode) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerLockMode(lockMode);
        }
    }

    /**
     * Enable or disable the drawer indicator.
     *
     * @param enable true to enable, false to disable
     */
    public void setDrawerIndicatorEnabled(boolean enable) {
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(enable);
        }
    }

    /**
     * updates the account list in the drawer.
     */
    public void updateAccountList() {
        Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.getAccountType(this));

        ArrayList<Account> persistingAccounts = new ArrayList<>();

        for (Account acc: accounts) {
            boolean pendingForRemoval = arbitraryDataProvider.getBooleanValue(acc,
                    ManageAccountsActivity.PENDING_FOR_REMOVAL);

            if (!pendingForRemoval) {
                persistingAccounts.add(acc);
            }
        }

        if (mNavigationView != null && mDrawerLayout != null) {
            if (persistingAccounts.size() > 0) {
                repopulateAccountList(persistingAccounts);
                setAccountInDrawer(accountManager.getCurrentAccount());
                populateDrawerOwnCloudAccounts();

                // activate second/end account avatar
                if (mAvatars[1] != null) {
                    View accountEndView = findNavigationViewChildById(R.id.drawer_account_end);
                    accountEndView.setTag(mAvatars[1].name);

                    DisplayUtils.setAvatar(mAvatars[1], this, mOtherAccountAvatarRadiusDimension, getResources(),
                            accountEndView, this);
                    mAccountEndAccountAvatar.setVisibility(View.VISIBLE);
                } else {
                    mAccountEndAccountAvatar.setVisibility(View.GONE);
                }

                // activate third/middle account avatar
                if (mAvatars[2] != null) {
                    View accountMiddleView = findNavigationViewChildById(R.id.drawer_account_middle);
                    accountMiddleView.setTag(mAvatars[2].name);

                    DisplayUtils.setAvatar(mAvatars[2], this, mOtherAccountAvatarRadiusDimension, getResources(),
                            accountMiddleView, this);
                    mAccountMiddleAccountAvatar.setVisibility(View.VISIBLE);
                } else {
                    mAccountMiddleAccountAvatar.setVisibility(View.GONE);
                }
            } else {
                mAccountEndAccountAvatar.setVisibility(View.GONE);
                mAccountMiddleAccountAvatar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * re-populates the account list.
     *
     * @param accounts list of accounts
     */
    private void repopulateAccountList(List<Account> accounts) {
        // remove all accounts from list
        mNavigationView.getMenu().removeGroup(R.id.drawer_menu_accounts);

        // add all accounts to list
        for (Account account: accounts) {
            try {
                // show all accounts except the currently active one and those pending for removal

                if (!getAccount().name.equals(account.name)) {
                    MenuItem accountMenuItem = mNavigationView.getMenu().add(
                        R.id.drawer_menu_accounts,
                        account.hashCode(),
                        MENU_ORDER_ACCOUNT,
                        DisplayUtils.getAccountNameDisplayText(this, account, account.name, account.name))
                        .setIcon(TextDrawable.createAvatar(account, mMenuAccountAvatarRadiusDimension));
                    DisplayUtils.setAvatar(account, this, mMenuAccountAvatarRadiusDimension, getResources(),
                                           accountMenuItem, this);
                }
            } catch (Exception e) {
                Log_OC.e(TAG, "Error calculating RGB value for account menu item.", e);
                mNavigationView.getMenu().add(
                    R.id.drawer_menu_accounts,
                    account.hashCode(),
                    MENU_ORDER_ACCOUNT,
                    DisplayUtils.getAccountNameDisplayText(this, account, account.name, account.name))
                    .setIcon(R.drawable.ic_user);
            }
        }

        // re-add add-account and manage-accounts
        mNavigationView.getMenu().add(R.id.drawer_menu_accounts, R.id.drawer_menu_account_add,
                MENU_ORDER_ACCOUNT_FUNCTION,
                getResources().getString(R.string.prefs_add_account)).setIcon(R.drawable.ic_account_plus);
        mNavigationView.getMenu().add(R.id.drawer_menu_accounts, R.id.drawer_menu_account_manage,
                MENU_ORDER_ACCOUNT_FUNCTION,
                getResources().getString(R.string.drawer_manage_accounts)).setIcon(R.drawable.nav_settings);

        // adding sets menu group back to visible, so safety check and setting invisible
        showMenu();
    }

    /**
     * Updates title bar and home buttons (state and icon).
     * <p/>
     * Assumes that navigation drawer is NOT visible.
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        super.updateActionBarTitleAndHomeButton(chosenFile);

        // set home button properties
        if (mDrawerToggle != null && chosenFile != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(isRoot(chosenFile));
        } else if (mDrawerToggle != null){
            mDrawerToggle.setDrawerIndicatorEnabled(false);
        }

        if (mDrawerToggle != null) {
            DrawerArrowDrawable icon = mDrawerToggle.getDrawerArrowDrawable();
            icon.setColorFilter(ThemeUtils.fontColor(this), PorterDuff.Mode.SRC_ATOP);
            mDrawerToggle.setDrawerArrowDrawable(icon);
        }
    }

    /**
     * sets the given account name in the drawer in case the drawer is available. The account name is shortened
     * beginning from the @-sign in the username.
     *
     * @param account the account to be set in the drawer
     */
    protected void setAccountInDrawer(Account account) {
        if (mDrawerLayout != null && account != null) {
            TextView username = (TextView) findNavigationViewChildById(R.id.drawer_username);
            TextView usernameFull = (TextView) findNavigationViewChildById(R.id.drawer_username_full);

            usernameFull.setText(DisplayUtils.getAccountNameDisplayText(this, account, account.name, account.name));
            usernameFull.setTextColor(ThemeUtils.fontColor(this));

            try {
                OwnCloudAccount oca = new OwnCloudAccount(account, this);
                username.setText(oca.getDisplayName());
                username.setTextColor(ThemeUtils.fontColor(this));
            } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                Log_OC.w(TAG, "Couldn't read display name of account fallback to account name");
                username.setText(UserAccountManager.getUsername(account));
            }

            View currentAccountView = findNavigationViewChildById(R.id.drawer_current_account);
            currentAccountView.setTag(account.name);

            DisplayUtils.setAvatar(account, this, mCurrentAccountAvatarRadiusDimension, getResources(),
                    currentAccountView, this);

            // check and show quota info if available
            getAndDisplayUserQuota();
        }
    }

    /**
     * Toggle between standard menu and account list including saving the state.
     */
    private void toggleAccountList() {
        mIsAccountChooserActive = !mIsAccountChooserActive;
        showMenu();
    }

    /**
     * depending on the #mIsAccountChooserActive flag shows the account chooser or the standard menu.
     */
    private void showMenu() {
        if (mNavigationView != null) {
            if (mIsAccountChooserActive) {
                if (mAccountChooserToggle != null) {
                    mAccountChooserToggle.setImageResource(R.drawable.ic_up);
                }
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, true);

                if (!getResources().getBoolean(R.bool.multiaccount_support) &&
                        mNavigationView.getMenu().findItem(R.id.drawer_menu_account_add) != null) {
                    mNavigationView.getMenu().removeItem(R.id.drawer_menu_account_add);
                }

                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_standard, false);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_external_links, false);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_bottom, false);
            } else {
                if (mAccountChooserToggle != null) {
                    mAccountChooserToggle.setImageResource(R.drawable.ic_down);
                }
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_standard, true);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_external_links, true);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_bottom, true);
            }
        }
    }

    /**
     * shows or hides the quota UI elements.
     *
     * @param showQuota show/hide quota information
     */
    private void showQuota(boolean showQuota) {
        if (showQuota) {
            mQuotaView.setVisibility(View.VISIBLE);
        } else {
            mQuotaView.setVisibility(View.GONE);
        }
    }

    /**
     * configured the quota to be displayed.
     *  @param usedSpace  the used space
     * @param totalSpace the total space
     * @param relative   the percentage of space already used
     * @param quotaValue {@link GetUserInfoRemoteOperation#SPACE_UNLIMITED} or other to determinate state
     */
    private void setQuotaInformation(long usedSpace, long totalSpace, int relative, long quotaValue) {
        if (GetUserInfoRemoteOperation.SPACE_UNLIMITED == quotaValue) {
            mQuotaTextPercentage.setText(String.format(
                    getString(R.string.drawer_quota_unlimited),
                    DisplayUtils.bytesToHumanReadable(usedSpace)));
        } else {
            mQuotaTextPercentage.setText(String.format(
                    getString(R.string.drawer_quota),
                    DisplayUtils.bytesToHumanReadable(usedSpace),
                    DisplayUtils.bytesToHumanReadable(totalSpace)));
        }

        mQuotaProgressBar.setProgress(relative);

        ThemeUtils.colorProgressBar(mQuotaProgressBar, DisplayUtils.getRelativeInfoColor(this, relative));

        updateQuotaLink();
        showQuota(true);
    }

    protected void unsetAllDrawerMenuItems() {
        if (mNavigationView != null && mNavigationView.getMenu() != null) {
            Menu menu = mNavigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setChecked(false);
            }
        }

        mCheckedMenuItem = Menu.NONE;
    }

    private void updateQuotaLink() {
        if (mQuotaTextLink != null) {
            if (getBaseContext().getResources().getBoolean(R.bool.show_external_links)) {
                List<ExternalLink> quotas = externalLinksProvider.getExternalLink(ExternalLinkType.QUOTA);

                float density = getResources().getDisplayMetrics().density;
                final int size = Math.round(24 * density);

                if (quotas.size() > 0) {
                    final ExternalLink firstQuota = quotas.get(0);
                    mQuotaTextLink.setText(firstQuota.name);
                    mQuotaTextLink.setClickable(true);
                    mQuotaTextLink.setVisibility(View.VISIBLE);
                    mQuotaTextLink.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, firstQuota.name);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, firstQuota.url);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, true);
                            externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, -1);
                            startActivity(externalWebViewIntent);
                        }
                    });


                    SimpleTarget target = new SimpleTarget<Drawable>() {
                        @Override
                        public void onResourceReady(Drawable resource, GlideAnimation glideAnimation) {
                            Drawable test = resource.getCurrent();
                            test.setBounds(0, 0, size, size);
                            mQuotaTextLink.setCompoundDrawablesWithIntrinsicBounds(test, null, null, null);
                        }

                        @Override
                        public void onLoadFailed(Exception e, Drawable errorDrawable) {
                            super.onLoadFailed(e, errorDrawable);

                            Drawable test = errorDrawable.getCurrent();
                            test.setBounds(0, 0, size, size);

                            mQuotaTextLink.setCompoundDrawablesWithIntrinsicBounds(test, null, null, null);
                        }
                    };

                    DisplayUtils.downloadIcon(getUserAccountManager(),
                                              this,
                                              firstQuota.iconUrl,
                                              target,
                                              R.drawable.ic_link,
                                              size,
                                              size);

                } else {
                    mQuotaTextLink.setVisibility(View.GONE);
                }
            } else {
                mQuotaTextLink.setVisibility(View.GONE);
            }
        }
    }

    /**
     * checks/highlights the provided menu item if the drawer has been initialized and the menu item exists.
     *
     * @param menuItemId the menu item to be highlighted
     */
    protected void setDrawerMenuItemChecked(int menuItemId) {
        if (mNavigationView != null && mNavigationView.getMenu() != null &&
                mNavigationView.getMenu().findItem(menuItemId) != null) {

            MenuItem item = mNavigationView.getMenu().findItem(menuItemId);
            item.setChecked(true);

            // reset all tinted icons
            for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
                MenuItem menuItem = mNavigationView.getMenu().getItem(i);
                if (menuItem.getIcon() != null) {
                    menuItem.getIcon().clearColorFilter();
                    menuItem.setTitle(Html.fromHtml("<font color='#000000'>" + menuItem.getTitle() + "</font>"));
                }
            }

            int elementColor = ThemeUtils.elementColor(this);
            ThemeUtils.tintDrawable(item.getIcon(), elementColor);

            String colorHex = ThemeUtils.colorToHexString(elementColor);
            item.setTitle(Html.fromHtml("<font color='" + colorHex + "'>" + item.getTitle() + "</font>"));

            mCheckedMenuItem = menuItemId;
        } else {
            Log_OC.w(TAG, "setDrawerMenuItemChecked has been called with invalid menu-item-ID");
        }
    }

    /**
     * Retrieves and shows the user quota if available
     */
    private void getAndDisplayUserQuota() {
        // set user space information
        Thread t = new Thread(new Runnable() {
            public void run() {
                final Account currentAccount = accountManager.getCurrentAccount();

                if (currentAccount == null) {
                    return;
                }

                final Context context = MainApp.getAppContext();
                RemoteOperationResult result = new GetUserInfoRemoteOperation().execute(currentAccount, context);

                if (result.isSuccess() && result.getData() != null) {
                    final UserInfo userInfo = (UserInfo) result.getData().get(0);
                    final Quota quota = userInfo.getQuota();

                    if (quota != null) {
                        final long used = quota.getUsed();
                        final long total = quota.getTotal();
                        final int relative = (int) Math.ceil(quota.getRelative());
                        final long quotaValue = quota.getQuota();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (quotaValue > 0 || quotaValue == GetUserInfoRemoteOperation.SPACE_UNLIMITED
                                    || quotaValue == GetUserInfoRemoteOperation.QUOTA_LIMIT_INFO_NOT_AVAILABLE) {
                                    /*
                                     * show quota in case
                                     * it is available and calculated (> 0) or
                                     * in case of legacy servers (==QUOTA_LIMIT_INFO_NOT_AVAILABLE)
                                     */
                                    setQuotaInformation(used, total, relative, quotaValue);
                                } else {
                                    /*
                                     * quotaValue < 0 means special cases like
                                     * {@link RemoteGetUserQuotaOperation.SPACE_NOT_COMPUTED},
                                     * {@link RemoteGetUserQuotaOperation.SPACE_UNKNOWN} or
                                     * {@link RemoteGetUserQuotaOperation.SPACE_UNLIMITED}
                                     * thus don't display any quota information.
                                     */
                                    showQuota(false);
                                }
                            }
                        });
                    }
                }
            }
        });

        t.start();
    }

    public void updateExternalLinksInDrawer() {
        if (mNavigationView != null && getBaseContext().getResources().getBoolean(R.bool.show_external_links)) {
            mNavigationView.getMenu().removeGroup(R.id.drawer_menu_external_links);

            float density = getResources().getDisplayMetrics().density;
            final int size = Math.round(24 * density);
            int greyColor = getResources().getColor(R.color.standard_grey);

            for (final ExternalLink link : externalLinksProvider.getExternalLink(ExternalLinkType.LINK)) {
                int id = mNavigationView.getMenu().add(R.id.drawer_menu_external_links,
                        MENU_ITEM_EXTERNAL_LINK + link.id, MENU_ORDER_EXTERNAL_LINKS, link.name)
                        .setCheckable(true).getItemId();

                MenuSimpleTarget target = new MenuSimpleTarget<Drawable>(id) {
                    @Override
                    public void onResourceReady(Drawable resource, GlideAnimation glideAnimation) {
                        setExternalLinkIcon(getIdMenuItem(), resource, greyColor);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        setExternalLinkIcon(getIdMenuItem(), errorDrawable, greyColor);
                    }
                };

                DisplayUtils.downloadIcon(getUserAccountManager(),
                                          this,
                                          link.iconUrl,
                                          target,
                                          R.drawable.ic_link,
                                          size,
                                          size);
            }

            setDrawerMenuItemChecked(mCheckedMenuItem);
        }
    }

    private void setExternalLinkIcon(int id, Drawable drawable, int greyColor) {
        MenuItem menuItem = mNavigationView.getMenu().findItem(id);

        if (menuItem != null) {
            if (drawable != null) {
                menuItem.setIcon(ThemeUtils.tintDrawable(drawable, greyColor));
            } else {
                menuItem.setIcon(R.drawable.ic_link);
            }
        }
    }

    public void updateHeaderBackground() {
        if (getAccount() != null &&
                getStorageManager().getCapability(getAccount().name).getServerBackground() != null) {
            final ViewGroup navigationHeader = (ViewGroup) findNavigationViewChildById(R.id.drawer_header_view);

            if (navigationHeader != null) {
                OCCapability capability = getStorageManager().getCapability(getAccount().name);
                String background = capability.getServerBackground();
                CapabilityBooleanType backgroundDefault = capability.getServerBackgroundDefault();
                CapabilityBooleanType backgroundPlain = capability.getServerBackgroundPlain();
                int primaryColor = ThemeUtils.primaryColor(getAccount(), false, this);

                if (backgroundDefault.isTrue() && backgroundPlain.isTrue()) {
                    // use only solid color
                    setNavigationHeaderBackground(new ColorDrawable(primaryColor), navigationHeader);
                } else if (backgroundDefault.isTrue() && backgroundPlain.isFalse()) {
                    // use nc13 background image with themed color
                    Drawable[] drawables = {new ColorDrawable(primaryColor),
                            getResources().getDrawable(R.drawable.background_nc13)};
                    LayerDrawable layerDrawable = new LayerDrawable(drawables);
                    setNavigationHeaderBackground(layerDrawable, navigationHeader);
                } else {
                    // use url
                    if (URLUtil.isValidUrl(background) || background.isEmpty()) {
                        // background image
                        SimpleTarget target = new SimpleTarget<Drawable>() {
                            @Override
                            public void onResourceReady(Drawable resource, GlideAnimation glideAnimation) {
                                Drawable[] drawables = {new ColorDrawable(primaryColor), resource};
                                LayerDrawable layerDrawable = new LayerDrawable(drawables);
                                setNavigationHeaderBackground(layerDrawable, navigationHeader);
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                Drawable[] drawables = {new ColorDrawable(primaryColor), errorDrawable};
                                LayerDrawable layerDrawable = new LayerDrawable(drawables);
                                setNavigationHeaderBackground(layerDrawable, navigationHeader);
                            }
                        };

                        int backgroundResource;
                        OwnCloudVersion ownCloudVersion = accountManager.getServerVersion(getAccount());
                        if (ownCloudVersion.compareTo(OwnCloudVersion.nextcloud_13) >= 0) {
                            backgroundResource = R.drawable.background_nc13;
                        } else {
                            backgroundResource = R.drawable.background;
                        }

                        Glide.with(this)
                                .load(background)
                                .centerCrop()
                                .placeholder(backgroundResource)
                                .error(backgroundResource)
                                .crossFade()
                                .into(target);
                    } else {
                        // plain color
                        setNavigationHeaderBackground(new ColorDrawable(primaryColor), navigationHeader);
                    }
                }
            }
        }
    }

    private void setNavigationHeaderBackground(Drawable drawable, ViewGroup navigationHeader) {
        final ImageView background = navigationHeader.findViewById(R.id.drawer_header_background);
        background.setImageDrawable(drawable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
            mCheckedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE);
        }

        mCurrentAccountAvatarRadiusDimension = getResources()
                .getDimension(R.dimen.nav_drawer_header_avatar_radius);
        mOtherAccountAvatarRadiusDimension = getResources()
                .getDimension(R.dimen.nav_drawer_header_avatar_other_accounts_radius);
        mMenuAccountAvatarRadiusDimension = getResources()
                .getDimension(R.dimen.nav_drawer_menu_avatar_radius);

        externalLinksProvider = new ExternalLinksProvider(getContentResolver());
        arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, mIsAccountChooserActive);
        outState.putInt(KEY_CHECKED_MENU_ITEM, mCheckedMenuItem);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
        mCheckedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE);

        // (re-)setup drawer state
        showMenu();

        // check/highlight the menu item if present
        if (mCheckedMenuItem > Menu.NONE || mCheckedMenuItem < Menu.NONE) {
            setDrawerMenuItemChecked(mCheckedMenuItem);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
            if (isDrawerOpen()) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            }
        }
        updateAccountList();
        updateExternalLinksInDrawer();
        updateQuotaLink();
        updateHeaderBackground();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen()) {
            closeDrawer();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDrawerMenuItemChecked(mCheckedMenuItem);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // update Account list and active account if Manage Account activity replies with
        // - ACCOUNT_LIST_CHANGED = true
        // - RESULT_OK
        if (requestCode == ACTION_MANAGE_ACCOUNTS && resultCode == RESULT_OK
                && data.getBooleanExtra(ManageAccountsActivity.KEY_ACCOUNT_LIST_CHANGED, false)) {

            // current account has changed
            if (data.getBooleanExtra(ManageAccountsActivity.KEY_CURRENT_ACCOUNT_CHANGED, false)) {
                setAccount(accountManager.getCurrentAccount());
                updateAccountList();
                restart();
            } else {
                updateAccountList();
            }
        } else if (requestCode == PassCodeManager.PASSCODE_ACTIVITY &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && data != null) {
            int result = data.getIntExtra(RequestCredentialsActivity.KEY_CHECK_RESULT,
                    RequestCredentialsActivity.KEY_CHECK_RESULT_FALSE);

            if (result == RequestCredentialsActivity.KEY_CHECK_RESULT_CANCEL) {
                Log_OC.d(TAG, "PassCodeManager cancelled");
                preferences.setLockTimestamp(0);
                finish();
            }
        }
    }

    /**
     * Finds a view that was identified by the id attribute from the drawer header.
     *
     * @param id the view's id
     * @return The view if found or <code>null</code> otherwise.
     */
    private View findNavigationViewChildById(int id) {
        NavigationView view = findViewById(R.id.nav_view);

        if (view != null) {
            return view.getHeaderView(0).findViewById(id);
        } else {
            return null;
        }
    }

    /**
     * Quota view can be either at navigation bottom or header
     *
     * @param id the view's id
     * @return The view if found or <code>null</code> otherwise.
     */
    private View findQuotaViewById(int id) {
        View v = ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(id);

        if (v != null) {
            return v;
        } else {
            return findViewById(id);
        }
    }

    /**
     * restart helper method which is called after a changing the current account.
     */
    protected abstract void restart();

    @Override
    protected void onAccountCreationSuccessful(AccountManagerFuture<Bundle> future) {
        super.onAccountCreationSuccessful(future);
        updateAccountList();
        restart();
    }

    /**
     * populates the avatar drawer array with the first three ownCloud {@link Account}s while the first element is
     * always the current account.
     */
    private void populateDrawerOwnCloudAccounts() {
        mAvatars = new Account[3];
        Account[] accountsAll = AccountManager.get(this).getAccountsByType(MainApp.getAccountType(this));

        ArrayList<Account> persistingAccounts = new ArrayList<>();

        for (Account acc: accountsAll) {
            boolean pendingForRemoval = arbitraryDataProvider.getBooleanValue(acc,
                    ManageAccountsActivity.PENDING_FOR_REMOVAL);

            if (!pendingForRemoval) {
                persistingAccounts.add(acc);
            }
        }

        Account currentAccount = accountManager.getCurrentAccount();

        mAvatars[0] = currentAccount;
        int j = 0;
        for (int i = 1; i <= 2 && i < persistingAccounts.size() && j < persistingAccounts.size(); j++) {
            if (!currentAccount.equals(persistingAccounts.get(j))) {
                mAvatars[i] = persistingAccounts.get(j);
                i++;
            }
        }
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        if (callContext instanceof MenuItem) {
            MenuItem mi = (MenuItem) callContext;
            mi.setIcon(avatarDrawable);
        } else if (callContext instanceof ImageView) {
            ImageView iv = (ImageView) callContext;
            iv.setImageDrawable(avatarDrawable);
        }
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        if (callContext instanceof MenuItem) {
            MenuItem mi = (MenuItem) callContext;
            return String.valueOf(mi.getTitle()).equals(tag);
        } else if (callContext instanceof ImageView) {
            ImageView iv = (ImageView) callContext;
            return String.valueOf(iv.getTag()).equals(tag);
        }
        return false;
    }

    /**
     * Adds other listeners to react on changes of the drawer layout.
     *
     * @param listener Object interested in changes of the drawer layout.
     */
    public void addDrawerListener(DrawerLayout.DrawerListener listener) {
        if (mDrawerLayout != null) {
            mDrawerLayout.addDrawerListener(listener);
        } else {
            Log_OC.e(TAG, "Drawer layout not ready to add drawer listener");
        }
    }

    public boolean isDrawerIndicatorAvailable() {
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if (preferences.getLockTimestamp() != 0) {
            preferences.setLockTimestamp(System.currentTimeMillis());
        }
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountRemovedEvent(AccountRemovedEvent event) {
        updateAccountList();
    }

    /**
     * Retrieves external links via api from 'external' app
     */
    public void fetchExternalLinks(final boolean force) {
        if (getBaseContext().getResources().getBoolean(R.bool.show_external_links)) {
            Thread t = new Thread(() -> {
                // fetch capabilities as early as possible
                if ((getCapabilities() == null || getCapabilities().getAccountName().isEmpty())
                        && getStorageManager() != null) {
                    GetCapabilitiesOperation getCapabilities = new GetCapabilitiesOperation();
                    getCapabilities.execute(getStorageManager(), getBaseContext());
                }

                Account account = accountManager.getCurrentAccount();

                if (account != null && getStorageManager() != null &&
                        getStorageManager().getCapability(account.name) != null &&
                        getStorageManager().getCapability(account.name).getExternalLinks().isTrue()) {

                    int count = arbitraryDataProvider.getIntegerValue(FilesSyncHelper.GLOBAL,
                            FileActivity.APP_OPENED_COUNT);

                    if (count > 10 || count == -1 || force) {
                        if (force) {
                            Log_OC.d("ExternalLinks", "force update");
                        }

                        arbitraryDataProvider.storeOrUpdateKeyValue(FilesSyncHelper.GLOBAL,
                                FileActivity.APP_OPENED_COUNT, "0");

                        Log_OC.d("ExternalLinks", "update via api");
                        RemoteOperation getExternalLinksOperation = new ExternalLinksOperation();
                        RemoteOperationResult result = getExternalLinksOperation.execute(account, this);

                        if (result.isSuccess() && result.getData() != null) {
                            externalLinksProvider.deleteAllExternalLinks();

                            ArrayList<ExternalLink> externalLinks = (ArrayList<ExternalLink>) (Object) result.getData();

                            for (ExternalLink link : externalLinks) {
                                externalLinksProvider.storeExternalLink(link);
                            }
                        }
                    } else {
                        arbitraryDataProvider.storeOrUpdateKeyValue(FilesSyncHelper.GLOBAL,
                                FileActivity.APP_OPENED_COUNT, String.valueOf(count + 1));
                    }
                } else {
                    externalLinksProvider.deleteAllExternalLinks();
                    Log_OC.d("ExternalLinks", "links disabled");
                }
                runOnUiThread(this::updateExternalLinksInDrawer);
            });

            t.start();
        }
    }
}
