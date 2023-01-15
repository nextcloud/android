package com.owncloud.android.utils;

import com.owncloud.android.datamodel.SyncedFolder;

public interface FileSyncProgress {
    void onStartFolderSync(SyncedFolder syncedFolder, int totalFiles);
    void onProgress(String path);
}
