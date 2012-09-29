/* ownCloud Android client application
 *   Copyright (C) 2011-2012  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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

/**
 * Custom database helper for ownCloud
 * 
 * @author Bartek Przybylski
 * 
 */
public class DbHandler {
    private SQLiteDatabase mDB;
    private OpenerHepler mHelper;
    private final String mDatabaseName = "ownCloud";
    private final int mDatabaseVersion = 1;
    
    private final String TABLE_INSTANT_UPLOAD = "instant_upload";

    public DbHandler(Context context) {
        mHelper = new OpenerHepler(context);
        mDB = mHelper.getWritableDatabase();
    }

    public void close() {
        mDB.close();
    }

    public boolean putFileForLater(String filepath, String account) {
        ContentValues cv = new ContentValues();
        cv.put("path", filepath);
        cv.put("account", account);
        return mDB.insert(TABLE_INSTANT_UPLOAD, null, cv) != -1;
    }
    
    public Cursor getAwaitingFiles() {
        return mDB.query(TABLE_INSTANT_UPLOAD, null, null, null, null, null, null);
    }
    
    public void clearFiles() {
        mDB.delete(TABLE_INSTANT_UPLOAD, null, null);
    }
    
    private class OpenerHepler extends SQLiteOpenHelper {
        public OpenerHepler(Context context) {
            super(context, mDatabaseName, null, mDatabaseVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_INSTANT_UPLOAD + " ("
            		+ " _id INTEGET PRIMARY KEY, "
            		+ " path TEXT,"
            		+ " account TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}
