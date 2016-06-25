/**
 *   ownCloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.utils.BitmapUtils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Base class to handle setup of the drawer implementation including user switching and avatar fetching and fallback
 * generation.
 */
public abstract class DrawerActivity extends ToolbarActivity {
    private static final String TAG = DrawerActivity.class.getSimpleName();
    private static final String KEY_IS_ACCOUNT_CHOOSER_ACTIVE = "IS_ACCOUNT_CHOOSER_ACTIVE";
    private static final String KEY_CHECKED_MENU_ITEM = "CHECKED_MENU_ITEM";
    private static final int ACTION_MANAGE_ACCOUNTS = 101;
    private static final int MENU_ORDER_ACCOUNT = 1;
    private static final int MENU_ORDER_ACCOUNT_FUNCTION = 2;

    /**
     * Reference to the drawer layout.
     */
    private DrawerLayout mDrawerLayout;

    /**
     * Reference to the drawer toggle.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    /**
     * Reference to the navigation view.
     */
    private NavigationView mNavigationView;

    /**
     * Reference to the account chooser toggle.
     */
    private ImageView mAccountChooserToggle;

    /**
     * Reference to the current account avatar.
     */
    private ImageView mAccountCurrentAccountAvatar;

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
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            mAccountChooserToggle = (ImageView) findNavigationViewChildById(R.id.drawer_account_chooser_toogle);
            mAccountChooserToggle.setImageResource(R.drawable.ic_down);
            mIsAccountChooserActive = false;

            mAccountCurrentAccountAvatar = (ImageView) findNavigationViewChildById(R.id.drawer_current_account);
            mAccountMiddleAccountAvatar = (ImageView) findNavigationViewChildById(R.id.drawer_account_middle);
            mAccountEndAccountAvatar = (ImageView) findNavigationViewChildById(R.id.drawer_account_end);

            setupDrawerContent(mNavigationView);

            findNavigationViewChildById(R.id.drawer_active_user)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            toggleAccountList();
                        }
                    });
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                // standard behavior of drawer is to switch to the standard menu on closing
                if (mIsAccountChooserActive) {
                    toggleAccountList();
                }
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                invalidateOptionsMenu();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * setup drawer content, basically setting the item selected listener.
     *
     * @param navigationView the drawers navigation view
     */
    protected void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        mDrawerLayout.closeDrawers();

                        switch (menuItem.getItemId()) {
                            case R.id.nav_all_files:
                                menuItem.setChecked(true);
                                mCheckedMenuItem = menuItem.getItemId();

                                allFilesOption();
                                break;
                            case R.id.nav_uploads:
                                Intent uploadListIntent = new Intent(getApplicationContext(),
                                        UploadListActivity.class);
                                uploadListIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(uploadListIntent);
                                break;
                            case R.id.nav_settings:
                                Intent settingsIntent = new Intent(getApplicationContext(),
                                        Preferences.class);
                                startActivity(settingsIntent);
                                break;
                            case R.id.drawer_menu_account_add:
                                createAccount();
                                break;
                            case R.id.drawer_menu_account_manage:
                                Intent manageAccountsIntent = new Intent(getApplicationContext(),
                                        ManageAccountsActivity.class);
                                startActivityForResult(manageAccountsIntent, ACTION_MANAGE_ACCOUNTS);
                                break;
                            case Menu.NONE:
                                // account clicked
                                accountClicked(menuItem.getTitle().toString());
                            default:
                                Log_OC.i(TAG, "Unknown drawer menu item clicked: " + menuItem.getTitle());
                        }

                        return true;
                    }
                });

        // handle correct state
        if (mIsAccountChooserActive) {
            mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, true);
        } else {
            mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
        }
    }

    /**
     * sets the new/current account and restarts. In case the given account equals the actual/current account the
     * call will be ignored.
     *
     * @param accountName The account name to be set
     */
    private void accountClicked(String accountName) {
        if (!AccountUtils.getCurrentOwnCloudAccount(getApplicationContext()).name.equals(accountName)) {
            AccountUtils.setCurrentOwnCloudAccount(getApplicationContext(), accountName);
            restart();
        }
    }

    /**
     * click method for mini avatars in drawer header.
     *
     * @param view the clicked ImageView
     */
    public void onAccountDrawerClick(View view) {
        accountClicked(view.getContentDescription().toString());
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
     * @param enable <code>true</code> to enable, <code>false</code> to disable
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
        Account[] accounts = AccountManager.get(this).getAccountsByType(MainApp.getAccountType());
        if (accounts.length > 0 && mNavigationView != null) {
            repopulateAccountList(accounts);
            setAccountInDrawer(AccountUtils.getCurrentOwnCloudAccount(this));
            populateDrawerOwnCloudAccounts();

            // activate second/end account avatar
            if(mAvatars[1] != null) {
                setAvatar(mAvatars[1], R.id.drawer_account_end);
                mAccountEndAccountAvatar.setVisibility(View.VISIBLE);
            } else {
                mAccountEndAccountAvatar.setVisibility(View.GONE);
            }

            // activate third/middle account avatar
            if(mAvatars[2] != null) {
                setAvatar(mAvatars[2], R.id.drawer_account_middle);
                mAccountMiddleAccountAvatar.setVisibility(View.VISIBLE);
            } else {
                mAccountMiddleAccountAvatar.setVisibility(View.GONE);
            }
        } else {
            mAccountEndAccountAvatar.setVisibility(View.GONE);
            mAccountMiddleAccountAvatar.setVisibility(View.GONE);
        }
    }

    /**
     * re-populates the account list.
     *
     * @param accounts list of accounts
     */
    private void repopulateAccountList(Account[] accounts) {
        // remove all accounts from list
        mNavigationView.getMenu().removeGroup(R.id.drawer_menu_accounts);

        // add all accounts to list
        for (int i = 0; i < accounts.length; i++) {
            try {
                mNavigationView.getMenu().add(
                        R.id.drawer_menu_accounts,
                        Menu.NONE,
                        MENU_ORDER_ACCOUNT,
                        accounts[i].name)
                        .setIcon(createAvatar(accounts[i].name, 16));
            } catch (Exception e) {
                Log_OC.e(TAG, "Error calculating RGB value for account menu item.", e);
                mNavigationView.getMenu().add(
                        R.id.drawer_menu_accounts,
                        Menu.NONE,
                        MENU_ORDER_ACCOUNT,
                        accounts[i].name)
                        .setIcon(R.drawable.ic_account_circle);
            }
        }

        // re-add add-account and manage-accounts
        mNavigationView.getMenu().add(R.id.drawer_menu_accounts, R.id.drawer_menu_account_add,
                MENU_ORDER_ACCOUNT_FUNCTION,
                getResources().getString(R.string.prefs_add_account)).setIcon(R.drawable.ic_account_plus);
        mNavigationView.getMenu().add(R.id.drawer_menu_accounts, R.id.drawer_menu_account_manage,
                MENU_ORDER_ACCOUNT_FUNCTION,
                getResources().getString(R.string.drawer_manage_accounts)).setIcon(R.drawable.ic_settings);

        // adding sets menu group back to visible, so safety check and setting invisible
        showMenu();
    }

    /**
     * Method that gets called on drawer menu click for 'All Files'.
     */
    public abstract void allFilesOption();

    /**
     * Updates title bar and home buttons (state and icon).
     * <p/>
     * Assumes that navigation drawer is NOT visible.
     */
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        super.updateActionBarTitleAndHomeButton(chosenFile);

        /// set home button properties
        if(mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(isRoot(chosenFile));
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
            usernameFull.setText(account.name);
            int lastAtPos = account.name.lastIndexOf("@");
            username.setText(account.name.substring(0, lastAtPos));

            setAvatar(account, R.id.drawer_current_account);
        }
    }

    /**
     * fetches and sets the avatar of the current account in the drawer in case the drawer is available.
     *
     * @param account the account to be set in the drawer
     */
    private void setAvatar(Account account, int avatarViewId) {
        if (mDrawerLayout != null && account != null) {
            int lastAtPos = account.name.lastIndexOf("@");
            String username = account.name.substring(0, lastAtPos);

            ImageView userIcon = (ImageView) findNavigationViewChildById(avatarViewId);
            userIcon.setContentDescription(account.name);

            // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache("a_" + username);

            if (thumbnail != null) {
                RoundedBitmapDrawable roundedAvatar = RoundedBitmapDrawableFactory.create
                        (MainApp.getAppContext().getResources(), thumbnail);
                roundedAvatar.setCircular(true);
                userIcon.setImageDrawable(roundedAvatar);
            } else {
                // generate new avatar
                if (ThumbnailsCacheManager.cancelPotentialAvatarWork(username, userIcon)) {
                    final ThumbnailsCacheManager.AvatarGenerationTask task =
                            new ThumbnailsCacheManager.AvatarGenerationTask(
                                    userIcon, getStorageManager(), account
                            );
                    if (thumbnail == null) {
                        try {
                            userIcon.setImageDrawable(
                                    createAvatar(
                                            account.name,
                                            getResources().getDimension(R.dimen.nav_drawer_header_avatar_radius)
                                    )
                            );
                        } catch (Exception e) {
                            Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e);
                            userIcon.setImageResource(R.drawable.ic_account_circle);
                        }
                    } else {
                        final ThumbnailsCacheManager.AsyncAvatarDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncAvatarDrawable(
                                        getResources(),
                                        thumbnail,
                                        task
                                );
                        RoundedBitmapDrawable roundedAvatar = RoundedBitmapDrawableFactory.create
                                (MainApp.getAppContext().getResources(), asyncDrawable.getBitmap());
                        roundedAvatar.setCircular(true);
                        userIcon.setImageDrawable(roundedAvatar);
                    }
                    task.execute(username);
                }
            }
        }
    }

    /**
     * creates an avatar in form of  a TextDrawable with the first letter of the account name in a circle with the
     * given radius.
     *
     * @param accountName the account name
     * @param radiusInDp  the circle's radius
     * @return the avatar as a TextDrawable
     * @throws UnsupportedEncodingException if the charset is not supported when calculating the color values
     * @throws NoSuchAlgorithmException     if the specified algorithm is not available when calculating the color values
     */
    @NonNull
    private TextDrawable createAvatar(String accountName, float radiusInDp) throws UnsupportedEncodingException,
            NoSuchAlgorithmException {
        int[] rgb = BitmapUtils.calculateRGB(accountName);
        float radiusInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                radiusInDp,
                getResources().getDisplayMetrics());
        return new TextDrawable(
                accountName.substring(0, 1).toUpperCase(), rgb[0], rgb[1], rgb[2], radiusInPx);
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
        if(mNavigationView != null) {
            if (mIsAccountChooserActive) {
                mAccountChooserToggle.setImageResource(R.drawable.ic_up);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, true);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_standard, false);
            } else {
                mAccountChooserToggle.setImageResource(R.drawable.ic_down);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
                mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_standard, true);
            }
        }
    }

    /**
     * checks/highlights the provided menu item if the drawer has been initialized and the menu item exists.
     *
     * @param menuItemId the menu item to be highlighted
     */
    protected void setDrawerMenuItemChecked(int menuItemId) {
        if (mNavigationView != null && mNavigationView.getMenu() != null && mNavigationView.getMenu().findItem
                (menuItemId) != null) {
            mNavigationView.getMenu().findItem(menuItemId).setChecked(true);
            mCheckedMenuItem = menuItemId;
        } else {
            Log_OC.w(TAG, "setDrawerMenuItemChecked has been called with invalid menu-item-ID");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mIsAccountChooserActive = savedInstanceState.getBoolean(KEY_IS_ACCOUNT_CHOOSER_ACTIVE, false);
            mCheckedMenuItem = savedInstanceState.getInt(KEY_CHECKED_MENU_ITEM, Menu.NONE);
        }
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
        if (requestCode == ACTION_MANAGE_ACCOUNTS
                && resultCode == RESULT_OK
                && data.getBooleanExtra(ManageAccountsActivity.KEY_ACCOUNT_LIST_CHANGED, false)) {

            // current account has changed
            if (data.getBooleanExtra(ManageAccountsActivity.KEY_CURRENT_ACCOUNT_CHANGED, false)) {
                setAccount(AccountUtils.getCurrentOwnCloudAccount(this));
                restart();
            } else {
                updateAccountList();
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
        return ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(id);
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
        Account[] accountsAll = AccountManager.get(this).getAccountsByType
                (MainApp.getAccountType());
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(this);

        mAvatars[0] = currentAccount;
        int j = 0;
        for(int i = 1 ; i <= 2 && i < accountsAll.length && j < accountsAll.length; j++) {
            if(!currentAccount.equals(accountsAll[j])) {
                mAvatars[i] = accountsAll[j];
                i++;
            }
        }
    }
}
