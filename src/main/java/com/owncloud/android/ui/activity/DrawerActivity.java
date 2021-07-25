/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz  <hello@ezaquarii.com>
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2016 Nextcloud
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Infomaniak Network SA
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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.webkit.URLUtil;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.java.util.Optional;
import com.nextcloud.ui.ChooseAccountDialogFragment;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ExternalLinksProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.ExternalLink;
import com.owncloud.android.lib.common.ExternalLinkType;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.Quota;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.ExternalLinksOperation;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.operations.GetCapabilitiesOperation;
import com.owncloud.android.ui.activities.ActivitiesActivity;
import com.owncloud.android.ui.events.AccountRemovedEvent;
import com.owncloud.android.ui.events.ChangeMenuEvent;
import com.owncloud.android.ui.events.DummyDrawerEvent;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.preview.PreviewTextStringFragment;
import com.owncloud.android.ui.trashbin.TrashbinActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.DrawerMenuUtil;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.svg.MenuSimpleTarget;
import com.owncloud.android.utils.svg.SVGorImage;
import com.owncloud.android.utils.svg.SvgOrImageBitmapTranscoder;
import com.owncloud.android.utils.svg.SvgOrImageDecoder;
import com.owncloud.android.utils.theme.ThemeBarUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeDrawableUtils;
import com.owncloud.android.utils.theme.ThemeMenuUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
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
    private static final int MENU_ORDER_EXTERNAL_LINKS = 3;
    private static final int MENU_ITEM_EXTERNAL_LINK = 111;

    /**
     * Reference to the drawer layout.
     */
    private DrawerLayout mDrawerLayout;

    /**
     * Reference to the drawer toggle.
     */
    protected ActionBarDrawerToggle mDrawerToggle;

    /**
     * Reference to the navigation view.
     */
    private NavigationView mNavigationView;

    /**
     * Reference to the navigation view header.
     */
    private View mNavigationViewHeader;

    /**
     * Flag to signal if the account chooser is active.
     */
    private boolean mIsAccountChooserActive;

    /**
     * Id of the checked menu item.
     */
    private int mCheckedMenuItem = Menu.NONE;

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

    @Inject
    ClientFactory clientFactory;

    /**
     * Initializes the drawer, its content and highlights the menu item with the given id. This method needs to be
     * called after the content view has been set.
     *
     * @param menuItemId the menu item to be checked/highlighted
     */
    protected void setupDrawer(int menuItemId) {
        setupDrawer();
        setDrawerMenuItemChecked(menuItemId);
    }

    /**
     * Initializes the drawer and its content. This method needs to be called after the content view has been set.
     */
    protected void setupDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout);

        mNavigationView = findViewById(R.id.nav_view);
        if (mNavigationView != null) {

            // Setting up drawer header
            mNavigationViewHeader = mNavigationView.getHeaderView(0);
            updateHeader();

            setupDrawerMenu(mNavigationView);
            getAndDisplayUserQuota();
            setupQuotaElement();
        }

        setupDrawerToggle();

        if (getSupportActionBar() != null) {
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
        mDrawerToggle.setDrawerSlideAnimationEnabled(true);
        Drawable backArrow = ResourcesCompat.getDrawable(getResources(),
                                                         R.drawable.ic_arrow_back,
                                                         null);
        mDrawerToggle.setHomeAsUpIndicator(
            ThemeDrawableUtils.tintDrawable(backArrow, ThemeColorUtils.appBarPrimaryFontColor(this)));
        mDrawerToggle.getDrawerArrowDrawable().setColor(ThemeColorUtils.appBarPrimaryFontColor(this));
    }

    /**
     * setup quota elements of the drawer.
     */
    private void setupQuotaElement() {
        mQuotaView = (LinearLayout) findQuotaViewById(R.id.drawer_quota);
        mQuotaProgressBar = (ProgressBar) findQuotaViewById(R.id.drawer_quota_ProgressBar);
        mQuotaTextPercentage = (TextView) findQuotaViewById(R.id.drawer_quota_percentage);
        mQuotaTextLink = (TextView) findQuotaViewById(R.id.drawer_quota_link);
        ThemeBarUtils.colorProgressBar(mQuotaProgressBar, ThemeColorUtils.primaryColor(this));
    }

    public void updateHeader() {
        if (getAccount() != null &&
            getStorageManager().getCapability(getAccount().name).getServerBackground() != null) {

            OCCapability capability = getStorageManager().getCapability(getAccount().name);
            String logo = capability.getServerLogo();
            int primaryColor = ThemeColorUtils.primaryColor(getAccount(), false, this);

            // set background to primary color
            LinearLayout drawerHeader = mNavigationViewHeader.findViewById(R.id.drawer_header_view);
            drawerHeader.setBackgroundColor(ThemeColorUtils.unchangedPrimaryColor(getAccount(), this));

            if (!TextUtils.isEmpty(logo) && URLUtil.isValidUrl(logo)) {
                // background image
                GenericRequestBuilder<Uri, InputStream, SVGorImage, Bitmap> requestBuilder = Glide.with(this)
                    .using(Glide.buildStreamModelLoader(Uri.class, this), InputStream.class)
                    .from(Uri.class)
                    .as(SVGorImage.class)
                    .transcode(new SvgOrImageBitmapTranscoder(128, 128), Bitmap.class)
                    .sourceEncoder(new StreamEncoder())
                    .cacheDecoder(new FileToStreamDecoder<>(new SvgOrImageDecoder()))
                    .decoder(new SvgOrImageDecoder());

                // background image
                SimpleTarget target = new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                        Drawable[] drawables = {new ColorDrawable(primaryColor), new BitmapDrawable(resource)};
                        LayerDrawable layerDrawable = new LayerDrawable(drawables);

                        String name = capability.getServerName();
                        setDrawerHeaderLogo(layerDrawable, name);
                    }
                };

                requestBuilder
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .load(Uri.parse(logo))
                    .into(target);
            }
        }
    }

    private void setDrawerHeaderLogo(Drawable drawable, String name) {
        ImageView imageHeader = mNavigationViewHeader.findViewById(R.id.drawer_header_logo);
        imageHeader.setImageDrawable(drawable);
        imageHeader.setScaleType(ImageView.ScaleType.FIT_START);
        imageHeader.setAdjustViewBounds(true);

        imageHeader.setMaxWidth(DisplayUtils.convertDpToPixel(100f, this));

        MarginLayoutParams oldParam = (MarginLayoutParams) imageHeader.getLayoutParams();
        MarginLayoutParams params = new MarginLayoutParams(LayoutParams.WRAP_CONTENT,
                                                           LayoutParams.MATCH_PARENT);
        params.leftMargin = oldParam.leftMargin;
        params.rightMargin = oldParam.rightMargin;

        imageHeader.setLayoutParams(new LinearLayout.LayoutParams(params));

        if (!TextUtils.isEmpty(name)) {
            TextView serverName = mNavigationViewHeader.findViewById(R.id.drawer_header_server_name);
            serverName.setText(name);
            serverName.setTextColor(ThemeColorUtils.unchangedFontColor(this));
        }

    }
    
    /**
     * setup drawer header, basically the logo color
     */
    private void setupDrawerHeader(FrameLayout drawerHeader) {
        drawerHeader.setBackgroundColor(ThemeColorUtils.primaryColor(getAccount(), true, this));
    }

    /**
     * setup drawer content, basically setting the item selected listener.
     *
     * @param navigationView the drawers navigation view
     */
    private void setupDrawerMenu(NavigationView navigationView) {
        navigationView.setItemIconTintList(null);

        // setup actions for drawer menu items
        navigationView.setNavigationItemSelectedListener(
            menuItem -> {
                mDrawerLayout.closeDrawers();
                // pending runnable will be executed after the drawer has been closed
                pendingRunnable = () -> onNavigationItemClicked(menuItem);
                return true;
            });

        User account = accountManager.getUser();
        filterDrawerMenu(navigationView.getMenu(), account);
    }

    private void filterDrawerMenu(final Menu menu, @NonNull final User user) {
        FileDataStorageManager storageManager = new FileDataStorageManager(user.toPlatformAccount(),
                                                                           getContentResolver());
        OCCapability capability = storageManager.getCapability(user.getAccountName());

        DrawerMenuUtil.filterSearchMenuItems(menu, user, getResources());
        DrawerMenuUtil.filterTrashbinMenuItem(menu, capability);
        DrawerMenuUtil.filterActivityMenuItem(menu, capability);

        DrawerMenuUtil.setupHomeMenuItem(menu, getResources());

        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_community,
                                      !getResources().getBoolean(R.bool.participate_enabled));
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_shared, !getResources().getBoolean(R.bool.shared_enabled));
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_contacts, !getResources().getBoolean(R.bool.contacts_backup)
            || !getResources().getBoolean(R.bool.show_drawer_contacts_backup));

        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_logout, !getResources().getBoolean(R.bool.show_drawer_logout));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DummyDrawerEvent event) {
        unsetAllDrawerMenuItems();
    }


    private void onNavigationItemClicked(final MenuItem menuItem) {
        setDrawerMenuItemChecked(menuItem.getItemId());

        int itemId = menuItem.getItemId();

        if (itemId == R.id.nav_all_files) {
            if (this instanceof FileDisplayActivity &&
                !(((FileDisplayActivity) this).getLeftFragment() instanceof GalleryFragment) &&
                !(((FileDisplayActivity) this).getLeftFragment() instanceof PreviewTextStringFragment)) {
                showFiles(false);
                ((FileDisplayActivity) this).browseToRoot();
                EventBus.getDefault().post(new ChangeMenuEvent());
            } else {
                Intent intent = new Intent(getApplicationContext(), FileDisplayActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setAction(FileDisplayActivity.ALL_FILES);
                intent.putExtra(FileDisplayActivity.DRAWER_MENU_ID, menuItem.getItemId());
                startActivity(intent);
            }
        } else if (itemId == R.id.nav_favorites) {
            handleSearchEvents(new SearchEvent("", SearchRemoteOperation.SearchType.FAVORITE_SEARCH),
                               menuItem.getItemId());
        } else if (itemId == R.id.nav_gallery) {
            startPhotoSearch(menuItem);
        } else if (itemId == R.id.nav_on_device) {
            EventBus.getDefault().post(new ChangeMenuEvent());
            showFiles(true);
        } else if (itemId == R.id.nav_uploads) {
            startActivity(UploadListActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.nav_trashbin) {
            startActivity(TrashbinActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.nav_activity) {
            startActivity(ActivitiesActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.nav_notifications) {
            startActivity(NotificationsActivity.class);
        } else if (itemId == R.id.nav_contacts) {
            ContactsPreferenceActivity.startActivity(this);
        } else if (itemId == R.id.nav_settings) {
            startActivity(SettingsActivity.class);
        } else if (itemId == R.id.nav_community) {
            startActivity(CommunityActivity.class);
        } else if (itemId == R.id.nav_logout) {
            mCheckedMenuItem = -1;
            menuItem.setChecked(false);
            final Optional<User> optionalUser = getUser();
            if (optionalUser.isPresent()) {
                UserInfoActivity.openAccountRemovalConfirmationDialog(optionalUser.get(), getSupportFragmentManager());
            }
        } else if (itemId == R.id.nav_shared) {
            handleSearchEvents(new SearchEvent("", SearchRemoteOperation.SearchType.SHARED_FILTER),
                               menuItem.getItemId());
        } else if (itemId == R.id.nav_recently_modified) {
            handleSearchEvents(new SearchEvent("", SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH),
                               menuItem.getItemId());
        } else {
            if (menuItem.getItemId() >= MENU_ITEM_EXTERNAL_LINK &&
                menuItem.getItemId() <= MENU_ITEM_EXTERNAL_LINK + 100) {
                // external link clicked
                externalLinkClicked(menuItem);
            } else {
                Log_OC.w(TAG, "Unknown drawer menu item clicked: " + menuItem.getTitle());
            }
        }
    }

    private void startActivity(Class<? extends Activity> activity) {
        startActivity(new Intent(getApplicationContext(), activity));
    }

    private void startActivity(Class<? extends Activity> activity, int flags) {
        Intent intent = new Intent(getApplicationContext(), activity);
        intent.setFlags(flags);
        startActivity(intent);
    }

    public void showManageAccountsDialog() {
        ChooseAccountDialogFragment choseAccountDialog = ChooseAccountDialogFragment.newInstance(accountManager.getUser());
        choseAccountDialog.show(getSupportFragmentManager(), "fragment_chose_account");
    }

    public void openManageAccounts() {
        Intent manageAccountsIntent = new Intent(getApplicationContext(), ManageAccountsActivity.class);
        startActivityForResult(manageAccountsIntent, ACTION_MANAGE_ACCOUNTS);
    }

    public void openAddAccount() {
        boolean isProviderOrOwnInstallationVisible = getResources()
            .getBoolean(R.bool.show_provider_or_own_installation);

        if (isProviderOrOwnInstallationVisible) {
            Intent firstRunIntent = new Intent(getApplicationContext(), FirstRunActivity.class);
            firstRunIntent.putExtra(FirstRunActivity.EXTRA_ALLOW_CLOSE, true);
            startActivity(firstRunIntent);
        } else {
            startAccountCreation();
        }
    }

    private void startPhotoSearch(MenuItem menuItem) {
        SearchEvent searchEvent = new SearchEvent("image/%", SearchRemoteOperation.SearchType.PHOTO_SEARCH);
        MainApp.showOnlyFilesOnDevice(false);

        Intent intent = new Intent(getApplicationContext(), FileDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent));
        intent.putExtra(FileDisplayActivity.DRAWER_MENU_ID, menuItem.getItemId());
        startActivity(intent);
    }

    private void handleSearchEvents(SearchEvent searchEvent, int menuItemId) {
        if (this instanceof FileDisplayActivity) {
            if (((FileDisplayActivity) this).getListOfFilesFragment() instanceof GalleryFragment) {
                Intent intent = new Intent(getApplicationContext(), FileDisplayActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setAction(Intent.ACTION_SEARCH);
                intent.putExtra(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent));
                intent.putExtra(FileDisplayActivity.DRAWER_MENU_ID, menuItemId);
                startActivity(intent);
            } else {
                EventBus.getDefault().post(searchEvent);
            }
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
     * sets the new/current account and restarts. In case the given account equals the actual/current account the call
     * will be ignored.
     *
     * @param hashCode HashCode of account to be set
     */
    public void accountClicked(int hashCode) {
        final User currentUser = accountManager.getUser();
        if (currentUser.hashCode() != hashCode && accountManager.setCurrentOwnCloudAccount(hashCode)) {
            fetchExternalLinks(true);
            restart();
        }
    }

    private void externalLinkClicked(MenuItem menuItem) {
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
     * @param lockMode The new lock mode for the given drawer. One of {@link DrawerLayout#LOCK_MODE_UNLOCKED}, {@link
     *                 DrawerLayout#LOCK_MODE_LOCKED_CLOSED} or {@link DrawerLayout#LOCK_MODE_LOCKED_OPEN}.
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
     * Updates title bar and home buttons (state and icon). Assumes that navigation drawer is NOT visible.
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        super.updateActionBarTitleAndHomeButton(chosenFile);

        // set home button properties
        if (mDrawerToggle != null) {
            if (chosenFile != null && isRoot(chosenFile)) {
                mDrawerToggle.setDrawerIndicatorEnabled(true);
            } else {
                mDrawerToggle.setDrawerIndicatorEnabled(false);
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
     *
     * @param usedSpace  the used space
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

        ThemeBarUtils.colorProgressBar(mQuotaProgressBar, DisplayUtils.getRelativeInfoColor(this, relative));

        updateQuotaLink();
        showQuota(true);
    }

    private void unsetAllDrawerMenuItems() {
        if (mNavigationView != null) {
            mNavigationView.getMenu();
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
                    mQuotaTextLink.setOnClickListener(v -> {
                        Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, firstQuota.name);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, firstQuota.url);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, true);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_MENU_ITEM_ID, -1);
                        startActivity(externalWebViewIntent);
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
                                              clientFactory,
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
        if (mNavigationView != null && mNavigationView.getMenu().findItem(menuItemId) != null) {
            mCheckedMenuItem = menuItemId;
            MenuItem currentItem = mNavigationView.getMenu().findItem(menuItemId);
            int drawerColor = getResources().getColor(R.color.drawer_text_color);
            int activeColor = ThemeColorUtils.primaryColor(null, true, true, this);

            currentItem.setChecked(true);

            // For each menu item, change the color of the selected item, and of the other items
            for (int i = 0; i < mNavigationView.getMenu().size(); i++) {
                MenuItem menuItem = mNavigationView.getMenu().getItem(i);
                if (menuItem.getIcon() != null) {
                    if (menuItem == currentItem) {
                        ThemeDrawableUtils.tintDrawable(currentItem.getIcon(), activeColor);
                        ThemeMenuUtils.tintMenuItemText(currentItem, activeColor);
                    } else {
                        ThemeDrawableUtils.tintDrawable(menuItem.getIcon(), drawerColor);
                        ThemeMenuUtils.tintMenuItemText(menuItem, drawerColor);
                    }
                }
            }
        } else {
            Log_OC.w(TAG, "setDrawerMenuItemChecked has been called with invalid menu-item-ID");
        }
    }

    /**
     * Retrieves and shows the user quota if available
     */
    private void getAndDisplayUserQuota() {
        // set user space information
        Thread t = new Thread(() -> {
            final User user = accountManager.getUser();

            if (user.isAnonymous()) {
                return;
            }

            final Context context = MainApp.getAppContext();
            NextcloudClient nextcloudClient = null;
            try {
                nextcloudClient = OwnCloudClientManagerFactory
                    .getDefaultSingleton()
                    .getNextcloudClientFor(user.toOwnCloudAccount(),
                                           context);
            } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                Log_OC.e(this, "Error retrieving user quota", e);
            }

            if (nextcloudClient == null) {
                return;
            }

            RemoteOperationResult<UserInfo> result = new GetUserInfoRemoteOperation().execute(nextcloudClient);

            if (result.isSuccess() && result.getResultData() != null) {
                final UserInfo userInfo = result.getResultData();
                final Quota quota = userInfo.getQuota();

                if (quota != null) {
                    final long used = quota.getUsed();
                    final long total = quota.getTotal();
                    final int relative = (int) Math.ceil(quota.getRelative());
                    final long quotaValue = quota.getQuota();

                    runOnUiThread(() -> {
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
                    });
                }
            }
        });

        t.start();
    }

    private void updateExternalLinksInDrawer() {
        if (mNavigationView != null && getBaseContext().getResources().getBoolean(R.bool.show_external_links)) {
            mNavigationView.getMenu().removeGroup(R.id.drawer_menu_external_links);

            float density = getResources().getDisplayMetrics().density;
            final int size = Math.round(24 * density);
            int greyColor = ContextCompat.getColor(this, R.color.drawer_menu_icon);

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
                                          clientFactory,
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
                menuItem.setIcon(ThemeDrawableUtils.tintDrawable(drawable, greyColor));
            } else {
                menuItem.setIcon(R.drawable.ic_link);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
            mCheckedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE);
        }

        externalLinksProvider = new ExternalLinksProvider(getContentResolver());
        arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, mIsAccountChooserActive);
        outState.putInt(KEY_CHECKED_MENU_ITEM, mCheckedMenuItem);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
        mCheckedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE);

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
        updateExternalLinksInDrawer();
        updateQuotaLink();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
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
                setAccount(accountManager.getCurrentAccount(), false);
                restart();
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
    private void restart() {
        Intent i = new Intent(this, FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setAction(FileDisplayActivity.RESTART);
        startActivity(i);

        fetchExternalLinks(false);
    }

    /**
     * show the file list to the user.
     *
     * @param onDeviceOnly flag to decide if all files or only the ones on the device should be shown
     */
    public void showFiles(boolean onDeviceOnly) {
        MainApp.showOnlyFilesOnDevice(onDeviceOnly);
        Intent fileDisplayActivity = new Intent(getApplicationContext(), FileDisplayActivity.class);
        fileDisplayActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(fileDisplayActivity);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        if (callContext instanceof MenuItem) {
            MenuItem menuItem = (MenuItem) callContext;
            menuItem.setIcon(avatarDrawable);
        } else if (callContext instanceof ImageView) {
            ImageView imageView = (ImageView) callContext;
            imageView.setImageDrawable(avatarDrawable);
        } else if (callContext instanceof MaterialButton) {
            MaterialButton materialButton = (MaterialButton) callContext;
            materialButton.setIcon(avatarDrawable);
        }
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        if (callContext instanceof MenuItem) {
            MenuItem menuItem = (MenuItem) callContext;
            return String.valueOf(menuItem.getTitle()).equals(tag);
        } else if (callContext instanceof ImageView) {
            ImageView imageView = (ImageView) callContext;
            return String.valueOf(imageView.getTag()).equals(tag);
        } else if (callContext instanceof MaterialButton) {
            MaterialButton materialButton = (MaterialButton) callContext;
            return String.valueOf(materialButton.getTag()).equals(tag);
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
            preferences.setLockTimestamp(SystemClock.elapsedRealtime());
        }
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountRemovedEvent(AccountRemovedEvent event) {
        restart();
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
                    GetCapabilitiesOperation getCapabilities = new GetCapabilitiesOperation(getStorageManager());
                    getCapabilities.execute(getBaseContext());
                }

                User user = accountManager.getUser();
                String name = user.getAccountName();
                if (getStorageManager() != null && getStorageManager().getCapability(name).getExternalLinks().isTrue()) {

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
                        RemoteOperationResult result = getExternalLinksOperation.execute(user.toPlatformAccount(), this);

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
