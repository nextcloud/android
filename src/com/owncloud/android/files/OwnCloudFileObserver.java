/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

import com.owncloud.android.Log_OC;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;


import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;

public class OwnCloudFileObserver extends FileObserver {

    public static int CHANGES_ONLY = CLOSE_WRITE;
    
    private static String TAG = OwnCloudFileObserver.class.getSimpleName();
    
    private String mPath;
    private int mMask;
    private Account mOCAccount;
    //private OCFile mFile;
    private Context mContext;

    
    public OwnCloudFileObserver(String path, Account account, Context context, int mask) {
        super(path, mask);
        if (path == null)
            throw new IllegalArgumentException("NULL path argument received"); 
        /*if (file == null)
            throw new IllegalArgumentException("NULL file argument received");*/ 
        if (account == null)
            throw new IllegalArgumentException("NULL account argument received"); 
        if (context == null)
            throw new IllegalArgumentException("NULL context argument received");
        /*if (!path.equals(file.getStoragePath()) && !path.equals(FileStorageUtils.getDefaultSavePathFor(account.name, file)))
            throw new IllegalArgumentException("File argument is not linked to the local file set in path argument"); */
        mPath = path;
        //mFile = file;
        mOCAccount = account;
        mContext = context; 
        mMask = mask;
    }
    
    @Override
    public void onEvent(int event, String path) {
        Log_OC.d(TAG, "Got file modified with event " + event + " and path " + mPath + ((path != null) ? File.separator + path : ""));
        if ((event & mMask) == 0) {
            Log_OC.wtf(TAG, "Incorrect event " + event + " sent for file " + mPath + ((path != null) ? File.separator + path : "") +
                         " with registered for " + mMask + " and original path " +
                         mPath);
            return;
        }
        FileDataStorageManager storageManager = new FileDataStorageManager(mOCAccount, mContext.getContentResolver());
        OCFile file = storageManager.getFileByLocalPath(mPath);     // a fresh object is needed; many things could have occurred to the file since it was registered to observe
                                                                    // again, assuming that local files are linked to a remote file AT MOST, SOMETHING TO BE DONE; 
        SynchronizeFileOperation sfo = new SynchronizeFileOperation(file, 
                                                                    null, 
                                                                    storageManager, 
                                                                    mOCAccount, 
                                                                    true, 
                                                                    true, 
                                                                    mContext);
        RemoteOperationResult result = sfo.execute(mOCAccount, mContext);
        if (result.getCode() == ResultCode.SYNC_CONFLICT) {
            // ISSUE 5: if the user is not running the app (this is a service!), this can be very intrusive; a notification should be preferred
            Intent i = new Intent(mContext, ConflictsResolveActivity.class);
            i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
            i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, mOCAccount);
            mContext.startActivity(i);
        }
        // TODO save other errors in some point where the user can inspect them later;
        //      or maybe just toast them;
        //      or nothing, very strange fails
    }
    
}
