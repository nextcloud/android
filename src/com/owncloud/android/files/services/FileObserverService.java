/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.files.OwnCloudFileObserver;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class FileObserverService extends Service {

    public final static int CMD_INIT_OBSERVED_LIST = 1;
    public final static int CMD_ADD_OBSERVED_FILE = 2;
    public final static int CMD_DEL_OBSERVED_FILE = 3;

    public final static String KEY_FILE_CMD = "KEY_FILE_CMD";
    public final static String KEY_CMD_ARG_FILE = "KEY_CMD_ARG_FILE";
    public final static String KEY_CMD_ARG_ACCOUNT = "KEY_CMD_ARG_ACCOUNT";

    private static String TAG = FileObserverService.class.getSimpleName();

    private static Map<String, OwnCloudFileObserver> mObserversMap;
    private static DownloadCompletedReceiverBis mDownloadReceiver;
    private IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        FileObserverService getService() {
            return FileObserverService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        mDownloadReceiver = new DownloadCompletedReceiverBis();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileDownloader.DOWNLOAD_ADDED_MESSAGE);
        filter.addAction(FileDownloader.DOWNLOAD_FINISH_MESSAGE);        
        registerReceiver(mDownloadReceiver, filter);
        
        mObserversMap = new HashMap<String, OwnCloudFileObserver>();
        //initializeObservedList();
    }
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mDownloadReceiver);
        mObserversMap = null;   // TODO study carefully the life cycle of Services to grant the best possible observance
        Log.d(TAG, "Bye, bye");
    }
    
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // this occurs when system tries to restart
        // service, so we need to reinitialize observers
        if (intent == null) {
            initializeObservedList();
            return Service.START_STICKY;
        }
            
        if (!intent.hasExtra(KEY_FILE_CMD)) {
            Log.e(TAG, "No KEY_FILE_CMD argument given");
            return Service.START_STICKY;
        }

        switch (intent.getIntExtra(KEY_FILE_CMD, -1)) {
            case CMD_INIT_OBSERVED_LIST:
                initializeObservedList();
                break;
            case CMD_ADD_OBSERVED_FILE:
                addObservedFile( (OCFile)intent.getParcelableExtra(KEY_CMD_ARG_FILE), 
                                 (Account)intent.getParcelableExtra(KEY_CMD_ARG_ACCOUNT));
                break;
            case CMD_DEL_OBSERVED_FILE:
                removeObservedFile( (OCFile)intent.getParcelableExtra(KEY_CMD_ARG_FILE), 
                                    (Account)intent.getParcelableExtra(KEY_CMD_ARG_ACCOUNT));
                break;
            default:
                Log.wtf(TAG, "Incorrect key given");
        }

        return Service.START_STICKY;
    }

    
    /**
     * Read from the local database the list of files that must to be kept synchronized and 
     * starts file observers to monitor local changes on them
     */
    private void initializeObservedList() {
        mObserversMap.clear();
        Cursor c = getContentResolver().query(
                ProviderTableMeta.CONTENT_URI,
                null,
                ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ?",
                new String[] {String.valueOf(1)},
                null);
        if (c == null || !c.moveToFirst()) return;
        AccountManager acm = AccountManager.get(this);
        Account[] accounts = acm.getAccounts();
        do {
            Account account = null;
            for (Account a : accounts)
                if (a.name.equals(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_ACCOUNT_OWNER)))) {
                    account = a;
                    break;
                }

            if (account == null) continue;
            FileDataStorageManager storage =
                    new FileDataStorageManager(account, getContentResolver());
            if (!storage.fileExists(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH))))
                continue;

            String path = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
            if (path == null || path.length() <= 0)
                continue;
            OwnCloudFileObserver observer =
                    new OwnCloudFileObserver(   path, 
                                                account, 
                                                getApplicationContext(), 
                                                OwnCloudFileObserver.CHANGES_ONLY);
            mObserversMap.put(path, observer);
            if (new File(path).exists()) {
                observer.startWatching();
                Log.d(TAG, "Started watching file " + path);
            }
            
        } while (c.moveToNext());
        c.close();
    }
    
    
    /**
     * Registers the local copy of a remote file to be observed for local changes,
     * an automatically updated in the ownCloud server.
     * 
     * This method does NOT perform a {@link SynchronizeFileOperation} over the file. 
     *
     * TODO We are ignoring that, currently, a local file can be linked to different files
     * in ownCloud if it's uploaded several times. That's something pending to update: we 
     * will avoid that the same local file is linked to different remote files.
     * 
     * @param file      Object representing a remote file which local copy must be observed.
     * @param account   OwnCloud account containing file.
     */
    private void addObservedFile(OCFile file, Account account) {
        if (file == null) {
            Log.e(TAG, "Trying to add a NULL file to observer");
            return;
        }
        String localPath = file.getStoragePath();
        if (localPath == null || localPath.length() <= 0) { // file downloading / to be download for the first time
            localPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
        }
        OwnCloudFileObserver observer = mObserversMap.get(localPath);
        if (observer == null) {
            /// the local file was never registered to observe before
            observer = new OwnCloudFileObserver(    localPath, 
                                                    account, 
                                                    getApplicationContext(), 
                                                    OwnCloudFileObserver.CHANGES_ONLY);
            mObserversMap.put(localPath, observer);
            Log.d(TAG, "Observer added for path " + localPath);
        
            if (file.isDown()) {
                observer.startWatching();
                Log.d(TAG, "Started watching " + localPath);
            }   // else - the observance can't be started on a file not already down; mDownloadReceiver will get noticed when the download of the file finishes
        }
        
    }

    
    /**
     * Unregisters the local copy of a remote file to be observed for local changes.
     *
     * Starts to watch it, if the file has a local copy to watch.
     * 
     * TODO We are ignoring that, currently, a local file can be linked to different files
     * in ownCloud if it's uploaded several times. That's something pending to update: we 
     * will avoid that the same local file is linked to different remote files.
     *
     * @param file      Object representing a remote file which local copy must be not observed longer.
     * @param account   OwnCloud account containing file.
     */
    private void removeObservedFile(OCFile file, Account account) {
        if (file == null) {
            Log.e(TAG, "Trying to remove a NULL file");
            return;
        }
        String localPath = file.getStoragePath();
        if (localPath == null || localPath.length() <= 0) {
            localPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
        }
        
        OwnCloudFileObserver observer = mObserversMap.get(localPath);
        if (observer != null) {
            observer.stopWatching();
            mObserversMap.remove(observer);
            Log.d(TAG, "Stopped watching " + localPath);
        }
        
    }


    /**
     *  Private receiver listening to events broadcast by the FileDownloader service.
     * 
     *  Starts and stops the observance on registered files when they are being download,
     *  in order to avoid to start unnecessary synchronizations. 
     */
    private class DownloadCompletedReceiverBis extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String downloadPath = intent.getStringExtra(FileDownloader.EXTRA_FILE_PATH);
            OwnCloudFileObserver observer = mObserversMap.get(downloadPath);
            if (observer != null) {
                if (intent.getAction().equals(FileDownloader.DOWNLOAD_FINISH_MESSAGE) &&
                        new File(downloadPath).exists()) {  // the download could be successful, or not; in both cases, the file could be down, due to a former download or upload   
                    observer.startWatching();
                    Log.d(TAG, "Watching again " + downloadPath);
                
                } else if (intent.getAction().equals(FileDownloader.DOWNLOAD_ADDED_MESSAGE)) {
                    observer.stopWatching();
                    Log.d(TAG, "Disabling observance of " + downloadPath);
                } 
            }
        }
        
    }
    
}
