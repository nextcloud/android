package com.owncloud.android.ui.helpers;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by JARP on 7/3/17.
 */

public class VirtualStorageManager {

    private FileDataStorageManager mStorageManager;
    private Account mAccount;
    private Context mContext;
    private static final String TAG = VirtualStorageManager.class.getSimpleName();

    public VirtualStorageManager(@NonNull FileDataStorageManager storageManager, @NonNull Account account, @NonNull Context context) {
        mStorageManager = storageManager;
        mAccount = account;
        mContext = context;
    }

    @WorkerThread
    public Vector<OCFile> parseRemoteOperation(ArrayList<Object> objects, ExtendedListFragment.SearchType searchType) {
        Vector<OCFile> mFiles = new Vector<>();
        if (objects == null || objects.isEmpty()) return mFiles;
        if (searchType == ExtendedListFragment.SearchType.SHARED_FILTER) {
            return parseShares(objects);
        } else {
            return parseVirtuals(objects, searchType);
        }
    }

    @WorkerThread
    private Vector<OCFile> parseVirtuals(ArrayList<Object> objects, ExtendedListFragment.SearchType searchType) {
        Vector<OCFile> mFiles = new Vector<>();
        if (objects == null || objects.isEmpty()) return mFiles;
        VirtualFolderType type;
        boolean onlyImages = false;
        switch (searchType) {
            case FAVORITE_SEARCH:
                type = VirtualFolderType.FAVORITE;
                break;
            case PHOTO_SEARCH:
                type = VirtualFolderType.PHOTOS;
                onlyImages = true;
                break;
            default:
                type = VirtualFolderType.NONE;
                break;
        }

        mStorageManager.deleteVirtuals(type);
        ArrayList<ContentValues> contentValues = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            OCFile ocFile = FileStorageUtils.fillOCFile((RemoteFile) objects.get(i));
            searchForLocalFileInDefaultPath(ocFile);
            OCFile temp=mStorageManager.getFileByPath(ocFile.getRemotePath());
            if(temp!=null){
                ocFile=temp;
            }else {
                ocFile = mStorageManager.saveFileWithParent(ocFile, mContext);
            }
            if (!onlyImages || MimeTypeUtil.isImage(ocFile)) {
                mFiles.add(ocFile);
            }

            ContentValues cv = new ContentValues();
            cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, type.toString());
            cv.put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.getFileId());

            contentValues.add(cv);
        }

        mStorageManager.saveVirtuals(type, contentValues);
        return mFiles;
    }

    @WorkerThread
    private Vector<OCFile> parseShares(ArrayList<Object> objects) {

        Vector<OCFile> mFiles = new Vector<>();
        if (objects == null || objects.isEmpty()) return mFiles;

        ArrayList<OCShare> shares = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            // check type before cast as of long running data fetch it is possible that old result is filled
            if (objects.get(i) instanceof OCShare) {
                OCShare ocShare = (OCShare) objects.get(i);

                shares.add(ocShare);

                // get ocFile from Server to have an up-to-date copy
                ReadRemoteFileOperation operation = new ReadRemoteFileOperation(ocShare.getPath());
                RemoteOperationResult result = operation.execute(mAccount, mContext);
                if (result.isSuccess()) {
                    OCFile file = FileStorageUtils.fillOCFile((RemoteFile) result.getData().get(0));

                    ShareType newShareType = ocShare.getShareType();
                    if (newShareType == ShareType.PUBLIC_LINK) {
                        file.setShareViaLink(true);
                    } else if (newShareType == ShareType.USER || newShareType == ShareType.GROUP ||
                            newShareType == ShareType.EMAIL || newShareType == ShareType.FEDERATED) {
                        file.setShareWithSharee(true);
                    }

                    mStorageManager.saveFile(file);

                    if (!mFiles.contains(file)) {
                        mFiles.add(file);
                    }
                } else {
                    Log_OC.e(TAG, "Error in getting prop for file: " + ocShare.getPath());
                }
            }
        }
        mStorageManager.saveShares(shares);
        return mFiles;
    }

    private void searchForLocalFileInDefaultPath(OCFile file) {
        if (file.getStoragePath() == null && !file.isFolder()) {
            File f = new File(FileStorageUtils.getDefaultSavePathFor(mAccount.name, file));
            if (f.exists()) {
                file.setStoragePath(f.getAbsolutePath());
                file.setLastSyncDateForData(f.lastModified());
            }
        }
    }
}
