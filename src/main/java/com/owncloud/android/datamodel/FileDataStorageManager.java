package com.owncloud.android.datamodel;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface FileDataStorageManager {
    public static final int ROOT_PARENT_ID = 0;

    OCFile getFileByPath(String path);

    Account getAccount();

    @Nullable
    OCFile getFileById(long id);

    void saveConflict(OCFile file, String etagInConflict);

    void deleteFileInMediaScan(String path);

    boolean saveFile(OCFile file);

    boolean fileExists(long id);

    boolean fileExists(String path);

    List<OCFile> getFolderContent(OCFile f, boolean onlyOnDevice);

    void saveFolder(OCFile folder, Collection<OCFile> updatedFiles, Collection<OCFile> filesToRemove);

    boolean removeFile(OCFile file, boolean removeDBData, boolean removeLocalCopy);

    boolean removeFolder(OCFile folder, boolean removeDBData, boolean removeLocalContent);

    void moveLocalFile(OCFile file, String targetPath, String targetParentPath);

    boolean saveShare(OCShare share);

    List<OCShare> getSharesWithForAFile(String filePath, String accountName);

    void removeShare(OCShare share);

    void copyLocalFile(OCFile file, String targetPath);

    OCShare getFirstShareByPathAndType(String path, ShareType type, String shareWith);

    OCCapability saveCapabilities(OCCapability capability);

    @NonNull
    OCCapability getCapability(String accountName);

    void saveSharesDB(List<OCShare> shares);

    void removeSharesForFile(String remotePath);

    OCShare getShareById(long id);

    void triggerMediaScan(String path);

    OCFile getFileByLocalPath(String path);

    @Nullable
    OCFile getFileByRemoteId(String remoteId);

    List<OCFile> getFolderImages(OCFile folder, boolean onlyOnDevice);

    OCFile saveFileWithParent(OCFile file);

    void saveNewFile(OCFile newFile);

    void migrateStoredFiles(String srcPath, String dstPath) throws RemoteException, OperationApplicationException;

    void saveShares(Collection<OCShare> shares);

    void updateSharedFiles(Collection<OCFile> sharedFiles);

    void saveSharesInFolder(ArrayList<OCShare> shares, OCFile folder);

    void deleteVirtuals(VirtualFolderType type);

    void saveVirtuals(VirtualFolderType type, List<ContentValues> values);

    void saveVirtual(VirtualFolderType type, OCFile file);

    List<OCFile> getVirtualFolderContent(VirtualFolderType type, boolean onlyImages);

    void deleteAllFiles();
}
