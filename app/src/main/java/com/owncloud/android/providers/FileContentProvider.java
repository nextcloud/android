/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
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
import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;

import com.nextcloud.client.core.Clock;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import dagger.android.AndroidInjection;
import third_parties.aosp.SQLiteTokenizer;


/**
 * The ContentProvider for the ownCloud App.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class FileContentProvider extends ContentProvider {

    private static final int SINGLE_FILE = 1;
    private static final int DIRECTORY = 2;
    private static final int ROOT_DIRECTORY = 3;
    private static final int SHARES = 4;
    private static final int CAPABILITIES = 5;
    private static final int UPLOADS = 6;
    private static final int SYNCED_FOLDERS = 7;
    private static final int EXTERNAL_LINKS = 8;
    private static final int ARBITRARY_DATA = 9;
    private static final int VIRTUAL = 10;
    private static final int FILESYSTEM = 11;
    private static final String TAG = FileContentProvider.class.getSimpleName();
    // todo avoid string concatenation and use string formatting instead later.
    private static final String ERROR = "ERROR ";
    private static final String SQL = "SQL";
    private static final String INTEGER = " INTEGER, ";
    private static final String TEXT = " TEXT, ";
    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String ADD_COLUMN = " ADD COLUMN ";
    private static final String UPGRADE_VERSION_MSG = "OUT of the ADD in onUpgrade; oldVersion == %d, newVersion == %d";
    private static final int SINGLE_PATH_SEGMENT = 1;
    public static final int ARBITRARY_DATA_TABLE_INTRODUCTION_VERSION = 20;
    public static final int MINIMUM_PATH_SEGMENTS_SIZE = 1;

    private static final String[] PROJECTION_CONTENT_TYPE = new String[]{
        ProviderTableMeta._ID, ProviderTableMeta.FILE_CONTENT_TYPE
    };
    private static final String[] PROJECTION_REMOTE_ID = new String[]{
        ProviderTableMeta._ID, ProviderTableMeta.FILE_REMOTE_ID
    };
    private static final String[] PROJECTION_FILE_AND_STORAGE_PATH = new String[]{
        ProviderTableMeta._ID, ProviderTableMeta.FILE_STORAGE_PATH, ProviderTableMeta.FILE_PATH
    };
    private static final String[] PROJECTION_FILE_PATH_AND_OWNER = new String[]{
        ProviderTableMeta._ID, ProviderTableMeta.FILE_PATH, ProviderTableMeta.FILE_ACCOUNT_OWNER
    };

    private static final Map<String, String> FILE_PROJECTION_MAP;

    static {
        HashMap<String,String> tempMap = new HashMap<>();
        for (String projection : ProviderTableMeta.FILE_ALL_COLUMNS) {
            tempMap.put(projection, projection);
        }
        FILE_PROJECTION_MAP = Collections.unmodifiableMap(tempMap);
    }


    @Inject protected Clock clock;
    private DataBaseHelper mDbHelper;
    private Context mContext;
    private UriMatcher mUriMatcher;

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        if (isCallerNotAllowed(uri)) {
            return -1;
        }

        int count;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            count = delete(db, uri, where, whereArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        mContext.getContentResolver().notifyChange(uri, null);
        return count;
    }

    private int delete(SQLiteDatabase db, Uri uri, String where, String... whereArgs) {
        if (isCallerNotAllowed(uri)) {
            return -1;
        }

        // verify where for public paths
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
            case SINGLE_FILE:
            case DIRECTORY:
                VerificationUtils.verifyWhere(where);
        }

        int count;
        switch (mUriMatcher.match(uri)) {
            case SINGLE_FILE:
                count = deleteSingleFile(db, uri, where, whereArgs);
                break;
            case DIRECTORY:
                count = deleteDirectory(db, uri, where, whereArgs);
                break;
            case ROOT_DIRECTORY:
                count = db.delete(ProviderTableMeta.FILE_TABLE_NAME, where, whereArgs);
                break;
            case SHARES:
                count = db.delete(ProviderTableMeta.OCSHARES_TABLE_NAME, where, whereArgs);
                break;
            case CAPABILITIES:
                count = db.delete(ProviderTableMeta.CAPABILITIES_TABLE_NAME, where, whereArgs);
                break;
            case UPLOADS:
                count = db.delete(ProviderTableMeta.UPLOADS_TABLE_NAME, where, whereArgs);
                break;
            case SYNCED_FOLDERS:
                count = db.delete(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME, where, whereArgs);
                break;
            case EXTERNAL_LINKS:
                count = db.delete(ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME, where, whereArgs);
                break;
            case ARBITRARY_DATA:
                count = db.delete(ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME, where, whereArgs);
                break;
            case VIRTUAL:
                count = db.delete(ProviderTableMeta.VIRTUAL_TABLE_NAME, where, whereArgs);
                break;
            case FILESYSTEM:
                count = db.delete(ProviderTableMeta.FILESYSTEM_TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException(String.format(Locale.US, "Unknown uri: %s", uri.toString()));
        }

        return count;
    }

    private int deleteDirectory(SQLiteDatabase db, Uri uri, String where, String... whereArgs) {
        int count = 0;

        Cursor children = query(uri, PROJECTION_CONTENT_TYPE, null, null, null);
        if (children != null) {
            if (children.moveToFirst()) {
                long childId;
                boolean isDir;
                while (!children.isAfterLast()) {
                    childId = children.getLong(children.getColumnIndexOrThrow(ProviderTableMeta._ID));
                    isDir = MimeType.DIRECTORY.equals(children.getString(
                        children.getColumnIndexOrThrow(ProviderTableMeta.FILE_CONTENT_TYPE)
                    ));
                    if (isDir) {
                        count += delete(db, ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, childId),
                                        null, (String[]) null);
                    } else {
                        count += delete(db, ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, childId),
                                        null, (String[]) null);
                    }
                    children.moveToNext();
                }
            }
            children.close();
        }

        if (uri.getPathSegments().size() > MINIMUM_PATH_SEGMENTS_SIZE) {
            count += deleteWithUri(db, uri, where, whereArgs);
        }

        return count;
    }

    private int deleteSingleFile(SQLiteDatabase db, Uri uri, String where, String... whereArgs) {
        int count = 0;

        try (Cursor c = query(db, uri, PROJECTION_REMOTE_ID, where, whereArgs, null)) {
            if (c.moveToFirst()) {
                String id = c.getString(c.getColumnIndexOrThrow(ProviderTableMeta._ID));
                Log_OC.d(TAG, "Removing FILE " + id);
            }

            count = deleteWithUri(db, uri, where, whereArgs);
        } catch (Exception e) {
            Log_OC.d(TAG, "DB-Error removing file!", e);
        }

        return count;
    }

    private int deleteWithUri(SQLiteDatabase db, Uri uri, String where, String[] whereArgs) {
        final String[] argsWithUri = VerificationUtils.prependUriFirstSegmentToSelectionArgs(whereArgs, uri);
        return db.delete(ProviderTableMeta.FILE_TABLE_NAME,
                         ProviderTableMeta._ID + "=?"
                             + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""), argsWithUri);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
                return ProviderTableMeta.CONTENT_TYPE;
            case SINGLE_FILE:
                return ProviderTableMeta.CONTENT_TYPE_ITEM;
            default:
                throw new IllegalArgumentException(String.format(Locale.US, "Unknown Uri id: %s", uri));
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (isCallerNotAllowed(uri)) {
            return null;
        }

        Uri newUri;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            newUri = insert(db, uri, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        mContext.getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    private Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
        // verify only for those requests that are not internal (files table)
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
            case SINGLE_FILE:
            case DIRECTORY:
                VerificationUtils.verifyColumns(values);
                break;
        }


        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
            case SINGLE_FILE:
                String where = ProviderTableMeta.FILE_PATH + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";

                String remotePath = values.getAsString(ProviderTableMeta.FILE_PATH);
                String accountName = values.getAsString(ProviderTableMeta.FILE_ACCOUNT_OWNER);
                String[] whereArgs = {remotePath, accountName};

                Cursor doubleCheck = query(db, uri, PROJECTION_FILE_PATH_AND_OWNER, where, whereArgs, null);
                // ugly patch; serious refactoring is needed to reduce work in
                // FileDataStorageManager and bring it to FileContentProvider
                if (!doubleCheck.moveToFirst()) {
                    doubleCheck.close();
                    long rowId = db.insert(ProviderTableMeta.FILE_TABLE_NAME, null, values);
                    if (rowId > 0) {
                        return ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, rowId);
                    } else {
                        throw new SQLException(ERROR + uri);
                    }
                } else {
                    // file is already inserted; race condition, let's avoid a duplicated entry
                    Uri insertedFileUri = ContentUris.withAppendedId(
                        ProviderTableMeta.CONTENT_URI_FILE,
                        doubleCheck.getLong(doubleCheck.getColumnIndexOrThrow(ProviderTableMeta._ID))
                    );
                    doubleCheck.close();

                    return insertedFileUri;
                }

            case SHARES:
                Uri insertedShareUri;
                long idShares = db.insert(ProviderTableMeta.OCSHARES_TABLE_NAME, null, values);
                if (idShares > 0) {
                    insertedShareUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_SHARE, idShares);
                } else {
                    throw new SQLException(ERROR + uri);

                }
                updateFilesTableAccordingToShareInsertion(db, values);

                return insertedShareUri;

            case CAPABILITIES:
                Uri insertedCapUri;
                long idCapabilities = db.insert(ProviderTableMeta.CAPABILITIES_TABLE_NAME, null, values);
                if (idCapabilities > 0) {
                    insertedCapUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_CAPABILITIES, idCapabilities);
                } else {
                    throw new SQLException(ERROR + uri);
                }
                return insertedCapUri;

            case UPLOADS:
                Uri insertedUploadUri;
                long uploadId = db.insert(ProviderTableMeta.UPLOADS_TABLE_NAME, null, values);
                if (uploadId > 0) {
                    insertedUploadUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_UPLOADS, uploadId);
                } else {
                    throw new SQLException(ERROR + uri);
                }
                return insertedUploadUri;

            case SYNCED_FOLDERS:
                Uri insertedSyncedFolderUri;
                long syncedFolderId = db.insert(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME, null, values);
                if (syncedFolderId > 0) {
                    insertedSyncedFolderUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                                                                         syncedFolderId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedSyncedFolderUri;

            case EXTERNAL_LINKS:
                Uri insertedExternalLinkUri;
                long externalLinkId = db.insert(ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME, null, values);
                if (externalLinkId > 0) {
                    insertedExternalLinkUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS,
                                                                         externalLinkId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedExternalLinkUri;

            case ARBITRARY_DATA:
                Uri insertedArbitraryDataUri;
                long arbitraryDataId = db.insert(ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME, null, values);
                if (arbitraryDataId > 0) {
                    insertedArbitraryDataUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_ARBITRARY_DATA,
                                                                          arbitraryDataId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedArbitraryDataUri;
            case VIRTUAL:
                Uri insertedVirtualUri;
                long virtualId = db.insert(ProviderTableMeta.VIRTUAL_TABLE_NAME, null, values);

                if (virtualId > 0) {
                    insertedVirtualUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_VIRTUAL, virtualId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }

                return insertedVirtualUri;
            case FILESYSTEM:
                Uri insertedFilesystemUri;
                long filesystemId = db.insert(ProviderTableMeta.FILESYSTEM_TABLE_NAME, null, values);
                if (filesystemId > 0) {
                    insertedFilesystemUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILESYSTEM,
                                                                       filesystemId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedFilesystemUri;
            default:
                throw new IllegalArgumentException("Unknown uri id: " + uri);
        }
    }

    private void updateFilesTableAccordingToShareInsertion(SQLiteDatabase db, ContentValues newShare) {
        ContentValues fileValues = new ContentValues();
        ShareType newShareType = ShareType.fromValue(newShare.getAsInteger(ProviderTableMeta.OCSHARES_SHARE_TYPE));

        switch (newShareType) {
            case PUBLIC_LINK:
                fileValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, 1);
                break;

            case USER:
            case GROUP:
            case EMAIL:
            case FEDERATED:
            case FEDERATED_GROUP:
            case ROOM:
            case CIRCLE:
            case DECK:
            case GUEST:
                fileValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, 1);
                break;

            default:
                // everything should be handled
        }

        String where = ProviderTableMeta.FILE_PATH + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{
            newShare.getAsString(ProviderTableMeta.OCSHARES_PATH),
            newShare.getAsString(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)
        };
        db.update(ProviderTableMeta.FILE_TABLE_NAME, fileValues, where, whereArgs);
    }


    @Override
    public boolean onCreate() {
        AndroidInjection.inject(this);
        mDbHelper = new DataBaseHelper(getContext());
        mContext = getContext();

        if (mContext == null) {
            return false;
        }

        String authority = mContext.getResources().getString(R.string.authority);
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(authority, null, ROOT_DIRECTORY);
        mUriMatcher.addURI(authority, "file/", SINGLE_FILE);
        mUriMatcher.addURI(authority, "file/#", SINGLE_FILE);
        mUriMatcher.addURI(authority, "dir/", DIRECTORY);
        mUriMatcher.addURI(authority, "dir/#", DIRECTORY);
        mUriMatcher.addURI(authority, "shares/", SHARES);
        mUriMatcher.addURI(authority, "shares/#", SHARES);
        mUriMatcher.addURI(authority, "capabilities/", CAPABILITIES);
        mUriMatcher.addURI(authority, "capabilities/#", CAPABILITIES);
        mUriMatcher.addURI(authority, "uploads/", UPLOADS);
        mUriMatcher.addURI(authority, "uploads/#", UPLOADS);
        mUriMatcher.addURI(authority, "synced_folders", SYNCED_FOLDERS);
        mUriMatcher.addURI(authority, "external_links", EXTERNAL_LINKS);
        mUriMatcher.addURI(authority, "arbitrary_data", ARBITRARY_DATA);
        mUriMatcher.addURI(authority, "virtual", VIRTUAL);
        mUriMatcher.addURI(authority, "filesystem", FILESYSTEM);

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        // skip check for files as they need to be queried to get access via document provider
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
            case SINGLE_FILE:
            case DIRECTORY:
                break;

            default:
                if (isCallerNotAllowed(uri)) {
                    return null;
                }
        }

        Cursor result;
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            result = query(db, uri, projection, selection, selectionArgs, sortOrder);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return result;
    }

    private Cursor query(SQLiteDatabase db,
                         Uri uri,
                         String[] projectionArray,
                         String selection,
                         String[] selectionArgs,
                         String sortOrder) {

        // verify only for those requests that are not internal
        final int uriMatch = mUriMatcher.match(uri);

        SQLiteQueryBuilder sqlQuery = new SQLiteQueryBuilder();


        switch (uriMatch) {
            case ROOT_DIRECTORY:
            case DIRECTORY:
            case SINGLE_FILE:
                VerificationUtils.verifyWhere(selection); // prevent injection in public paths
                sqlQuery.setTables(ProviderTableMeta.FILE_TABLE_NAME);
                break;
            case SHARES:
                sqlQuery.setTables(ProviderTableMeta.OCSHARES_TABLE_NAME);
                break;
            case CAPABILITIES:
                sqlQuery.setTables(ProviderTableMeta.CAPABILITIES_TABLE_NAME);
                break;
            case UPLOADS:
                sqlQuery.setTables(ProviderTableMeta.UPLOADS_TABLE_NAME);
                break;
            case SYNCED_FOLDERS:
                sqlQuery.setTables(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME);
                break;
            case EXTERNAL_LINKS:
                sqlQuery.setTables(ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME);
                break;
            case ARBITRARY_DATA:
                sqlQuery.setTables(ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME);
                break;
            case VIRTUAL:
                sqlQuery.setTables(ProviderTableMeta.VIRTUAL_TABLE_NAME);
                break;
            case FILESYSTEM:
                sqlQuery.setTables(ProviderTableMeta.FILESYSTEM_TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Unknown uri id: " + uri);
        }


        // add ID to arguments if Uri has more than one segment
        if (uriMatch != ROOT_DIRECTORY && uri.getPathSegments().size() > SINGLE_PATH_SEGMENT ) {
            String idColumn = uriMatch == DIRECTORY ? ProviderTableMeta.FILE_PARENT : ProviderTableMeta._ID;
            sqlQuery.appendWhere(idColumn + "=?");
            selectionArgs = VerificationUtils.prependUriFirstSegmentToSelectionArgs(selectionArgs, uri);
        }


        String order;
        if (TextUtils.isEmpty(sortOrder)) {
            switch (uriMatch) {
                case SHARES:
                    order = ProviderTableMeta.OCSHARES_DEFAULT_SORT_ORDER;
                    break;
                case CAPABILITIES:
                    order = ProviderTableMeta.CAPABILITIES_DEFAULT_SORT_ORDER;
                    break;
                case UPLOADS:
                    order = ProviderTableMeta.UPLOADS_DEFAULT_SORT_ORDER;
                    break;
                case SYNCED_FOLDERS:
                    order = ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH;
                    break;
                case EXTERNAL_LINKS:
                    order = ProviderTableMeta.EXTERNAL_LINKS_NAME;
                    break;
                case ARBITRARY_DATA:
                    order = ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID;
                    break;
                case VIRTUAL:
                    order = ProviderTableMeta.VIRTUAL_TYPE;
                    break;
                default: // Files
                    order = ProviderTableMeta.FILE_DEFAULT_SORT_ORDER;
                    break;
                case FILESYSTEM:
                    order = ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH;
                    break;
            }
        } else {
            if (uriMatch == ROOT_DIRECTORY || uriMatch == SINGLE_FILE || uriMatch == DIRECTORY) {
                VerificationUtils.verifySortOrder(sortOrder);
            }
            order = sortOrder;
        }

        // DB case_sensitive
        db.execSQL("PRAGMA case_sensitive_like = true");

        // only file list is accessible via content provider, so only this has to be protected with projectionMap
        if ((uriMatch == ROOT_DIRECTORY || uriMatch == SINGLE_FILE ||
            uriMatch == DIRECTORY) && projectionArray != null) {
            sqlQuery.setProjectionMap(FILE_PROJECTION_MAP);
        }

        // if both are null, let them pass to query
        if (selectionArgs == null && selection != null) {
            selectionArgs = new String[]{selection};
            selection = "(?)";
        }

        sqlQuery.setStrict(true);
        Cursor c = sqlQuery.query(db, projectionArray, selection, selectionArgs, null, null, order);
        c.setNotificationUri(mContext.getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (isCallerNotAllowed(uri)) {
            return -1;
        }

        int count;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            count = update(db, uri, values, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        mContext.getContentResolver().notifyChange(uri, null);
        return count;
    }

    private int update(SQLiteDatabase db, Uri uri, ContentValues values, String selection, String... selectionArgs) {
        // verify contentValues and selection for public paths to prevent injection
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
            case SINGLE_FILE:
            case DIRECTORY:
                VerificationUtils.verifyColumns(values);
                VerificationUtils.verifyWhere(selection);
        }

        switch (mUriMatcher.match(uri)) {
            case DIRECTORY:
                return 0;
            case SHARES:
                return db.update(ProviderTableMeta.OCSHARES_TABLE_NAME, values, selection, selectionArgs);
            case CAPABILITIES:
                return db.update(ProviderTableMeta.CAPABILITIES_TABLE_NAME, values, selection, selectionArgs);
            case UPLOADS:
                return db.update(ProviderTableMeta.UPLOADS_TABLE_NAME, values, selection, selectionArgs);
            case SYNCED_FOLDERS:
                return db.update(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME, values, selection, selectionArgs);
            case ARBITRARY_DATA:
                return db.update(ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME, values, selection, selectionArgs);
            case FILESYSTEM:
                return db.update(ProviderTableMeta.FILESYSTEM_TABLE_NAME, values, selection, selectionArgs);
            default:
                return db.update(ProviderTableMeta.FILE_TABLE_NAME, values, selection, selectionArgs);
        }
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> operations)
        throws OperationApplicationException {
        Log_OC.d("FileContentProvider", "applying batch in provider " + this +
            " (temporary: " + isTemporary() + ")");
        ContentProviderResult[] results = new ContentProviderResult[operations.size()];
        int i = 0;

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        database.beginTransaction();  // it's supposed that transactions can be nested
        try {
            for (ContentProviderOperation operation : operations) {
                results[i] = operation.apply(this, results, i);
                i++;
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        Log_OC.d("FileContentProvider", "applied batch in provider " + this);
        return results;
    }

    private boolean checkIfColumnExists(SQLiteDatabase database, String table, String column) {
        Cursor cursor = database.rawQuery("SELECT * FROM " + table + " LIMIT 0", null);
        boolean exists = cursor.getColumnIndex(column) != -1;
        cursor.close();

        return exists;
    }

    private void createFilesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.FILE_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                       + ProviderTableMeta.FILE_NAME + TEXT
                       + ProviderTableMeta.FILE_ENCRYPTED_NAME + TEXT
                       + ProviderTableMeta.FILE_PATH + TEXT
                       + ProviderTableMeta.FILE_PATH_DECRYPTED + TEXT
                       + ProviderTableMeta.FILE_PARENT + INTEGER
                       + ProviderTableMeta.FILE_CREATION + INTEGER
                       + ProviderTableMeta.FILE_MODIFIED + INTEGER
                       + ProviderTableMeta.FILE_CONTENT_TYPE + TEXT
                       + ProviderTableMeta.FILE_CONTENT_LENGTH + INTEGER
                       + ProviderTableMeta.FILE_STORAGE_PATH + TEXT
                       + ProviderTableMeta.FILE_ACCOUNT_OWNER + TEXT
                       + ProviderTableMeta.FILE_LAST_SYNC_DATE + INTEGER
                       + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA + INTEGER
                       + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA + INTEGER
                       + ProviderTableMeta.FILE_ETAG + TEXT
                       + ProviderTableMeta.FILE_ETAG_ON_SERVER + TEXT
                       + ProviderTableMeta.FILE_SHARED_VIA_LINK + INTEGER
                       + ProviderTableMeta.FILE_PERMISSIONS + " TEXT null,"
                       + ProviderTableMeta.FILE_REMOTE_ID + " TEXT null,"
                       + ProviderTableMeta.FILE_UPDATE_THUMBNAIL + INTEGER //boolean
                       + ProviderTableMeta.FILE_IS_DOWNLOADING + INTEGER //boolean
                       + ProviderTableMeta.FILE_FAVORITE + INTEGER // boolean
                       + ProviderTableMeta.FILE_IS_ENCRYPTED + INTEGER // boolean
                       + ProviderTableMeta.FILE_ETAG_IN_CONFLICT + TEXT
                       + ProviderTableMeta.FILE_SHARED_WITH_SHAREE + INTEGER
                       + ProviderTableMeta.FILE_MOUNT_TYPE + INTEGER
                       + ProviderTableMeta.FILE_HAS_PREVIEW + INTEGER
                       + ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT + INTEGER
                       + ProviderTableMeta.FILE_OWNER_ID + TEXT
                       + ProviderTableMeta.FILE_OWNER_DISPLAY_NAME + TEXT
                       + ProviderTableMeta.FILE_NOTE + TEXT
                       + ProviderTableMeta.FILE_SHAREES + TEXT
                       + ProviderTableMeta.FILE_RICH_WORKSPACE + TEXT
                       + ProviderTableMeta.FILE_METADATA_SIZE + TEXT
                       + ProviderTableMeta.FILE_LOCKED + INTEGER // boolean
                       + ProviderTableMeta.FILE_LOCK_TYPE + INTEGER
                       + ProviderTableMeta.FILE_LOCK_OWNER + TEXT
                       + ProviderTableMeta.FILE_LOCK_OWNER_DISPLAY_NAME + TEXT
                       + ProviderTableMeta.FILE_LOCK_OWNER_EDITOR + TEXT
                       + ProviderTableMeta.FILE_LOCK_TIMESTAMP + INTEGER
                       + ProviderTableMeta.FILE_LOCK_TIMEOUT + INTEGER
                       + ProviderTableMeta.FILE_LOCK_TOKEN + " TEXT );"
        );
    }

    private void createOCSharesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.OCSHARES_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                       + ProviderTableMeta.OCSHARES_FILE_SOURCE + INTEGER
                       + ProviderTableMeta.OCSHARES_ITEM_SOURCE + INTEGER
                       + ProviderTableMeta.OCSHARES_SHARE_TYPE + INTEGER
                       + ProviderTableMeta.OCSHARES_SHARE_WITH + TEXT
                       + ProviderTableMeta.OCSHARES_PATH + TEXT
                       + ProviderTableMeta.OCSHARES_PERMISSIONS + INTEGER
                       + ProviderTableMeta.OCSHARES_SHARED_DATE + INTEGER
                       + ProviderTableMeta.OCSHARES_EXPIRATION_DATE + INTEGER
                       + ProviderTableMeta.OCSHARES_TOKEN + TEXT
                       + ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME + TEXT
                       + ProviderTableMeta.OCSHARES_IS_DIRECTORY + INTEGER  // boolean
                       + ProviderTableMeta.OCSHARES_USER_ID + INTEGER
                       + ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + INTEGER
                       + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + TEXT
                       + ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED + INTEGER
                       + ProviderTableMeta.OCSHARES_NOTE + TEXT
                       + ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD + INTEGER
                       + ProviderTableMeta.OCSHARES_SHARE_LINK + TEXT
                       + ProviderTableMeta.OCSHARES_SHARE_LABEL + " TEXT );");
    }

    private void createCapabilitiesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                       + ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + TEXT
                       + ProviderTableMeta.CAPABILITIES_VERSION_MAYOR + INTEGER
                       + ProviderTableMeta.CAPABILITIES_VERSION_MINOR + INTEGER
                       + ProviderTableMeta.CAPABILITIES_VERSION_MICRO + INTEGER
                       + ProviderTableMeta.CAPABILITIES_VERSION_STRING + TEXT
                       + ProviderTableMeta.CAPABILITIES_VERSION_EDITION + TEXT
                       + ProviderTableMeta.CAPABILITIES_EXTENDED_SUPPORT + INTEGER
                       + ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL + INTEGER
                       + ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED + INTEGER // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED + INTEGER  // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED + INTEGER    // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED + INTEGER  // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS + INTEGER
                       + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED + INTEGER // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL + INTEGER    // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD + INTEGER       // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL + INTEGER      // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_RESHARING + INTEGER           // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING + INTEGER     // boolean
                       + ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING + INTEGER     // boolean
                       + ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING + INTEGER   // boolean
                       + ProviderTableMeta.CAPABILITIES_FILES_UNDELETE + INTEGER  // boolean
                       + ProviderTableMeta.CAPABILITIES_FILES_VERSIONING + INTEGER   // boolean
                       + ProviderTableMeta.CAPABILITIES_EXTERNAL_LINKS + INTEGER  // boolean
                       + ProviderTableMeta.CAPABILITIES_SERVER_NAME + TEXT
                       + ProviderTableMeta.CAPABILITIES_SERVER_COLOR + TEXT
                       + ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR + TEXT
                       + ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR + TEXT
                       + ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN + TEXT
                       + ProviderTableMeta.CAPABILITIES_SERVER_LOGO + TEXT
                       + ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL + TEXT
                       + ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION + INTEGER
                       + ProviderTableMeta.CAPABILITIES_ACTIVITY + INTEGER
                       + ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT + INTEGER
                       + ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN + INTEGER
                       + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT + INTEGER
                       + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST + TEXT
                       + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING + INTEGER
                       + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES + INTEGER
                       + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST + TEXT
                       + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD + INTEGER
                       + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME + TEXT
                       + ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG + TEXT
                       + ProviderTableMeta.CAPABILITIES_USER_STATUS + INTEGER
                       + ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI + INTEGER
                       + ProviderTableMeta.CAPABILITIES_ETAG + TEXT
                       + ProviderTableMeta.CAPABILITIES_FILES_LOCKING_VERSION + " TEXT );");
    }

    private void createUploadsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.UPLOADS_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                       + ProviderTableMeta.UPLOADS_LOCAL_PATH + TEXT
                       + ProviderTableMeta.UPLOADS_REMOTE_PATH + TEXT
                       + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + TEXT
                       + ProviderTableMeta.UPLOADS_FILE_SIZE + " LONG, "
                       + ProviderTableMeta.UPLOADS_STATUS + INTEGER               // UploadStatus
                       + ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + INTEGER      // Upload LocalBehaviour
                       + ProviderTableMeta.UPLOADS_UPLOAD_TIME + INTEGER
                       + ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY + INTEGER  // boolean
                       + ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + INTEGER  // boolean
                       + ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + INTEGER
                       + ProviderTableMeta.UPLOADS_LAST_RESULT + INTEGER     // Upload LastResult
                       + ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + INTEGER  // boolean
                       + ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + INTEGER // boolean
                       + ProviderTableMeta.UPLOADS_CREATED_BY + INTEGER    // Upload createdBy
                       + ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN + " TEXT );");

        /* before:
        // PRIMARY KEY should always imply NOT NULL. Unfortunately, due to a
        // bug in some early versions, this is not the case in SQLite.
        //db.execSQL("CREATE TABLE " + TABLE_UPLOAD + " (" + " path TEXT PRIMARY KEY NOT NULL UNIQUE,"
        //        + " uploadStatus INTEGER NOT NULL, uploadObject TEXT NOT NULL);");
        // uploadStatus is used to easy filtering, it has precedence over
        // uploadObject.getUploadStatus()
        */
    }

    private void createSyncedFoldersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "                 // id
                       + ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH + " TEXT, "           // local path
                       + ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH + " TEXT, "          // remote path
                       + ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY + " INTEGER, "         // wifi_only
                       + ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY + " INTEGER, "     // charging only
                       + ProviderTableMeta.SYNCED_FOLDER_EXISTING + " INTEGER, "          // existing
                       + ProviderTableMeta.SYNCED_FOLDER_ENABLED + " INTEGER, "           // enabled
                       + ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS + " INTEGER, " // enable date
                       + ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE + " INTEGER, " // subfolder by date
                       + ProviderTableMeta.SYNCED_FOLDER_ACCOUNT + "  TEXT, "             // account
                       + ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION + " INTEGER, "     // upload action
                       + ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY + " INTEGER, " // name collision policy
                       + ProviderTableMeta.SYNCED_FOLDER_TYPE + " INTEGER, "              // type
                       + ProviderTableMeta.SYNCED_FOLDER_HIDDEN + " INTEGER );"           // hidden
        );
    }

    private void createExternalLinksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "          // id
                       + ProviderTableMeta.EXTERNAL_LINKS_ICON_URL + " TEXT, "     // icon url
                       + ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE + " TEXT, "     // language
                       + ProviderTableMeta.EXTERNAL_LINKS_TYPE + " INTEGER, "      // type
                       + ProviderTableMeta.EXTERNAL_LINKS_NAME + " TEXT, "         // name
                       + ProviderTableMeta.EXTERNAL_LINKS_URL + " TEXT, "          // url
                       + ProviderTableMeta.EXTERNAL_LINKS_REDIRECT + " INTEGER );" // redirect
        );
    }

    private void createArbitraryData(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "      // id
                       + ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID + " TEXT, " // cloud id (account name + FQDN)
                       + ProviderTableMeta.ARBITRARY_DATA_KEY + " TEXT, "      // key
                       + ProviderTableMeta.ARBITRARY_DATA_VALUE + " TEXT );"   // value
        );
    }

    private void createVirtualTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.VIRTUAL_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "          // id
                       + ProviderTableMeta.VIRTUAL_TYPE + " TEXT, "                // type
                       + ProviderTableMeta.VIRTUAL_OCFILE_ID + " INTEGER )"        // file id
                  );
    }

    private void createFileSystemTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ProviderTableMeta.FILESYSTEM_TABLE_NAME + "("
                       + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "      // id
                       + ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH + " TEXT, "
                       + ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER + " INTEGER, "
                       + ProviderTableMeta.FILESYSTEM_FILE_FOUND_RECENTLY + " LONG, "
                       + ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD + " INTEGER, "
                       + ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID + " STRING, "
                       + ProviderTableMeta.FILESYSTEM_CRC32 + " STRING, "
                       + ProviderTableMeta.FILESYSTEM_FILE_MODIFIED + " LONG );"
        );
    }

    /**
     * Version 10 of database does not modify its scheme. It coincides with the upgrade of the
     * ownCloud account names structure to include in it the path to the server instance. Updating
     * the account names and path to local files in the files table is a must to keep the existing
     * account working and the database clean.
     *
     * @param db Database where table of files is included.
     */
    private void updateAccountName(SQLiteDatabase db) {
        Log_OC.d(SQL, "THREAD:  " + Thread.currentThread().getName());
        AccountManager ama = AccountManager.get(getContext());
        try {
            // get accounts from AccountManager ;  we can't be sure if accounts in it are updated or not although
            // we know the update was previously done in {link @FileActivity#onCreate} because the changes through
            // AccountManager are not synchronous
            Account[] accounts = AccountManager.get(getContext()).getAccountsByType(MainApp.getAccountType(mContext));
            String serverUrl;
            String username;
            String oldAccountName;
            String newAccountName;
            String[] accountOwner = new String[1];

            for (Account account : accounts) {
                // build both old and new account name
                serverUrl = ama.getUserData(account, AccountUtils.Constants.KEY_OC_BASE_URL);
                username = AccountUtils.getUsernameForAccount(account);
                oldAccountName = AccountUtils.buildAccountNameOld(Uri.parse(serverUrl), username);
                newAccountName = AccountUtils.buildAccountName(Uri.parse(serverUrl), username);

                // update values in database
                db.beginTransaction();
                try {
                    ContentValues cv = new ContentValues();
                    cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, newAccountName);
                    accountOwner[0] = oldAccountName;
                    int num = db.update(ProviderTableMeta.FILE_TABLE_NAME,
                                        cv,
                                        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                        accountOwner);

                    Log_OC.d(SQL, "Updated account in database: old name == " + oldAccountName +
                        ", new name == " + newAccountName + " (" + num + " rows updated )");

                    // update path for downloaded files
                    updateDownloadedFiles(db, newAccountName, oldAccountName);

                    db.setTransactionSuccessful();

                } catch (SQLException e) {
                    Log_OC.e(TAG, "SQL Exception upgrading account names or paths in database", e);
                } finally {
                    db.endTransaction();
                }
            }
        } catch (Exception e) {
            Log_OC.e(TAG, "Exception upgrading account names or paths in database", e);
        }
    }

    /**
     * Rename the local ownCloud folder of one account to match the a rename of the account itself.
     * Updates the table of files in database so that the paths to the local files keep being the
     * same.
     *
     * @param db             Database where table of files is included.
     * @param newAccountName New name for the target OC account.
     * @param oldAccountName Old name of the target OC account.
     */
    private void updateDownloadedFiles(SQLiteDatabase db, String newAccountName,
                                       String oldAccountName) {

        String whereClause = ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND " +
            ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL";

        Cursor c = db.query(ProviderTableMeta.FILE_TABLE_NAME,
                            PROJECTION_FILE_AND_STORAGE_PATH,
                            whereClause,
                            new String[]{newAccountName},
                            null, null, null);

        try {
            if (c.moveToFirst()) {
                // create storage path
                String oldAccountPath = FileStorageUtils.getSavePath(oldAccountName);
                String newAccountPath = FileStorageUtils.getSavePath(newAccountName);

                // move files
                File oldAccountFolder = new File(oldAccountPath);
                File newAccountFolder = new File(newAccountPath);
                oldAccountFolder.renameTo(newAccountFolder);

                String[] storagePath = new String[1];

                // update database
                do {
                    // Update database
                    String oldPath = c.getString(
                        c.getColumnIndexOrThrow(ProviderTableMeta.FILE_STORAGE_PATH));
                    OCFile file = new OCFile(
                        c.getString(c.getColumnIndexOrThrow(ProviderTableMeta.FILE_PATH)));
                    String newPath = FileStorageUtils.getDefaultSavePathFor(newAccountName, file);

                    ContentValues cv = new ContentValues();
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, newPath);
                    storagePath[0] = oldPath;
                    db.update(ProviderTableMeta.FILE_TABLE_NAME,
                              cv,
                              ProviderTableMeta.FILE_STORAGE_PATH + "=?",
                              storagePath);

                    Log_OC.v(SQL, "Updated path of downloaded file: old file name == " + oldPath +
                        ", new file name == " + newPath);

                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }
    }

    private boolean isCallerNotAllowed(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case SHARES:
            case CAPABILITIES:
            case UPLOADS:
            case SYNCED_FOLDERS:
            case EXTERNAL_LINKS:
            case ARBITRARY_DATA:
            case VIRTUAL:
            case FILESYSTEM:
                String callingPackage = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
                return callingPackage == null || !callingPackage.equals(mContext.getPackageName());

            case ROOT_DIRECTORY:
            case SINGLE_FILE:
            case DIRECTORY:
            default:
                return false;
        }
    }


    static class VerificationUtils {

        private static boolean isValidColumnName(@NonNull String columnName) {
            return ProviderTableMeta.FILE_ALL_COLUMNS.contains(columnName);
        }

        @VisibleForTesting
        public static void verifyColumns(@Nullable ContentValues contentValues) {
            if (contentValues == null || contentValues.keySet().size() == 0) {
                return;
            }

            for (String name : contentValues.keySet()) {
                verifyColumnName(name);
            }
        }

        @VisibleForTesting
        public static void verifyColumnName(@NonNull String columnName) {
            if (!isValidColumnName(columnName)) {
                throw new IllegalArgumentException(String.format("Column name \"%s\" is not allowed", columnName));
            }
        }

        public static String[] prependUriFirstSegmentToSelectionArgs(@Nullable final String[] originalArgs, final Uri uri) {
            String[] args;
            if (originalArgs == null) {
                args = new String[1];
            } else {
                args = new String[originalArgs.length + 1];
                System.arraycopy(originalArgs, 0, args, 1, originalArgs.length);
            }
            args[0] = uri.getPathSegments().get(1);
            return args;
        }

        public static void verifySortOrder(@Nullable String sortOrder) {
            if (sortOrder == null) {
                return;
            }
            SQLiteTokenizer.tokenize(sortOrder, SQLiteTokenizer.OPTION_NONE, VerificationUtils::verifySortToken);
        }

        private static void verifySortToken(String token){
            // accept empty tokens and valid column names
            if (TextUtils.isEmpty(token) || isValidColumnName(token)) {
                return;
            }
            // accept only a small subset of keywords
            if(SQLiteTokenizer.isKeyword(token)){
                switch (token.toUpperCase(Locale.ROOT)) {
                    case "ASC":
                    case "DESC":
                    case "COLLATE":
                    case "NOCASE":
                        return;
                }
            }
            // if none of the above, invalid token
            throw new IllegalArgumentException("Invalid token " + token);
        }

        public static void verifyWhere(@Nullable String where) {
            if (where == null) {
                return;
            }
            SQLiteTokenizer.tokenize(where, SQLiteTokenizer.OPTION_NONE, VerificationUtils::verifyWhereToken);
        }

        private static void verifyWhereToken(String token) {
            // allow empty, valid column names, functions (min,max,count) and types
            if (TextUtils.isEmpty(token) || isValidColumnName(token)
                || SQLiteTokenizer.isFunction(token) || SQLiteTokenizer.isType(token)) {
                return;
            }

            // Disallow dangerous keywords, allow others
            if (SQLiteTokenizer.isKeyword(token)) {
                switch (token.toUpperCase(Locale.ROOT)) {
                    case "SELECT":
                    case "FROM":
                    case "WHERE":
                    case "GROUP":
                    case "HAVING":
                    case "WINDOW":
                    case "VALUES":
                    case "ORDER":
                    case "LIMIT":
                        throw new IllegalArgumentException("Invalid token " + token);
                    default:
                        return;
                }
            }

            // if none of the above: invalid token
            throw new IllegalArgumentException("Invalid token " + token);
        }
    }

    class DataBaseHelper extends SQLiteOpenHelper {
        DataBaseHelper(Context context) {
            super(context, ProviderMeta.DB_NAME, null, ProviderMeta.DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // files table
            Log_OC.i(SQL, "Entering in onCreate");
            createFilesTable(db);

            // Create OCShares table
            createOCSharesTable(db);

            // Create capabilities table
            createCapabilitiesTable(db);

            // Create uploads table
            createUploadsTable(db);

            // Create synced folders table
            createSyncedFoldersTable(db);

            // Create external links table
            createExternalLinksTable(db);

            // Create arbitrary data table
            createArbitraryData(db);

            // Create virtual table
            createVirtualTable(db);

            // Create filesystem table
            createFileSystemTable(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log_OC.i(SQL, "Entering in onUpgrade");
            boolean upgraded = false;
            if (oldVersion == 1 && newVersion >= 2) {
                Log_OC.i(SQL, "Entering in the #2 ADD in onUpgrade");
                db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderTableMeta.FILE_KEEP_IN_SYNC + " INTEGER " +
                               " DEFAULT 0");
                upgraded = true;
            }
            if (oldVersion < 3 && newVersion >= 3) {
                Log_OC.i(SQL, "Entering in the #3 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA +
                                   " INTEGER " + " DEFAULT 0");

                    // assume there are not local changes pending to upload
                    db.execSQL("UPDATE " + ProviderTableMeta.FILE_TABLE_NAME +
                                   " SET " + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA + " = "
                                   + System.currentTimeMillis() +
                                   " WHERE " + ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (oldVersion < 4 && newVersion >= 4) {
                Log_OC.i(SQL, "Entering in the #4 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA +
                                   " INTEGER " + " DEFAULT 0");

                    db.execSQL("UPDATE " + ProviderTableMeta.FILE_TABLE_NAME +
                                   " SET " + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA + " = " +
                                   ProviderTableMeta.FILE_MODIFIED +
                                   " WHERE " + ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 5 && newVersion >= 5) {
                Log_OC.i(SQL, "Entering in the #5 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_ETAG + " TEXT " +
                                   " DEFAULT NULL");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 6 && newVersion >= 6) {
                Log_OC.i(SQL, "Entering in the #6 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_SHARED_VIA_LINK + " INTEGER " +
                                   " DEFAULT 0");

                    // Create table OCShares
                    createOCSharesTable(db);

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 7 && newVersion >= 7) {
                Log_OC.i(SQL, "Entering in the #7 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_PERMISSIONS + " TEXT " +
                                   " DEFAULT NULL");

                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_REMOTE_ID + " TEXT " +
                                   " DEFAULT NULL");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 8 && newVersion >= 8) {
                Log_OC.i(SQL, "Entering in the #8 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_UPDATE_THUMBNAIL + " INTEGER " +
                                   " DEFAULT 0");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 9 && newVersion >= 9) {
                Log_OC.i(SQL, "Entering in the #9 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_IS_DOWNLOADING + " INTEGER " +
                                   " DEFAULT 0");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 10 && newVersion >= 10) {
                Log_OC.i(SQL, "Entering in the #10 ADD in onUpgrade");
                updateAccountName(db);
                upgraded = true;
            }
            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 11 && newVersion >= 11) {
                Log_OC.i(SQL, "Entering in the #11 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_ETAG_IN_CONFLICT + " TEXT " +
                                   " DEFAULT NULL");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 12 && newVersion >= 12) {
                Log_OC.i(SQL, "Entering in the #12 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_SHARED_WITH_SHAREE + " INTEGER " +
                                   " DEFAULT 0");
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 13 && newVersion >= 13) {
                Log_OC.i(SQL, "Entering in the #13 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    // Create capabilities table
                    createCapabilitiesTable(db);
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 14 && newVersion >= 14) {
                Log_OC.i(SQL, "Entering in the #14 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    // drop old instant_upload table
                    db.execSQL("DROP TABLE IF EXISTS " + "instant_upload" + ";");
                    // Create uploads table
                    createUploadsTable(db);
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 15 && newVersion >= 15) {
                Log_OC.i(SQL, "Entering in the #15 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    // drop old capabilities table
                    db.execSQL("DROP TABLE IF EXISTS " + "capabilities" + ";");
                    // Create uploads table
                    createCapabilitiesTable(db);
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 16 && newVersion >= 16) {
                Log_OC.i(SQL, "Entering in the #16 ADD synced folders table");
                db.beginTransaction();
                try {
                    // Create synced folders table
                    createSyncedFoldersTable(db);
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 17 && newVersion >= 17) {
                Log_OC.i(SQL, "Entering in the #17 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_FAVORITE +
                                   " INTEGER " + " DEFAULT 0");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 18 && newVersion >= 18) {
                Log_OC.i(SQL, "Entering in the #18 Adding external link column to capabilities");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_EXTERNAL_LINKS +
                                   " INTEGER " + " DEFAULT -1");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 19 && newVersion >= 19) {
                Log_OC.i(SQL, "Entering in the #19 Adding external link column to capabilities");
                db.beginTransaction();
                try {
                    createExternalLinksTable(db);
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 20 && newVersion >= 20) {
                Log_OC.i(SQL, "Entering in the #20 Adding arbitrary data table");
                db.beginTransaction();
                try {
                    createArbitraryData(db);
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 21 && newVersion >= 21) {
                Log_OC.i(SQL, "Entering in the #21 Adding virtual table");
                db.beginTransaction();
                try {
                    createVirtualTable(db);
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 22 && newVersion >= 22) {
                Log_OC.i(SQL, "Entering in the #22 Adding user theming to capabilities table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_NAME + " TEXT ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_COLOR + " TEXT ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL + " TEXT ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN + " TEXT ");
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 23 && newVersion >= 23) {
                Log_OC.i(SQL, "Entering in the #23 adding type column for synced folders, Create filesystem table");
                db.beginTransaction();
                try {
                    // add type column default being CUSTOM (0)
                    if (!checkIfColumnExists(db, ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME,
                                             ProviderTableMeta.SYNCED_FOLDER_TYPE)) {
                        Log_OC.i(SQL, "Add type column and default value 0 (CUSTOM) to synced_folders table");
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.SYNCED_FOLDER_TYPE +
                                       " INTEGER " + " DEFAULT 0");
                    } else {
                        Log_OC.i(SQL, "Type column of synced_folders table already exists");
                    }

                    if (!checkIfColumnExists(db, ProviderTableMeta.UPLOADS_TABLE_NAME,
                                             ProviderTableMeta.UPLOADS_IS_WIFI_ONLY)) {
                        Log_OC.i(SQL, "Add charging and wifi columns to uploads");
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.UPLOADS_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.UPLOADS_IS_WIFI_ONLY +
                                       " INTEGER " + " DEFAULT 0");
                    } else {
                        Log_OC.i(SQL, "Wifi column of uploads table already exists");
                    }

                    if (!checkIfColumnExists(db, ProviderTableMeta.UPLOADS_TABLE_NAME,
                                             ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.UPLOADS_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY +
                                       " INTEGER " + " DEFAULT 0");
                    } else {
                        Log_OC.i(SQL, "Charging column of uploads table already exists");
                    }

                    // create Filesystem table
                    Log_OC.i(SQL, "Create filesystem table");
                    createFileSystemTable(db);

                    upgraded = true;
                    db.setTransactionSuccessful();

                } catch (Throwable t) {
                    Log_OC.e(TAG, "ERROR!", t);
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 24 && newVersion >= 24) {
                Log_OC.i(SQL, "Entering in the #24 Re-adding user theming to capabilities table");
                db.beginTransaction();
                try {
                    if (!checkIfColumnExists(db, ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderTableMeta.CAPABILITIES_SERVER_NAME)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_NAME + " TEXT ");
                    }

                    if (!checkIfColumnExists(db, ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderTableMeta.CAPABILITIES_SERVER_COLOR)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_COLOR + " TEXT ");
                    }

                    if (!checkIfColumnExists(db, ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL + " TEXT ");
                    }

                    if (!checkIfColumnExists(db, ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN + " TEXT ");
                    }

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 25 && newVersion >= 25) {
                Log_OC.i(SQL, "Entering in the #25 Adding encryption flag to file");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_IS_ENCRYPTED + " INTEGER ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_ENCRYPTED_NAME + " TEXT ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION + " INTEGER ");
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 26 && newVersion >= 26) {
                Log_OC.i(SQL, "Entering in the #26 Adding text and element color to capabilities");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR + " TEXT ");

                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 27 && newVersion >= 27) {
                Log_OC.i(SQL, "Entering in the #27 Adding token to ocUpload");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.UPLOADS_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN + " TEXT ");
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 28 && newVersion >= 28) {
                Log_OC.i(SQL, "Entering in the #28 Adding CRC32 column to filesystem table");
                db.beginTransaction();
                try {
                    if (!checkIfColumnExists(db, ProviderTableMeta.FILESYSTEM_TABLE_NAME,
                                             ProviderTableMeta.FILESYSTEM_CRC32)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.FILESYSTEM_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.FILESYSTEM_CRC32 + " TEXT ");
                    }
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 29 && newVersion >= 29) {
                Log_OC.i(SQL, "Entering in the #29 Adding background default/plain to capabilities");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT + " INTEGER ");

                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 30 && newVersion >= 30) {
                Log_OC.i(SQL, "Entering in the #30 Re-add 25, 26 if needed");
                db.beginTransaction();
                try {
                    if (!checkIfColumnExists(db, ProviderTableMeta.FILE_TABLE_NAME,
                                             ProviderTableMeta.FILE_IS_ENCRYPTED)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.FILE_IS_ENCRYPTED + " INTEGER ");
                    }
                    if (!checkIfColumnExists(db, ProviderTableMeta.FILE_TABLE_NAME,
                                             ProviderTableMeta.FILE_ENCRYPTED_NAME)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.FILE_ENCRYPTED_NAME + " TEXT ");
                    }
                    if (oldVersion > ARBITRARY_DATA_TABLE_INTRODUCTION_VERSION) {
                        if (!checkIfColumnExists(db, ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                                 ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION)) {
                            db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                           ADD_COLUMN + ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION + " INTEGER ");
                        }
                        if (!checkIfColumnExists(db, ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                                 ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR)) {
                            db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                           ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR + " TEXT ");
                        }
                        if (!checkIfColumnExists(db, ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                                 ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR)) {
                            db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                           ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR + " TEXT ");
                        }
                        if (!checkIfColumnExists(db, ProviderTableMeta.FILESYSTEM_TABLE_NAME,
                                                 ProviderTableMeta.FILESYSTEM_CRC32)) {
                            try {
                                db.execSQL(ALTER_TABLE + ProviderTableMeta.FILESYSTEM_TABLE_NAME +
                                               ADD_COLUMN + ProviderTableMeta.FILESYSTEM_CRC32 + " TEXT ");
                            } catch (SQLiteException e) {
                                Log_OC.d(TAG, "Known problem on adding same column twice when upgrading from 24->30");
                            }
                        }
                    }

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 31 && newVersion >= 31) {
                Log_OC.i(SQL, "Entering in the #31 add mount type");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_MOUNT_TYPE + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 32 && newVersion >= 32) {
                Log_OC.i(SQL, "Entering in the #32 add ocshares.is_password_protected");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.OCSHARES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED + " INTEGER "); // boolean

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 33 && newVersion >= 33) {
                Log_OC.i(SQL, "Entering in the #3 Adding activity to capability");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_ACTIVITY + " INTEGER ");
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 34 && newVersion >= 34) {
                Log_OC.i(SQL, "Entering in the #34 add redirect to external links");
                db.beginTransaction();
                try {
                    if (!checkIfColumnExists(db, ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME,
                                             ProviderTableMeta.EXTERNAL_LINKS_REDIRECT)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.EXTERNAL_LINKS_REDIRECT + " INTEGER "); // boolean
                    }
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 35 && newVersion >= 35) {
                Log_OC.i(SQL, "Entering in the #35 add note to share table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.OCSHARES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.OCSHARES_NOTE + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 36 && newVersion >= 36) {
                Log_OC.i(SQL, "Entering in the #36 add has-preview to file table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_HAS_PREVIEW + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 37 && newVersion >= 37) {
                Log_OC.i(SQL, "Entering in the #37 add hide-download to share table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.OCSHARES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 38 && newVersion >= 38) {
                Log_OC.i(SQL, "Entering in the #38 add richdocuments");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT + " INTEGER "); // boolean
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST + " TEXT "); // string

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 39 && newVersion >= 39) {
                Log_OC.i(SQL, "Entering in the #39 add richdocuments direct editing");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING + " INTEGER "); // bool

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 40 && newVersion >= 40) {
                Log_OC.i(SQL, "Entering in the #40 add unreadCommentsCount to file table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 41 && newVersion >= 41) {
                Log_OC.i(SQL, "Entering in the #41 add eTagOnServer");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_ETAG_ON_SERVER + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 42 && newVersion >= 42) {
                Log_OC.i(SQL, "Entering in the #42 add richDocuments templates");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 43 && newVersion >= 43) {
                Log_OC.i(SQL, "Entering in the #43 add ownerId and owner display name to file table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_OWNER_ID + " TEXT ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_OWNER_DISPLAY_NAME + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 44 && newVersion >= 44) {
                Log_OC.i(SQL, "Entering in the #44 add note to file table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_NOTE + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 45 && newVersion >= 45) {
                Log_OC.i(SQL, "Entering in the #45 add sharees to file table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_SHAREES + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 46 && newVersion >= 46) {
                Log_OC.i(SQL, "Entering in the #46 add optional mimetypes to capabilities table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST
                                   + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 47 && newVersion >= 47) {
                Log_OC.i(SQL, "Entering in the #47 add askForPassword to capability table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD +
                                   " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 48 && newVersion >= 48) {
                Log_OC.i(SQL, "Entering in the #48 add product name to capabilities table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 49 && newVersion >= 49) {
                Log_OC.i(SQL, "Entering in the #49 add extended support to capabilities table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_EXTENDED_SUPPORT + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 50 && newVersion >= 50) {
                Log_OC.i(SQL, "Entering in the #50 add persistent enable date to synced_folders table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS + " INTEGER ");

                    db.execSQL("UPDATE " + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME + " SET " +
                                   ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS + " = CASE " +
                                   " WHEN enabled = 0 THEN " + SyncedFolder.EMPTY_ENABLED_TIMESTAMP_MS + " " +
                                   " ELSE " + clock.getCurrentTime() +
                                   " END ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 51 && newVersion >= 51) {
                Log_OC.i(SQL, "Entering in the #51 add show/hide to folderSync table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.SYNCED_FOLDER_HIDDEN + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 52 && newVersion >= 52) {
                Log_OC.i(SQL, "Entering in the #52 add etag for directEditing to capability");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 53 && newVersion >= 53) {
                Log_OC.i(SQL, "Entering in the #53 add rich workspace to file table");
                db.beginTransaction();
                try {
                    if (!checkIfColumnExists(db, ProviderTableMeta.FILE_TABLE_NAME,
                                             ProviderTableMeta.FILE_RICH_WORKSPACE)) {
                        db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                       ADD_COLUMN + ProviderTableMeta.FILE_RICH_WORKSPACE + " TEXT ");
                    }
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 54 && newVersion >= 54) {
                Log_OC.i(SQL, "Entering in the #54 add synced.existing," +
                    " rename uploads.force_overwrite to uploads.name_collision_policy");
                db.beginTransaction();
                try {
                    // Add synced.existing
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.SYNCED_FOLDER_EXISTING + " INTEGER "); // boolean


                    // Rename uploads.force_overwrite to uploads.name_collision_policy
                    String tmpTableName = ProviderTableMeta.UPLOADS_TABLE_NAME + "_old";
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.UPLOADS_TABLE_NAME + " RENAME TO " + tmpTableName);
                    createUploadsTable(db);
                    db.execSQL("INSERT INTO " + ProviderTableMeta.UPLOADS_TABLE_NAME + " (" +
                                   ProviderTableMeta._ID + ", " +
                                   ProviderTableMeta.UPLOADS_LOCAL_PATH + ", " +
                                   ProviderTableMeta.UPLOADS_REMOTE_PATH + ", " +
                                   ProviderTableMeta.UPLOADS_ACCOUNT_NAME + ", " +
                                   ProviderTableMeta.UPLOADS_FILE_SIZE + ", " +
                                   ProviderTableMeta.UPLOADS_STATUS + ", " +
                                   ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + ", " +
                                   ProviderTableMeta.UPLOADS_UPLOAD_TIME + ", " +
                                   ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY + ", " +
                                   ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + ", " +
                                   ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + ", " +
                                   ProviderTableMeta.UPLOADS_LAST_RESULT + ", " +
                                   ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + ", " +
                                   ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + ", " +
                                   ProviderTableMeta.UPLOADS_CREATED_BY + ", " +
                                   ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN +
                                   ") " +
                                   " SELECT " +
                                   ProviderTableMeta._ID + ", " +
                                   ProviderTableMeta.UPLOADS_LOCAL_PATH + ", " +
                                   ProviderTableMeta.UPLOADS_REMOTE_PATH + ", " +
                                   ProviderTableMeta.UPLOADS_ACCOUNT_NAME + ", " +
                                   ProviderTableMeta.UPLOADS_FILE_SIZE + ", " +
                                   ProviderTableMeta.UPLOADS_STATUS + ", " +
                                   ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + ", " +
                                   ProviderTableMeta.UPLOADS_UPLOAD_TIME + ", " +
                                   "force_overwrite" + ", " + // See FileUploader.NameCollisionPolicy
                                   ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + ", " +
                                   ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + ", " +
                                   ProviderTableMeta.UPLOADS_LAST_RESULT + ", " +
                                   ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + ", " +
                                   ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + ", " +
                                   ProviderTableMeta.UPLOADS_CREATED_BY + ", " +
                                   ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN +
                                   " FROM " + tmpTableName);
                    db.execSQL("DROP TABLE " + tmpTableName);

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 55 && newVersion >= 55) {
                Log_OC.i(SQL, "Entering in the #55 add synced.name_collision_policy.");
                db.beginTransaction();
                try {
                    // Add synced.name_collision_policy
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY + " INTEGER "); // integer

                    // make sure all existing folders set to FileUploader.NameCollisionPolicy.ASK_USER.
                    db.execSQL("UPDATE " + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME + " SET " +
                                   ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY + " = " +
                                   NameCollisionPolicy.ASK_USER.serialize());
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 56 && newVersion >= 56) {
                Log_OC.i(SQL, "Entering in the #56 add decrypted remote path");
                db.beginTransaction();
                try {
                    // Add synced.name_collision_policy
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_PATH_DECRYPTED + " TEXT "); // strin

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 57 && newVersion >= 57) {
                Log_OC.i(SQL, "Entering in the #57 add etag for capabilities");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_ETAG + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 58 && newVersion >= 58) {
                Log_OC.i(SQL, "Entering in the #58 add public link to share table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.OCSHARES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.OCSHARES_SHARE_LINK + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 59 && newVersion >= 59) {
                Log_OC.i(SQL, "Entering in the #59 add public label to share table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.OCSHARES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.OCSHARES_SHARE_LABEL + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 60 && newVersion >= 60) {
                Log_OC.i(SQL, "Entering in the #60 add user status to capability table");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_USER_STATUS + " INTEGER ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI + " INTEGER ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 61 && newVersion >= 61) {
                Log_OC.i(SQL, "Entering in the #61 reset eTag to force capability refresh");
                db.beginTransaction();
                try {
                    db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 62 && newVersion >= 62) {
                Log_OC.i(SQL, "Entering in the #62 add logo to capability");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.CAPABILITIES_SERVER_LOGO + " TEXT ");

                    // force refresh
                    db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (oldVersion < 63 && newVersion >= 63) {
                Log_OC.i(SQL, "Adding file locking columns");
                db.beginTransaction();
                try {
                    // locking capabilities
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.CAPABILITIES_TABLE_NAME + ADD_COLUMN + ProviderTableMeta.CAPABILITIES_FILES_LOCKING_VERSION + " TEXT ");
                    // force refresh
                    db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");
                    // locking properties
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LOCKED + " INTEGER "); // boolean
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LOCK_TYPE + " INTEGER ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LOCK_OWNER + " TEXT ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LOCK_OWNER_DISPLAY_NAME + " TEXT ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LOCK_OWNER_EDITOR + " TEXT ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LOCK_TIMESTAMP + " INTEGER ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LOCK_TIMEOUT + " INTEGER ");
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_LOCK_TOKEN + " TEXT ");
                    db.execSQL("UPDATE " + ProviderTableMeta.FILE_TABLE_NAME + " SET " + ProviderTableMeta.FILE_ETAG + " = '' WHERE 1=1");

                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }

            if (oldVersion < 64 && newVersion >= 64) {
                Log_OC.i(SQL, "Entering in the #64 add metadata size to files");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderTableMeta.FILE_METADATA_SIZE + " TEXT ");

                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            if (!upgraded) {
                Log_OC.i(SQL, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
            }
        }
    }
}
