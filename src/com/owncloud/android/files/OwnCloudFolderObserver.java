package com.owncloud.android.files;

import java.io.File;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.utils.Log_OC;

public class OwnCloudFolderObserver extends FileObserver {

    private static int MASK = (FileObserver.CREATE | FileObserver.MOVED_TO);

    private static String TAG = OwnCloudFolderObserver.class.getSimpleName();

    private String mPath;
    private int mMask;
    private Account mOCAccount;
    private Context mContext;

    public OwnCloudFolderObserver(String path, Account account, Context context) {
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
    }

    @Override
    public void onEvent(int event, String path) {
        Log_OC.d(TAG, "Got file modified with event " + event + " and path " + mPath
                + ((path != null) ? File.separator + path : ""));
        if ((event & MASK) == 0) {
            Log_OC.wtf(TAG, "Incorrect event " + event + " sent for file " + mPath
                    + ((path != null) ? File.separator + path : "") + " with registered for " + mMask
                    + " and original path " + mPath);

        } else {
            if ((event & FileObserver.CREATE) != 0) {
                // TODO Enable a flag
            }
            if ((event & FileObserver.MOVED_TO) != 0) {
                // TODO Start sync
            }
        }
    }

    private void startSyncOperation() {
        // TODO Move to common file because it is being used in OCFileObserver
        // too

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
    }

}
