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
import android.net.Uri;
import android.os.Binder;
import android.text.TextUtils;

import com.nextcloud.client.core.Clock;
import com.nextcloud.client.database.NextcloudDatabase;
import com.owncloud.android.R;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.utils.MimeType;

import java.util.ArrayList;
import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQueryBuilder;
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
    private static final int VIRTUAL = 10;
    private static final int FILESYSTEM = 11;
    private static final String TAG = FileContentProvider.class.getSimpleName();
    // todo avoid string concatenation and use string formatting instead later.
    private static final String ERROR = "ERROR ";
    private static final int SINGLE_PATH_SEGMENT = 1;
    public static final int MINIMUM_PATH_SEGMENTS_SIZE = 1;

    private static final String[] PROJECTION_CONTENT_TYPE = new String[]{
        ProviderTableMeta._ID, ProviderTableMeta.FILE_CONTENT_TYPE
    };
    private static final String[] PROJECTION_REMOTE_ID = new String[]{
        ProviderTableMeta._ID, ProviderTableMeta.FILE_REMOTE_ID
    };
    private static final String[] PROJECTION_FILE_PATH_AND_OWNER = new String[]{
        ProviderTableMeta._ID, ProviderTableMeta.FILE_PATH, ProviderTableMeta.FILE_ACCOUNT_OWNER
    };


    @Inject protected Clock clock;
    @Inject NextcloudDatabase database;
    private SupportSQLiteOpenHelper mDbHelper;
    private Context mContext;
    private UriMatcher mUriMatcher;

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        if (isCallerNotAllowed(uri)) {
            return -1;
        }

        int count;
        SupportSQLiteDatabase db = mDbHelper.getWritableDatabase();
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

    private int delete(SupportSQLiteDatabase db, Uri uri, String where, String... whereArgs) {
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

    private int deleteDirectory(SupportSQLiteDatabase db, Uri uri, String where, String... whereArgs) {
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

    private int deleteSingleFile(SupportSQLiteDatabase db, Uri uri, String where, String... whereArgs) {
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

    private int deleteWithUri(SupportSQLiteDatabase db, Uri uri, String where, String[] whereArgs) {
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
        SupportSQLiteDatabase db = mDbHelper.getWritableDatabase();
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

    private Uri insert(SupportSQLiteDatabase db, Uri uri, ContentValues values) {
        // verify only for those requests that are not internal (files table)
        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY, SINGLE_FILE, DIRECTORY -> VerificationUtils.verifyColumns(values);
        }

        switch (mUriMatcher.match(uri)) {
            case ROOT_DIRECTORY, SINGLE_FILE -> {
                String where = ProviderTableMeta.FILE_PATH + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
                String remotePath = values.getAsString(ProviderTableMeta.FILE_PATH);
                String accountName = values.getAsString(ProviderTableMeta.FILE_ACCOUNT_OWNER);
                String[] whereArgs = {remotePath, accountName};
                Cursor doubleCheck = query(db, uri, PROJECTION_FILE_PATH_AND_OWNER, where, whereArgs, null);
                // ugly patch; serious refactoring is needed to reduce work in
                // FileDataStorageManager and bring it to FileContentProvider
                if (!doubleCheck.moveToFirst()) {
                    doubleCheck.close();
                    long rowId = db.insert(ProviderTableMeta.FILE_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
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
            }
            case SHARES -> {
                Uri insertedShareUri;
                long idShares = db.insert(ProviderTableMeta.OCSHARES_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                if (idShares > 0) {
                    insertedShareUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_SHARE, idShares);
                } else {
                    throw new SQLException(ERROR + uri);

                }
                updateFilesTableAccordingToShareInsertion(db, values);
                return insertedShareUri;
            }
            case CAPABILITIES -> {
                Uri insertedCapUri;
                long idCapabilities = db.insert(ProviderTableMeta.CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                if (idCapabilities > 0) {
                    insertedCapUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_CAPABILITIES, idCapabilities);
                } else {
                    throw new SQLException(ERROR + uri);
                }
                return insertedCapUri;
            }
            case UPLOADS -> {
                Uri insertedUploadUri;
                long uploadId = db.insert(ProviderTableMeta.UPLOADS_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                if (uploadId > 0) {
                    insertedUploadUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_UPLOADS, uploadId);
                } else {
                    throw new SQLException(ERROR + uri);
                }
                return insertedUploadUri;
            }
            case SYNCED_FOLDERS -> {
                Uri insertedSyncedFolderUri;
                long syncedFolderId = db.insert(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                if (syncedFolderId > 0) {
                    insertedSyncedFolderUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                                                                         syncedFolderId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedSyncedFolderUri;
            }
            case EXTERNAL_LINKS -> {
                Uri insertedExternalLinkUri;
                long externalLinkId = db.insert(ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                if (externalLinkId > 0) {
                    insertedExternalLinkUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS,
                                                                         externalLinkId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedExternalLinkUri;
            }
            case VIRTUAL -> {
                Uri insertedVirtualUri;
                long virtualId = db.insert(ProviderTableMeta.VIRTUAL_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                if (virtualId > 0) {
                    insertedVirtualUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_VIRTUAL, virtualId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedVirtualUri;
            }
            case FILESYSTEM -> {
                Uri insertedFilesystemUri;
                long filesystemId = db.insert(ProviderTableMeta.FILESYSTEM_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values);
                if (filesystemId > 0) {
                    insertedFilesystemUri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_FILESYSTEM,
                                                                       filesystemId);
                } else {
                    throw new SQLException("ERROR " + uri);
                }
                return insertedFilesystemUri;
            }
            default -> throw new IllegalArgumentException("Unknown uri id: " + uri);
        }
    }

    private void updateFilesTableAccordingToShareInsertion(SupportSQLiteDatabase db, ContentValues newShare) {
        ContentValues fileValues = new ContentValues();
        ShareType newShareType = ShareType.fromValue(newShare.getAsInteger(ProviderTableMeta.OCSHARES_SHARE_TYPE));

        switch (newShareType) {
            case PUBLIC_LINK -> fileValues.put(ProviderTableMeta.FILE_SHARED_VIA_LINK, 1);
            case USER, GROUP, EMAIL, FEDERATED, FEDERATED_GROUP, ROOM, CIRCLE, DECK, GUEST ->
                fileValues.put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, 1);
            default -> {
            }
            // everything should be handled
        }

        String where = ProviderTableMeta.FILE_PATH + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?";
        String[] whereArgs = new String[]{
            newShare.getAsString(ProviderTableMeta.OCSHARES_PATH),
            newShare.getAsString(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)
        };
        db.update(ProviderTableMeta.FILE_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, fileValues, where, whereArgs);
    }


    @Override
    public boolean onCreate() {
        AndroidInjection.inject(this);
        mDbHelper = database.getOpenHelper();
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
        SupportSQLiteDatabase db = mDbHelper.getReadableDatabase();
        db.beginTransaction();
        try {
            result = query(db, uri, projection, selection, selectionArgs, sortOrder);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return result;
    }

    private Cursor query(SupportSQLiteDatabase db,
                         Uri uri,
                         String[] projectionArray,
                         String selection,
                         String[] selectionArgs,
                         String sortOrder) {

        // verify only for those requests that are not internal
        final int uriMatch = mUriMatcher.match(uri);

        String tableName;
        switch (uriMatch) {
            case ROOT_DIRECTORY:
            case DIRECTORY:
            case SINGLE_FILE:
                VerificationUtils.verifyWhere(selection); // prevent injection in public paths
                tableName = ProviderTableMeta.FILE_TABLE_NAME;
                break;
            case SHARES:
                tableName = ProviderTableMeta.OCSHARES_TABLE_NAME;
                break;
            case CAPABILITIES:
                tableName = ProviderTableMeta.CAPABILITIES_TABLE_NAME;
                break;
            case UPLOADS:
                tableName = ProviderTableMeta.UPLOADS_TABLE_NAME;
                break;
            case SYNCED_FOLDERS:
                tableName = ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME;
                break;
            case EXTERNAL_LINKS:
                tableName = ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME;
                break;
            case VIRTUAL:
                tableName = ProviderTableMeta.VIRTUAL_TABLE_NAME;
                break;
            case FILESYSTEM:
                tableName = ProviderTableMeta.FILESYSTEM_TABLE_NAME;
                break;
            default:
                throw new IllegalArgumentException("Unknown uri id: " + uri);
        }

        SupportSQLiteQueryBuilder queryBuilder = SupportSQLiteQueryBuilder.builder(tableName);


        // add ID to arguments if Uri has more than one segment
        if (uriMatch != ROOT_DIRECTORY && uri.getPathSegments().size() > SINGLE_PATH_SEGMENT) {
            String idColumn = uriMatch == DIRECTORY ? ProviderTableMeta.FILE_PARENT : ProviderTableMeta._ID;
            selection = idColumn + "=? AND " + selection;
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

        // only file list is publicly accessible via content provider, so only this has to be protected
        if ((uriMatch == ROOT_DIRECTORY || uriMatch == SINGLE_FILE ||
            uriMatch == DIRECTORY) && projectionArray != null && projectionArray.length > 0) {
            for (String column : projectionArray) {
                VerificationUtils.verifyColumnName(column);
            }
        }

        // if both are null, let them pass to query
        if (selectionArgs == null && selection != null) {
            selectionArgs = new String[]{selection};
            selection = "(?)";
        }

        if (!TextUtils.isEmpty(selection)) {
            queryBuilder.selection(selection, selectionArgs);
        }
        if (!TextUtils.isEmpty(order)) {
            queryBuilder.orderBy(order);
        }
        if (projectionArray != null && projectionArray.length > 0) {
            queryBuilder.columns(projectionArray);
        }
        final SupportSQLiteQuery supportSQLiteQuery = queryBuilder.create();
        final Cursor c = db.query(supportSQLiteQuery);
        c.setNotificationUri(mContext.getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (isCallerNotAllowed(uri)) {
            return -1;
        }

        int count;
        SupportSQLiteDatabase db = mDbHelper.getWritableDatabase();
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

    private int update(SupportSQLiteDatabase db, Uri uri, ContentValues values, String selection, String... selectionArgs) {
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
                return db.update(ProviderTableMeta.OCSHARES_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
            case CAPABILITIES:
                return db.update(ProviderTableMeta.CAPABILITIES_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
            case UPLOADS:
                return db.update(ProviderTableMeta.UPLOADS_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
            case SYNCED_FOLDERS:
                return db.update(ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
            case FILESYSTEM:
                return db.update(ProviderTableMeta.FILESYSTEM_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
            default:
                return db.update(ProviderTableMeta.FILE_TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, selection, selectionArgs);
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

        SupportSQLiteDatabase database = mDbHelper.getWritableDatabase();
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

    private boolean isCallerNotAllowed(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case SHARES:
            case CAPABILITIES:
            case UPLOADS:
            case SYNCED_FOLDERS:
            case EXTERNAL_LINKS:
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
            if (contentValues == null || contentValues.keySet().isEmpty()) {
                return;
            }

            for (String name : contentValues.keySet()) {
                verifyColumnName(name);
            }
        }

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
}
