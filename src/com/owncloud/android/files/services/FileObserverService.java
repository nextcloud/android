/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.files.OwnCloudFileObserver;
import com.owncloud.android.files.OwnCloudFileObserver.FileObserverStatusListener;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
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

public class FileObserverService extends Service implements FileObserverStatusListener {

    public final static String KEY_FILE_CMD = "KEY_FILE_CMD";
    public final static String KEY_CMD_ARG_FILE = "KEY_CMD_ARG_FILE";
    public final static String KEY_CMD_ARG_ACCOUNT = "KEY_CMD_ARG_ACCOUNT";

    public final static int CMD_INIT_OBSERVED_LIST = 1;
    public final static int CMD_ADD_OBSERVED_FILE = 2;
    public final static int CMD_DEL_OBSERVED_FILE = 3;
    public final static int CMD_ADD_DOWNLOADING_FILE = 4;

    private static String TAG = FileObserverService.class.getSimpleName();
    private static List<OwnCloudFileObserver> mObservers;
    private static List<DownloadCompletedReceiver> mDownloadReceivers;
    private static Object mReceiverListLock = new Object();
    private IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        FileObserverService getService() {
            return FileObserverService.this;
        }
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
            case CMD_ADD_DOWNLOADING_FILE:
                addDownloadingFile( (OCFile)intent.getParcelableExtra(KEY_CMD_ARG_FILE),
                                    (Account)intent.getParcelableExtra(KEY_CMD_ARG_ACCOUNT));
                break;
            default:
                Log.wtf(TAG, "Incorrect key given");
        }

        return Service.START_STICKY;
    }

    private void initializeObservedList() {
        if (mObservers != null) return; // nothing to do here
        mObservers = new ArrayList<OwnCloudFileObserver>();
        mDownloadReceivers = new ArrayList<DownloadCompletedReceiver>();
        Cursor c = getContentResolver().query(
                ProviderTableMeta.CONTENT_URI,
                null,
                ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ?",
                new String[] {String.valueOf(1)},
                null);
        if (!c.moveToFirst()) return;
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
            OwnCloudFileObserver observer =
                    new OwnCloudFileObserver(path, OwnCloudFileObserver.CHANGES_ONLY);
            observer.setContext(getApplicationContext());
            observer.setAccount(account);
            observer.setStorageManager(storage);
            observer.setOCFile(storage.getFileByPath(c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH))));
            observer.addObserverStatusListener(this);
            observer.startWatching();
            mObservers.add(observer);
            Log.d(TAG, "Started watching file " + path);
            
        } while (c.moveToNext());
        c.close();
    }
    
    /**
     * Registers the local copy of a remote file to be observed for local changes,
     * an automatically updated in the ownCloud server.
     *
     * @param file      Object representing a remote file which local copy must be observed.
     * @param account   OwnCloud account containing file.
     */
    private void addObservedFile(OCFile file, Account account) {
        if (file == null) {
            Log.e(TAG, "Trying to observe a NULL file");
            return;
        }
        if (mObservers == null) {
            // this is very rare case when service was killed by system
            // and observers list was deleted in that procedure
            initializeObservedList();
        }
        String localPath = file.getStoragePath();
        if (!file.isDown()) {
            // this is a file downloading / to be download for the first time
            localPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
        }
        OwnCloudFileObserver tmpObserver = null, observer = null;
        for (int i = 0; i < mObservers.size(); ++i) {
            tmpObserver = mObservers.get(i);
            if (tmpObserver.getPath().equals(localPath)) {
                observer = tmpObserver;
            }
            tmpObserver.setContext(getApplicationContext());   // 'refreshing' context to all the observers? why?
        }
        if (observer == null) {
            /// the local file was never registered to observe before
            observer = new OwnCloudFileObserver(localPath, OwnCloudFileObserver.CHANGES_ONLY);
            //Account account = AccountUtils.getCurrentOwnCloudAccount(getApplicationContext());
            observer.setAccount(account);
            FileDataStorageManager storage =
                    new FileDataStorageManager(account, getContentResolver());  // I don't trust in this resolver's life span...
            observer.setStorageManager(storage);
            //observer.setOCFile(storage.getFileByLocalPath(path));   // ISSUE 10 - the fix in FileDetailsFragment to avoid path == null was not enough; it the file was never down before, this sets a NULL OCFile in the observer
            observer.setOCFile(file);
            observer.addObserverStatusListener(this);
            observer.setContext(getApplicationContext());
            
        } else {
            /* LET'S IGNORE THAT, CURRENTLY, A LOCAL FILE CAN BE LINKED TO DIFFERENT FILES IN OWNCLOUD;
             * we should change that
             * 
            /// the local file is already observed for some other OCFile(s)
            observer.addOCFile(account, file);  // OCFiles should have a reference to the account containing them to not be confused
            */ 
        }

        mObservers.add(observer);
        Log.d(TAG, "Observer added for path " + localPath);
        
        if (!file.isDown()) {
            // if the file is not down, it can't be observed for changes
            DownloadCompletedReceiver receiver = new DownloadCompletedReceiver(localPath, observer);
            registerReceiver(receiver, new IntentFilter(FileDownloader.DOWNLOAD_FINISH_MESSAGE));

        } else {
            observer.startWatching();
            Log.d(TAG, "Started watching " + localPath);
            
        }
        
    }

    
    /**
     * Unregisters the local copy of a remote file to be observed for local changes.
     *
     * @param file      Object representing a remote file which local copy must be not observed longer.
     * @param account   OwnCloud account containing file.
     */
    private void removeObservedFile(OCFile file, Account account) {
        if (file == null) {
            Log.e(TAG, "Trying to unobserve a NULL file");
            return;
        }
        if (mObservers == null) {
            initializeObservedList();
        }
        String localPath = file.getStoragePath();
        if (!file.isDown()) {
            // this happens when a file not in the device is set to be kept synchronized, and quickly unset again,
            // while the download is not finished
            localPath = FileStorageUtils.getDefaultSavePathFor(account.name, file);
        }
        
        for (int i = 0; i < mObservers.size(); ++i) {
            OwnCloudFileObserver observer = mObservers.get(i);
            if (observer.getPath().equals(localPath)) {
                observer.stopWatching();
                mObservers.remove(i);       // assuming, again, that a local file can be only linked to only ONE remote file; currently false
                if (!file.isDown()) {
                    // TODO unregister download receiver ;forget this until list of receivers is replaced for a single receiver
                }
                Log.d(TAG, "Stopped watching " + localPath);
                break;
            }
        }
        
    }

    
    /**
     * Temporarily disables the observance of a file that is going to be download.
     *
     * @param file      Object representing the remote file which local copy must not be observed temporarily.
     * @param account   OwnCloud account containing file.
     */
    private void addDownloadingFile(OCFile file, Account account) {
        OwnCloudFileObserver observer = null;
        for (OwnCloudFileObserver o : mObservers) {
            if (o.getRemotePath().equals(file.getRemotePath()) && o.getAccount().equals(account)) {
                observer = o;
                break;
            }
        }
        if (observer == null) {
            Log.e(TAG, "Couldn't find observer for remote file " + file.getRemotePath());
            return;
        }
        observer.stopWatching();
        DownloadCompletedReceiver dcr = new DownloadCompletedReceiver(observer.getPath(), observer);
        registerReceiver(dcr, new IntentFilter(FileDownloader.DOWNLOAD_FINISH_MESSAGE));
    }

    
    private static void addReceiverToList(DownloadCompletedReceiver r) {
        synchronized(mReceiverListLock) {
            mDownloadReceivers.add(r);
        }
    }
    
    private static void removeReceiverFromList(DownloadCompletedReceiver r) {
        synchronized(mReceiverListLock) {
            mDownloadReceivers.remove(r);
        }
    }

    @Override
    public void onObservedFileStatusUpdate(String localPath, String remotePath, Account account, RemoteOperationResult result) {
        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                // ISSUE 5: if the user is not running the app (this is a service!), this can be very intrusive; a notification should be preferred
                Intent i = new Intent(getApplicationContext(), ConflictsResolveActivity.class);
                i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("remotepath", remotePath);
                i.putExtra("localpath", localPath);
                i.putExtra("account", account);
                startActivity(i);
                
            } else {
                // TODO send notification to the notification bar?
            }
        } // else, nothing else to do; now it's duty of FileUploader service 
    }

    private class DownloadCompletedReceiver extends BroadcastReceiver {
        String mPath;
        OwnCloudFileObserver mObserver;
        
        public DownloadCompletedReceiver(String path, OwnCloudFileObserver observer) {
            mPath = path;
            mObserver = observer;
            addReceiverToList(this);
        }
        
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPath.equals(intent.getStringExtra(FileDownloader.EXTRA_FILE_PATH))) {
                if ((new File(mPath)).exists()) {   
                    // the download could be successful, or not; in both cases, the file could be down, due to a former download or upload
                    context.unregisterReceiver(this);
                    removeReceiverFromList(this);
                    mObserver.startWatching();
                    Log.d(TAG, "Started watching " + mPath);
                    return;
                }   // else - keep waiting for a future retry of the download ; 
                    // mObserver.startWatching() won't ever work if the file is not in the device when it's called 
            }
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof DownloadCompletedReceiver)
                return mPath.equals(((DownloadCompletedReceiver)o).mPath);
            return super.equals(o);
        }
    }
}
