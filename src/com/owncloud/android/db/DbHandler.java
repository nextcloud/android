/* ownCloud Android client application
 *   Copyright (C) 2011-2012  Bartek Przybylski
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
package com.owncloud.android.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Custom database helper for ownCloud
 * 
 * @author Bartek Przybylski
 * 
 */
public class DbHandler {
    private SQLiteDatabase mDB;
    private OpenerHelper mHelper;
    private final String mDatabaseName = "ownCloud";
    private final int mDatabaseVersion = 3;

    private final String TABLE_INSTANT_UPLOAD = "instant_upload";

    public static final int UPLOAD_STATUS_UPLOAD_LATER = 0;
    public static final int UPLOAD_STATUS_UPLOAD_FAILED = 1;

    public DbHandler(Context context) {
        mHelper = new OpenerHelper(context);
        mDB = mHelper.getWritableDatabase();
    }

    public void close() {
        mDB.close();
    }

    public boolean putFileForLater(String filepath, String account, String message) {
        ContentValues cv = new ContentValues();
        cv.put("path", filepath);
        cv.put("account", account);
        cv.put("attempt", UPLOAD_STATUS_UPLOAD_LATER);
        cv.put("message", message);
        long result = mDB.insert(TABLE_INSTANT_UPLOAD, null, cv);
        Log.d(TABLE_INSTANT_UPLOAD, "putFileForLater returns with: " + result + " for file: " + filepath);
        return result != -1;
    }

    public int updateFileState(String filepath, Integer status, String message) {
        ContentValues cv = new ContentValues();
        cv.put("attempt", status);
        cv.put("message", message);
        int result = mDB.update(TABLE_INSTANT_UPLOAD, cv, "path=?", new String[] { filepath });
        Log.d(TABLE_INSTANT_UPLOAD, "updateFileState returns with: " + result + " for file: " + filepath);
        return result;
    }

    public Cursor getAwaitingFiles() {
        return mDB.query(TABLE_INSTANT_UPLOAD, null, "attempt=" + UPLOAD_STATUS_UPLOAD_LATER, null, null, null, null);
    }

    public Cursor getFailedFiles() {
        return mDB.query(TABLE_INSTANT_UPLOAD, null, "attempt>" + UPLOAD_STATUS_UPLOAD_LATER, null, null, null, null);
    }

    public void clearFiles() {
        mDB.delete(TABLE_INSTANT_UPLOAD, null, null);
    }

    /**
     * 
     * @param localPath
     * @return true when one or more pending files was removed
     */
    public boolean removeIUPendingFile(String localPath) {
        long result = mDB.delete(TABLE_INSTANT_UPLOAD, "path = ?", new String[] { localPath });
        Log.d(TABLE_INSTANT_UPLOAD, "delete returns with: " + result + " for file: " + localPath);
        return result != 0;

    }

    private class OpenerHelper extends SQLiteOpenHelper {
        public OpenerHelper(Context context) {
            super(context, mDatabaseName, null, mDatabaseVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_INSTANT_UPLOAD + " (" + " _id INTEGER PRIMARY KEY, " + " path TEXT,"
                    + " account TEXT,attempt INTEGER,message TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE " + TABLE_INSTANT_UPLOAD + " ADD COLUMN attempt INTEGER;");
            }
            db.execSQL("ALTER TABLE " + TABLE_INSTANT_UPLOAD + " ADD COLUMN message TEXT;");

        }
    }
}
