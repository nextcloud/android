/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Daniel Kesselberg <mail@danielkesselberg.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.datamodel.FileDataStorageManager;
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
    public RemoteOperationResult<OCCapability> run(NextcloudClient client) {
        final FileDataStorageManager storageManager = getStorageManager();

        OCCapability currentCapability = null;
        if (!storageManager.getUser().isAnonymous()) {
            currentCapability = storageManager.getCapability(storageManager.getUser().getAccountName());
        }

        RemoteOperationResult<OCCapability> result = new GetCapabilitiesRemoteOperation(currentCapability)
            .execute(client);

        if (result.isSuccess() && result.getResultData() != null) {
            // Read data from the result
            OCCapability capability = result.getResultData();

            // Save the capabilities into database
            storageManager.saveCapabilities(capability);

            // update cached entry
            CapabilityUtils.updateCapability(capability);
        }

        return result;
    }
}
