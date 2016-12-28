/**
 * ownCloud Android client application
 *
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


package com.owncloud.android.providers;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.GetRemoteShareesOperation;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.utils.ErrorMessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * Content provider for search suggestions, to search for users and groups existing in an ownCloud server.
 */
public class UsersAndGroupsSearchProvider extends ContentProvider {

    private static final String TAG = UsersAndGroupsSearchProvider.class.getSimpleName();

    private static final String[] COLUMNS = {
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_ICON_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    private static final int SEARCH = 1;

    private static final int RESULTS_PER_PAGE = 50;
    private static final int REQUESTED_PAGE = 1;

    public static String AUTHORITY;
    public static String ACTION_SHARE_WITH;

    public static final String CONTENT = "content";

    public static String DATA_USER;
    public static String DATA_GROUP;
    public static String DATA_REMOTE;

    private UriMatcher mUriMatcher;

    private static HashMap<String, ShareType> sShareTypes = new HashMap<>();

    public static ShareType getShareType(String authority) {

        return sShareTypes.get(authority);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        // TODO implement
        return null;
    }

    @Override
    public boolean onCreate() {

        AUTHORITY = getContext().getResources().getString(R.string.users_and_groups_search_authority);
        ACTION_SHARE_WITH = getContext().getResources().getString(R.string.users_and_groups_share_with);
        DATA_USER = AUTHORITY + ".data.user";
        DATA_GROUP = AUTHORITY + ".data.group";
        DATA_REMOTE = AUTHORITY + ".data.remote";

        sShareTypes.put(DATA_USER, ShareType.USER);
        sShareTypes.put(DATA_GROUP, ShareType.GROUP);
        sShareTypes.put(DATA_REMOTE, ShareType.FEDERATED);

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH);
        return true;
    }

    /**
     * TODO description
     * <p/>
     * Reference: http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#CustomContentProvider
     *
     * @param uri           Content {@link Uri}, formattted as
     *                      "content://com.nextcloud.android.providers.UsersAndGroupsSearchProvider/" +
     *                      {@link android.app.SearchManager#SUGGEST_URI_PATH_QUERY} + "/" + 'userQuery'
     * @param projection    Expected to be NULL.
     * @param selection     Expected to be NULL.
     * @param selectionArgs Expected to be NULL.
     * @param sortOrder     Expected to be NULL.
     * @return              Cursor with users and groups in the ownCloud server that match 'userQuery'.
     */
    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log_OC.d(TAG, "query received in thread " + Thread.currentThread().getName());

        int match = mUriMatcher.match(uri);
        switch (match) {
            case SEARCH:
                return searchForUsersOrGroups(uri);

            default:
                return null;
        }
    }

    private Cursor searchForUsersOrGroups(Uri uri) {
        MatrixCursor response = null;


        String userQuery = uri.getLastPathSegment().toLowerCase();


        /// need to trust on the AccountUtils to get the current account since the query in the client side is not
        /// directly started by our code, but from SearchView implementation
        Account account = AccountUtils.getCurrentOwnCloudAccount(getContext());

        /// request to the OC server about users and groups matching userQuery
        GetRemoteShareesOperation searchRequest = new GetRemoteShareesOperation(
                userQuery, REQUESTED_PAGE, RESULTS_PER_PAGE
        );
        RemoteOperationResult result = searchRequest.execute(account, getContext());
        List<JSONObject> names = new ArrayList<JSONObject>();
        if (result.isSuccess()) {
            for (Object o : result.getData()) {
                // Get JSonObjects from response
                names.add((JSONObject) o);
            }
        } else {
            showErrorMessage(result);
        }

        /// convert the responses from the OC server to the expected format
        if (names.size() > 0) {
            response = new MatrixCursor(COLUMNS);
            Iterator<JSONObject> namesIt = names.iterator();
            JSONObject item;
            String displayName = null;
            int icon = 0;
            Uri dataUri = null;
            int count = 0;

            Uri userBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_USER).build();
            Uri groupBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_GROUP).build();
            Uri remoteBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_REMOTE).build();

            FileDataStorageManager manager = new FileDataStorageManager(account, getContext().getContentResolver());
            boolean federatedShareAllowed = manager.getCapability(account.name).getFilesSharingFederationOutgoing()
                    .isTrue();

            try {
                while (namesIt.hasNext()) {
                    item = namesIt.next();
                    String userName = item.getString(GetRemoteShareesOperation.PROPERTY_LABEL);
                    JSONObject value = item.getJSONObject(GetRemoteShareesOperation.NODE_VALUE);
                    int type = value.getInt(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE);
                    String shareWith = value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH);

                    if (ShareType.GROUP.getValue() == type) {
                        displayName = getContext().getString(R.string.share_group_clarification, userName);
                        icon = R.drawable.ic_group;
                        dataUri = Uri.withAppendedPath(groupBaseUri, shareWith);
                    } else if (ShareType.FEDERATED.getValue() == type && federatedShareAllowed) {
                        icon = R.drawable.ic_user;
                        if (userName.equals(shareWith)) {
                            displayName = getContext().getString(R.string.share_remote_clarification, userName);
                        } else {
                            String[] uriSplitted = shareWith.split("@");
                            displayName = getContext().getString(R.string.share_known_remote_clarification, userName,
                                uriSplitted[uriSplitted.length - 1]);
                        }
                        dataUri = Uri.withAppendedPath(remoteBaseUri, shareWith);
                    } else if (ShareType.USER.getValue() == type) {
                        displayName = userName;
                        icon = R.drawable.ic_user;
                        dataUri = Uri.withAppendedPath(userBaseUri, shareWith);
                    }

                    if (displayName != null && dataUri != null) {
                        response.newRow()
                            .add(count++)             // BaseColumns._ID
                            .add(displayName)         // SearchManager.SUGGEST_COLUMN_TEXT_1
                            .add(icon)                // SearchManager.SUGGEST_COLUMN_ICON_1
                            .add(dataUri);
                    }
                }

            } catch (JSONException e) {
                Log_OC.e(TAG, "Exception while parsing data of users/groups", e);
            }
        }

        return response;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO implementation
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO implementation
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO implementation
        return 0;
    }

    /**
     * Show error message
     *
     * @param result Result with the failure information.
     */
    public void showErrorMessage(final RemoteOperationResult result) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // The Toast must be shown in the main thread to grant that will be hidden correctly; otherwise
                // the thread may die before, an exception will occur, and the message will be left on the screen
                // until the app dies

                // Edited: this toast message has no sense. If operation is being passed as null to getErrorCauseMessage(),
                // the returned message is always null and therefore an empty toast is shown. Pending to review and change/delete

//                Toast.makeText(
//                        getContext().getApplicationContext(),
//                        ErrorMessageAdapter.getErrorCauseMessage(
//                                result,
//                                null,
//                                getContext().getResources()
//                        ),
//                        Toast.LENGTH_SHORT
//                ).show();
            }
        });
    }

}
