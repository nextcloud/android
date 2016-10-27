package com.owncloud.android.services.observer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.HashMap;

public class SyncedFolderObserverService extends Service {
    private static final String TAG = "SyncedFolderObserverService";
    private SyncedFolderProvider mProvider;
    private HashMap<String, SyncedFolderObserver> syncedFolderMap = new HashMap<>();
    private final IBinder mBinder = new SyncedFolderObserverBinder();

    @Override
    public void onCreate() {
        mProvider = new SyncedFolderProvider(MainApp.getAppContext().getContentResolver());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "start");
        for (SyncedFolder syncedFolder : mProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                Log_OC.d(TAG, "stop observer: " + syncedFolder.getLocalPath());
                SyncedFolderObserver observer = new SyncedFolderObserver(syncedFolder);
                observer.startWatching();
                syncedFolderMap.put(syncedFolder.getLocalPath(), observer);
            }
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        for (SyncedFolderObserver observer : syncedFolderMap.values()) {
            observer.stopWatching();
            syncedFolderMap.remove(observer);
        }
    }

    /**
     * Restart oberver if it is enabled
     * If syncedFolder exists already, use it, otherwise create new observer
     * @param syncedFolder
     */
    public void restartObserver(SyncedFolder syncedFolder){
        if (syncedFolderMap.containsKey(syncedFolder.getLocalPath())) {
            Log_OC.d(TAG, "stop observer: " + syncedFolder.getLocalPath());
            syncedFolderMap.get(syncedFolder.getLocalPath()).stopWatching();
            syncedFolderMap.remove(syncedFolder.getLocalPath());
        }

        if (syncedFolder.isEnabled()) {
            Log_OC.d(TAG, "start observer: " + syncedFolder.getLocalPath());
            if (syncedFolderMap.containsKey(syncedFolder.getLocalPath())) {
                syncedFolderMap.get(syncedFolder.getLocalPath()).startWatching();
            } else {
                SyncedFolderObserver observer = new SyncedFolderObserver(syncedFolder);
                observer.startWatching();
                syncedFolderMap.put(syncedFolder.getLocalPath(), observer);
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class SyncedFolderObserverBinder extends Binder {
        public SyncedFolderObserverService getService() {
            return SyncedFolderObserverService.this;
        }
    }

}
