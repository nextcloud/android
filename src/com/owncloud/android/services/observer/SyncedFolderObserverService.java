package com.owncloud.android.services.observer;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.ArrayList;

public class SyncedFolderObserverService extends Service {
    private static final String TAG = "SyncedFolderObserverService";
    private ContentResolver database;
    private ArrayList<SyncedFolderObserver> syncedFolderObservers = new ArrayList<>();

    @Override
    public void onCreate() {
        database = MainApp.getAppContext().getContentResolver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        for (SyncedFolder syncedFolder : getSyncedFolders()) {
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

    private SyncedFolder[] getSyncedFolders() {
        Cursor c = database.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_SYNCED_FOLDERS,
                null,
                "1=1",
                null,
                null
        );
        SyncedFolder[] list = new SyncedFolder[c.getCount()];
        if (c.moveToFirst()) {
            do {
                SyncedFolder syncedFolder = createSyncedFolderFromCursor(c);
                if (syncedFolder == null) {
                    Log_OC.e(TAG, "SyncedFolder could not be created from cursor");
                } else {
                    list[c.getPosition()] = syncedFolder;
                }
            } while (c.moveToNext());

        }
        c.close();

        return list;
    }

    private SyncedFolder createSyncedFolderFromCursor(Cursor c) {
        SyncedFolder syncedFolder = null;
        if (c != null) {
            long id = c.getLong(c.getColumnIndex(ProviderMeta.ProviderTableMeta._ID));
            String localPath = c.getString(c.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH));
            String remotePath = c.getString(c.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH));
            Boolean wifiOnly = c.getInt(c.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY)) == 1;
            Boolean chargingOnly = c.getInt(c.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY)) == 1;
            Boolean subfolderByDate = c.getInt(c.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE)) == 1;
            String accountName = c.getString(c.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ACCOUNT));
            Integer uploadAction = c.getInt(c.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION));
            Boolean enabled = c.getInt(c.getColumnIndex(ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED)) == 1;

            syncedFolder = new SyncedFolder(id, localPath, remotePath, wifiOnly, chargingOnly, subfolderByDate,
                    accountName, uploadAction, enabled);
        }
        return syncedFolder;
    }
}
