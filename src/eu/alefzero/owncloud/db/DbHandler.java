/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
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
package eu.alefzero.owncloud.db;

import java.util.Vector;

import eu.alefzero.owncloud.OwnCloudSession;

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
    private final String TABLE_SESSIONS = "sessions";
    private final int mDatabaseVersion = 1;

    public DbHandler(Context context) {
        mHelper = new OpenerHepler(context);
        mDB = mHelper.getWritableDatabase();
    }

    public Vector<OwnCloudSession> getSessionList() {
        Cursor c = mDB
                .query(TABLE_SESSIONS, null, null, null, null, null, null);
        Vector<OwnCloudSession> v = new Vector<OwnCloudSession>();
        if (!c.moveToFirst()) {
            return v;
        }
        while (!c.isAfterLast()) {
            v.add(new OwnCloudSession(c.getString(c
                    .getColumnIndex("sessionName")), c.getString(c
                    .getColumnIndex("sessionUrl")), c.getInt(c
                    .getColumnIndex("_id"))));
            c.moveToNext();
        }
        c.close();
        return v;
    }

    public void addSession(String sessionName, String uri) {
        ContentValues cv = new ContentValues();
        cv.put("sessionName", sessionName);
        cv.put("sessionUrl", uri);
        mDB.insert(TABLE_SESSIONS, null, cv);
    }

    public void removeSessionWithId(int sessionId) {
        mDB.delete(TABLE_SESSIONS, "_id = ?",
                new String[] { String.valueOf(sessionId) });
    }

    public void changeSessionFields(int id, String hostname, String uri) {
        ContentValues cv = new ContentValues();
        cv.put("sessionName", hostname);
        cv.put("sessionUrl", uri);
        mDB.update(TABLE_SESSIONS, cv, "_id = ?",
                new String[] { String.valueOf(id) });
    }

    public void close() {
        mDB.close();
    }

    private class OpenerHepler extends SQLiteOpenHelper {
        public OpenerHepler(Context context) {
            super(context, mDatabaseName, null, mDatabaseVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_SESSIONS + " ("
                    + " _id INTEGER PRIMARY KEY, " + " sessionName TEXT, "
                    + " sessionUrl  TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}
