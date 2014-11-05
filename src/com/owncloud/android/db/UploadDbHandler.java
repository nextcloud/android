/* ownCloud Android client application
 *   Copyright (C) 2011-2012  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

import java.util.ArrayList;
import java.util.List;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database helper for storing list of files to be uploaded, including status information for each file.
 * 
 * @author Bartek Przybylski
 * @author LukeOwncloud
 * 
 */
public class UploadDbHandler {
    private SQLiteDatabase mDB;
    private OpenerHelper mHelper;
    private final String mDatabaseName;
    private final int mDatabaseVersion = 4;

    static private final String TAG = "UploadDbHandler";
    static private final String TABLE_UPLOAD = "list_of_uploads";

    public void recreateDb() {
        mDB.beginTransaction();
        try {
            mHelper.onUpgrade(mDB, 0, mDatabaseVersion);
            mDB.setTransactionSuccessful();
        } finally {
            mDB.endTransaction();
        }

    }

    public enum UploadStatus {
        UPLOAD_LATER(0), UPLOAD_FAILED(1), UPLOAD_IN_PROGRESS(2), UPLOAD_PAUSED(3), UPLOAD_SUCCEEDED(4);
        private final int value;
        private UploadStatus(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    };
    
    public UploadDbHandler(Context context) {
        mDatabaseName = MainApp.getDBName();
        mHelper = new OpenerHelper(context);
        mDB = mHelper.getWritableDatabase();
    }

    public void close() {
        mDB.close();
    }

    /**
     * Store a file persistently (to be uploaded later).
     * @param filepath local file path to file
     * @param account account for uploading
     * @param message optional message. can be null.
     * @return false if an error occurred, else true. 
     */
    public boolean storeFile(String filepath, String account, String message) {
        ///OBSOLETE
        Log_OC.i(TAG, "obsolete method called");
        return false;
//        ContentValues cv = new ContentValues();
//        cv.put("path", filepath);
//        cv.put("account", account);
//        cv.put("attempt", UploadStatus.UPLOAD_STATUS_UPLOAD_LATER.getValue());
//        cv.put("message", message);
//        long result = mDB.insert(TABLE_UPLOAD, null, cv);
//        Log_OC.d(TABLE_UPLOAD, "putFileForLater returns with: " + result + " for file: " + filepath);
//        return result != -1;
    }

    /**
     * Update upload status of file.
     * 
     * @param filepath local file path to file. used as identifier.
     * @param status new status.
     * @param message new message.
     * @return 1 if file status was updated, else 0.
     */
    public int updateFileState(String filepath, UploadStatus status, String message) {
      ///OBSOLETE
        Log_OC.i(TAG, "obsolete method called");
        return 0;
//        ContentValues cv = new ContentValues();
//        cv.put("attempt", status.getValue());
//        cv.put("message", message);
//        int result = mDB.update(TABLE_UPLOAD, cv, "path=?", new String[] { filepath });
//        Log_OC.d(TABLE_UPLOAD, "updateFileState returns with: " + result + " for file: " + filepath);
//        return result;
    }

    /**
     * Get all files with status {@link UploadStatus}.UPLOAD_STATUS_UPLOAD_LATER.
     * @return
     */
    public Cursor getAwaitingFiles() {
        //OBSOLETE
        Log_OC.i(TAG, "obsolete method called");
        return null;
//        return mDB.query(TABLE_UPLOAD, null, "attempt=" + UploadStatus.UPLOAD_STATUS_UPLOAD_LATER, null, null, null, null);
    }

  //ununsed until now. uncomment if needed.
//    public Cursor getFailedFiles() {
//        return mDB.query(TABLE_INSTANT_UPLOAD, null, "attempt>" + UploadStatus.UPLOAD_STATUS_UPLOAD_LATER, null, null, null, null);
//    }

  //ununsed until now. uncomment if needed.
//    public void clearFiles() {
//        mDB.delete(TABLE_INSTANT_UPLOAD, null, null);
//    }

    /**
     * Remove file from upload list. Should be called when upload succeed or failed and should not be retried. 
     * @param localPath
     * @return true when one or more pending files was removed
     */
    public boolean removeFile(String localPath) {
      //OBSOLETE
        Log_OC.i(TAG, "obsolete method called");
        return false;
//        long result = mDB.delete(TABLE_UPLOAD, "path = ?", new String[] { localPath });
//        Log_OC.d(TABLE_UPLOAD, "delete returns with: " + result + " for file: " + localPath);
//        return result != 0;

    }

    private class OpenerHelper extends SQLiteOpenHelper {
        public OpenerHelper(Context context) {
            super(context, mDatabaseName, null, mDatabaseVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_UPLOAD + " (" + " path TEXT PRIMARY KEY,"
                    + " uploadObject TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (newVersion == 4) {
                db.execSQL("DROP TABLE IF EXISTS " + "instant_upload" + ";"); //drop old db (name)
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_UPLOAD + ";");
                onCreate(db);
            }

        }
    }

    public boolean storeUpload(UploadDbObject uploadObject, String message) {
        ContentValues cv = new ContentValues();
        cv.put("path", uploadObject.getLocalPath());
        cv.put("uploadObject", uploadObject.toString());
        
        long result = mDB.insert(TABLE_UPLOAD, null, cv);
        Log_OC.d(TAG, "putFileForLater returns with: " + result + " for file: " + uploadObject.getLocalPath());
        return result != -1;        
    }
    
    public List<UploadDbObject> getAllStoredUploads() {
        Cursor c = mDB.query(TABLE_UPLOAD, null, null, null, null, null, null);
        List<UploadDbObject> list = new ArrayList<UploadDbObject>();
        if (c.moveToFirst()) {
          do {
              String file_path = c.getString(c.getColumnIndex("path"));
              String uploadObjectString = c.getString(c.getColumnIndex("uploadObject"));
              UploadDbObject uploadObject = UploadDbObject.fromString(uploadObjectString);
              if(uploadObject == null) {
                  Log_OC.e(TAG, "Could not deserialize UploadDbObject " + uploadObjectString);
              } else {
                  list.add(uploadObject);
              }
          } while (c.moveToNext());
        }
        return list;
    }
}
