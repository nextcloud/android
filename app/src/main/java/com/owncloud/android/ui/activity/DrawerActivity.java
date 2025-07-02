/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021-2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2020 Infomaniak Network SA
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.files.DeepLinkConstants;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.ui.ChooseAccountDialogFragment;
import com.nextcloud.ui.composeActivity.ComposeActivity;
import com.nextcloud.ui.composeActivity.ComposeDestination;
import com.nextcloud.utils.GlideHelper;
import com.nextcloud.utils.LinkHelper;
import com.nextcloud.utils.extensions.ActivityExtensionsKt;
import com.nextcloud.utils.extensions.ViewExtensionsKt;
import com.nextcloud.utils.mdm.MDMConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.ExternalLinksProvider;
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
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.ui.fragment.GroupfolderListFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.fragment.SharedListFragment;
import com.owncloud.android.ui.preview.PreviewTextStringFragment;
import com.owncloud.android.ui.trashbin.TrashbinActivity;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.DrawableUtil;
import com.owncloud.android.utils.DrawerMenuUtil;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.theme.CapabilityUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hct.Hct;
import kotlin.Unit;

import static com.nextcloud.utils.extensions.DrawerActivityExtensionsKt.getMenuItemIdFromTitle;

/**
 * Base class to handle setup of the drawer implementation including user switching and avatar fetching and fallback
 * generation.
 */
public abstract class DrawerActivity extends ToolbarActivity
    implements DisplayUtils.AvatarGenerationListener, Injectable {

    private static final String TAG = DrawerActivity.class.getSimpleName();
    private static final String KEY_IS_ACCOUNT_CHOOSER_ACTIVE = "IS_ACCOUNT_CHOOSER_ACTIVE";
    private static final int ACTION_MANAGE_ACCOUNTS = 101;
    private static final int MENU_ORDER_EXTERNAL_LINKS = 3;
    private static final int MENU_ITEM_EXTERNAL_LINK = 111;
    private static final int MAX_LOGO_SIZE_PX = 1000;
    private static final int RELATIVE_THRESHOLD_WARNING = 80;

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
    private NavigationView drawerNavigationView;

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
    public static int menuItemId = Menu.NONE;

    /**
     * container layout of the quota view.
     */
    private LinearLayout mQuotaView;

    /**
     * progress bar of the quota view.
     */
    private LinearProgressIndicator mQuotaProgressBar;

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

    private BottomNavigationView bottomNavigationView;

    @Inject
    AppPreferences preferences;

    @Inject
    ClientFactory clientFactory;

    /**
     * Initializes the drawer and its content. This method needs to be called after the content view has been set.
     */
    protected void setupDrawer() {
        mDrawerLayout = findViewById(R.id.drawer_layout);

        drawerNavigationView = findViewById(R.id.nav_view);
        if (drawerNavigationView != null) {

            // Setting up drawer header
            mNavigationViewHeader = drawerNavigationView.getHeaderView(0);
            updateHeader();

            setupDrawerMenu(drawerNavigationView);
            getAndDisplayUserQuota();
            setupQuotaElement();
        }

        setupDrawerToggle();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            themeBottomNavigationMenu();
            checkAssistantBottomNavigationMenu();
            handleBottomNavigationViewClicks();
        }
    }

    private void themeBottomNavigationMenu() {
        viewThemeUtils.platform.colorBottomNavigationView(bottomNavigationView);
    }

    @SuppressFBWarnings("RV")
    private void checkAssistantBottomNavigationMenu() {
        boolean isAssistantAvailable = getCapabilities().getAssistant().isTrue();

        bottomNavigationView
            .getMenu()
            .findItem(R.id.nav_assistant)
            .setVisible(isAssistantAvailable);
    }

    @SuppressFBWarnings("RV")
    private void handleBottomNavigationViewClicks() {
        bottomNavigationView.setOnItemSelectedListener(menuItem -> {
            menuItemId = menuItem.getItemId();

            exitSelectionMode();
            resetOnlyPersonalAndOnDevice();

            if (menuItemId == R.id.nav_all_files) {
                showFiles(false,false);
                if (this instanceof FileDisplayActivity fda) {
                    fda.browseToRoot();
                }
                EventBus.getDefault().post(new ChangeMenuEvent());
            } else if (menuItemId == R.id.nav_favorites) {
                setupToolbar();
                handleSearchEvents(new SearchEvent("", SearchRemoteOperation.SearchType.FAVORITE_SEARCH), menuItemId);
            } else if (menuItemId == R.id.nav_assistant && !(this instanceof ComposeActivity)) {
                startComposeActivity(ComposeDestination.AssistantScreen, R.string.assistant_screen_top_bar_title);
            } else if (menuItemId == R.id.nav_gallery) {
                setupToolbar();
                startPhotoSearch(menuItem.getItemId());
            }

            // Remove extra icon from the action bar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setIcon(null);
            }

            setNavigationViewItemChecked();

            return false;
        });
    }

    @Nullable
    public OCFileListFragment getOCFileListFragment() {
        Fragment fragment = ActivityExtensionsKt.lastFragment(this);
        if (fragment instanceof OCFileListFragment fileListFragment) {
            return fileListFragment;
        }

        fragment = getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES);
        if (fragment instanceof OCFileListFragment fileListFragment) {
            return fileListFragment;
        }

        return null;
    }

    private void exitSelectionMode() {
        Fragment fragment = getOCFileListFragment();
        if (fragment instanceof OCFileListFragment fileListFragment) {
            fileListFragment.exitSelectionMode();
        }
    }

    /**
     * initializes and sets up the drawer toggle.
     */
    private void setupDrawerToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {
            private boolean isMenuItemChecked = false;

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if (slideOffset > 0 && !isMenuItemChecked) {
                    Integer menuItemIdFromTitle = getMenuItemIdFromTitle(DrawerActivity.this);
                    if (menuItemIdFromTitle != null && menuItemIdFromTitle != menuItemId) {
                        menuItemId = menuItemIdFromTitle;
                    }
                    setNavigationViewItemChecked();
                    isMenuItemChecked = true;
                }
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                supportInvalidateOptionsMenu();
                mDrawerToggle.setDrawerIndicatorEnabled(isDrawerIndicatorAvailable());

                if (pendingRunnable != null) {
                    new Handler().post(pendingRunnable);
                    pendingRunnable = null;
                }

                isMenuItemChecked = false;
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

        if (backArrow != null) {
            viewThemeUtils.platform.tintToolbarArrowDrawable(this, mDrawerToggle, backArrow);
        }
    }

    /**
     * setup quota elements of the drawer.
     */
    private void setupQuotaElement() {
        mQuotaView = (LinearLayout) findQuotaViewById(R.id.drawer_quota);
        mQuotaProgressBar = (LinearProgressIndicator) findQuotaViewById(R.id.drawer_quota_ProgressBar);
        mQuotaTextPercentage = (TextView) findQuotaViewById(R.id.drawer_quota_percentage);
        mQuotaTextLink = (TextView) findQuotaViewById(R.id.drawer_quota_link);
        viewThemeUtils.material.colorProgressBar(mQuotaProgressBar);
    }

    public void updateHeader() {
        int primaryColor = themeColorUtils.unchangedPrimaryColor(getAccount(), this);
        boolean isClientBranded = getResources().getBoolean(R.bool.is_branded_client);

        if (getAccount() != null &&
            getCapabilities().getServerBackground() != null && !isClientBranded) {

            OCCapability capability = getCapabilities();
            String serverLogoURL = capability.getServerLogo();

            // set background to primary color
            LinearLayout drawerHeader = mNavigationViewHeader.findViewById(R.id.drawer_header_view);
            drawerHeader.setBackgroundColor(primaryColor);

            if (!TextUtils.isEmpty(serverLogoURL) && URLUtil.isValidUrl(serverLogoURL)) {
                Target<Drawable> target = createSVGLogoTarget(primaryColor, capability);
                getClientRepository().getNextcloudClient(nextcloudClient -> {
                    GlideHelper.INSTANCE.loadIntoTarget(DrawerActivity.this,
                                                        nextcloudClient,
                                                        serverLogoURL,
                                                        target,
                                                        R.drawable.background);
                    return Unit.INSTANCE;
                });
            }
        }

        // hide ecosystem apps according to user preference or in branded client
        ConstraintLayout banner = mNavigationViewHeader.findViewById(R.id.drawer_ecosystem_apps);
        boolean shouldHideTopBanner = isClientBranded || !preferences.isShowEcosystemApps();

        if (shouldHideTopBanner) {
            hideTopBanner(banner);
        } else {
            showTopBanner(banner, primaryColor);
        }
    }

    private Target<Drawable> createSVGLogoTarget(int primaryColor, OCCapability capability) {
        return new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                Bitmap bitmap;

                if (resource instanceof PictureDrawable pictureDrawable) {
                    bitmap = Bitmap.createBitmap(
                        pictureDrawable.getIntrinsicWidth(),
                        pictureDrawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);

                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawPicture(pictureDrawable.getPicture());

                } else if (resource instanceof BitmapDrawable bitmapDrawable) {
                    bitmap = bitmapDrawable.getBitmap();
                } else {
                    Log_OC.e(TAG, "Unsupported drawable type: " + resource.getClass().getName());
                    return;
                }

                // Scale down if necessary
                Bitmap logo = bitmap;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int max = Math.max(width, height);
                if (max > MAX_LOGO_SIZE_PX) {
                    logo = BitmapUtils.scaleBitmap(bitmap, MAX_LOGO_SIZE_PX, width, height, max);
                }

                Drawable[] drawables = {
                    new ColorDrawable(primaryColor),
                    new BitmapDrawable(getResources(), logo)
                };
                LayerDrawable layerDrawable = new LayerDrawable(drawables);

                String name = capability.getServerName();
                setDrawerHeaderLogo(layerDrawable, name);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {}
        };
    }

    private void hideTopBanner(ConstraintLayout banner) {
        banner.setVisibility(View.GONE);
    }

    private void showTopBanner(ConstraintLayout banner, int primaryColor) {
        LinearLayout notesView = banner.findViewById(R.id.drawer_ecosystem_notes);
        LinearLayout talkView = banner.findViewById(R.id.drawer_ecosystem_talk);
        LinearLayout moreView = banner.findViewById(R.id.drawer_ecosystem_more);
        LinearLayout assistantView = banner.findViewById(R.id.drawer_ecosystem_assistant);

        notesView.setOnClickListener(v -> LinkHelper.INSTANCE.openAppOrStore(LinkHelper.APP_NEXTCLOUD_NOTES, getUser(), this));
        talkView.setOnClickListener(v -> LinkHelper.INSTANCE.openAppOrStore(LinkHelper.APP_NEXTCLOUD_TALK, getUser(), this));
        moreView.setOnClickListener(v -> LinkHelper.INSTANCE.openAppStore("Nextcloud", true, this));
        assistantView.setOnClickListener(v -> {
            DrawerActivity.menuItemId = Menu.NONE;
            startComposeActivity(ComposeDestination.AssistantScreen, R.string.assistant_screen_top_bar_title);
        });
        if (getCapabilities() != null && getCapabilities().getAssistant().isTrue()) {
            assistantView.setVisibility(View.VISIBLE);
        } else {
            assistantView.setVisibility(View.GONE);
        }

        List<LinearLayout> views = Arrays.asList(notesView, talkView, moreView, assistantView);

        int iconColor;
        if (Hct.fromInt(primaryColor).getTone() < 80.0) {
            iconColor = Color.WHITE;
        } else {
            iconColor = getColor(R.color.grey_800_transparent);
        }

        for (LinearLayout view : views) {
            ImageView imageView = (ImageView) view.getChildAt(0);
            imageView.setImageTintList(ColorStateList.valueOf(iconColor));
            GradientDrawable background = (GradientDrawable) imageView.getBackground();
            background.setStroke(DisplayUtils.convertDpToPixel(1, this), iconColor);
            TextView textView = (TextView) view.getChildAt(1);
            textView.setTextColor(iconColor);
        }

        banner.setVisibility(View.VISIBLE);
    }

    private void setDrawerHeaderLogo(Drawable drawable, String serverName) {
        ImageView imageHeader = mNavigationViewHeader.findViewById(R.id.drawer_header_logo);
        imageHeader.setImageDrawable(drawable);
        imageHeader.setAdjustViewBounds(true);

        if (!TextUtils.isEmpty(serverName)) {
            TextView serverNameView = mNavigationViewHeader.findViewById(R.id.drawer_header_server_name);
            serverNameView.setVisibility(View.VISIBLE);
            serverNameView.setText(serverName);
            serverNameView.setTextColor(themeColorUtils.unchangedFontColor(this));
        }

    }

    /**
     * setup drawer content, basically setting the item selected listener.
     *
     * @param navigationView the drawers navigation view
     */
    private void setupDrawerMenu(NavigationView navigationView) {

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
        OCCapability capability = getCapabilities();

        DrawerMenuUtil.filterSearchMenuItems(menu, user, getResources());
        DrawerMenuUtil.filterTrashbinMenuItem(menu, capability);
        DrawerMenuUtil.filterActivityMenuItem(menu, capability);
        DrawerMenuUtil.filterGroupfoldersMenuItem(menu, capability);
        DrawerMenuUtil.filterAssistantMenuItem(menu, capability, getResources());
        DrawerMenuUtil.setupHomeMenuItem(menu, getResources());
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_community, !getResources().getBoolean(R.bool.participate_enabled));
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_shared, !getResources().getBoolean(R.bool.shared_enabled));
        DrawerMenuUtil.removeMenuItem(menu, R.id.nav_logout, !getResources().getBoolean(R.bool.show_drawer_logout));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DummyDrawerEvent event) {
        unsetAllDrawerMenuItems();
    }

    private void onNavigationItemClicked(final MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        menuItemId = itemId;
        setNavigationViewItemChecked();

        if (itemId == R.id.nav_all_files || itemId == R.id.nav_personal_files) {
            if (this instanceof FileDisplayActivity fda &&
                !(fda.getLeftFragment() instanceof GalleryFragment) &&
                !(fda.getLeftFragment() instanceof SharedListFragment) &&
                !(fda.getLeftFragment() instanceof GroupfolderListFragment) &&
                !(fda.getLeftFragment() instanceof PreviewTextStringFragment)) {
                showFiles(false, itemId == R.id.nav_personal_files);
                fda.browseToRoot();
                EventBus.getDefault().post(new ChangeMenuEvent());
            } else {
                MainApp.showOnlyFilesOnDevice(false);
                MainApp.showOnlyPersonalFiles(itemId == R.id.nav_personal_files);
                Intent intent = new Intent(getApplicationContext(), FileDisplayActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                if (this instanceof ComposeActivity) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                }

                intent.setAction(FileDisplayActivity.ALL_FILES);
                startActivity(intent);
            }

            closeDrawer();
        } else if (itemId == R.id.nav_favorites) {
            resetOnlyPersonalAndOnDevice();
            setupToolbar();
            handleSearchEvents(new SearchEvent("", SearchRemoteOperation.SearchType.FAVORITE_SEARCH), menuItem.getItemId());
        } else if (itemId == R.id.nav_gallery) {
            resetOnlyPersonalAndOnDevice();
            setupToolbar();
            startPhotoSearch(menuItem.getItemId());
        } else if (itemId == R.id.nav_on_device) {
            EventBus.getDefault().post(new ChangeMenuEvent());
            showFiles(true, false);
        } else if (itemId == R.id.nav_uploads) {
            resetOnlyPersonalAndOnDevice();
            startActivity(UploadListActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.nav_trashbin) {
            resetOnlyPersonalAndOnDevice();
            startActivity(TrashbinActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.nav_activity) {
            resetOnlyPersonalAndOnDevice();
            startActivity(ActivitiesActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.nav_settings) {
            resetOnlyPersonalAndOnDevice();
            startActivity(SettingsActivity.class);
        } else if (itemId == R.id.nav_community) {
            resetOnlyPersonalAndOnDevice();
            startActivity(CommunityActivity.class);
        } else if (itemId == R.id.nav_logout) {
            resetOnlyPersonalAndOnDevice();
            menuItemId = Menu.NONE;
            MenuItem isNewMenuItemChecked = menuItem.setChecked(false);
            Log_OC.d(TAG,"onNavigationItemClicked nav_logout setChecked " + isNewMenuItemChecked);
            final Optional<User> optionalUser = getUser();
            if (optionalUser.isPresent()) {
                UserInfoActivity.openAccountRemovalDialog(optionalUser.get(), getSupportFragmentManager());
            }
        } else if (itemId == R.id.nav_shared) {
            resetOnlyPersonalAndOnDevice();
            startSharedSearch(menuItem);
        } else if (itemId == R.id.nav_recently_modified) {
            resetOnlyPersonalAndOnDevice();
            startRecentlyModifiedSearch(menuItem);
        } else if (itemId == R.id.nav_assistant) {
            resetOnlyPersonalAndOnDevice();
            startComposeActivity(ComposeDestination.AssistantScreen, R.string.assistant_screen_top_bar_title);
        } else if (itemId == R.id.nav_groupfolders) {
            resetOnlyPersonalAndOnDevice();
            Intent intent = new Intent(getApplicationContext(), FileDisplayActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(FileDisplayActivity.LIST_GROUPFOLDERS);
            startActivity(intent);
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

    private void startComposeActivity(ComposeDestination destination, int titleId) {
        Intent composeActivity = new Intent(getApplicationContext(), ComposeActivity.class);
        composeActivity.putExtra(ComposeActivity.DESTINATION, destination);
        composeActivity.putExtra(ComposeActivity.TITLE, titleId);
        startActivity(composeActivity);
    }

    void startActivity(Class<? extends Activity> activity) {
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
        if (MDMConfig.INSTANCE.showIntro(this)) {
            Intent firstRunIntent = new Intent(getApplicationContext(), FirstRunActivity.class);
            firstRunIntent.putExtra(FirstRunActivity.EXTRA_ALLOW_CLOSE, true);
            startActivity(firstRunIntent);
        } else {
            startAccountCreation();
        }
    }

    private void startSharedSearch(MenuItem menuItem) {
        SearchEvent searchEvent = new SearchEvent("", SearchRemoteOperation.SearchType.SHARED_FILTER);
        MainApp.showOnlyFilesOnDevice(false);

        launchActivityForSearch(searchEvent, menuItem.getItemId());
    }

    private void startRecentlyModifiedSearch(MenuItem menuItem) {
        SearchEvent searchEvent = new SearchEvent("", SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH);
        MainApp.showOnlyFilesOnDevice(false);

        launchActivityForSearch(searchEvent, menuItem.getItemId());
    }

    public void startPhotoSearch(int id) {
        SearchEvent searchEvent = new SearchEvent("image/%", SearchRemoteOperation.SearchType.PHOTO_SEARCH);
        MainApp.showOnlyFilesOnDevice(false);

        launchActivityForSearch(searchEvent, id);
    }

    private void handleSearchEvents(SearchEvent searchEvent, int menuItemId) {
        if (this instanceof FileDisplayActivity) {
            final Fragment leftFragment = ((FileDisplayActivity) this).getLeftFragment();
            if (leftFragment instanceof GalleryFragment || leftFragment instanceof SharedListFragment) {
                launchActivityForSearch(searchEvent, menuItemId);
            } else {
                EventBus.getDefault().post(searchEvent);
            }
        } else {
            launchActivityForSearch(searchEvent, menuItemId);
        }
    }

    private void launchActivityForSearch(SearchEvent searchEvent, int menuItemId) {
        DrawerActivity.menuItemId = menuItemId;
        Intent intent = new Intent(getApplicationContext(), FileDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (this instanceof ComposeActivity) {
            //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        intent.setAction(Intent.ACTION_SEARCH);
        intent.putExtra(OCFileListFragment.SEARCH_EVENT, searchEvent);
        startActivity(intent);
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
            if (menuItem.getTitle().toString().equalsIgnoreCase(link.getName())) {
                if (link.getRedirect()) {
                    DisplayUtils.startLinkIntent(this, link.getUrl());
                } else {
                    Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, link.getName());
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, link.getUrl());
                    externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, true);
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

    public void toggleDrawer() {
        if (isDrawerOpen()) {
            closeDrawer();
        } else {
            openDrawer();
        }
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
     * Updates title bar and home buttons (state and icon). Assumes that navigation drawer is NOT visible.
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        super.updateActionBarTitleAndHomeButton(chosenFile);

        // set home button properties
        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(chosenFile != null && isRoot(chosenFile));
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

        if (relative < RELATIVE_THRESHOLD_WARNING) {
            viewThemeUtils.material.colorProgressBar(mQuotaProgressBar);
        } else {
            viewThemeUtils.material.colorProgressBar(mQuotaProgressBar,
                                                     getResources().getColor(R.color.infolevel_warning, getTheme()));
        }

        updateQuotaLink();
        showQuota(true);
    }

    private void unsetAllDrawerMenuItems() {
        if (drawerNavigationView != null) {
            drawerNavigationView.getMenu();
            Menu menu = drawerNavigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setChecked(false);
            }
        }

        menuItemId = Menu.NONE;
    }

    private void updateQuotaLink() {
        if (mQuotaTextLink != null) {
            if (MDMConfig.INSTANCE.externalSiteSupport(this)) {
                List<ExternalLink> quotas = externalLinksProvider.getExternalLink(ExternalLinkType.QUOTA);

                float density = getResources().getDisplayMetrics().density;
                final int size = Math.round(24 * density);

                if (!quotas.isEmpty()) {
                    final ExternalLink firstQuota = quotas.get(0);
                    mQuotaTextLink.setText(firstQuota.getName());
                    mQuotaTextLink.setClickable(true);
                    mQuotaTextLink.setVisibility(View.VISIBLE);
                    mQuotaTextLink.setOnClickListener(v -> {
                        Intent externalWebViewIntent = new Intent(getApplicationContext(), ExternalSiteWebView.class);
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, firstQuota.getName());
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, firstQuota.getUrl());
                        externalWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, true);
                        menuItemId = Menu.NONE;
                        startActivity(externalWebViewIntent);
                    });

                    Target<Drawable> quotaTarget = createQuotaDrawableTarget(size, mQuotaTextLink);
                    getClientRepository().getNextcloudClient(nextcloudClient -> {
                        GlideHelper.INSTANCE.loadIntoTarget(this,
                                                            nextcloudClient,
                                                            firstQuota.getIconUrl(),
                                                            quotaTarget,
                                                            R.drawable.ic_link);
                        return Unit.INSTANCE;
                    });
                } else {
                    mQuotaTextLink.setVisibility(View.GONE);
                }
            } else {
                mQuotaTextLink.setVisibility(View.GONE);
            }
        }
    }

    private Target<Drawable> createQuotaDrawableTarget(int size, TextView quotaTextLink) {
        return new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                Drawable drawable = resource.getCurrent();
                drawable.setBounds(0, 0, size, size);
                quotaTextLink.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);

                Drawable drawable = errorDrawable != null ? errorDrawable.getCurrent() : null;
                if (drawable != null) {
                    drawable.setBounds(0, 0, size, size);
                    quotaTextLink.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                }
            }
        };
    }


    /**
     * Sets the menu item as checked in both the drawer and bottom navigation views, if applicable.
     */
    @SuppressFBWarnings("RV")
    public void setNavigationViewItemChecked() {
        if (drawerNavigationView != null) {
            MenuItem menuItem = drawerNavigationView.getMenu().findItem(menuItemId);

            if (menuItem != null && !menuItem.isChecked()) {
                viewThemeUtils.platform.colorNavigationView(drawerNavigationView);
                menuItem.setChecked(true);
            }
        }

        if (bottomNavigationView != null) {
            MenuItem menuItem = bottomNavigationView.getMenu().findItem(menuItemId);

            // Don't highlight assistant bottom navigation item because Assistant screen doesn't have same bottom navigation bar
            if (menuItem != null && !menuItem.isChecked() && menuItem.getItemId() != R.id.nav_assistant) {
                menuItem.setChecked(true);
            }
        }

        Log_OC.d(TAG, "New menu item is: " + menuItemId);
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
        if (drawerNavigationView == null || !MDMConfig.INSTANCE.externalSiteSupport(this)) {
            return;
        }

        drawerNavigationView.getMenu().removeGroup(R.id.drawer_menu_external_links);

        int greyColor = ContextCompat.getColor(this, R.color.drawer_menu_icon);

        for (final ExternalLink link : externalLinksProvider.getExternalLink(ExternalLinkType.LINK)) {
            int id = drawerNavigationView
                .getMenu()
                .add(R.id.drawer_menu_external_links,
                     MENU_ITEM_EXTERNAL_LINK +
                         link.getId(), MENU_ORDER_EXTERNAL_LINKS,
                     link.getName()
                    )
                .setCheckable(true)
                .getItemId();

            Target<Drawable> iconTarget = createMenuItemTarget(id, greyColor);
            getClientRepository().getNextcloudClient(nextcloudClient -> {
                GlideHelper.INSTANCE.loadIntoTarget(
                    this,
                    nextcloudClient,
                    link.getIconUrl(),
                    iconTarget,
                    R.drawable.ic_link);
                return Unit.INSTANCE;
            });
        }
    }

    private Target<Drawable> createMenuItemTarget(int menuItemId, int tintColor) {
        return new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                setExternalLinkIcon(menuItemId, resource, tintColor);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                setExternalLinkIcon(menuItemId, errorDrawable, tintColor);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        };
    }

    private void setExternalLinkIcon(int id, Drawable drawable, int greyColor) {
        MenuItem menuItem = drawerNavigationView.getMenu().findItem(id);
        if (menuItem == null) {
            return;
        }

        if (drawable == null) {
            menuItem.setIcon(R.drawable.ic_link);
            return;
        }

        final var resizedDrawable = DrawableUtil.INSTANCE.getResizedDrawable(this, drawable,32);
        menuItem.setIcon(viewThemeUtils.platform.colorDrawable(resizedDrawable, greyColor));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
        }

        externalLinksProvider = new ExternalLinksProvider(getContentResolver());
        arbitraryDataProvider = new ArbitraryDataProviderImpl(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, mIsAccountChooserActive);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
        setNavigationViewItemChecked();
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
        Fragment fileDetailsSharingProcessFragment =
            getSupportFragmentManager().findFragmentByTag(FileDetailsSharingProcessFragment.TAG);
        if (fileDetailsSharingProcessFragment != null) {
            ((FileDetailsSharingProcessFragment) fileDetailsSharingProcessFragment).onBackPressed();
        } else {
            super.onBackPressed();
        }
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
        } else if (requestCode == PassCodeManager.PASSCODE_ACTIVITY && data != null) {
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

    private void resetOnlyPersonalAndOnDevice() {
        MainApp.showOnlyFilesOnDevice(false);
        MainApp.showOnlyPersonalFiles(false);
    }

    /**
     * show the file list to the user.
     *
     * @param onDeviceOnly flag to decide if all files or only the ones on the device should be shown
     */
    public void showFiles(boolean onDeviceOnly, boolean onlyPersonalFiles) {
        MainApp.showOnlyFilesOnDevice(onDeviceOnly);
        MainApp.showOnlyPersonalFiles(onlyPersonalFiles);
        Intent fileDisplayActivity = new Intent(getApplicationContext(), FileDisplayActivity.class);
        fileDisplayActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (this instanceof ComposeActivity) {
            fileDisplayActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        fileDisplayActivity.setAction(FileDisplayActivity.ALL_FILES);
        startActivity(fileDisplayActivity);
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        if (callContext instanceof MenuItem menuItem) {
            MenuItem newIcon = menuItem.setIcon(avatarDrawable);
            Log_OC.d(TAG,"avatarGenerated new icon: " + newIcon);
        } else if (callContext instanceof ImageView imageView) {
            imageView.setImageDrawable(avatarDrawable);
        } else if (callContext instanceof MaterialButton materialButton) {
            materialButton.setIcon(avatarDrawable);
        }
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        if (callContext instanceof MenuItem menuItem) {
            return String.valueOf(menuItem.getTitle()).equals(tag);
        } else if (callContext instanceof ImageView imageView) {
            return String.valueOf(imageView.getTag()).equals(tag);
        } else if (callContext instanceof MaterialButton materialButton) {
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

    public AppPreferences getAppPreferences() {
        return preferences;
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
        if (!MDMConfig.INSTANCE.externalSiteSupport(this)) {
            return;
        }

        User user = accountManager.getUser();
        if (user.isAnonymous()) {
            Log_OC.d(TAG, "Trying to execute a sync operation with a storage manager for an anonymous account");
            return;
        }

        Thread t = new Thread(() -> {
            // fetch capabilities as early as possible
            final OCCapability capability = getCapabilities();
            if ((capability == null || capability.getAccountName() == null || !capability.getAccountName().isEmpty())
                && getStorageManager() != null) {
                GetCapabilitiesOperation getCapabilities = new GetCapabilitiesOperation(getStorageManager());
                getCapabilities.execute(getBaseContext());
            }

            if (getStorageManager() != null && CapabilityUtils.getCapability(user, this)
                .getExternalLinks().isTrue()) {

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
                    RemoteOperationResult result = getExternalLinksOperation.execute(user, this);

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

    protected void handleDeepLink(@NonNull Uri uri) {
        String path = uri.getLastPathSegment();
        if (path == null) return;

        DeepLinkConstants deepLinkType = DeepLinkConstants.Companion.fromPath(path);
        if (deepLinkType == null) {
            DisplayUtils.showSnackMessage(this, getString(R.string.invalid_url));
            return;
        }

        switch (deepLinkType) {
            case OPEN_AUTO_UPLOAD:
                startActivity(new Intent(this, SyncedFoldersActivity.class));
                break;
            case OPEN_EXTERNAL_URL:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri.getQueryParameter("url")));
                startActivity(intent);
                break;
            case ACTION_CREATE_NEW:
                findViewById(R.id.fab_main).callOnClick();
                break;
            case ACTION_APP_UPDATE:
                LinkHelper.INSTANCE.openAppStore(getPackageName(), false, this);
                break;
            case OPEN_NOTIFICATIONS:
                startActivity(NotificationsActivity.class);
                break;
            default:
                handleNavItemClickEvent(deepLinkType.getNavId());
                break;
        }
    }

    private void handleNavItemClickEvent(@IdRes int menuItemId) {
        if (drawerNavigationView == null) {
            drawerNavigationView = findViewById(R.id.nav_view);
        }
        Menu navMenu = drawerNavigationView.getMenu();
        onNavigationItemClicked(navMenu.findItem(menuItemId));
    }

    public void showBottomNavigationBar(boolean show) {
        ViewExtensionsKt.setVisibleIf(bottomNavigationView, show);
    }

    public BottomNavigationView getBottomNavigationView() {
       return bottomNavigationView;
    }
}
