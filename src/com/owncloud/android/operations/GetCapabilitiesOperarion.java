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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.GetRemoteCapabilitiesOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.common.SyncOperation;

/**
 * Get and save capabilities from the server
 */
public class GetCapabilitiesOperarion extends SyncOperation {

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        GetRemoteCapabilitiesOperation getCapabilities = new GetRemoteCapabilitiesOperation();
        RemoteOperationResult result = getCapabilities.execute(client);

        if (result.isSuccess()
                && result.getData() != null && result.getData().size() > 0) {
            // Read data from the result
            OCCapability capability = (OCCapability) result.getData().get(0);

            // Save the capabilities into database
            getStorageManager().saveCapabilities(capability);
        }

        return result;
    }

}
