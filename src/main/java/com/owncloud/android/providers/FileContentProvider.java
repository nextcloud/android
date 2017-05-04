/**
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.providers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The ContentProvider for the ownCloud App.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class FileContentProvider extends ContentProvider {

    private DataBaseHelper mDbHelper;

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

    private static final String TAG = FileContentProvider.class.getSimpleName();

    private UriMatcher mUriMatcher;
    // todo avoid string concatenation and use string formatting instead later.
    private static final String ERROR = "ERROR ";
    private static final String SQL = "SQL";
    private static final String INTEGER = " INTEGER, ";
    private static final String TEXT = " TEXT, ";
    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String ADD_COLUMN = " ADD COLUMN ";
    private static final String UPGRADE_VERSION_MSG = "OUT of the ADD in onUpgrade; oldVersion == %d, newVersion == %d";

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        //Log_OC.d(TAG, "Deleting " + uri + " at provider " + this);
        int count = 0;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            count = delete(db, uri, where, whereArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private int delete(SQLiteDatabase db, Uri uri, String where, String[] whereArgs) {
        int count = 0;
        switch (mUriMatcher.match(uri)) {
            case SINGLE_FILE:
                Cursor c = query(db, uri, null, where, whereArgs, null);
                String remoteId = "";
                try {
                    if (c != null && c.moveToFirst()) {
                        remoteId = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_REMOTE_ID));
                        //ThumbnailsCacheManager.removeFileFromCache(remoteId);
                    }
                    Log_OC.d(TAG, "Removing FILE " + remoteId);

                    count = db.delete(ProviderTableMeta.FILE_TABLE_NAME,
                            ProviderTableMeta._ID
                                    + "="
                                    + uri.getPathSegments().get(1)
                                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : ""),
                            whereArgs);
                } catch (Exception e) {
                    Log_OC.d(TAG, "DB-Error removing file!", e);
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                break;
            case DIRECTORY:
                // deletion of folder is recursive
            /*
            Uri folderUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, Long.parseLong(uri.getPathSegments().get(1)));
            Cursor folder = query(db, folderUri, null, null, null, null);
            String folderName = "(unknown)";
            if (folder != null && folder.moveToFirst()) {
                folderName = folder.getString(folder.getColumnIndex(ProviderTableMeta.FILE_PATH));
            }
            */
                Cursor children = query(uri, null, null, null, null);
                if (children != null && children.moveToFirst()) {
                    long childId;
                    boolean isDir;
                    while (!children.isAfterLast()) {
                        childId = children.getLong(children.getColumnIndex(ProviderTableMeta._ID));
                        isDir = MimeType.DIRECTORY.equals(children.getString(
                                children.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)
                        ));
                        //remotePath = children.getString(children.getColumnIndex(ProviderTableMeta.FILE_PATH));
                        if (isDir) {
                            count += delete(
                                    db,
                                    ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, childId),
                                    null,
                                    null
                            );
                        } else {
                            count += delete(
                                    db,
                                    ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILE, childId),
                                    null,
                                    null
                            );
                        }
                        children.moveToNext();
                    }
                    children.close();
                } /*else {
                Log_OC.d(TAG, "No child to remove in DIRECTORY " + folderName);
            }
            Log_OC.d(TAG, "Removing DIRECTORY " + folderName + " (or maybe not) ");
            */
                count += db.delete(ProviderTableMeta.FILE_TABLE_NAME,
                        ProviderTableMeta._ID
                                + "="
                                + uri.getPathSegments().get(1)
                                + (!TextUtils.isEmpty(where) ? " AND (" + where
                                + ")" : ""), whereArgs);
            /* Just for log
             if (folder != null) {
                folder.close();
            }*/
                break;
            case ROOT_DIRECTORY:
                //Log_OC.d(TAG, "Removing ROOT!");
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
            default:
                //Log_OC.e(TAG, "Unknown uri " + uri);
                throw new IllegalArgumentException("Unknown uri: " + uri.toString());
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
                return ProviderTableMeta.CONTENT_TYPE;
            case SINGLE_FILE:
                return ProviderTableMeta.CONTENT_TYPE_ITEM;
            default:
                throw new IllegalArgumentException("Unknown Uri id."
                        + uri.toString());
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri newUri = null;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            newUri = insert(db, uri, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    private Uri insert(SQLiteDatabase db, Uri uri, ContentValues values) {
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
            case SINGLE_FILE:
                String remotePath = values.getAsString(ProviderTableMeta.FILE_PATH);
                String accountName = values.getAsString(ProviderTableMeta.FILE_ACCOUNT_OWNER);
                String[] projection = new String[]{
                        ProviderTableMeta._ID, ProviderTableMeta.FILE_PATH,
                        ProviderTableMeta.FILE_ACCOUNT_OWNER
                };
                String where = ProviderTableMeta.FILE_PATH + "=? AND " +
                        ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
                String[] whereArgs = new String[]{remotePath, accountName};
                Cursor doubleCheck = query(db, uri, projection, where, whereArgs, null);
                // ugly patch; serious refactorization is needed to reduce work in
                // FileDataStorageManager and bring it to FileContentProvider
                if (doubleCheck == null || !doubleCheck.moveToFirst()) {
                    if (doubleCheck != null) {
                        doubleCheck.close();
                    }
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
                            doubleCheck.getLong(doubleCheck.getColumnIndex(ProviderTableMeta._ID))
                    );
                    doubleCheck.close();

                    return insertedFileUri;
                }

            case SHARES:
                Uri insertedShareUri = null;
                long rowId = db.insert(ProviderTableMeta.OCSHARES_TABLE_NAME, null, values);
                if (rowId > 0) {
                    insertedShareUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_SHARE, rowId);
                } else {
                    throw new SQLException(ERROR + uri);

                }
                updateFilesTableAccordingToShareInsertion(db, values);
                return insertedShareUri;

            case CAPABILITIES:
                Uri insertedCapUri = null;
                long id = db.insert(ProviderTableMeta.CAPABILITIES_TABLE_NAME, null, values);
                if (id > 0) {
                    insertedCapUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_CAPABILITIES, id);
                } else {
                    throw new SQLException(ERROR + uri);

                }
                return insertedCapUri;

            case UPLOADS:
                Uri insertedUploadUri = null;
                long uploadId = db.insert(ProviderTableMeta.UPLOADS_TABLE_NAME, null, values);
                if (uploadId > 0) {
                    insertedUploadUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_UPLOADS, uploadId);
                    trimSuccessfulUploads(db);
                } else {
                    throw new SQLException(ERROR + uri);

                }
                return insertedUploadUri;

            case SYNCED_FOLDERS:
                Uri insertedSyncedFolderUri = null;
                long syncedFolderId = db.insert(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME, null, values);
                if (syncedFolderId > 0) {
                    insertedSyncedFolderUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS, syncedFolderId);
                } else {
                    throw new SQLException("ERROR " + uri);

                }
                return insertedSyncedFolderUri;

            case EXTERNAL_LINKS:
                Uri insertedExternalLinkUri = null;
                long externalLinkId = db.insert(ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME, null, values);
                if (externalLinkId > 0) {
                    insertedExternalLinkUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS, externalLinkId);
                } else {
                    throw new SQLException("ERROR " + uri);

                }
                return insertedExternalLinkUri;

            case ARBITRARY_DATA:
                Uri insertedArbitraryDataUri = null;
                long arbitraryDataId = db.insert(ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME, null, values);
                if (arbitraryDataId > 0) {
                    insertedArbitraryDataUri =
                            ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_ARBITRARY_DATA, arbitraryDataId);
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
            default:
                throw new IllegalArgumentException("Unknown uri id: " + uri);
        }

    }

    private void updateFilesTableAccordingToShareInsertion(
            SQLiteDatabase db, ContentValues newShare
    ) {
        ContentValues fileValues = new ContentValues();
        int newShareType = newShare.getAsInteger(ProviderTableMeta.OCSHARES_SHARE_TYPE);
        if (newShareType == ShareType.PUBLIC_LINK.getValue()) {
            fileValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, 1);
        } else if (
                newShareType == ShareType.USER.getValue() ||
                        newShareType == ShareType.GROUP.getValue() ||
                        newShareType == ShareType.EMAIL.getValue() ||
                        newShareType == ShareType.FEDERATED.getValue()) {
            fileValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, 1);
        }

        String where = ProviderTableMeta.FILE_PATH + "=? AND " +
                ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{
                newShare.getAsString(ProviderTableMeta.OCSHARES_PATH),
                newShare.getAsString(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)
        };
        db.update(ProviderTableMeta.FILE_TABLE_NAME, fileValues, where, whereArgs);
    }


    @Override
    public boolean onCreate() {
        mDbHelper = new DataBaseHelper(getContext());

        String authority = getContext().getResources().getString(R.string.authority);
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

        return true;
    }


    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {

        Cursor result = null;
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

    private Cursor query(
            SQLiteDatabase db,
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {

        SQLiteQueryBuilder sqlQuery = new SQLiteQueryBuilder();

        sqlQuery.setTables(ProviderTableMeta.FILE_TABLE_NAME);

        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY:
                break;
            case DIRECTORY:
                String folderId = uri.getPathSegments().get(1);
                sqlQuery.appendWhere(ProviderTableMeta.FILE_PARENT + "="
                        + folderId);
                break;
            case SINGLE_FILE:
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            case SHARES:
                sqlQuery.setTables(ProviderTableMeta.OCSHARES_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            case CAPABILITIES:
                sqlQuery.setTables(ProviderTableMeta.CAPABILITIES_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            case UPLOADS:
                sqlQuery.setTables(ProviderTableMeta.UPLOADS_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            case SYNCED_FOLDERS:
                sqlQuery.setTables(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            case EXTERNAL_LINKS:
                sqlQuery.setTables(ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            case ARBITRARY_DATA:
                sqlQuery.setTables(ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                            + uri.getPathSegments().get(1));
                }
                break;
            case VIRTUAL:
                sqlQuery.setTables(ProviderTableMeta.VIRTUAL_TABLE_NAME);
                if (uri.getPathSegments().size() > 1) {
                    sqlQuery.appendWhere(ProviderTableMeta._ID + "=" + uri.getPathSegments().get(1));
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown uri id: " + uri);
        }

        String order;
        if (TextUtils.isEmpty(sortOrder)) {
            switch (mUriMatcher.match(uri)) {
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
            }
        } else {
            order = sortOrder;
        }

        // DB case_sensitive
        db.execSQL("PRAGMA case_sensitive_like = true");
        Cursor c = sqlQuery.query(db, projection, selection, selectionArgs, null, null, order);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        int count = 0;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            count = update(db, uri, values, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    private int update(
            SQLiteDatabase db,
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs
    ) {
        switch (mUriMatcher.match(uri)) {
            case DIRECTORY:
                return 0; //updateFolderSize(db, selectionArgs[0]);
            case SHARES:
                return db.update(ProviderTableMeta.OCSHARES_TABLE_NAME, values, selection, selectionArgs);
            case CAPABILITIES:
                return db.update(ProviderTableMeta.CAPABILITIES_TABLE_NAME, values, selection, selectionArgs);
            case UPLOADS:
                int ret = db.update(ProviderTableMeta.UPLOADS_TABLE_NAME, values, selection, selectionArgs);
                trimSuccessfulUploads(db);
                return ret;
            case SYNCED_FOLDERS:
                return db.update(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME, values, selection, selectionArgs);
            case ARBITRARY_DATA:
                return db.update(ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME, values, selection, selectionArgs);
            default:
                return db.update(ProviderTableMeta.FILE_TABLE_NAME, values, selection, selectionArgs);
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        Log_OC.d("FileContentProvider", "applying batch in provider " + this +
                " (temporary: " + isTemporary() + ")");
        ContentProviderResult[] results = new ContentProviderResult[operations.size()];
        int i = 0;

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.beginTransaction();  // it's supposed that transactions can be nested
        try {
            for (ContentProviderOperation operation : operations) {
                results[i] = operation.apply(this, results, i);
                i++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        Log_OC.d("FileContentProvider", "applied batch in provider " + this);
        return results;
    }


    class DataBaseHelper extends SQLiteOpenHelper {

        public DataBaseHelper(Context context) {
            super(context, ProviderMeta.DB_NAME, null, ProviderMeta.DB_VERSION);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // files table
            Log_OC.i(SQL, "Entering in onCreate");
            createFilesTable(db);

            // Create ocshares table
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
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log_OC.i(SQL, "Entering in onUpgrade");
            boolean upgraded = false;
            if (oldVersion == 1 && newVersion >= 2) {
                Log_OC.i(SQL, "Entering in the #1 ADD in onUpgrade");
                db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                        ADD_COLUMN + ProviderTableMeta.FILE_KEEP_IN_SYNC + " INTEGER " +
                        " DEFAULT 0");
                upgraded = true;
            }
            if (oldVersion < 3 && newVersion >= 3) {
                Log_OC.i(SQL, "Entering in the #2 ADD in onUpgrade");
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
                Log_OC.i(SQL, "Entering in the #3 ADD in onUpgrade");
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
                Log_OC.i(SQL, "Entering in the #4 ADD in onUpgrade");
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
                Log_OC.i(SQL, "Entering in the #5 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                            ADD_COLUMN + ProviderTableMeta.FILE_SHARED_VIA_LINK + " INTEGER " +
                            " DEFAULT 0");

                    db.execSQL(ALTER_TABLE + ProviderTableMeta.FILE_TABLE_NAME +
                            ADD_COLUMN + ProviderTableMeta.FILE_PUBLIC_LINK + " TEXT " +
                            " DEFAULT NULL");

                    // Create table ocshares
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
                Log_OC.i(SQL, "Entering in the #20 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    // add type column default being LEGACY (3)
                    db.execSQL(ALTER_TABLE + ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                            ADD_COLUMN + ProviderTableMeta.SYNCED_FOLDER_TYPE +
                            " INTEGER " + " DEFAULT 3");

                    ContentResolver contentResolver = getContext().getContentResolver();

                    SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);

                    final List<MediaFolder> imageMediaFolders = MediaProvider.getImageFolders(contentResolver, 0);
                    final List<MediaFolder> videoMediaFolders = MediaProvider.getVideoFolders(contentResolver, 0);

                    ArrayList<Long> idsToDelete = new ArrayList<>();
                    for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
                        idsToDelete.add(syncedFolder.getId());
                        for (int i = 0; i < imageMediaFolders.size(); i++) {
                            if (imageMediaFolders.get(i).absolutePath.equals(syncedFolder.getLocalPath())) {
                                SyncedFolder imageSyncedFolder = (SyncedFolder) syncedFolder.clone();
                                imageSyncedFolder.setType(MediaFolder.IMAGE);
                                syncedFolderProvider.storeFolderSync(imageSyncedFolder);
                                break;
                            }
                        }

                        for (int j = 0; j < videoMediaFolders.size(); j++) {
                            if (videoMediaFolders.get(j).absolutePath.equals(syncedFolder.getLocalPath())) {
                                SyncedFolder videoSyncedFolder = (SyncedFolder) syncedFolder.clone();
                                videoSyncedFolder.setType(MediaFolder.VIDEO);
                                syncedFolderProvider.storeFolderSync(videoSyncedFolder);
                                break;
                            }
                        }
                    }

                    syncedFolderProvider.deleteSyncedFoldersInList(idsToDelete);

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
        }
    }

    private void createFilesTable(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + ProviderTableMeta.FILE_TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                + ProviderTableMeta.FILE_NAME + TEXT
                + ProviderTableMeta.FILE_PATH + TEXT
                + ProviderTableMeta.FILE_PARENT + INTEGER
                + ProviderTableMeta.FILE_CREATION + INTEGER
                + ProviderTableMeta.FILE_MODIFIED + INTEGER
                + ProviderTableMeta.FILE_CONTENT_TYPE + TEXT
                + ProviderTableMeta.FILE_CONTENT_LENGTH + INTEGER
                + ProviderTableMeta.FILE_STORAGE_PATH + TEXT
                + ProviderTableMeta.FILE_ACCOUNT_OWNER + TEXT
                + ProviderTableMeta.FILE_LAST_SYNC_DATE + INTEGER
                + ProviderTableMeta.FILE_KEEP_IN_SYNC + INTEGER
                + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA + INTEGER
                + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA + INTEGER
                + ProviderTableMeta.FILE_ETAG + TEXT
                + ProviderTableMeta.FILE_SHARED_VIA_LINK + INTEGER
                + ProviderTableMeta.FILE_PUBLIC_LINK + TEXT
                + ProviderTableMeta.FILE_PERMISSIONS + " TEXT null,"
                + ProviderTableMeta.FILE_REMOTE_ID + " TEXT null,"
                + ProviderTableMeta.FILE_UPDATE_THUMBNAIL + INTEGER //boolean
                + ProviderTableMeta.FILE_IS_DOWNLOADING + INTEGER //boolean
                + ProviderTableMeta.FILE_FAVORITE + INTEGER // boolean
                + ProviderTableMeta.FILE_ETAG_IN_CONFLICT + TEXT
                + ProviderTableMeta.FILE_SHARED_WITH_SHAREE + " INTEGER);"
        );
    }

    private void createOCSharesTable(SQLiteDatabase db) {
        // Create ocshares table
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
                + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " TEXT );");
    }

    private void createCapabilitiesTable(SQLiteDatabase db) {
        // Create capabilities table
        db.execSQL("CREATE TABLE " + ProviderTableMeta.CAPABILITIES_TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                + ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME + TEXT
                + ProviderTableMeta.CAPABILITIES_VERSION_MAYOR + INTEGER
                + ProviderTableMeta.CAPABILITIES_VERSION_MINOR + INTEGER
                + ProviderTableMeta.CAPABILITIES_VERSION_MICRO + INTEGER
                + ProviderTableMeta.CAPABILITIES_VERSION_STRING + TEXT
                + ProviderTableMeta.CAPABILITIES_VERSION_EDITION + TEXT
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
                + ProviderTableMeta.CAPABILITIES_FILES_DROP + INTEGER  // boolean
                + ProviderTableMeta.CAPABILITIES_EXTERNAL_LINKS + INTEGER  // boolean
                + ProviderTableMeta.CAPABILITIES_SERVER_NAME + TEXT
                + ProviderTableMeta.CAPABILITIES_SERVER_COLOR + TEXT
                + ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN + TEXT
                + ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL + " TEXT );");
    }

    private void createUploadsTable(SQLiteDatabase db) {
        // Create uploads table
        db.execSQL("CREATE TABLE " + ProviderTableMeta.UPLOADS_TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                + ProviderTableMeta.UPLOADS_LOCAL_PATH + TEXT
                + ProviderTableMeta.UPLOADS_REMOTE_PATH + TEXT
                + ProviderTableMeta.UPLOADS_ACCOUNT_NAME + TEXT
                + ProviderTableMeta.UPLOADS_FILE_SIZE + " LONG, "
                + ProviderTableMeta.UPLOADS_STATUS + INTEGER               // UploadStatus
                + ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + INTEGER      // Upload LocalBehaviour
                + ProviderTableMeta.UPLOADS_UPLOAD_TIME + INTEGER
                + ProviderTableMeta.UPLOADS_FORCE_OVERWRITE + INTEGER  // boolean
                + ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + INTEGER  // boolean
                + ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + INTEGER
                + ProviderTableMeta.UPLOADS_LAST_RESULT + INTEGER     // Upload LastResult
                + ProviderTableMeta.UPLOADS_CREATED_BY + " INTEGER );"    // Upload createdBy
        );


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
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "                          // id
                + ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH + " TEXT, "           // local path
                + ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH + " TEXT, "           // remote path
                + ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY + " INTEGER, "          // wifi_only
                + ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY + " INTEGER, "      // charging only
                + ProviderTableMeta.SYNCED_FOLDER_ENABLED + " INTEGER, "            // enabled
                + ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE + " INTEGER, "  // subfolder by date
                + ProviderTableMeta.SYNCED_FOLDER_ACCOUNT + "  TEXT, "              // account
                + ProviderTableMeta.SYNCED_FOLDER_TYPE + " INTEGER, "               // type
                + ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION + " INTEGER );"     // upload action
        );
    }

    private void createExternalLinksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "          // id
                + ProviderTableMeta.EXTERNAL_LINKS_ICON_URL + " TEXT, "     // icon url
                + ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE + " TEXT, "     // language
                + ProviderTableMeta.EXTERNAL_LINKS_TYPE + " INTEGER, "      // type
                + ProviderTableMeta.EXTERNAL_LINKS_NAME + " TEXT, "         // name
                + ProviderTableMeta.EXTERNAL_LINKS_URL + " TEXT )"          // url
        );
    }

    private void createArbitraryData(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.ARBITRARY_DATA_TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "      // id
                + ProviderTableMeta.ARBITRARY_DATA_CLOUD_ID + " TEXT, " // cloud id (account name + FQDN)
                + ProviderTableMeta.ARBITRARY_DATA_KEY + " TEXT, "      // key
                + ProviderTableMeta.ARBITRARY_DATA_VALUE + " TEXT) "    // value
        );
    }

    private void createVirtualTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderTableMeta.VIRTUAL_TABLE_NAME + "("
                + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "          // id
                + ProviderTableMeta.VIRTUAL_TYPE + " TEXT, "                // type
                + ProviderTableMeta.VIRTUAL_OCFILE_ID + " INTEGER )"        // file id
        );
    }

    /**
     * Version 10 of database does not modify its scheme. It coincides with the upgrade of the ownCloud account names
     * structure to include in it the path to the server instance. Updating the account names and path to local files
     * in the files table is a must to keep the existing account working and the database clean.
     *
     * See {@link com.owncloud.android.authentication.AccountUtils#updateAccountVersion(android.content.Context)}
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
            Account[] accounts = AccountManager.get(getContext()).getAccountsByType(
                    MainApp.getAccountType());
            String serverUrl;
            String username;
            String oldAccountName;
            String newAccountName;

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
                    int num = db.update(ProviderTableMeta.FILE_TABLE_NAME,
                            cv,
                            ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                            new String[]{oldAccountName});

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
     * Rename the local ownCloud folder of one account to match the a rename of the account itself. Updates the
     * table of files in database so that the paths to the local files keep being the same.
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
                null,
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

                // update database
                do {
                    // Update database
                    String oldPath = c.getString(
                            c.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
                    OCFile file = new OCFile(
                            c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH)));
                    String newPath = FileStorageUtils.getDefaultSavePathFor(newAccountName, file);

                    ContentValues cv = new ContentValues();
                    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, newPath);
                    db.update(ProviderTableMeta.FILE_TABLE_NAME,
                            cv,
                            ProviderTableMeta.FILE_STORAGE_PATH + "=?",
                            new String[]{oldPath});

                    Log_OC.v(SQL, "Updated path of downloaded file: old file name == " + oldPath +
                            ", new file name == " + newPath);

                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

    }

    /**
     * Grants that total count of successful uploads stored is not greater than MAX_SUCCESSFUL_UPLOADS.
     *
     * Removes older uploads if needed.
     */
    private void trimSuccessfulUploads(SQLiteDatabase db) {
        Cursor c = null;
        try {
            String MAX_SUCCESSFUL_UPLOADS = "30";
            c = db.rawQuery(
                    "delete from " + ProviderTableMeta.UPLOADS_TABLE_NAME +
                            " where " + ProviderTableMeta.UPLOADS_STATUS + " == "
                            + UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED.getValue() +
                            " and " + ProviderTableMeta._ID +
                            " not in (select " + ProviderTableMeta._ID +
                            " from " + ProviderTableMeta.UPLOADS_TABLE_NAME +
                            " where " + ProviderTableMeta.UPLOADS_STATUS + " == "
                            + UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED.getValue() +
                            " order by " + ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP +
                            " desc limit " + MAX_SUCCESSFUL_UPLOADS +
                            ")",
                    null
            );
            c.moveToFirst(); // do something with the cursor, or deletion doesn't happen; true story

        } catch (Exception e) {
            Log_OC.e(
                    TAG,
                    "Something wrong trimming successful uploads, database could grow more than expected",
                    e
            );

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }


}
