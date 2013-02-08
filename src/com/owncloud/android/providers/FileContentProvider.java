/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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

package com.owncloud.android.providers;

import java.util.HashMap;

import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * The ContentProvider for the ownCloud App.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileContentProvider extends ContentProvider {

    private DataBaseHelper mDbHelper;

    private static HashMap<String, String> mProjectionMap;
    static {
        mProjectionMap = new HashMap<String, String>();
        mProjectionMap.put(ProviderTableMeta._ID, ProviderTableMeta._ID);
        mProjectionMap.put(ProviderTableMeta.FILE_PARENT,
                ProviderTableMeta.FILE_PARENT);
        mProjectionMap.put(ProviderTableMeta.FILE_PATH,
                ProviderTableMeta.FILE_PATH);
        mProjectionMap.put(ProviderTableMeta.FILE_NAME,
                ProviderTableMeta.FILE_NAME);
        mProjectionMap.put(ProviderTableMeta.FILE_CREATION,
                ProviderTableMeta.FILE_CREATION);
        mProjectionMap.put(ProviderTableMeta.FILE_MODIFIED,
                ProviderTableMeta.FILE_MODIFIED);
        mProjectionMap.put(ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
                ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA);
        mProjectionMap.put(ProviderTableMeta.FILE_CONTENT_LENGTH,
                ProviderTableMeta.FILE_CONTENT_LENGTH);
        mProjectionMap.put(ProviderTableMeta.FILE_CONTENT_TYPE,
                ProviderTableMeta.FILE_CONTENT_TYPE);
        mProjectionMap.put(ProviderTableMeta.FILE_STORAGE_PATH,
                ProviderTableMeta.FILE_STORAGE_PATH);
        mProjectionMap.put(ProviderTableMeta.FILE_LAST_SYNC_DATE,
                ProviderTableMeta.FILE_LAST_SYNC_DATE);
        mProjectionMap.put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA,
                ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA);
        mProjectionMap.put(ProviderTableMeta.FILE_KEEP_IN_SYNC,
                ProviderTableMeta.FILE_KEEP_IN_SYNC);
        mProjectionMap.put(ProviderTableMeta.FILE_ACCOUNT_OWNER,
                ProviderTableMeta.FILE_ACCOUNT_OWNER);
    }

    private static final int SINGLE_FILE = 1;
    private static final int DIRECTORY = 2;
    private static final int ROOT_DIRECTORY = 3;
    private static final UriMatcher mUriMatcher;
    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(ProviderMeta.AUTHORITY_FILES, "/", ROOT_DIRECTORY);
        mUriMatcher.addURI(ProviderMeta.AUTHORITY_FILES, "file/", SINGLE_FILE);
        mUriMatcher.addURI(ProviderMeta.AUTHORITY_FILES, "file/#", SINGLE_FILE);
        mUriMatcher.addURI(ProviderMeta.AUTHORITY_FILES, "dir/#", DIRECTORY);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int count = 0;
        switch (mUriMatcher.match(uri)) {
        case SINGLE_FILE:
            count = db.delete(ProviderTableMeta.DB_NAME,
                    ProviderTableMeta._ID
                            + "="
                            + uri.getPathSegments().get(1)
                            + (!TextUtils.isEmpty(where) ? " AND (" + where
                                    + ")" : ""), whereArgs);
            break;
        case ROOT_DIRECTORY:
            count = db.delete(ProviderTableMeta.DB_NAME, where, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown uri: " + uri.toString());
        }
        getContext().getContentResolver().notifyChange(uri, null);
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
        if (mUriMatcher.match(uri) != SINGLE_FILE &&
            mUriMatcher.match(uri) != ROOT_DIRECTORY) {
            
            throw new IllegalArgumentException("Unknown uri id: " + uri);
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long rowId = db.insert(ProviderTableMeta.DB_NAME, null, values);
        if (rowId > 0) {
            Uri insertedFileUri = ContentUris.withAppendedId(
                    ProviderTableMeta.CONTENT_URI_FILE, rowId);
            getContext().getContentResolver().notifyChange(insertedFileUri,
                    null);
            return insertedFileUri;
        }
        throw new SQLException("ERROR " + uri);
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new DataBaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder sqlQuery = new SQLiteQueryBuilder();

        sqlQuery.setTables(ProviderTableMeta.DB_NAME);
        sqlQuery.setProjectionMap(mProjectionMap);

        switch (mUriMatcher.match(uri)) {
        case ROOT_DIRECTORY:
            break;
        case DIRECTORY:
            sqlQuery.appendWhere(ProviderTableMeta.FILE_PARENT + "="
                    + uri.getPathSegments().get(1));
            break;
        case SINGLE_FILE:
            if (uri.getPathSegments().size() > 1) {
                sqlQuery.appendWhere(ProviderTableMeta._ID + "="
                        + uri.getPathSegments().get(1));
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown uri id: " + uri);
        }

        String order;
        if (TextUtils.isEmpty(sortOrder)) {
            order = ProviderTableMeta.DEFAULT_SORT_ORDER;
        } else {
            order = sortOrder;
        }

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = sqlQuery.query(db, projection, selection, selectionArgs,
                null, null, order);

        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return mDbHelper.getWritableDatabase().update(
                ProviderTableMeta.DB_NAME, values, selection, selectionArgs);
    }

    class DataBaseHelper extends SQLiteOpenHelper {

        public DataBaseHelper(Context context) {
            super(context, ProviderMeta.DB_NAME, null, ProviderMeta.DB_VERSION);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // files table
            Log.i("SQL", "Entering in onCreate");
            db.execSQL("CREATE TABLE " + ProviderTableMeta.DB_NAME + "("
                    + ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                    + ProviderTableMeta.FILE_NAME + " TEXT, "
                    + ProviderTableMeta.FILE_PATH + " TEXT, "
                    + ProviderTableMeta.FILE_PARENT + " INTEGER, "
                    + ProviderTableMeta.FILE_CREATION + " INTEGER, "
                    + ProviderTableMeta.FILE_MODIFIED + " INTEGER, "
                    + ProviderTableMeta.FILE_CONTENT_TYPE + " TEXT, "
                    + ProviderTableMeta.FILE_CONTENT_LENGTH + " INTEGER, "
                    + ProviderTableMeta.FILE_STORAGE_PATH + " TEXT, "
                    + ProviderTableMeta.FILE_ACCOUNT_OWNER + " TEXT, "
                    + ProviderTableMeta.FILE_LAST_SYNC_DATE + " INTEGER, "
                    + ProviderTableMeta.FILE_KEEP_IN_SYNC + " INTEGER, "
                    + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA + " INTEGER, "
                    + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA + " INTEGER );"
                    );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i("SQL", "Entering in onUpgrade");
            boolean upgraded = false; 
            if (oldVersion == 1 && newVersion >= 2) {
                Log.i("SQL", "Entering in the #1 ADD in onUpgrade");
                db.execSQL("ALTER TABLE " + ProviderTableMeta.DB_NAME +
                           " ADD COLUMN " + ProviderTableMeta.FILE_KEEP_IN_SYNC  + " INTEGER " +
                           " DEFAULT 0");
                upgraded = true;
            }
            if (oldVersion < 3 && newVersion >= 3) {
                Log.i("SQL", "Entering in the #2 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE " + ProviderTableMeta.DB_NAME +
                               " ADD COLUMN " + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA  + " INTEGER " +
                               " DEFAULT 0");
                    
                    // assume there are not local changes pending to upload
                    db.execSQL("UPDATE " + ProviderTableMeta.DB_NAME + 
                            " SET " + ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA + " = " + System.currentTimeMillis() + 
                            " WHERE " + ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL");
                 
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (oldVersion < 4 && newVersion >= 4) {
                Log.i("SQL", "Entering in the #3 ADD in onUpgrade");
                db.beginTransaction();
                try {
                    db .execSQL("ALTER TABLE " + ProviderTableMeta.DB_NAME +
                           " ADD COLUMN " + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA  + " INTEGER " +
                           " DEFAULT 0");
                
                    db.execSQL("UPDATE " + ProviderTableMeta.DB_NAME + 
                           " SET " + ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA + " = " + ProviderTableMeta.FILE_MODIFIED + 
                           " WHERE " + ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL");
                
                    upgraded = true;
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            if (!upgraded)
                Log.i("SQL", "OUT of the ADD in onUpgrade; oldVersion == " + oldVersion + ", newVersion == " + newVersion);
        }

    }

}
