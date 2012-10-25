package com.owncloud.android.files.services;

import java.util.ArrayList;
import java.util.List;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.files.OwnCloudFileObserver;

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

    public final static String KEY_FILE_CMD = "KEY_FILE_CMD";
    public final static String KEY_CMD_ARG = "KEY_CMD_ARG";

    public final static int CMD_INIT_OBSERVED_LIST = 1;
    public final static int CMD_ADD_OBSERVED_FILE = 2;
    public final static int CMD_DEL_OBSERVED_FILE = 3;
    public final static int CMD_ADD_DOWNLOADING_FILE = 4;

    private static String TAG = "FileObserverService";
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
                addObservedFile(intent.getStringExtra(KEY_CMD_ARG));
                break;
            case CMD_DEL_OBSERVED_FILE:
                removeObservedFile(intent.getStringExtra(KEY_CMD_ARG));
                break;
            case CMD_ADD_DOWNLOADING_FILE:
                addDownloadingFile(intent.getStringExtra(KEY_CMD_ARG));
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
            observer.startWatching();
            mObservers.add(observer);
            Log.d(TAG, "Started watching file " + path);
            
        } while (c.moveToNext());
        c.close();
    }
    
    private void addObservedFile(String path) {
        if (path == null) return;
        if (mObservers == null) {
            // this is very rare case when service was killed by system
            // and observers list was deleted in that procedure
            initializeObservedList();
        }
        boolean duplicate = false;
        OwnCloudFileObserver observer = null;
        for (int i = 0; i < mObservers.size(); ++i) {
            observer = mObservers.get(i);
            if (observer.getPath().equals(path))
                duplicate = true;
            observer.setContext(getBaseContext());
        }
        if (duplicate) return;
        observer = new OwnCloudFileObserver(path, OwnCloudFileObserver.CHANGES_ONLY);
        observer.setContext(getBaseContext());
        Account account = AccountUtils.getCurrentOwnCloudAccount(getBaseContext());
        observer.setAccount(account);
        FileDataStorageManager storage =
                new FileDataStorageManager(account, getContentResolver());
        observer.setStorageManager(storage);
        observer.setOCFile(storage.getFileByLocalPath(path));

        DownloadCompletedReceiver receiver = new DownloadCompletedReceiver(path, observer);
        registerReceiver(receiver, new IntentFilter(FileDownloader.DOWNLOAD_FINISH_MESSAGE));

        mObservers.add(observer);
        Log.d(TAG, "Observer added for path " + path);
    }
    
    private void removeObservedFile(String path) {
        if (path == null) return;
        if (mObservers == null) {
            initializeObservedList();
            return;
        }
        for (int i = 0; i < mObservers.size(); ++i) {
            OwnCloudFileObserver observer = mObservers.get(i);
            if (observer.getPath().equals(path)) {
                observer.stopWatching();
                mObservers.remove(i);
                break;
            }
        }
        Log.d(TAG, "Stopped watching " + path);
    }
        
    private void addDownloadingFile(String remotePath) {
        OwnCloudFileObserver observer = null;
        for (OwnCloudFileObserver o : mObservers) {
            if (o.getRemotePath().equals(remotePath)) {
                observer = o;
                break;
            }
        }
        if (observer == null) {
            Log.e(TAG, "Couldn't find observer for remote file " + remotePath);
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
                context.unregisterReceiver(this);
                removeReceiverFromList(this);
                mObserver.startWatching();
                Log.d(TAG, "Started watching " + mPath);
                return;
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
