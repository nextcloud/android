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
import java.util.Observable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Database helper for storing list of files to be uploaded, including status information for each file.
 * 
 * @author Bartek Przybylski
 * @author LukeOwncloud
 * 
 */
public class UploadDbHandler extends Observable {
    private SQLiteDatabase mDB;
    private OpenerHelper mHelper;
    private final String mDatabaseName;
    private final int mDatabaseVersion = 4;

    static private final String TAG = "UploadDbHandler";
    static private final String TABLE_UPLOAD = "list_of_uploads";

    //for testing only
    public void recreateDb() {
        // mDB.beginTransaction();
        // try {
        // mHelper.onUpgrade(mDB, 0, mDatabaseVersion);
        // mDB.setTransactionSuccessful();
        // } finally {
        // mDB.endTransaction();
        // }

    }

    public enum UploadStatus {
        UPLOAD_LATER(0), UPLOAD_FAILED(1), UPLOAD_IN_PROGRESS(2), UPLOAD_PAUSED(3), UPLOAD_SUCCEEDED(4), UPLOAD_FAILED_GIVE_UP(
                5);
        private final int value;
        private UploadStatus(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    };
    
    private UploadDbHandler(Context context) {
        mDatabaseName = MainApp.getDBName();
        mHelper = new OpenerHelper(context);
    }
    
    private static UploadDbHandler me = null;
    static public UploadDbHandler getInstance(Context context) {
        if(me == null) {
            me = new UploadDbHandler(context);            
        }
        return me;
    }
    
    public void close() {
        getDB().close();
        setDB(null);
        me = null;
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
            // PRIMARY KEY should always imply NOT NULL. Unfortunately, due to a
            // bug in some early versions, this is not the case in SQLite.
            db.execSQL("CREATE TABLE " + TABLE_UPLOAD + " (" + " path TEXT PRIMARY KEY NOT NULL UNIQUE,"
                    + " uploadStatus INTEGER NOT NULL, uploadObject TEXT NOT NULL);");
            // uploadStatus is used to easy filtering, it has precedence over
            // uploadObject.getUploadStatus()
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

    /**
     * Stores an upload object in DB.
     * @param uploadObject
     * @param message
     * @return true on success.
     */
    public boolean storeUpload(UploadDbObject uploadObject) {
        ContentValues cv = new ContentValues();
        cv.put("path", uploadObject.getLocalPath());
        cv.put("uploadStatus", uploadObject.getUploadStatus().value);
        cv.put("uploadObject", uploadObject.toString());
        
        long result = getDB().insert(TABLE_UPLOAD, null, cv);
        Log_OC.d(TAG, "putFileForLater returns with: " + result + " for file: " + uploadObject.getLocalPath());
        if (result == 1) {
            notifyObserversNow();
        } else {
            Log_OC.e(TAG, "Failed to insert item into upload db.");
        }
        return result != -1;        
    }


    /**
     * Update upload status of file in DB.
     * 
     */
    public void updateUpload(UploadDbObject uploadDbObject) {
        updateUpload(uploadDbObject.getLocalPath(), uploadDbObject.getUploadStatus(), uploadDbObject.getLastResult());        
    }
    
    /**
     * Update upload status of file.
     * @param filepath filepath local file path to file. used as identifier.
     * @param status  new status.
     * @param result new result of upload operation 
     * @return 1 if file status was updated, else 0.
     */
    public int updateUpload(String filepath, UploadStatus status, RemoteOperationResult result) {
        Cursor c = getDB().query(TABLE_UPLOAD, null, "path=?", new String[] {filepath}, null, null, null);
        if(c.getCount() != 1) {
            Log_OC.e(TAG, c.getCount() + " items for path=" + filepath + " available in UploadDb. Expected 1." );
            return 0;
        }
        if (c.moveToFirst()) {
            //read upload object and update
            String uploadObjectString = c.getString(c.getColumnIndex("uploadObject"));
            UploadDbObject uploadObject = UploadDbObject.fromString(uploadObjectString);            
            uploadObject.setLastResult(result);
            uploadObject.setUploadStatus(status);
            uploadObjectString = uploadObject.toString();
            //store update upload object to db
            ContentValues cv = new ContentValues();
            cv.put("uploadStatus", status.value);
            cv.put("uploadObject", uploadObjectString);
            int r = getDB().update(TABLE_UPLOAD, cv, "path=?", new String[] { filepath });
            if (r == 1) {
                notifyObserversNow();
            } else {
                Log_OC.e(TAG, "Failed to update upload db.");
            }
            return r;
        }
        return 0;
    }
    
    /**
     * Should be called when some value of this DB was changed. All observers are informed.
     */
    public void notifyObserversNow() {
        Log_OC.d("UploadListAdapter", "notifyObserversNow");
        setChanged();
        notifyObservers();
    }
    
    /**
     * Remove upload from upload list. Should be called when cleaning up upload list. 
     * @param localPath
     * @return true when one or more upload entries were removed
     */
    public boolean removeUpload(String localPath) {
        long result = getDB().delete(TABLE_UPLOAD, "path = ?", new String[] { localPath });
        Log_OC.d(TABLE_UPLOAD, "delete returns with: " + result + " for file: " + localPath);
        return result != 0;
    }

    public List<UploadDbObject> getAllStoredUploads() {
        return getUploads(null, null);
    }

    private List<UploadDbObject> getUploads(String selection, String[] selectionArgs) {
        Cursor c = getDB().query(TABLE_UPLOAD, null, selection, selectionArgs, null, null, null);
        List<UploadDbObject> list = new ArrayList<UploadDbObject>();
        if (c.moveToFirst()) {
            do {
                String uploadObjectString = c.getString(c.getColumnIndex("uploadObject"));
                UploadDbObject uploadObject = UploadDbObject.fromString(uploadObjectString);
                if (uploadObject == null) {
                    Log_OC.e(TAG, "Could not deserialize UploadDbObject " + uploadObjectString);
                } else {
                    list.add(uploadObject);
                }
            } while (c.moveToNext());
        }
        return list;
    }

    public List<UploadDbObject> getAllPendingUploads() {
        return getUploads("uploadStatus!=" + UploadStatus.UPLOAD_SUCCEEDED.value + " AND uploadStatus!="
                + UploadStatus.UPLOAD_FAILED_GIVE_UP.value, null);
    }

    private SQLiteDatabase getDB() {
        if (mDB == null) {
            mDB = mHelper.getWritableDatabase();
        }
        return mDB;
    }

    private void setDB(SQLiteDatabase mDB) {
        this.mDB = mDB;
    }
    
}
