/**
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * Copyright (C) 2015 ownCloud Inc.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.CreateShareViaLinkOperation;
import com.owncloud.android.operations.GetSharesForFileOperation;
import com.owncloud.android.operations.UnshareOperation;
import com.owncloud.android.operations.UpdateSharePermissionsOperation;
import com.owncloud.android.ui.dialog.ShareLinkToDialog;
import com.owncloud.android.ui.fragment.EditShareFragment;
import com.owncloud.android.ui.fragment.SearchShareesFragment;
import com.owncloud.android.ui.fragment.ShareFileFragment;
import com.owncloud.android.ui.fragment.ShareFragmentListener;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.GetShareWithUsersAsyncTask;

import org.nextcloud.providers.UsersAndGroupsSearchProvider;


/**
 * Activity for sharing files
 */

public class ShareActivity extends FileActivity
        implements ShareFragmentListener {

    private static final String TAG = ShareActivity.class.getSimpleName();

    private static final String TAG_SHARE_FRAGMENT = "SHARE_FRAGMENT";
    private static final String TAG_SEARCH_FRAGMENT = "SEARCH_USER_AND_GROUPS_FRAGMENT";
    private static final String TAG_EDIT_SHARE_FRAGMENT = "EDIT_SHARE_FRAGMENT";
    private static final String TAG_PUBLIC_LINK = "PUBLIC_LINK";

    /// Tags for dialog fragments
    private static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG";
    private static final String FTAG_SHARE_PASSWORD_DIALOG = "SHARE_PASSWORD_DIALOG";


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
            String dataString = intent.getDataString();
            String shareWith = dataString.substring(dataString.lastIndexOf('/') + 1);
            doShareWith(
                    shareWith,
                    data.getAuthority()
            );

        } else {
            Log_OC.e(TAG, "Unexpected intent " + intent.toString());
        }
    }

    private void doShareWith(String shareeName, String dataAuthority) {

        ShareType shareType = UsersAndGroupsSearchProvider.getShareType(dataAuthority);

        getFileOperationsHelper().shareFileWithSharee(
                getFile(),
                shareeName,
                shareType,
                getAppropiatePermissions(shareType)
        );
    }


    private int getAppropiatePermissions(ShareType shareType) {

        // check if the Share is FERERATED
        boolean isFederated = ShareType.FEDERATED.equals(shareType);

        if (getFile().isSharedWithMe()) {
            return OCShare.READ_PERMISSION_FLAG;    // minimum permissions

        } else if (getFile().isFolder()) {
            return (isFederated) ? OCShare.FEDERATED_PERMISSIONS_FOR_FOLDER_UP_TO_OC9 :
                    OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER;

        } else {    // isFile
            return (isFederated) ? OCShare.FEDERATED_PERMISSIONS_FOR_FILE_UP_TO_OC9 :
                    OCShare.MAXIMUM_PERMISSIONS_FOR_FILE;
        }
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
    public void showEditShare(OCShare share) {
        // replace current fragment with EditShareFragment on demand
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment editShareFragment = EditShareFragment.newInstance(share, getFile(), getAccount());
        ft.replace(R.id.share_fragment_container, editShareFragment, TAG_EDIT_SHARE_FRAGMENT);
        ft.addToBackStack(null);    // BACK button will recover the previous fragment
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

        if (result.isSuccess() ||
                (operation instanceof GetSharesForFileOperation &&
                        result.getCode() == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND
                )
                ) {
            Log_OC.d(TAG, "Refreshing view on successful operation or finished refresh");
            refreshSharesFromStorageManager();
        }

        if (operation instanceof CreateShareViaLinkOperation) {
            onCreateShareViaLinkOperationFinish((CreateShareViaLinkOperation) operation, result);
        }

        if (operation instanceof UnshareOperation && result.isSuccess() && getEditShareFragment() != null) {
            getSupportFragmentManager().popBackStack();
        }

        if (operation instanceof UpdateSharePermissionsOperation
                && getEditShareFragment() != null && getEditShareFragment().isAdded()) {
            getEditShareFragment().onUpdateSharePermissionsFinished(result);
        }

    }


    /**
     * Updates the view, reading data from {@link com.owncloud.android.datamodel.FileDataStorageManager}
     */
    private void refreshSharesFromStorageManager() {

        ShareFileFragment shareFileFragment = getShareFileFragment();
        if (shareFileFragment != null
                && shareFileFragment.isAdded()) {   // only if added to the view hierarchy!!
            shareFileFragment.refreshCapabilitiesFromDB();
            shareFileFragment.refreshUsersOrGroupsListFromDB();
            shareFileFragment.refreshPublicShareFromDB();
        }

        SearchShareesFragment searchShareesFragment = getSearchFragment();
        if (searchShareesFragment != null &&
                searchShareesFragment.isAdded()) {  // only if added to the view hierarchy!!
            searchShareesFragment.refreshUsersOrGroupsListFromDB();
        }

        EditShareFragment editShareFragment = getEditShareFragment();
        if (editShareFragment != null &&
                editShareFragment.isAdded()) {
            editShareFragment.refreshUiFromDB();
        }

    }

    /**
     * Shortcut to get access to the {@link ShareFileFragment} instance, if any
     *
     * @return A {@link ShareFileFragment} instance, or null
     */
    private ShareFileFragment getShareFileFragment() {
        return (ShareFileFragment) getSupportFragmentManager().findFragmentByTag(TAG_SHARE_FRAGMENT);
    }

    /**
     * Shortcut to get access to the {@link SearchShareesFragment} instance, if any
     *
     * @return A {@link SearchShareesFragment} instance, or null
     */
    private SearchShareesFragment getSearchFragment() {
        return (SearchShareesFragment) getSupportFragmentManager().findFragmentByTag(TAG_SEARCH_FRAGMENT);
    }

    /**
     * Shortcut to get access to the {@link EditShareFragment} instance, if any
     *
     * @return A {@link EditShareFragment} instance, or null
     */
    private EditShareFragment getEditShareFragment() {
        return (EditShareFragment) getSupportFragmentManager().findFragmentByTag(TAG_EDIT_SHARE_FRAGMENT);
    }


    private void onCreateShareViaLinkOperationFinish(CreateShareViaLinkOperation operation,
                                                     RemoteOperationResult result) {
        if (result.isSuccess()) {
            updateFileFromDB();

            // Create dialog to allow the user choose an app to send the link
            Intent intentToShareLink = new Intent(Intent.ACTION_SEND);

            // if share to user and share via link multiple ocshares are returned,
            // therefore filtering for public_link
            String link = "";
            for (Object object : result.getData()) {
                OCShare shareLink = (OCShare) object;
                if (TAG_PUBLIC_LINK.equalsIgnoreCase(shareLink.getShareType().name())) {
                    link = shareLink.getShareLink();
                    break;
                }
            }

            intentToShareLink.putExtra(Intent.EXTRA_TEXT, link);
            intentToShareLink.setType("text/plain");
            String username = AccountUtils.getUsernameForAccount(getAccount());
            if (username != null) {
                intentToShareLink.putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(
                        R.string.subject_user_shared_with_you,
                        username,
                        getFile().getFileName()
                    )
                );
            } else {
                intentToShareLink.putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(
                        R.string.subject_shared_with_you,
                        getFile().getFileName()
                    )
                );
            }

            String[] packagesToExclude = new String[]{getPackageName()};
            DialogFragment chooserDialog = ShareLinkToDialog.newInstance(intentToShareLink, packagesToExclude);
            chooserDialog.show(getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);

        } else {
            // Detect Failure (403) --> maybe needs password
            String password = operation.getPassword();
            if (result.getCode() == RemoteOperationResult.ResultCode.SHARE_FORBIDDEN    &&
                    (password == null || password.length() == 0)                        &&
                    getCapabilities().getFilesSharingPublicEnabled().isUnknown()) {
                    // Was tried without password, but not sure that it's optional.

                // Try with password before giving up; see also ShareFileFragment#OnShareViaLinkListener
                ShareFileFragment shareFileFragment = getShareFileFragment();
                if (shareFileFragment != null
                    && shareFileFragment.isAdded()) {   // only if added to the view hierarchy!!

                    shareFileFragment.requestPasswordForShareViaLink(true);
                }

            } else {
                Toast t = Toast.makeText(this,
                    ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                    Toast.LENGTH_LONG);
                t.show();
            }
        }

    }


}
