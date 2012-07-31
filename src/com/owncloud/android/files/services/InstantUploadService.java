/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.files.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.utils.OwnCloudVersion;

import eu.alefzero.webdav.WebdavClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

public class InstantUploadService extends Service {

    public static String KEY_FILE_PATH = "KEY_FILEPATH";
    public static String KEY_FILE_SIZE = "KEY_FILESIZE";
    public static String KEY_MIME_TYPE = "KEY_MIMETYPE";
    public static String KEY_DISPLAY_NAME = "KEY_FILENAME";
    public static String KEY_ACCOUNT = "KEY_ACCOUNT";
    
    private static String TAG = "InstantUploadService";
    private static String INSTANT_UPLOAD_DIR = "/InstantUpload";
    private UploaderRunnable mUploaderRunnable;
    
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null ||
            !intent.hasExtra(KEY_ACCOUNT) || !intent.hasExtra(KEY_DISPLAY_NAME) ||
            !intent.hasExtra(KEY_FILE_PATH) || !intent.hasExtra(KEY_FILE_SIZE) ||
            !intent.hasExtra(KEY_MIME_TYPE)) {
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
        
        public void addElementToQueue(String filename,
                                      String filepath,
                                      String mimetype,
                                      long length,
                                      Account account) {
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
            AccountManager am = AccountManager.get(getApplicationContext());
            
            while ((working_map = getFirstObject()) != null) {
                Account account = (Account) working_map.get(KEY_ACCOUNT);
                String username = account.name.substring(0, account.name.lastIndexOf('@'));
                String password = am.getPassword(account);
                String filename = (String) working_map.get(KEY_DISPLAY_NAME);
                String filepath = (String) working_map.get(KEY_FILE_PATH);
                String mimetype = (String) working_map.get(KEY_MIME_TYPE);
                
                String oc_base_url = am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL);
                String oc_version = am.getUserData(account, AccountAuthenticator.KEY_OC_VERSION);
                OwnCloudVersion ocv = new OwnCloudVersion(oc_version);
                String webdav_path = AccountUtils.getWebdavPath(ocv);
                WebdavClient wdc = new WebdavClient(account, getApplicationContext());
                wdc.allowSelfsignedCertificates();
                wdc.setCredentials(username, password);
                
                MkColMethod mkcol = new MkColMethod(oc_base_url+webdav_path+INSTANT_UPLOAD_DIR);
                int status = 0;
                try {
                    status = wdc.executeMethod(mkcol);
                    Log.e(TAG, "mkcol returned " + status);
                    wdc.putFile(filepath, INSTANT_UPLOAD_DIR + "/" + filename, mimetype);
                } catch (HttpException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
