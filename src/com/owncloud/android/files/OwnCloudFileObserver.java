/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

package com.owncloud.android.files;

import java.io.File;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.os.Handler;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.utils.Log_OC;

public class OwnCloudFileObserver extends FileObserver {

    private static int MASK = (FileObserver.MODIFY | FileObserver.CLOSE_WRITE);
    private static int IN_IGNORE = 32768;

    private static String TAG = OwnCloudFileObserver.class.getSimpleName();

    private String mPath;
    private int mMask;
    private Account mOCAccount;
    private Context mContext;
    private boolean mModified;
    private long mFileLastModified;
    private boolean mRestartWatching;
    private Handler mHandler;

    public OwnCloudFileObserver(String path, Account account, Context context, Handler handler) {
        super(path, FileObserver.ALL_EVENTS);
        if (path == null)
            throw new IllegalArgumentException("NULL path argument received");
        if (account == null)
            throw new IllegalArgumentException("NULL account argument received");
        if (context == null)
            throw new IllegalArgumentException("NULL context argument received");
        mPath = path;
        mOCAccount = account;
        mContext = context;
        mModified = false;
        mFileLastModified = new File(path).lastModified();
        mHandler = handler;
        Log_OC.d(TAG, "Create Observer - FileLastModified: " + mFileLastModified);
    }

    @Override
    public void onEvent(int event, String path) {
        Log_OC.d(TAG, "Got file modified with event " + event + " and path " + mPath
                + ((path != null) ? File.separator + path : ""));
        if ((event & MASK) == 0) {
            Log_OC.wtf(TAG, "Incorrect event " + event + " sent for file " + mPath
                    + ((path != null) ? File.separator + path : "") + " with registered for " + mMask
                    + " and original path " + mPath);

            // in case need start watching again
            if ((event & IN_IGNORE) != 0 && mRestartWatching) {
                mRestartWatching = false;

                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        startWatching();
                    }
                }, 5000);

            }
        } else {
            if ((event & FileObserver.MODIFY) != 0) {
                // file changed
                mModified = true;
            }
            // not sure if it's possible, but let's assume that both kind of
            // events can be received at the same time
            if ((event & FileObserver.CLOSE_WRITE) != 0) {
                // file closed
                if (mModified) {
                    mModified = false;
                    mRestartWatching = false;
                    startSyncOperation();
                } else if (isFileUpdated()) {
                    // if file has been modified but Modify event type has not
                    // been launched
                    mRestartWatching = true;
                    mFileLastModified = new File(mPath).lastModified();
                    Log_OC.d(TAG, "CLOSE_WRITE - New FileLastModified: " + mFileLastModified);
                    startSyncOperation();
                }
            }
        }
    }

    private void startSyncOperation() {
        FileDataStorageManager storageManager = new FileDataStorageManager(mOCAccount, mContext.getContentResolver());
        // a fresh object is needed; many things could have occurred to the file
        // since it was registered to observe again, assuming that local files
        // are linked to a remote file AT MOST, SOMETHING TO BE DONE;
        OCFile file = storageManager.getFileByLocalPath(mPath);
        SynchronizeFileOperation sfo = new SynchronizeFileOperation(file, null, mOCAccount, true, mContext);
        RemoteOperationResult result = sfo.execute(storageManager, mContext);
        if (result.getCode() == ResultCode.SYNC_CONFLICT) {
            // ISSUE 5: if the user is not running the app (this is a service!),
            // this can be very intrusive; a notification should be preferred
            Intent i = new Intent(mContext, ConflictsResolveActivity.class);
            i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
            i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, mOCAccount);
            mContext.startActivity(i);
        }
        // TODO save other errors in some point where the user can inspect them
        // later;
        // or maybe just toast them;
        // or nothing, very strange fails
    }

    /**
     * Check if the timestamp of last file modification in local is more current
     * that the timestamp when setting observer to the file
     * 
     * @return boolean: True if file is updated, False if not
     */
    private boolean isFileUpdated() {
        Log_OC.d(TAG, "FileLastModified: " + mFileLastModified);
        return (new File(mPath).lastModified() > mFileLastModified);
    }
}
