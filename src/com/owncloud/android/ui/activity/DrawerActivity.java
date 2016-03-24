package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.utils.BitmapUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Base class to handle setup of the drawer implementation.
 */
public abstract class DrawerActivity extends ToolbarActivity {
    private static final String TAG = DrawerActivity.class.getSimpleName();

    // Navigation Drawer
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;
    private ImageView mAccountChooserToggle;

    private List<Integer> mAccountMenuItemIds = new ArrayList<Integer>();
    private Account mCurrentAccount;

    private boolean mIsAccountChooserActive;

    /**
     * Initializes the drawer and its content. This method needs to be called after the content view has been set.
     */
    protected void setupDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            setupDrawerContent(mNavigationView);
            mAccountChooserToggle = (ImageView) findNavigationViewChildById(R.id.drawer_account_chooser_toogle);
            mAccountChooserToggle.setImageResource(R.drawable.ic_down);
            mIsAccountChooserActive = false;

            findNavigationViewChildById(R.id.drawer_active_user)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            toggleAccountList();
                        }
                    });
        }

        // TODO re-enable when "Accounts" is available in Navigation Drawer
//        // load Account in the Drawer Title
//        // User-Icon
//        ImageView userIcon = (ImageView) navigationDrawerLayout.findViewById(R.id.drawer_userIcon);
//        userIcon.setImageResource(DisplayUtils.getSeasonalIconId());
//
//        // Username
//        TextView username = (TextView) navigationDrawerLayout.findViewById(R.id.drawer_username);
//        Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
//
//        if (account != null) {
//            int lastAtPos = account.name.lastIndexOf("@");
//            username.setText(account.name.substring(0, lastAtPos));
//        }
/*
        // Display username in drawer
        setUsernameInDrawer(navigationDrawerLayout, AccountUtils.getCurrentOwnCloudAccount(getApplicationContext()));

        // load slide menu items
        mDrawerTitles = getResources().getStringArray(R.array.drawer_items);

        // nav drawer content description from resources
        mDrawerContentDescriptions = getResources().
                getStringArray(R.array.drawer_content_descriptions);

        // nav drawer items
        mDrawerItems = new ArrayList<NavigationDrawerItem>();
        // adding nav drawer items to array
        // TODO re-enable when "Accounts" is available in Navigation Drawer
        // Accounts
        // mDrawerItems.add(new NavigationDrawerItem(mDrawerTitles[0],
        // mDrawerContentDescriptions[0]));
        // All Files
        mDrawerItems.add(new NavigationDrawerItem(mDrawerTitles[0], mDrawerContentDescriptions[0],
                R.drawable.ic_folder_open));

        // TODO Enable when "On Device" is recovered
        // On Device
        //mDrawerItems.add(new NavigationDrawerItem(mDrawerTitles[2],
        //        mDrawerContentDescriptions[2]));
*/
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                updateActionBarTitleAndHomeButton(null);
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(R.string.app_name);
                mDrawerToggle.setDrawerIndicatorEnabled(true);
                invalidateOptionsMenu();
            }
        };
        /*
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
*/
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
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
                                allFilesOption();
                                break;
                            case R.id.nav_settings:
                                Intent settingsIntent = new Intent(getApplicationContext(),
                                        Preferences.class);
                                startActivity(settingsIntent);
                                break;
                            case R.id.drawer_menu_account_add:
                                AccountManager am = AccountManager.get(getApplicationContext());
                                am.addAccount(MainApp.getAccountType(), null, null, null, DrawerActivity.this,
                                        null, null);
                            case R.id.drawer_menu_account_manage:
                                Toast.makeText(getApplicationContext(), "Not implemented yet", Toast.LENGTH_SHORT);
                        }

                        return true;
                    }
                });

        // hide accounts
        mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
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
    // TODO call updateAccountList() after n.o. accounts changed
    public void updateAccountList() {
        AccountManager am = (AccountManager) this.getSystemService(this.ACCOUNT_SERVICE);

        // populate UI
        repopulateAccountList(am.getAccountsByType(MainApp.getAccountType()));
        setUsernameInDrawer(AccountUtils.getCurrentOwnCloudAccount(this).name);
    }

    /**
     * re-populates the account list.
     *
     * @param accounts list of accounts
     */
    private void repopulateAccountList(Account[] accounts) {
        // remove all accounts from list
        mNavigationView.getMenu().removeItem(Menu.NONE);

        // add all accounts to list
        for (int i = 0; i < accounts.length; i++) {
            try {
                int[] rgb = calculateRGB(accounts[i].name);
                TextDrawable icon = new TextDrawable(accounts[i].name.substring(0, 1).toUpperCase()
                        , rgb[0], rgb[1], rgb[2]);
                mNavigationView.getMenu().add(R.id.drawer_menu_accounts, Menu.NONE, 0, accounts[i].name).setIcon(icon);
            } catch (Exception e) {
                Log_OC.e(TAG, "Error calculating RGB value for account menu item.", e);
                mNavigationView.getMenu().add(R.id.drawer_menu_accounts, Menu.NONE, 0, accounts[i].name).setIcon(R
                        .drawable.ic_account_circle);
            }
        }

        // adding sets menu group back to visible, so safety check and setting invisible
        if (!mIsAccountChooserActive) {
            mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
        }
    }

    private int[] calculateRGB(String accountName) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // using adapted algorithm from /core/js/placeholder.js:50
        int lastAtPos = accountName.lastIndexOf("@");
        String username = accountName.substring(0, lastAtPos);
        byte[] seed = username.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
//                        Integer seedMd5Int = Math.abs(new String(Hex.encodeHex(seedMd5))
//                      .hashCode());
        Integer seedMd5Int = String.format(Locale.ROOT, "%032x",
                new BigInteger(1, md.digest(seed))).hashCode();

        double maxRange = Integer.MAX_VALUE;
        float hue = (float) (seedMd5Int / maxRange * 360);

        return BitmapUtils.HSLtoRGB(hue, 90.0f, 65.0f, 1.0f);
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
        mDrawerToggle.setDrawerIndicatorEnabled(isRoot(chosenFile));
    }

    /**
     * sets the given account name in the drawer in case the drawer is available. The account name is shortened
     * beginning from the @-sign in the username.
     *
     * @param accountName the account to be set in the drawer
     */
    protected void setUsernameInDrawer(String accountName) {
        if (mDrawerLayout != null && accountName != null) {
            TextView username = (TextView) ((NavigationView) findViewById(R.id.nav_view))
                    .getHeaderView(0).findViewById(R.id.drawer_username);
            TextView usernameFull = (TextView) ((NavigationView) findViewById(R.id.nav_view))
                    .getHeaderView(0).findViewById(R.id.drawer_username_full);
            usernameFull.setText(accountName);
            int lastAtPos = accountName.lastIndexOf("@");
            username.setText(accountName.substring(0, lastAtPos));

            ImageView usericon = (ImageView) ((NavigationView) findViewById(R.id.nav_view))
                    .getHeaderView(0).findViewById(R.id.drawer_usericon);
            try {
                int[] rgb = calculateRGB(accountName);
                TextDrawable icon = new TextDrawable(
                        accountName.substring(0, 1).toUpperCase(), rgb[0], rgb[1], rgb[2]);
                usericon.setImageDrawable(icon);
            } catch (Exception e) {
                Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e);
                usericon.setImageResource(R.drawable.ic_account_circle);
            }
        }
    }

    /**
     * Toggle between drawer menu and account list.
     */
    private void toggleAccountList() {
        if (mIsAccountChooserActive) {
            // TODO close accounts list and display drawer menu again
            mAccountChooserToggle.setImageResource(R.drawable.ic_down);
            mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, false);
            mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_standard, true);

        } else {
            // TODO show accounts list
            mAccountChooserToggle.setImageResource(R.drawable.ic_up);
            mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_accounts, true);
            mNavigationView.getMenu().setGroupVisible(R.id.drawer_menu_standard, false);
        }

        mIsAccountChooserActive = !mIsAccountChooserActive;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
            if (isDrawerOpen()) {
                getSupportActionBar().setTitle(R.string.app_name);
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

    /**
     * Finds a view that was identified by the id attribute from the drawer header.
     *
     * @param id the view's id
     * @return The view if found or <code>null</code> otherwise.
     */
    private View findNavigationViewChildById(int id) {
        return ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(id);
    }
}
