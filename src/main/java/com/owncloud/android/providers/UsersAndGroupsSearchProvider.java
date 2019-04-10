/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.providers;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
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
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Content provider for search suggestions, to search for users and groups existing in an ownCloud
 * server.
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

    public static String ACTION_SHARE_WITH;

    public static final String CONTENT = "content";

    private String DATA_USER;
    private String DATA_GROUP;
    private String DATA_ROOM;
    private String DATA_REMOTE;
    private String DATA_EMAIL;

    private UriMatcher mUriMatcher;

    private static Map<String, ShareType> sShareTypes = new HashMap<>();

    public static ShareType getShareType(String authority) {

        return sShareTypes.get(authority);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // TODO implement
        return null;
    }

    @Override
    public boolean onCreate() {
        if (getContext() == null) {
            return false;
        }

        String AUTHORITY = getContext().getResources().getString(R.string.users_and_groups_search_authority);
        ACTION_SHARE_WITH = getContext().getResources().getString(R.string.users_and_groups_share_with);
        DATA_USER = AUTHORITY + ".data.user";
        DATA_GROUP = AUTHORITY + ".data.group";
        DATA_ROOM = AUTHORITY + ".data.room";
        DATA_REMOTE = AUTHORITY + ".data.remote";
        DATA_EMAIL = AUTHORITY + ".data.email";

        sShareTypes.put(DATA_USER, ShareType.USER);
        sShareTypes.put(DATA_GROUP, ShareType.GROUP);
        sShareTypes.put(DATA_ROOM, ShareType.ROOM);
        sShareTypes.put(DATA_REMOTE, ShareType.FEDERATED);
        sShareTypes.put(DATA_EMAIL, ShareType.EMAIL);

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH);

        return true;
    }

    /**
     * returns sharee from server
     *
     * Reference: http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#CustomContentProvider
     *
     * @param uri           Content {@link Uri}, formatted as "content://com.nextcloud.android.providers.UsersAndGroupsSearchProvider/"
     *                      + {@link android.app.SearchManager#SUGGEST_URI_PATH_QUERY} + "/" +
     *                      'userQuery'
     * @param projection    Expected to be NULL.
     * @param selection     Expected to be NULL.
     * @param selectionArgs Expected to be NULL.
     * @param sortOrder     Expected to be NULL.
     * @return Cursor with possible sharees in the server that match 'query'.
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
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
        String lastPathSegment = uri.getLastPathSegment();

        if (lastPathSegment == null) {
            throw new IllegalArgumentException("Wrong URI passed!");
        }

        // need to trust on the AccountUtils to get the current account since the query in the client side is not
        // directly started by our code, but from SearchView implementation
        Account account = AccountUtils.getCurrentOwnCloudAccount(getContext());

        if (account == null) {
            throw new IllegalArgumentException("Account may not be null!");
        }

        String userQuery = lastPathSegment.toLowerCase(Locale.ROOT);

        // request to the OC server about users and groups matching userQuery
        GetRemoteShareesOperation searchRequest = new GetRemoteShareesOperation(userQuery, REQUESTED_PAGE,
                                                                                RESULTS_PER_PAGE);
        RemoteOperationResult result = searchRequest.execute(account, getContext());
        List<JSONObject> names = new ArrayList<>();

        if (result.isSuccess()) {
            for (Object o : result.getData()) {
                names.add((JSONObject) o);
            }
        } else {
            showErrorMessage(result);
        }

        MatrixCursor response = null;
        // convert the responses from the OC server to the expected format
        if (names.size() > 0) {
            if (getContext() == null) {
                throw new IllegalArgumentException("Context may not be null!");
            }

            response = new MatrixCursor(COLUMNS);

            Uri userBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_USER).build();
            Uri groupBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_GROUP).build();
            Uri roomBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_ROOM).build();
            Uri remoteBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_REMOTE).build();
            Uri emailBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_EMAIL).build();

            FileDataStorageManager manager = new FileDataStorageManager(account, getContext().getContentResolver());
            boolean federatedShareAllowed = manager.getCapability(account.name).getFilesSharingFederationOutgoing()
                .isTrue();

            try {
                Iterator<JSONObject> namesIt = names.iterator();
                JSONObject item;
                String displayName;
                int icon = 0;
                Uri dataUri;
                int count = 0;
                while (namesIt.hasNext()) {
                    item = namesIt.next();
                    dataUri = null;
                    displayName = null;
                    String userName = item.getString(GetRemoteShareesOperation.PROPERTY_LABEL);
                    JSONObject value = item.getJSONObject(GetRemoteShareesOperation.NODE_VALUE);
                    ShareType type = ShareType.fromValue(value.getInt(GetRemoteShareesOperation.PROPERTY_SHARE_TYPE));
                    String shareWith = value.getString(GetRemoteShareesOperation.PROPERTY_SHARE_WITH);

                    switch (type) {
                        case GROUP:
                            displayName = getContext().getString(R.string.share_group_clarification, userName);
                            icon = R.drawable.ic_group;
                            dataUri = Uri.withAppendedPath(groupBaseUri, shareWith);
                            break;

                        case FEDERATED:
                            if (federatedShareAllowed) {
                                icon = R.drawable.ic_user;
                                dataUri = Uri.withAppendedPath(remoteBaseUri, shareWith);

                                if (userName.equals(shareWith)) {
                                    displayName = getContext().getString(R.string.share_remote_clarification, userName);
                                } else {
                                    String[] uriSplitted = shareWith.split("@");
                                    displayName = getContext().getString(R.string.share_known_remote_clarification,
                                                                         userName, uriSplitted[uriSplitted.length - 1]);
                                }
                            }
                            break;

                        case USER:
                            displayName = userName;
                            icon = R.drawable.ic_user;
                            dataUri = Uri.withAppendedPath(userBaseUri, shareWith);
                            break;

                        case EMAIL:
                            icon = R.drawable.ic_email;
                            displayName = getContext().getString(R.string.share_email_clarification, userName);
                            dataUri = Uri.withAppendedPath(emailBaseUri, shareWith);
                            break;

                        case ROOM:
                            icon = R.drawable.ic_chat_bubble;
                            displayName = getContext().getString(R.string.share_room_clarification, userName);
                            dataUri = Uri.withAppendedPath(roomBaseUri, shareWith);
                            break;

                        default:
                            break;
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
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    /**
     * Show error message
     *
     * @param result Result with the failure information.
     */
    private void showErrorMessage(final RemoteOperationResult result) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            // The Toast must be shown in the main thread to grant that will be hidden correctly; otherwise
            // the thread may die before, an exception will occur, and the message will be left on the screen
            // until the app dies

            Context context = getContext();

            if (context == null) {
                throw new IllegalArgumentException("Context may not be null!");
            }

            Toast.makeText(getContext().getApplicationContext(),
                           ErrorMessageAdapter.getErrorCauseMessage(result, null, getContext().getResources()),
                           Toast.LENGTH_SHORT).show();
        });
    }
}
