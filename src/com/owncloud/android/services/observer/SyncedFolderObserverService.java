/**
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * @author Mario Danic
 * Copyright (C) 2016 Tobias Kaminsky, Andy Scherzinger
 * Copyright (C) 2017 Mario Danic
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
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
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileEntry;

import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.CopyOnWriteArrayList;

public class SyncedFolderObserverService extends Service {
    private static final String TAG = "SyncedFolderObserverService";
    private SyncedFolderProvider mProvider;
    private final IBinder mBinder = new SyncedFolderObserverBinder();
    private FileAlterationMonitor monitor;
    private FileFilter fileFilter;
    private CopyOnWriteArrayList<SerializablePair<SyncedFolder, FileEntry>> pairArrayList = new CopyOnWriteArrayList<>();
    private File file;

    @Override
    public void onCreate() {
        mProvider = new SyncedFolderProvider(MainApp.getAppContext().getContentResolver());
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

        if (file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                boolean cont = true;
                while (cont) {
                    Object obj = ois.readObject();
                    if (obj != null)
                        pairArrayList.add((SerializablePair<SyncedFolder, FileEntry>) obj);
                    else
                        cont = false;
                }

                readPerstistanceEntries = true;
            } catch (FileNotFoundException e) {
                Log_OC.d(TAG, "Failed with FileNotFound while reading persistence file");
            } catch (EOFException e) {
                Log_OC.d(TAG, "Failed with EOFException while reading persistence file");
                readPerstistanceEntries = true;
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


        if (readPerstistanceEntries && pairArrayList.size() > 0) {
            for (int i = 0; i < pairArrayList.size(); i++) {
                SyncedFolder syncFolder = pairArrayList.get(i).getKey();
                FileAlterationMagicObserver observer = new FileAlterationMagicObserver(syncFolder, fileFilter);
                observer.setRootEntry(pairArrayList.get(i).getValue());
                observer.addListener(new FileAlterationMagicListener(syncFolder));
                monitor.addObserver(observer);

            }
        } else {
            for (SyncedFolder syncedFolder : mProvider.getSyncedFolders()) {
                if (syncedFolder.isEnabled()) {
                    FileAlterationMagicObserver observer = new FileAlterationMagicObserver(syncedFolder, fileFilter);

                    try {
                        observer.init();
                    } catch (Exception e) {
                        Log_OC.d(TAG, "Failed getting an observer to intialize");
                    }

                    observer.addListener(new FileAlterationMagicListener(syncedFolder));
                    monitor.addObserver(observer);
                }
            }
        }

        if (!readPerstistanceEntries) {
            syncToDisk(false);
        }

        try {
            monitor.start();
        } catch (Exception e) {
            Log_OC.d(TAG, "Something went very wrong at onStartCommand");
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    public void syncToDisk(boolean destructive) {
        pairArrayList = new CopyOnWriteArrayList<>();
        for (FileAlterationObserver fileAlterationObserver : monitor.getObservers()) {
            FileAlterationMagicObserver fileAlterationMagicObserver =
                    (FileAlterationMagicObserver) fileAlterationObserver;
            pairArrayList.add(new SerializablePair<SyncedFolder, FileEntry>(fileAlterationMagicObserver.getSyncedFolder(),
                    fileAlterationMagicObserver.getRootEntry()));
        }

        if (pairArrayList.size() > 0) {
            try {
                File newFile = new File(file.getAbsolutePath());

                if (!newFile.exists()) {
                    newFile.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(new File(file.getAbsolutePath()), false);
                ObjectOutputStream os = new ObjectOutputStream(fos);
                for (int i = 0; i < pairArrayList.size(); i++) {
                    os.writeObject(pairArrayList.get(i));
                }
                os.close();

                if (fos != null) {
                    fos.close();
                }

            } catch (FileNotFoundException e) {
                Log_OC.d(TAG, "Failed writing to nc_sync_persistance file via FileNotFound");
            } catch (IOException e) {
                Log_OC.d(TAG, "Failed writing to nc_persisten file via IOException");
            }

        } else {
            if (file.exists()) {
                FileUtils.deleteQuietly(file);
            }
        }

        if (destructive) {
            monitor.removeObserver(null);
            try {
                monitor.stop();
            } catch (Exception e) {
                Log_OC.d(TAG, "Failed in stopping monitor");
            }
        }
    }

    @Override
    public void onDestroy() {
        syncToDisk(true);
    }

    /**
     * Restart oberver if it is enabled
     * If syncedFolder exists already, use it, otherwise create new observer
     *
     * @param syncedFolder
     */

    public void restartObserver(SyncedFolder syncedFolder) {
        boolean found = false;
        FileAlterationMagicObserver fileAlterationMagicObserver;
        for (FileAlterationObserver fileAlterationObserver : monitor.getObservers()) {
            fileAlterationMagicObserver =
                    (FileAlterationMagicObserver) fileAlterationObserver;
            if (fileAlterationMagicObserver.getSyncedFolderID() == syncedFolder.getId()) {
                if (syncedFolder.isEnabled()) {
                    for (FileAlterationListener fileAlterationListener : fileAlterationMagicObserver.getListeners()) {
                        fileAlterationMagicObserver.removeListener(fileAlterationListener);
                    }
                    fileAlterationObserver.addListener(new FileAlterationMagicListener(syncedFolder));
                } else {
                    monitor.removeObserver(fileAlterationObserver);
                }
                found = true;
                break;
            }
        }

        if (!found) {
            fileAlterationMagicObserver = new FileAlterationMagicObserver(syncedFolder, fileFilter);
            try {
                fileAlterationMagicObserver.init();
            } catch (Exception e) {
                Log_OC.d(TAG, "Failed getting an observer to intialize");
            }

            fileAlterationMagicObserver.addListener(new FileAlterationMagicListener(syncedFolder));
            monitor.addObserver(fileAlterationMagicObserver);
        }

        syncToDisk(false);

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
