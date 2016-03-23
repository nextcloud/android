package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;

/**
 * Base class to handle setup of the drawer implementation.
 */
public abstract class DrawerActivity extends ToolbarActivity {
    // Navigation Drawer
    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;

    /**
     * Initializes the drawer and its content. This method needs to be called after the content view has been set.
     */
    protected void setupDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupDrawerContent(navigationView);
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
                        }

                        return true;
                    }
                });
    }

    /**
     * checks if the drawer exists and is opened.
     *
     * @return <code>true</code> if the drawer is open, else <code>false</code>
     */
    public boolean isDrawerOpen() {
        if(mDrawerLayout != null) {
            return mDrawerLayout.isDrawerOpen(GravityCompat.START);
        } else {
            return false;
        }
    }

    /**
     * closes the navigation drawer.
     */
    public void closeNavDrawer() {
        if(mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
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
     * @param accountName            the account to be set in the drawer
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
        }
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
            closeNavDrawer();
            return;
        }
        super.onBackPressed();
    }
}
