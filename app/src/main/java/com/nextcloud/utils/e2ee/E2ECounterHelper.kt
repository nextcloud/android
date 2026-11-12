/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import android.content.Context
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.theme.CapabilityUtils

object E2ECounterHelper {

    private const val NO_METADATA_COUNTER = -1L
    private const val INCREMENTER = 1L

    fun getCounter(context: Context, parentFile: OCFile, metadata: Any?): Long =
        getCounter(CapabilityUtils.getCapability(context), parentFile, metadata)

    fun getCounter(capability: OCCapability, parentFile: OCFile, metadata: Any?): Long {
        if (!E2EVersionHelper.isV2Plus(capability)) {
            return NO_METADATA_COUNTER
        }

        if (metadata is DecryptedFolderMetadataFile) {
            return metadata.metadata.counter + INCREMENTER
        }

        return parentFile.e2eCounter + INCREMENTER
    }
}
