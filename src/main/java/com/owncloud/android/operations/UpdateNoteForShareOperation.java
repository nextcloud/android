/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.shares.GetShareRemoteOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.UpdateShareRemoteOperation;
import com.owncloud.android.operations.common.SyncOperation;

import java.util.List;


/**
 * Updates a note of a private share.
 */
public class UpdateNoteForShareOperation extends SyncOperation<List<OCShare>> {

    private final long shareId;
    private final String note;

    public UpdateNoteForShareOperation(long shareId, String note) {
        this.shareId = shareId;
        this.note = note;
    }

    @Override
    protected RemoteOperationResult<List<OCShare>> run(OwnCloudClient client) {

        OCShare share = getStorageManager().getShareById(shareId);

        if (share == null) {
            return new RemoteOperationResult<>(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND);
        }

        UpdateShareRemoteOperation updateOperation = new UpdateShareRemoteOperation(share.getRemoteId());
        updateOperation.setNote(note);
        RemoteOperationResult<List<OCShare>> result = updateOperation.execute(client);

        if (result.isSuccess()) {
            RemoteOperation<List<OCShare>> getShareOp = new GetShareRemoteOperation(share.getRemoteId());
            result = getShareOp.execute(client);
            if (result.isSuccess()) {
                getStorageManager().saveShare(result.getResultData().get(0));
            }
        }

        return result;
    }
}

