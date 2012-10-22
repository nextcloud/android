package com.owncloud.android.files;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;
import android.util.Log;

public class OwnCloudFileObserver extends FileObserver {

    public static int CHANGES_ONLY = CLOSE_WRITE | MOVED_FROM | MODIFY;
    
    private static String TAG = "OwnCloudFileObserver";
    private String mPath;
    private int mMask;
    FileDataStorageManager mStorage;
    Account mOCAccount;
    OCFile mFile;
    static Context mContext;
    
    public OwnCloudFileObserver(String path) {
        this(path, ALL_EVENTS);
    }
    
    public OwnCloudFileObserver(String path, int mask) {
        super(path, mask);
        mPath = path;
        mMask = mask;
    }
    
    public void setAccount(Account account) {
        mOCAccount = account;
    }
    
    public void setStorageManager(FileDataStorageManager manager) {
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
    
    @Override
    public void onEvent(int event, String path) {
        Log.d(TAG, "Got file modified with event " + event + " and path " + path);
        if ((event & mMask) == 0) {
            Log.wtf(TAG, "Incorrect event " + event + " sent for file " + path +
                         " with registered for " + mMask + " and original path " +
                         mPath);
            return;
        }
        Intent i = new Intent(mContext, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT, mOCAccount);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, mFile.getRemotePath());
        i.putExtra(FileUploader.KEY_LOCAL_FILE, mPath);
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        i.putExtra(FileUploader.KEY_FORCE_OVERWRITE, true);
        mContext.startService(i);
    }
    
}
