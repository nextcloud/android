/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

package com.owncloud.android.files.services;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.files.OwnCloudFileObserver;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.Log_OC;


import android.accounts.Account;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.IBinder;

/**
 * Service keeping a list of {@link FileObserver} instances that watch for local changes in 
 * favorite files (formerly known as kept-in-sync files) and try to synchronize them with the 
 * OC server as soon as possible.
 * 
 * Tries to be alive as long as possible; that is the reason why stopSelf() is never called.
 * 
 * It is expected that the system eventually kills the service when runs low of memory. 
 * To minimize the impact of this, the service always returns Service.START_STICKY, and the later 
 * restart of the service is explicitly considered in 
 * {@link FileObserverService#onStartCommand(Intent, int, int)}.
 *  
 * @author David A. Velasco
 */
public class FileObserverService extends Service {

    public final static String MY_NAME = FileObserverService.class.getCanonicalName();
    public final static String ACTION_INIT_OBSERVED_LIST = MY_NAME + ".action.INIT_OBSERVED_LIST";
    public final static String CMD_ADD_OBSERVED_FILE = MY_NAME + ".action.ADD_OBSERVED_FILE";
    public final static String CMD_DEL_OBSERVED_FILE = MY_NAME + ".action.DEL_OBSERVED_FILE";

    public final static String KEY_CMD_ARG_FILE = "KEY_CMD_ARG_FILE";
    public final static String KEY_CMD_ARG_ACCOUNT = "KEY_CMD_ARG_ACCOUNT";

    private static String TAG = FileObserverService.class.getSimpleName();

    private static Map<String, OwnCloudFileObserver> mObserversMap;
    private static Map<String, OwnCloudFileObserver> mObserverParentsMap;
    private static DownloadCompletedReceiver mDownloadReceiver;

    
    /**
     * Factory method to create intents that allow to start an ACTION_INIT_OBSERVED_LIST command.
     * 
     * @param context       Android context of the caller component.
     * @return              Intent that starts a command ACTION_INIT_OBSERVED_LIST when 
     *                      {@link Context#startService(Intent)} is called.
     */
    public static Intent makeInitIntent(Context context) {
        Intent i = new Intent(context, FileObserverService.class);
        i.setAction(ACTION_INIT_OBSERVED_LIST);
        return i;
    }
    
    
    /**
     * Factory method to create intents that allow to start or stop the observance of a file.
     * 
     * @param  context      Android context of the caller component.
     * @param  file         OCFile to start or stop to watch.
     * @param  account      OC account containing file.
     * @param  watchIt      'True' creates an intent to watch, 'false' an intent to stop watching.
     * @return              Intent to start or stop the observance of a file through a call
     *                      to {@link Context#startService(Intent)}.
     */
    public static Intent makeObservedFileIntent(
            Context context, OCFile file, Account account, boolean watchIt) {
        Intent intent = new Intent(context, FileObserverService.class);
        intent.setAction(
                watchIt ? 
                        FileObserverService.CMD_ADD_OBSERVED_FILE 
                        : 
                        FileObserverService.CMD_DEL_OBSERVED_FILE
        );
        intent.putExtra(FileObserverService.KEY_CMD_ARG_FILE, file);
        intent.putExtra(FileObserverService.KEY_CMD_ARG_ACCOUNT, account);
        return intent;
    }
    
    
    
    @Override
    public void onCreate() {
        Log_OC.d(TAG, "onCreate");
        super.onCreate();
        
        mDownloadReceiver = new DownloadCompletedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileDownloader.getDownloadAddedMessage());
        filter.addAction(FileDownloader.getDownloadFinishMessage());        
        registerReceiver(mDownloadReceiver, filter);
        
        mObserversMap = new HashMap<String, OwnCloudFileObserver>();
        mObserverParentsMap = new HashMap<String, OwnCloudFileObserver>();
    }
    
    
    @Override
    public void onDestroy() {
        Log_OC.d(TAG, "onDestroy - FINISHING OBSERVATION");
        
        unregisterReceiver(mDownloadReceiver);
        
        Iterator<OwnCloudFileObserver> it = mObserversMap.values().iterator();
        while (it.hasNext()) {
            it.next().stopWatching();
        }
        mObserversMap.clear();
        mObserversMap = null;
        
        it = mObserverParentsMap.values().iterator();
        while (it.hasNext()) {
            it.next().stopWatching();
        }
        mObserverParentsMap.clear();
        mObserverParentsMap = null;
        
        super.onDestroy();
    }
    
    
    @Override
    public IBinder onBind(Intent intent) {
        // this service cannot be bound
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command " + intent);
        
        if (intent == null || ACTION_INIT_OBSERVED_LIST.equals(intent.getAction())) {
            // NULL occurs when system tries to restart the service after its process
            // was killed
            initializeObservedList();
            return Service.START_STICKY;
            
        } else if (CMD_ADD_OBSERVED_FILE.equals(intent.getAction())) {
            addObservedFile(
                (OCFile)intent.getParcelableExtra(KEY_CMD_ARG_FILE), 
                (Account)intent.getParcelableExtra(KEY_CMD_ARG_ACCOUNT)
            );
            
        } else if (CMD_DEL_OBSERVED_FILE.equals(intent.getAction())) {
            removeObservedFile(
                (OCFile)intent.getParcelableExtra(KEY_CMD_ARG_FILE), 
                (Account)intent.getParcelableExtra(KEY_CMD_ARG_ACCOUNT)
            );
            
        } else {
            Log_OC.e(TAG, "Unknown action recieved; ignoring it: " + intent.getAction());
        }
             
        return Service.START_STICKY;
    }

    
    /**
     * Read from the local database the list of files that must to be kept synchronized and 
     * starts file observers to monitor local changes on them
     */
    private void initializeObservedList() {
        Log_OC.d(TAG, "Loading all kept-in-sync files from database to start watching them");
        
        //mObserversMap.clear();
        //mObserverParentsMap.clear();
        
        Cursor cursorOnKeptInSync = getContentResolver().query(
                ProviderTableMeta.CONTENT_URI,
                null,
                ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ?",
                new String[] {String.valueOf(1)},
                null
        );
        
        if (cursorOnKeptInSync != null) {
            
            if (cursorOnKeptInSync.moveToFirst()) {
            
                String localPath = "";
                //String remotePath = "";
                String accountName = "";
                Account account = null;
                do {
                    localPath = cursorOnKeptInSync.getString(
                            cursorOnKeptInSync.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH)
                    );
                    accountName = cursorOnKeptInSync.getString(
                            cursorOnKeptInSync.getColumnIndex(ProviderTableMeta.FILE_ACCOUNT_OWNER)
                    );
                    /*
                    remotePath = cursorOnKeptInSync.getString(
                            cursorOnKeptInSync.getColumnIndex(ProviderTableMeta.FILE_PATH)
                    );
                    */
                    
                    account = new Account(accountName, MainApp.getAccountType());
                    if (!AccountUtils.exists(account, this) ||
                            localPath == null || localPath.length() <= 0) {
                        continue;
                    }

                    OwnCloudFileObserver observer = mObserversMap.get(localPath);
                    if (observer == null) {
                        observer = new OwnCloudFileObserver(   
                                localPath, account, getApplicationContext()
                        );
                        mObserversMap.put(localPath, observer);
                        
                        // only if being added
                        if (new File(localPath).exists()) {
                            observer.startWatching();
                            Log_OC.d(TAG, "Started watching file " + localPath);
                        }
                    }
                            
                    String parentPath = (new File(localPath)).getParent();
                    OwnCloudFileObserver observerParent = mObserverParentsMap.get(parentPath);
                    if (observerParent == null) {
                        observerParent = new OwnCloudFileObserver(
                                parentPath, account, getApplicationContext()
                        );
                        mObserverParentsMap.put(parentPath, observer);
                        if (new File(parentPath).exists()) {
                            observerParent.startWatching();
                            Log_OC.d(TAG, "Started watching parent folder " + parentPath);
                        }
                    }
                    
                } while (cursorOnKeptInSync.moveToNext());
                
            }
            cursorOnKeptInSync.close();
        }
        
        // service does not stopSelf() ; that way it tries to be alive forever 

    }
    
    
    /**
     * Registers the local copy of a remote file to be observed for local changes,
     * an automatically updated in the ownCloud server.
     * 
     * This method does NOT perform a {@link SynchronizeFileOperation} over the file. 
     *
     * @param file      Object representing a remote file which local copy must be observed.
     * @param account   OwnCloud account containing file.
     */
    private void addObservedFile(OCFile file, Account account) {
        Log_OC.v(TAG, "Adding a file to be watched");
        
        if (file == null) {
            Log_OC.e(TAG, "Trying to add a NULL file to observer");
            return;
        }
        if (account == null) {
            Log_OC.e(TAG, "Trying to add a file with a NULL account to observer");
            return;
        }
        
        String localPath = file.getStoragePath();
        if (localPath == null || localPath.length() <= 0) { 
            // file downloading or to be downloaded for the first time
            localPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
        }
        OwnCloudFileObserver observer = mObserversMap.get(localPath);
        if (observer == null) {
            /// the local file was never registered to observe before
            observer = new OwnCloudFileObserver(
                    localPath, account, getApplicationContext()
            );
            mObserversMap.put(localPath, observer);
            Log_OC.d(TAG, "Observer added for path " + localPath);
            
            if (file.isDown()) {
                observer.startWatching();
                Log_OC.d(TAG, "Started watching " + localPath);
            }   
            // else - the observance can't be started on a file not already down; 
            //      mDownloadReceiver will get noticed when the download of the file finishes
        }
        
        String parentPath = (new File(localPath)).getParent();
        OwnCloudFileObserver observerParent = mObserverParentsMap.get(parentPath);
        if (observerParent == null) {
            observerParent = new OwnCloudFileObserver(
                    parentPath, account, getApplicationContext()
            );
            mObserverParentsMap.put(parentPath, observerParent);
            Log_OC.d(TAG, "Observer added for parent folder " + localPath);
            
            if (file.isDown()) {
                observerParent.startWatching();
                Log_OC.d(TAG, "Started watching parent folder " + parentPath);
            }   
        }
        
    }

    
    /**
     * Unregisters the local copy of a remote file to be observed for local changes.
     *
     * Starts to watch it, if the file has a local copy to watch.
     * 
     * @param file      Object representing a remote file which local copy must be not observed longer.
     * @param account   OwnCloud account containing file.
     */
    private void removeObservedFile(OCFile file, Account account) {
        Log_OC.v(TAG, "Removing a file from being watched");
        
        if (file == null) {
            Log_OC.e(TAG, "Trying to remove a NULL file");
            return;
        }
        if (account == null) {
            Log_OC.e(TAG, "Trying to add a file with a NULL account to observer");
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
            Log_OC.d(TAG, "Stopped watching " + localPath);
            
        } else {
            Log_OC.d(TAG, "No observer to remove for path " + localPath);
        }
        
    }


    /**
     *  Private receiver listening to events broadcast by the FileDownloader service.
     * 
     *  Starts and stops the observance on registered files when they are being download,
     *  in order to avoid to start unnecessary synchronizations. 
     */
    private class DownloadCompletedReceiver extends BroadcastReceiver {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            Log_OC.d(TAG, "Received broadcast intent " + intent);

            String downloadPath = intent.getStringExtra(FileDownloader.EXTRA_FILE_PATH);
            OwnCloudFileObserver observer = mObserversMap.get(downloadPath);
            if (observer != null) {
                if (intent.getAction().equals(FileDownloader.getDownloadFinishMessage()) &&
                        new File(downloadPath).exists()) {  
                    // no matter is the download was be successful or not; the file could be down, 
                    // anyway due to a former download or upload   
                    observer.startWatching();
                    Log_OC.d(TAG, "Resuming observance of " + downloadPath);
                
                } else if (intent.getAction().equals(FileDownloader.getDownloadAddedMessage())) {
                    observer.stopWatching();
                    Log_OC.d(TAG, "Pausing observance of " + downloadPath);
                }
                
            } else {
                Log_OC.d(TAG, "No observer for path " + downloadPath);
            }
        }
        
    }

}
