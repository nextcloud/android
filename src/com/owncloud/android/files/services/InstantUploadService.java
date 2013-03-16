/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.files.services;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.utils.FileStorageUtils;

import eu.alefzero.webdav.WebdavClient;

public class InstantUploadService extends Service {

    public static String KEY_FILE_PATH = "KEY_FILEPATH";
    public static String KEY_FILE_SIZE = "KEY_FILESIZE";
    public static String KEY_MIME_TYPE = "KEY_MIMETYPE";
    public static String KEY_DISPLAY_NAME = "KEY_FILENAME";
    public static String KEY_ACCOUNT = "KEY_ACCOUNT";

    private static String TAG = "InstantUploadService";
    // TODO make it configurable over the settings dialog
    public static final String INSTANT_UPLOAD_DIR = "/InstantUpload";
    private UploaderRunnable mUploaderRunnable;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.hasExtra(KEY_ACCOUNT) || !intent.hasExtra(KEY_DISPLAY_NAME)
                || !intent.hasExtra(KEY_FILE_PATH) || !intent.hasExtra(KEY_FILE_SIZE)
                || !intent.hasExtra(KEY_MIME_TYPE)) {
            Log.w(TAG, "Not all required information was provided, abording");
            return Service.START_NOT_STICKY;
        }

        if (mUploaderRunnable == null) {
            mUploaderRunnable = new UploaderRunnable();
        }

        String filename = intent.getStringExtra(KEY_DISPLAY_NAME);
        String filepath = intent.getStringExtra(KEY_FILE_PATH);
        String mimetype = intent.getStringExtra(KEY_MIME_TYPE);
        Account account = intent.getParcelableExtra(KEY_ACCOUNT);
        long filesize = intent.getLongExtra(KEY_FILE_SIZE, -1);

        mUploaderRunnable.addElementToQueue(filename, filepath, mimetype, filesize, account);

        // starting new thread for new download doesnt seems like a good idea
        // maybe some thread pool or single background thread would be better
        Log.d(TAG, "Starting instant upload thread");
        new Thread(mUploaderRunnable).start();

        return Service.START_STICKY;
    }

    private class UploaderRunnable implements Runnable {

        Object mLock;
        List<HashMap<String, Object>> mHashMapList;

        public UploaderRunnable() {
            mHashMapList = new LinkedList<HashMap<String, Object>>();
            mLock = new Object();
        }

        public void addElementToQueue(String filename, String filepath, String mimetype, long length, Account account) {
            HashMap<String, Object> new_map = new HashMap<String, Object>();
            new_map.put(KEY_ACCOUNT, account);
            new_map.put(KEY_DISPLAY_NAME, filename);
            new_map.put(KEY_FILE_PATH, filepath);
            new_map.put(KEY_MIME_TYPE, mimetype);
            new_map.put(KEY_FILE_SIZE, length);

            synchronized (mLock) {
                mHashMapList.add(new_map);
            }
        }

        private HashMap<String, Object> getFirstObject() {
            synchronized (mLock) {
                if (mHashMapList.size() == 0)
                    return null;
                HashMap<String, Object> ret = mHashMapList.get(0);
                mHashMapList.remove(0);
                return ret;
            }
        }

        public void run() {
            HashMap<String, Object> working_map;

            while ((working_map = getFirstObject()) != null) {
                Account account = (Account) working_map.get(KEY_ACCOUNT);
                String filename = (String) working_map.get(KEY_DISPLAY_NAME);
                String filepath = (String) working_map.get(KEY_FILE_PATH);
                String mimetype = (String) working_map.get(KEY_MIME_TYPE);
                
                WebdavClient wdc = OwnCloudClientUtils.createOwnCloudClient(account, getApplicationContext());

                wdc.createDirectory(INSTANT_UPLOAD_DIR); // fail could just mean that it already exists put will be tried anyway
                try {
                    wdc.putFile(filepath, FileStorageUtils.getInstantUploadFilePath(filename), mimetype);
                } catch (Exception e) {
                    // nothing to do; this service is deprecated, indeed
                }
            }
        }
    }

}
