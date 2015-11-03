/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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
 *
 */

package com.owncloud.android.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.providers.UsersAndGroupsSearchProvider;

import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.fragment.SearchShareesFragment;
import com.owncloud.android.ui.fragment.ShareFileFragment;
import com.owncloud.android.utils.GetShareWithUsersAsyncTask;


/**
 * Activity for sharing files
 */

public class ShareActivity extends FileActivity
        implements ShareFileFragment.OnShareFragmentInteractionListener,
        SearchShareesFragment.OnSearchFragmentInteractionListener {

    private static final String TAG = ShareActivity.class.getSimpleName();

    private static final String TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT";
    private static final String TAG_SEARCH_FRAGMENT = "SEARCH_USER_AND_GROUPS_FRAGMENT";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.share_activity);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (savedInstanceState == null) {
            // Add Share fragment on first creation
            Fragment fragment = ShareFileFragment.newInstance(getFile(), getAccount());
            ft.replace(R.id.share_fragment_container, fragment, TAG_SHARE_FRAGMENT);
            ft.commit();
        }

    }

    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);

        // Load data into the list
        Log_OC.d(TAG, "Refreshing lists on account set");
        refreshSharesFromStorageManager();

        // Request for a refresh of the data through the server (starts an Async Task)
        refreshUsersOrGroupsListFromServer();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        // Verify the action and get the query
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log_OC.w(TAG, "Ignored Intent requesting to query for " + query);

        } else if (UsersAndGroupsSearchProvider.ACTION_SHARE_WITH.equals(intent.getAction())) {
            Uri data = intent.getData();
            doShareWith(
                    data.getLastPathSegment(),
                    UsersAndGroupsSearchProvider.DATA_GROUP.equals(data.getAuthority())
            );

        } else {
            Log_OC.wtf(TAG, "Unexpected intent " + intent.toString());
        }
    }

    private void doShareWith(String shareeName, boolean isGroup) {
        getFileOperationsHelper().shareFileWithSharee(
                getFile(),
                shareeName,
                (isGroup ? ShareType.GROUP : ShareType.USER)
        );
    }

    @Override
    public void showSearchUsersAndGroups() {
        // replace ShareFragment with SearchFragment on demand
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment searchFragment = SearchShareesFragment.newInstance(getFile(), getAccount());
        ft.replace(R.id.share_fragment_container, searchFragment, TAG_SEARCH_FRAGMENT);
        ft.addToBackStack(null);    // BACK button will recover the ShareFragment
        ft.commit();
    }

    @Override
    // Call to Unshare operation
    public void unshareWith(OCShare share) {
        OCFile file = getFile();
        getFileOperationsHelper().unshareFileWithUserOrGroup(file, share.getShareType(), share.getShareWith());
    }

    /**
     * Get users and groups from the server to fill in the "share with" list
     */
    @Override
    public void refreshUsersOrGroupsListFromServer() {
        // Show loading
        showLoadingDialog(getString(R.string.common_loading));
        // Get Users and Groups
        GetShareWithUsersAsyncTask getTask = new GetShareWithUsersAsyncTask(this);
        Object[] params = {getFile(), getAccount(), getStorageManager()};
        getTask.execute(params);
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files
     * in the current account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (result.isSuccess()) {
            Log_OC.d(TAG, "Refreshing lists on successful sync");
            refreshSharesFromStorageManager();
        }

    }

    private void refreshSharesFromStorageManager() {
        ShareFileFragment shareFileFragment = getShareFileFragment();
        if (shareFileFragment != null) {          // only if added to the view hierarchy!!
            if (shareFileFragment.isAdded()) {
                shareFileFragment.refreshUsersOrGroupsListFromDB();
                shareFileFragment.refreshPublicShareFromDB();
            }
        }

        SearchShareesFragment searchShareesFragment = getSearchFragment();
        if (searchShareesFragment != null) {
            if (searchShareesFragment.isAdded()) {  // only if added to the view hierarchy!!
                searchShareesFragment.refreshUsersOrGroupsListFromDB();
            }
        }
    }

    /**
     * Shortcut to get access to the {@link ShareFileFragment} instance, if any
     *
     * @return  A {@link ShareFileFragment} instance, or null
     */
    private ShareFileFragment getShareFileFragment() {
        return (ShareFileFragment) getSupportFragmentManager().findFragmentByTag(TAG_SHARE_FRAGMENT);
    }

    /**
     * Shortcut to get access to the {@link SearchShareesFragment} instance, if any
     *
     * @return  A {@link SearchShareesFragment} instance, or null
     */
    private SearchShareesFragment getSearchFragment() {
        return (SearchShareesFragment) getSupportFragmentManager().findFragmentByTag(TAG_SEARCH_FRAGMENT);
    }

}
