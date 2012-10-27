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

import java.util.LinkedList;
import java.util.List;

import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.OwnCloudFileObserver.FileObserverStatusListener.Status;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.SynchronizeFileOperation;

import eu.alefzero.webdav.WebdavClient;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.util.Log;

public class OwnCloudFileObserver extends FileObserver {

    public static int CHANGES_ONLY = CLOSE_WRITE;
    
    private static String TAG = "OwnCloudFileObserver";
    private String mPath;
    private int mMask;
    DataStorageManager mStorage;
    Account mOCAccount;
    OCFile mFile;
    static Context mContext;
    List<FileObserverStatusListener> mListeners;
    
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
        Log.d(TAG, "Got file modified with event " + event + " and path " + path);
        if ((event & mMask) == 0) {
            Log.wtf(TAG, "Incorrect event " + event + " sent for file " + path +
                         " with registered for " + mMask + " and original path " +
                         mPath);
            for (FileObserverStatusListener l : mListeners)
                l.OnObservedFileStatusUpdate(mPath, getRemotePath(), mOCAccount, Status.INCORRECT_MASK);
            return;
        }
        WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(mOCAccount, mContext);
        SynchronizeFileOperation sfo = new SynchronizeFileOperation(mFile.getRemotePath(), mStorage, mOCAccount, mContext);
        RemoteOperationResult result = sfo.execute(wc);
        
        if (result.getExtraData() == Boolean.TRUE) {
            // inform user about conflict and let him decide what to do
            for (FileObserverStatusListener l : mListeners)
                l.OnObservedFileStatusUpdate(mPath, getRemotePath(), mOCAccount, Status.CONFLICT);
            return;
        }

        for (FileObserverStatusListener l : mListeners)
            l.OnObservedFileStatusUpdate(mPath, getRemotePath(), mOCAccount, Status.SENDING_TO_UPLOADER);
        
        Intent i = new Intent(mContext, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT, mOCAccount);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, mFile.getRemotePath());
        i.putExtra(FileUploader.KEY_LOCAL_FILE, mPath);
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        i.putExtra(FileUploader.KEY_FORCE_OVERWRITE, true);
        mContext.startService(i);
    }
    
    public interface FileObserverStatusListener {
        public enum Status {
            SENDING_TO_UPLOADER,
            CONFLICT,
            INCORRECT_MASK
        }
        
        public void OnObservedFileStatusUpdate(String localPath,
                                               String remotePath,
                                               Account account,
                                               FileObserverStatusListener.Status status);
    }
    
}
