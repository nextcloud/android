/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;

/**
 * Observer watching a folder to request the synchronization of kept-in-sync files
 * inside it.
 * 
 * Takes into account two possible update cases:
 *  - an editor directly updates the file;
 *  - an editor works on a temporary file, and later replaces the kept-in-sync file with the
 *  former.
 *  
 *  The second case requires to monitor the folder parent of the files, since a direct 
 *  {@link FileObserver} on it will not receive more events after the file is deleted to
 *  be replaced.
 */
public class FolderObserver extends FileObserver {

    private static String TAG = FolderObserver.class.getSimpleName();

    private static int UPDATE_MASK = (
            FileObserver.ATTRIB | FileObserver.MODIFY | 
            FileObserver.MOVED_TO | FileObserver.CLOSE_WRITE
    ); 
    
    private static int IN_IGNORE = 32768;
    /* 
    private static int ALL_EVENTS_EVEN_THOSE_NOT_DOCUMENTED = 0x7fffffff;   // NEVER use 0xffffffff
    */

    private String mPath;
    private Account mAccount;
    private Context mContext;
    private Map<String, Boolean> mObservedChildren;

    /**
     * Constructor.
     * 
     * Initializes the observer to receive events about the update of the passed folder, and
     * its children files.
     * 
     * @param path          Absolute path to the local folder to watch.
     * @param account       OwnCloud account associated to the folder.
     * @param context       Used to start an operation to synchronize the file, when needed.    
     */
    public FolderObserver(String path, Account account, Context context) {
        super(path, UPDATE_MASK);
        
        if (path == null)
            throw new IllegalArgumentException("NULL path argument received");
        if (account == null)
            throw new IllegalArgumentException("NULL account argument received");
        if (context == null)
            throw new IllegalArgumentException("NULL context argument received");
        
        mPath = path;
        mAccount = account;
        mContext = context;
        mObservedChildren = new HashMap<String, Boolean>();
    }


    /**
     * Receives and processes events about updates of the monitor folder and its children files.
     * 
     * @param event     Kind of event occurred.
     * @param path      Relative path of the file referred by the event.
     */
    @Override
    public void onEvent(int event, String path) {
        Log_OC.d(TAG, "Got event " + event + " on FOLDER " + mPath + " about "
                + ((path != null) ? path : ""));
        
        boolean shouldSynchronize = false;
        synchronized(mObservedChildren) {
            if (path != null && path.length() > 0 && mObservedChildren.containsKey(path)) {
                
                if (    ((event & FileObserver.MODIFY) != 0) ||
                        ((event & FileObserver.ATTRIB) != 0) ||
                        ((event & FileObserver.MOVED_TO) != 0) ) {
                    
                    if (mObservedChildren.get(path) != true) {
                        mObservedChildren.put(path, Boolean.valueOf(true));
                    }
                }
                
                if ((event & FileObserver.CLOSE_WRITE) != 0 && mObservedChildren.get(path)) {
                    mObservedChildren.put(path, Boolean.valueOf(false));
                    shouldSynchronize = true;
                }
            }
        }
        if (shouldSynchronize) {
            startSyncOperation(path);
        }
        
        if ((event & IN_IGNORE) != 0 &&
                (path == null || path.length() == 0)) {
            Log_OC.d(TAG, "Stopping the observance on " + mPath);
        }
        
    }
    

    /**
     * Adds a child file to the list of files observed by the folder observer.
     * 
     * @param fileName         Name of a file inside the observed folder. 
     */
    public void startWatching(String fileName) {
        synchronized (mObservedChildren) {
            if (!mObservedChildren.containsKey(fileName)) {
                mObservedChildren.put(fileName, Boolean.valueOf(false));
            }
        }
        
        if (new File(mPath).exists()) {
            startWatching();
            Log_OC.d(TAG, "Started watching parent folder " + mPath + "/");
        }
        // else - the observance can't be started on a file not existing;
    }

    
    /**
     * Removes a child file from the list of files observed by the folder observer.
     * 
     * @param fileName         Name of a file inside the observed folder. 
     */
    public void stopWatching(String fileName) {
        synchronized (mObservedChildren) {
            mObservedChildren.remove(fileName);
            if (mObservedChildren.isEmpty()) {
                stopWatching();
                Log_OC.d(TAG, "Stopped watching parent folder " + mPath + "/");
            }
        }
    }

    /**
     * @return      'True' when the folder is not watching any file inside.
     */
    public boolean isEmpty() {
        synchronized (mObservedChildren) {
            return mObservedChildren.isEmpty();
        }
    }
    
    
    /**
     * Triggers an operation to synchronize the contents of a file inside the observed folder with
     * its remote counterpart in the associated ownCloud account.
     *    
     * @param fileName          Name of a file inside the watched folder.
     */
    private void startSyncOperation(String fileName) {
        FileDataStorageManager storageManager = 
                new FileDataStorageManager(mAccount, mContext.getContentResolver());
        // a fresh object is needed; many things could have occurred to the file
        // since it was registered to observe again, assuming that local files
        // are linked to a remote file AT MOST, SOMETHING TO BE DONE;
        OCFile file = storageManager.getFileByLocalPath(mPath + File.separator + fileName);
        SynchronizeFileOperation sfo = 
                new SynchronizeFileOperation(file, null, mAccount, true, mContext);
        RemoteOperationResult result = sfo.execute(storageManager, mContext);
        if (result.getCode() == ResultCode.SYNC_CONFLICT) {
            // ISSUE 5: if the user is not running the app (this is a service!),
            // this can be very intrusive; a notification should be preferred
            Intent i = new Intent(mContext, ConflictsResolveActivity.class);
            i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
            i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, mAccount);
            mContext.startActivity(i);
        }
        // TODO save other errors in some point where the user can inspect them later;
        // or maybe just toast them;
        // or nothing, very strange fails
    }

}
