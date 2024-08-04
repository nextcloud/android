/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014-2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.GetSharesForFileRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.ArrayList;

/**
 * Provide a list shares for a specific file.
 */
public class GetSharesForFileOperation extends SyncOperation {

    private static final String TAG = GetSharesForFileOperation.class.getSimpleName();

    private final String path;
    private final boolean reshares;
    private final boolean subfiles;

    /**
     * Constructor
     *
     * @param path     Path to file or folder
     * @param reshares If set to false (default), only shares from the current user are returned If set to true, all
     *                 shares from the given file are returned
     * @param subfiles If set to false (default), lists only the folder being shared If set to true, all shared files
     *                 within the folder are returned.
     */
    public GetSharesForFileOperation(String path,
                                     boolean reshares,
                                     boolean subfiles,
                                     FileDataStorageManager storageManager) {
        super(storageManager);

        this.path = path;
        this.reshares = reshares;
        this.subfiles = subfiles;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        GetSharesForFileRemoteOperation operation = new GetSharesForFileRemoteOperation(path,
                                                                                        reshares,
                                                                                        subfiles);
        RemoteOperationResult result = operation.execute(client);

        if (result.isSuccess()) {

            // Update DB with the response
            Log_OC.d(TAG, "File = " + path + " Share list size  " + result.getData().size());
            ArrayList<OCShare> shares = new ArrayList<OCShare>();
            for (Object obj : result.getData()) {
                shares.add((OCShare) obj);
            }

            getStorageManager().saveSharesDB(shares);

        } else if (result.getCode() == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND) {
            // no share on the file - remove local shares
            getStorageManager().removeSharesForFile(path);

        }

        return result;
    }

}
