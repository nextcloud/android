package com.owncloud.android.services.observer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;

import java.util.ArrayList;

public class SyncedFolderObserverService extends Service {
    private static final String TAG = "SyncedFolderObserverService";
    private SyncedFolderProvider mProvider;
    private ArrayList<SyncedFolderObserver> syncedFolderObservers = new ArrayList<>();

    @Override
    public void onCreate() {
        mProvider = new SyncedFolderProvider(MainApp.getAppContext().getContentResolver());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        for (SyncedFolder syncedFolder : mProvider.getSyncedFolders()) {
            SyncedFolderObserver observer = new SyncedFolderObserver(syncedFolder.getLocalPath(),
                    syncedFolder.getRemotePath());

            observer.startWatching();
            syncedFolderObservers.add(observer);
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        for (SyncedFolderObserver observer : syncedFolderObservers) {
            observer.stopWatching();
            syncedFolderObservers.remove(observer);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
