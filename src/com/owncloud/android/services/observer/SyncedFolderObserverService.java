/**
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * @author Mario Danic
 * Copyright (C) 2016 Tobias Kaminsky, Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.services.observer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.SerializablePair;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.FileAlterationMagicListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileEntry;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SyncedFolderObserverService extends Service {
    private static final String TAG = "SyncedFolderObserverService";
    private SyncedFolderProvider mProvider;
    private HashMap<SyncedFolder, FileAlterationMagicObserver> syncedFolderMap = new HashMap<>();
    private final IBinder mBinder = new SyncedFolderObserverBinder();
    private FileAlterationMonitor monitor;
    private FileFilter fileFilter;
    private CopyOnWriteArrayList<SerializablePair<SyncedFolder, FileEntry>> pairArrayList = new CopyOnWriteArrayList<>();
    private File file;

    @Override
    public void onCreate() {
        mProvider = new SyncedFolderProvider(MainApp.getAppContext().getContentResolver());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        monitor = new FileAlterationMonitor();

        fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.getName().startsWith(".") && !pathname.getName().endsWith(".tmp");
            }
        };

        file = new File(MainApp.getAppContext().getExternalFilesDir(null).getAbsolutePath() + File.separator +
                "nc_persistence");

        boolean readPerstistanceEntries = false;

        if (file.exists() ) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                boolean cont = true;
                while(cont){
                    Object obj = ois.readObject();
                    if(obj != null)
                        pairArrayList.add((SerializablePair<SyncedFolder, FileEntry>) obj);
                    else
                        cont = false;
                }

                readPerstistanceEntries = true;
            } catch (FileNotFoundException e) {
                Log_OC.d(TAG, "Failed with FileNotFound while reading persistence file");
            } catch (IOException e) {
                Log_OC.d(TAG, "Failed with IOException while reading persistence file");
            } catch (ClassNotFoundException e) {
                Log_OC.d(TAG, "Failed with ClassNotFound while reading persistence file");
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    Log_OC.d(TAG, "Failed with closing FIS");
                }
            }

        }

        Log_OC.d(TAG, "start");
        if (pairArrayList.size() == 0) {
            for (SyncedFolder syncedFolder : mProvider.getSyncedFolders()) {
                if (syncedFolder.isEnabled() && !syncedFolderMap.containsKey(syncedFolder.getLocalPath())) {
                    Log_OC.d(TAG, "start observer: " + syncedFolder.getLocalPath());
                    FileAlterationMagicObserver observer = new FileAlterationMagicObserver(new File(
                            syncedFolder.getLocalPath()), fileFilter);

                    try {
                        observer.init();
                        SerializablePair<SyncedFolder, FileEntry> pair = new SerializablePair<>(syncedFolder,
                                observer.getRootEntry());
                        pairArrayList.add(pair);
                    } catch (Exception e) {
                        Log_OC.d(TAG, "Failed getting an observer to intialize");
                    }

                    observer.addListener(new FileAlterationMagicListener(syncedFolder));
                    monitor.addObserver(observer);
                    syncedFolderMap.put(syncedFolder, observer);
                }
            }
        } else {
            for(int i = 0; i < pairArrayList.size(); i++) {
                SyncedFolder syncFolder = pairArrayList.get(i).getKey();
                FileAlterationMagicObserver observer = new FileAlterationMagicObserver(new File(
                        syncFolder.getLocalPath()), fileFilter);
                observer.setRootEntry(pairArrayList.get(i).getValue());

                observer.addListener(new FileAlterationMagicListener(syncFolder));
                monitor.addObserver(observer);
                syncedFolderMap.put(syncFolder, observer);

            }
        }

        writePersistenceEntries(readPerstistanceEntries, file);

        try {
            monitor.start();
        } catch (Exception e) {
            Log_OC.d(TAG, "Something went very wrong at onStartCommand");
        }


        return Service.START_NOT_STICKY;
    }

    private void writePersistenceEntries(boolean readPerstistanceEntries, File file) {
        FileOutputStream fos = null;

        try {
            if (pairArrayList.size() > 0 && !readPerstistanceEntries) {
                File newFile = new File(file.getAbsolutePath());
                if (!newFile.exists()) {
                    newFile.createNewFile();
                }
                fos = new FileOutputStream (new File(file.getAbsolutePath()), false);
                ObjectOutputStream os = new ObjectOutputStream(fos);
                for (int i = 0; i < pairArrayList.size(); i++) {
                    os.writeObject(pairArrayList.get(i));
                }
                os.close();
            } else if (file.exists() && pairArrayList.size() == 0) {
                FileUtils.deleteQuietly(file);
            }

            if (fos != null) {
                fos.close();
            }

        } catch (FileNotFoundException e) {
            Log_OC.d(TAG, "Failed writing to nc_sync_persistance file via FileNotFound");
        } catch (IOException e) {
            Log_OC.d(TAG, "Failed writing to nc_sync_persistance file via IOException");
        }


    }

    @Override
    public void onDestroy() {

        for (SyncedFolder syncedFolder : syncedFolderMap.keySet()) {
            FileAlterationMagicObserver obs = syncedFolderMap.get(syncedFolder);
            for (int i = 0; i < pairArrayList.size(); i++) {
                SyncedFolder pairSyncedFolder = pairArrayList.get(i).getKey();
                if (pairSyncedFolder.equals(syncedFolder)) {
                    SerializablePair<SyncedFolder, FileEntry> newPairEntry = new SerializablePair<>(syncedFolder,
                            obs.getRootEntry());
                    pairArrayList.set(i, newPairEntry);
                    break;
                }
            }
            monitor.removeObserver(obs);
            syncedFolderMap.remove(obs);

            try {
                obs.destroy();
            } catch (Exception e) {
                Log_OC.d(TAG, "Something went very wrong at onDestroy");
            }
        }

        writePersistenceEntries(false, file);
    }

    /**
     * Restart oberver if it is enabled
     * If syncedFolder exists already, use it, otherwise create new observer
     *
     * @param syncedFolder
     */

    public void restartObserver(SyncedFolder syncedFolder) {
        FileAlterationMagicObserver fileAlterationObserver;
        if (syncedFolderMap.containsKey(syncedFolder)) {
            Log_OC.d(TAG, "stop observer: " + syncedFolder.getLocalPath());
            fileAlterationObserver = syncedFolderMap.get(syncedFolder);
            monitor.removeObserver(fileAlterationObserver);
            try {
                fileAlterationObserver.destroy();
            } catch (Exception e) {
                Log_OC.d(TAG, "Something went very wrong at onDestroy");
            }

            // remove it from the paired array list
            for (int i = 0; i < pairArrayList.size(); i++) {
                if (syncedFolder.equals(pairArrayList.get(i).getKey())) {
                    pairArrayList.remove(i);
                    break;
                }
            }
            syncedFolderMap.remove(syncedFolder);
        }

        if (syncedFolder.isEnabled()) {
            Log_OC.d(TAG, "start observer: " + syncedFolder.getLocalPath());
            if (syncedFolderMap.containsKey(syncedFolder)) {
                fileAlterationObserver = syncedFolderMap.get(syncedFolder);
                if (fileAlterationObserver.getListeners() == null) {
                    fileAlterationObserver.addListener(new FileAlterationMagicListener(syncedFolder));
                }
                monitor.addObserver(fileAlterationObserver);
            } else {
                fileAlterationObserver = new FileAlterationMagicObserver(new File(syncedFolder.getLocalPath()),
                        fileFilter);

                try {
                    fileAlterationObserver.init();
                    SerializablePair<SyncedFolder, FileEntry> pair = new SerializablePair<>(syncedFolder,
                            fileAlterationObserver.getRootEntry());
                    pairArrayList.add(pair);
                } catch (Exception e) {
                    Log_OC.d(TAG, "Failed getting an observer to intialize");
                }

                fileAlterationObserver.addListener(new FileAlterationMagicListener(syncedFolder));
                monitor.addObserver(fileAlterationObserver);
                try {
                    syncedFolderMap.put(syncedFolder, fileAlterationObserver);
                } catch (Exception e) {
                    Log_OC.d(TAG, "Something went very wrong on RestartObserver");
                }

            }
        }

        writePersistenceEntries(false, file);
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
