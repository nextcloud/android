/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.GetShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.UpdateShareRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;


/**
 * Updates a note of a private share.
 */
public class UpdateNoteForShareOperation extends SyncOperation {

    private final long shareId;
    private final String note;

    public UpdateNoteForShareOperation(long shareId, String note, FileDataStorageManager storageManager) {
        super(storageManager);

        this.shareId = shareId;
        this.note = note;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {

        OCShare share = getStorageManager().getShareById(shareId);

        if (share == null) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
        }

        UpdateShareRemoteOperation updateOperation = new UpdateShareRemoteOperation(share.getRemoteId());
        updateOperation.setNote(note);
        RemoteOperationResult result = updateOperation.execute(client);

        if (result.isSuccess()) {
            RemoteOperation getShareOp = new GetShareRemoteOperation(share.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess()) {
                getStorageManager().saveShare((OCShare) result.getData().get(0));
            }
        }

        return result;
    }
}

