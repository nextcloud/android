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
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.FileAlterationMagicListener;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.FileFilter;

public class SyncedFolderObserverService extends Service {
    private static final String TAG = "SyncedFolderObserverService";
    private final IBinder mBinder = new SyncedFolderObserverBinder();
    private FileAlterationMonitor monitor;
    private FileFilter fileFilter;

    @Override
    public void onCreate() {
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(MainApp.getAppContext().
                getContentResolver());
        monitor = new FileAlterationMonitor();

        fileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.getName().startsWith(".") && !pathname.getName().endsWith(".tmp");
            }
        };


        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                FileAlterationMagicObserver observer = new FileAlterationMagicObserver(syncedFolder, fileFilter);

                try {
                    observer.init();
                    observer.addListener(new FileAlterationMagicListener(syncedFolder));
                    monitor.addObserver(observer);
                } catch (Exception e) {
                    Log_OC.d(TAG, "Failed getting an observer to intialize " + e);
                }

            }
        }


        try {
            monitor.start();
        } catch (Exception e) {
            Log_OC.d(TAG, "Something went very wrong at onStartCommand");
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for(FileAlterationObserver fileAlterationObserver : monitor.getObservers()) {
            FileAlterationMagicObserver fileAlterationMagicObserver = (FileAlterationMagicObserver)
                    fileAlterationObserver;
            try {
                fileAlterationMagicObserver.destroy();
            } catch (Exception e) {
                Log_OC.d(TAG, "Something went very wrong on trying to destroy observers");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
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

        if (!found && syncedFolder.isEnabled()) {
            try {
                fileAlterationMagicObserver = new FileAlterationMagicObserver(syncedFolder, fileFilter);
                fileAlterationMagicObserver.init();
                fileAlterationMagicObserver.addListener(new FileAlterationMagicListener(syncedFolder));
                monitor.addObserver(fileAlterationMagicObserver);
            } catch (Exception e) {
                Log_OC.d(TAG, "Failed getting an observer to intialize");
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
