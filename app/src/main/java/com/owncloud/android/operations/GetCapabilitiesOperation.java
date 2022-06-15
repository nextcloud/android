/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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
package com.owncloud.android.operations;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.GetCapabilitiesRemoteOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.theme.CapabilityUtils;

/**
 * Get and save capabilities from the server
 */
public class GetCapabilitiesOperation extends SyncOperation {

    public GetCapabilitiesOperation(FileDataStorageManager storageManager) {
        super(storageManager);
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        final FileDataStorageManager storageManager = getStorageManager();

        OCCapability currentCapability = null;
        if (!storageManager.getUser().isAnonymous()) {
            currentCapability = storageManager.getCapability(storageManager.getUser().getAccountName());
        }

        RemoteOperationResult result = new GetCapabilitiesRemoteOperation(currentCapability).execute(client);

        if (result.isSuccess()
                && result.getData() != null && result.getData().size() > 0) {
            // Read data from the result
            OCCapability capability = (OCCapability) result.getData().get(0);

            // Save the capabilities into database
            storageManager.saveCapabilities(capability);

            // update cached entry
            CapabilityUtils.updateCapability(capability);
        }

        return result;
    }

}
