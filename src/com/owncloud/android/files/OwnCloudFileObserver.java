/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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
import java.util.LinkedList;
import java.util.List;

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFileOperation;

import eu.alefzero.webdav.WebdavClient;

import android.accounts.Account;
import android.content.Context;
import android.os.FileObserver;
import android.util.Log;

public class OwnCloudFileObserver extends FileObserver {

    public static int CHANGES_ONLY = CLOSE_WRITE;
    
    private static String TAG = OwnCloudFileObserver.class.getSimpleName();
    private String mPath;
    private int mMask;
    private DataStorageManager mStorage;
    private Account mOCAccount;
    private OCFile mFile;
    private Context mContext;
    private List<FileObserverStatusListener> mListeners;
    
    public OwnCloudFileObserver(String path) {
        this(path, ALL_EVENTS);
    }
    
    public OwnCloudFileObserver(String path, int mask) {
        super(path, mask);
        mPath = path;
        mMask = mask;
        mListeners = new LinkedList<FileObserverStatusListener>();
    }
    
    public void setAccount(Account account) {
        mOCAccount = account;
    }
    
    public void setStorageManager(DataStorageManager manager) {
        mStorage = manager;
    }
    
    public void setOCFile(OCFile file) {
        mFile = file;
    }
    
    public void setContext(Context context) {
        mContext = context;
    }

    public String getPath() {
        return mPath;
    }
    
    public String getRemotePath() {
        return mFile.getRemotePath();
    }
    
    public void addObserverStatusListener(FileObserverStatusListener listener) {
        mListeners.add(listener);
    }
    
    @Override
    public void onEvent(int event, String path) {
        Log.d(TAG, "Got file modified with event " + event + " and path " + mPath + ((path != null) ? File.separator + path : ""));
        if ((event & mMask) == 0) {
            Log.wtf(TAG, "Incorrect event " + event + " sent for file " + mPath + ((path != null) ? File.separator + path : "") +
                         " with registered for " + mMask + " and original path " +
                         mPath);
            /* Unexpected event that will be ignored; no reason to propagate it 
            for (FileObserverStatusListener l : mListeners)
                l.OnObservedFileStatusUpdate(mPath, getRemotePath(), mOCAccount, Status.INCORRECT_MASK);
            */
            return;
        }
        WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(mOCAccount, mContext);
        SynchronizeFileOperation sfo = new SynchronizeFileOperation(mFile, mStorage, mOCAccount, true, false, mContext);
        RemoteOperationResult result = sfo.execute(wc);
        for (FileObserverStatusListener l : mListeners) {
            l.onObservedFileStatusUpdate(mPath, getRemotePath(), mOCAccount, result);
        }
        
    }
    
    public interface FileObserverStatusListener {
        public void onObservedFileStatusUpdate(String localPath,
                                               String remotePath,
                                               Account account,
                                               RemoteOperationResult result);
    }

    public OCFile getOCFile() {
        return mFile;
    }

    public Account getAccount() {
        return mOCAccount;
    }
    
}
