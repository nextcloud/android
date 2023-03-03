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

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.GetShareesRemoteOperation;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.users.Status;
import com.owncloud.android.lib.resources.users.StatusType;
import com.owncloud.android.ui.TextDrawable;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.android.AndroidInjection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.owncloud.android.lib.resources.shares.GetShareesRemoteOperation.PROPERTY_CLEAR_AT;
import static com.owncloud.android.lib.resources.shares.GetShareesRemoteOperation.PROPERTY_ICON;
import static com.owncloud.android.lib.resources.shares.GetShareesRemoteOperation.PROPERTY_MESSAGE;
import static com.owncloud.android.lib.resources.shares.GetShareesRemoteOperation.PROPERTY_STATUS;


/**
 * Content provider for search suggestions, to search for users and groups existing in an ownCloud server.
 */
public class UsersAndGroupsSearchProvider extends ContentProvider {

    private static final String TAG = UsersAndGroupsSearchProvider.class.getSimpleName();

    private static final String[] COLUMNS = {
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_ICON_1,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    private static final int SEARCH = 1;

    private static final int RESULTS_PER_PAGE = 50;
    private static final int REQUESTED_PAGE = 1;

    @SuppressFBWarnings("MS_CANNOT_BE_FINAL")
    public static String ACTION_SHARE_WITH;

    public static final String CONTENT = "content";

    private String DATA_USER;
    private String DATA_GROUP;
    private String DATA_ROOM;
    private String DATA_REMOTE;
    private String DATA_EMAIL;
    private String DATA_CIRCLE;

    private UriMatcher mUriMatcher;

    @Inject
    protected UserAccountManager accountManager;
    @Inject
    protected UsersAndGroupsSearchConfig searchConfig;

    private static final Map<String, ShareType> sShareTypes = new HashMap<>();

    public static ShareType getShareType(String authority) {

        return sShareTypes.get(authority);
    }

    private static void setActionShareWith(@NonNull Context context) {
        ACTION_SHARE_WITH = context.getResources().getString(R.string.users_and_groups_share_with);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // TODO implement
        return null;
    }

    @Override
    public boolean onCreate() {
        AndroidInjection.inject(this);

        if (getContext() == null) {
            return false;
        }

        String AUTHORITY = getContext().getResources().getString(R.string.users_and_groups_search_authority);
        setActionShareWith(getContext());
        DATA_USER = AUTHORITY + ".data.user";
        DATA_GROUP = AUTHORITY + ".data.group";
        DATA_ROOM = AUTHORITY + ".data.room";
        DATA_REMOTE = AUTHORITY + ".data.remote";
        DATA_EMAIL = AUTHORITY + ".data.email";
        DATA_CIRCLE = AUTHORITY + ".data.circle";

        sShareTypes.put(DATA_USER, ShareType.USER);
        sShareTypes.put(DATA_GROUP, ShareType.GROUP);
        sShareTypes.put(DATA_ROOM, ShareType.ROOM);
        sShareTypes.put(DATA_REMOTE, ShareType.FEDERATED);
        sShareTypes.put(DATA_EMAIL, ShareType.EMAIL);
        sShareTypes.put(DATA_CIRCLE, ShareType.CIRCLE);

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
        if (match == SEARCH) {
            return searchForUsersOrGroups(uri);
        }
        return null;
    }

    private Cursor searchForUsersOrGroups(Uri uri) {

        // TODO check searchConfig and filter results
        Log.d(TAG, "searchForUsersOrGroups: searchConfig only users: " + searchConfig.getSearchOnlyUsers());

        String lastPathSegment = uri.getLastPathSegment();

        if (lastPathSegment == null) {
            throw new IllegalArgumentException("Wrong URI passed!");
        }

        // need to trust on the AccountUtils to get the current account since the query in the client side is not
        // directly started by our code, but from SearchView implementation
        User user = accountManager.getUser();

        String userQuery = lastPathSegment.toLowerCase(Locale.ROOT);

        // request to the OC server about users and groups matching userQuery
        GetShareesRemoteOperation searchRequest = new GetShareesRemoteOperation(userQuery, REQUESTED_PAGE,
                                                                                RESULTS_PER_PAGE);
        RemoteOperationResult result = searchRequest.execute(user, getContext());
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
            Uri circleBaseUri = new Uri.Builder().scheme(CONTENT).authority(DATA_CIRCLE).build();

            FileDataStorageManager manager = new FileDataStorageManager(user,
                                                                        getContext().getContentResolver());
            boolean federatedShareAllowed = manager.getCapability(user.getAccountName())
                .getFilesSharingFederationOutgoing()
                .isTrue();

            try {
                Iterator<JSONObject> namesIt = names.iterator();
                JSONObject item;
                String displayName;
                String subline = null;
                Object icon = 0;
                Uri dataUri;
                int count = 0;
                while (namesIt.hasNext()) {
                    item = namesIt.next();
                    dataUri = null;
                    displayName = null;
                    String userName = item.getString(GetShareesRemoteOperation.PROPERTY_LABEL);
                    String name = item.isNull("name") ? "" : item.getString("name");
                    JSONObject value = item.getJSONObject(GetShareesRemoteOperation.NODE_VALUE);
                    ShareType type = ShareType.fromValue(value.getInt(GetShareesRemoteOperation.PROPERTY_SHARE_TYPE));
                    String shareWith = value.getString(GetShareesRemoteOperation.PROPERTY_SHARE_WITH);

                    Status status;
                    JSONObject statusObject = item.optJSONObject(PROPERTY_STATUS);

                    if (statusObject != null) {
                        status = new Status(
                            StatusType.valueOf(statusObject.getString(PROPERTY_STATUS).toUpperCase(Locale.US)),
                            statusObject.isNull(PROPERTY_MESSAGE) ? "" : statusObject.getString(PROPERTY_MESSAGE),
                            statusObject.isNull(PROPERTY_ICON) ? "" : statusObject.getString(PROPERTY_ICON),
                            statusObject.isNull(PROPERTY_CLEAR_AT) ? -1 : statusObject.getLong(PROPERTY_CLEAR_AT));
                    } else {
                        status = new Status(StatusType.OFFLINE, "", "", -1);
                    }

                    switch (type) {
                        case GROUP:
                            displayName = userName;
                            icon = R.drawable.ic_group;
                            dataUri = Uri.withAppendedPath(groupBaseUri, shareWith);
                            break;

                        case FEDERATED:
                            if (federatedShareAllowed) {
                                icon = R.drawable.ic_user;
                                dataUri = Uri.withAppendedPath(remoteBaseUri, shareWith);

                                if (userName.equals(shareWith)) {
                                    displayName = name;
                                    subline = getContext().getString(R.string.remote);
                                } else {
                                    String[] uriSplitted = shareWith.split("@");
                                    displayName = name;
                                    subline = getContext().getString(R.string.share_known_remote_on_clarification,
                                                                     uriSplitted[uriSplitted.length - 1]);
                                }
                            }
                            break;

                        case USER:
                            displayName = userName;
                            subline = (status.getMessage() == null || status.getMessage().isEmpty()) ? null :
                                status.getMessage();
                            Uri.Builder builder =
                                Uri.parse("content://com.nextcloud.android.providers.UsersAndGroupsSearchProvider/icon")
                                    .buildUpon();

                            builder.appendQueryParameter("shareWith", shareWith);
                            builder.appendQueryParameter("displayName", displayName);
                            builder.appendQueryParameter("status", status.getStatus().toString());

                            if (!TextUtils.isEmpty(status.getIcon()) && !"null".equals(status.getIcon())) {
                                builder.appendQueryParameter("icon", status.getIcon());
                            }

                            icon = builder.build();

                            dataUri = Uri.withAppendedPath(userBaseUri, shareWith);
                            break;

                        case EMAIL:
                            icon = R.drawable.ic_email;
                            displayName = name;
                            subline = shareWith;
                            dataUri = Uri.withAppendedPath(emailBaseUri, shareWith);
                            break;

                        case ROOM:
                            icon = R.drawable.ic_talk;
                            displayName = userName;
                            dataUri = Uri.withAppendedPath(roomBaseUri, shareWith);
                            break;

                        case CIRCLE:
                            icon = R.drawable.ic_circles;
                            displayName = userName;
                            dataUri = Uri.withAppendedPath(circleBaseUri, shareWith);
                            break;

                        default:
                            break;
                    }

                    if (displayName != null && dataUri != null) {
                        response.newRow()
                            .add(count++)             // BaseColumns._ID
                            .add(displayName)         // SearchManager.SUGGEST_COLUMN_TEXT_1
                            .add(subline)             // SearchManager.SUGGEST_COLUMN_TEXT_2
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

    @Nullable
    @Override
    @SuppressFBWarnings("IOI_USE_OF_FILE_STREAM_CONSTRUCTORS") // TODO remove with API26
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(getContext());

        String userId = uri.getQueryParameter("shareWith");
        String displayName = uri.getQueryParameter("displayName");
        String accountName = accountManager.getUser().getAccountName();
        String serverName = accountName.substring(accountName.lastIndexOf('@') + 1);

        String eTag = arbitraryDataProvider.getValue(userId + "@" + serverName, ThumbnailsCacheManager.AVATAR);
        String avatarKey = "a_" + userId + "_" + serverName + "_" + eTag;

        StatusType status = StatusType.valueOf(uri.getQueryParameter("status"));
        String icon = uri.getQueryParameter("icon");

        if (icon == null) {
            icon = "";
        }

        Bitmap avatarBitmap = ThumbnailsCacheManager.getBitmapFromDiskCache(avatarKey);

        if (avatarBitmap == null) {
            float avatarRadius = getContext().getResources().getDimension(R.dimen.list_item_avatar_icon_radius);
            avatarBitmap = BitmapUtils.drawableToBitmap(TextDrawable.createNamedAvatar(displayName, avatarRadius));
        }

        Bitmap avatar = BitmapUtils.createAvatarWithStatus(avatarBitmap, status, icon, getContext());

        // create a file to write bitmap data
        File f = new File(getContext().getCacheDir(), "test");
        try {
            if (f.exists()) {
                if (!f.delete()) {
                    throw new IllegalStateException("Existing file could not be deleted!");
                }
            }
            if (!f.createNewFile()) {
                throw new IllegalStateException("File could not be created!");
            }

            //Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            avatar.compress(Bitmap.CompressFormat.PNG, 90, bos);
            byte[] bitmapData = bos.toByteArray();

            //write the bytes in file
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(bitmapData);
            } catch (FileNotFoundException e) {
                Log_OC.e(TAG, "File not found: " + e.getMessage());
            }

        } catch (Exception e) {
            Log_OC.e(TAG, "Error opening file: " + e.getMessage());
        }

        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
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
