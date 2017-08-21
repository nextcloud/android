/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
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

package com.owncloud.android.services.observer;

import android.accounts.Account;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.IBinder;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.utils.FileStorageUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Service keeping a list of {@link FolderObserver} instances that watch for local
 * changes in favorite files (formerly known as kept-in-sync files) and try to
 * synchronize them with the OC server as soon as possible.
 * 
 * Tries to be alive as long as possible; that is the reason why stopSelf() is
 * never called.
 * 
 * It is expected that the system eventually kills the service when runs low of
 * memory. To minimize the impact of this, the service always returns
 * Service.START_STICKY, and the later restart of the service is explicitly
 * considered in {@link FileObserverService#onStartCommand(Intent, int, int)}.
 */
public class FileObserverService extends Service {

    public final static String MY_NAME = FileObserverService.class.getCanonicalName();
    public final static String ACTION_START_OBSERVE = MY_NAME + ".action.START_OBSERVATION";
    public final static String ACTION_ADD_OBSERVED_FILE = MY_NAME + ".action.ADD_OBSERVED_FILE";
    public final static String ACTION_DEL_OBSERVED_FILE = MY_NAME + ".action.DEL_OBSERVED_FILE";

    private final static String ARG_FILE = "ARG_FILE";
    private final static String ARG_ACCOUNT = "ARG_ACCOUNT";

    private static final String TAG = FileObserverService.class.getSimpleName();

    private Map<String, FolderObserver> mFolderObserversMap;
    private DownloadCompletedReceiver mDownloadReceiver;

    /**
     * Factory method to create intents that allow to start an ACTION_START_OBSERVE command.
     * 
     * @param context   Android context of the caller component.
     * @return          Intent that starts a command ACTION_START_OBSERVE when
     *                  {@link Context#startService(Intent)} is called.
     */
    public static Intent makeInitIntent(Context context) {
        Intent i = new Intent(context, FileObserverService.class);
        i.setAction(ACTION_START_OBSERVE);
        return i;
    }

    /**
     * Factory method to create intents that allow to start or stop the
     * observance of a file.
     * 
     * @param context       Android context of the caller component.
     * @param file          OCFile to start or stop to watch.
     * @param account       OC account containing file.
     * @param watchIt       'True' creates an intent to watch, 'false' an intent to stop watching.
     * @return              Intent to start or stop the observance of a file through a call
     *                      to {@link Context#startService(Intent)}.
     */
    public static Intent makeObservedFileIntent(
            Context context, OCFile file, Account account, boolean watchIt) {
        Intent intent = new Intent(context, FileObserverService.class);
        intent.setAction(watchIt ? FileObserverService.ACTION_ADD_OBSERVED_FILE
                : FileObserverService.ACTION_DEL_OBSERVED_FILE);
        intent.putExtra(FileObserverService.ARG_FILE, file);
        intent.putExtra(FileObserverService.ARG_ACCOUNT, account);
        return intent;
    }

    /**
     * Initialize the service. 
     */
    @Override
    public void onCreate() {
        Log_OC.d(TAG, "onCreate");
        super.onCreate();

        mDownloadReceiver = new DownloadCompletedReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(FileDownloader.getDownloadAddedMessage());
        filter.addAction(FileDownloader.getDownloadFinishMessage());
        registerReceiver(mDownloadReceiver, filter);

        mFolderObserversMap = new HashMap<String, FolderObserver>();
    }

    /**
     * Release resources.
     */
    @Override
    public void onDestroy() {
        Log_OC.d(TAG, "onDestroy - finishing observation of favorite files");

        unregisterReceiver(mDownloadReceiver);

        Iterator<FolderObserver> itOCFolder = mFolderObserversMap.values().iterator();
        while (itOCFolder.hasNext()) {
            itOCFolder.next().stopWatching();
        }
        mFolderObserversMap.clear();
        mFolderObserversMap = null;

        super.onDestroy();
    }

    /**
     * This service cannot be bound.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Handles requests to:
     *  - (re)start watching                    (ACTION_START_OBSERVE)
     *  - add an {@link OCFile} to be watched   (ATION_ADD_OBSERVED_FILE)
     *  - stop observing an {@link OCFile}      (ACTION_DEL_OBSERVED_FILE) 
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log_OC.d(TAG, "Starting command " + intent);

        if (intent == null || ACTION_START_OBSERVE.equals(intent.getAction())) {
            // NULL occurs when system tries to restart the service after its
            // process was killed
            startObservation();
            return Service.START_STICKY;

        } else if (ACTION_ADD_OBSERVED_FILE.equals(intent.getAction())) {
            OCFile file = intent.getParcelableExtra(ARG_FILE);
            Account account = intent.getParcelableExtra(ARG_ACCOUNT);
            addObservedFile(file, account);

        } else if (ACTION_DEL_OBSERVED_FILE.equals(intent.getAction())) {
            removeObservedFile(intent.getParcelableExtra(ARG_FILE),
                    intent.getParcelableExtra(ARG_ACCOUNT));

        } else {
            Log_OC.e(TAG, "Unknown action received; ignoring it: " + intent.getAction());
        }

        return Service.START_STICKY;
    }

    
    /**
     * Read from the local database the list of files that must to be kept
     * synchronized and starts observers to monitor local changes on them.
     * 
     * Updates the list of currently observed files if called multiple times.
     */
    private void startObservation() {
        Log_OC.d(TAG, "Loading all kept-in-sync files from database to start watching them");

        if (MainApp.getAppContext() == null) {
            MainApp.setAppContext(getApplicationContext());
        }

        // query for any favorite file in any OC account
        Cursor cursorOnKeptInSync = getContentResolver().query(
                ProviderTableMeta.CONTENT_URI, 
                null,
                ProviderTableMeta.FILE_KEEP_IN_SYNC + " = ?", 
                new String[] { String.valueOf(1) }, 
                null
        );

        if (cursorOnKeptInSync != null) {

            if (cursorOnKeptInSync.moveToFirst()) {

                String localPath = "";
                String accountName = "";
                Account account = null;
                do {
                    localPath = cursorOnKeptInSync.getString(cursorOnKeptInSync
                            .getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
                    accountName = cursorOnKeptInSync.getString(cursorOnKeptInSync
                            .getColumnIndex(ProviderTableMeta.FILE_ACCOUNT_OWNER));

                    account = new Account(accountName, MainApp.getAccountType());
                    if (!AccountUtils.exists(account, this) || localPath == null || localPath.length() <= 0) {
                        continue;
                    }
                    
                    addObservedFile(localPath, account);

                } while (cursorOnKeptInSync.moveToNext());

            }
            cursorOnKeptInSync.close();
        }

        // service does not stopSelf() ; that way it tries to be alive forever

    }

    
    /**
     * Registers the local copy of a remote file to be observed for local
     * changes, an automatically updated in the ownCloud server.
     * 
     * This method does NOT perform a {@link SynchronizeFileOperation} over the
     * file.
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
        
        addObservedFile(localPath, account);
        
    }

    
    
    
    /**
     * Registers a local file to be observed for changes.
     * 
     * @param localPath     Absolute path in the local file system to the file to be observed.
     * @param account       OwnCloud account associated to the local file.
     */
    private void addObservedFile(String localPath, Account account) {
        File file = new File(localPath);
        String parentPath = file.getParent();
        FolderObserver observer = mFolderObserversMap.get(parentPath);
        if (observer == null) {
            observer = new FolderObserver(parentPath, account, getApplicationContext());
            mFolderObserversMap.put(parentPath, observer);
            Log_OC.d(TAG, "Observer added for parent folder " + parentPath + "/");
        }
        
        observer.startWatching(file.getName());
        Log_OC.d(TAG, "Added " + localPath + " to list of observed children");
    }

    
    /**
     * Unregisters the local copy of a remote file to be observed for local changes.
     * 
     * @param file      Object representing a remote file which local copy must be not 
     *                  observed longer.
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

        removeObservedFile(localPath);
    }

    
    /**
     * Unregisters a local file from being observed for changes.
     * 
     * @param localPath     Absolute path in the local file system to the target file.
     */
    private void removeObservedFile(String localPath) {
        File file = new File(localPath);
        String parentPath = file.getParent();
        FolderObserver observer = mFolderObserversMap.get(parentPath);
        if (observer != null) {
            observer.stopWatching(file.getName());
            if (observer.isEmpty()) {
                mFolderObserversMap.remove(parentPath);
                Log_OC.d(TAG, "Observer removed for parent folder " + parentPath + "/");
            }
        
        } else {
            Log_OC.d(TAG, "No observer to remove for path " + localPath);
        }
    }

    
    /**
     * Private receiver listening to events broadcasted by the {@link FileDownloader} service.
     * 
     * Pauses and resumes the observance on registered files while being download,
     * in order to avoid to unnecessary synchronizations.
     */
    private class DownloadCompletedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log_OC.d(TAG, "Received broadcast intent " + intent);

            File downloadedFile = new File(intent.getStringExtra(FileDownloader.EXTRA_FILE_PATH));
            String parentPath = downloadedFile.getParent();
            FolderObserver observer = mFolderObserversMap.get(parentPath);
            if (observer != null) {
                if (intent.getAction().equals(FileDownloader.getDownloadFinishMessage())
                        && downloadedFile.exists()) {
                    // no matter if the download was successful or not; the
                    // file could be down anyway due to a former download or upload
                    observer.startWatching(downloadedFile.getName());
                    Log_OC.d(TAG, "Resuming observance of " + downloadedFile.getAbsolutePath());

                } else if (intent.getAction().equals(FileDownloader.getDownloadAddedMessage())) {
                    observer.stopWatching(downloadedFile.getName());
                    Log_OC.d(TAG, "Pausing observance of " + downloadedFile.getAbsolutePath());
                }

            } else {
                Log_OC.d(TAG, "No observer for path " + downloadedFile.getAbsolutePath());
            }
        }

    }

}
