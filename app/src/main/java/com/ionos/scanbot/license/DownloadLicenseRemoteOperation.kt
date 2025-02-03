/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import org.apache.commons.httpclient.methods.GetMethod

class DownloadLicenseRemoteOperation(
    private val licenseKeyUrl: String,
) : RemoteOperation<String>() {

    var license: String? = null
        private set

    override fun run(client: OwnCloudClient?): RemoteOperationResult<String> {
        return try {
            if (client == null) throw IllegalArgumentException("Client should not be null")

            val getMethod = GetMethod(licenseKeyUrl)

            license = downloadFile(client, getMethod)
            RemoteOperationResult(true, getMethod)
        } catch (e: Exception) {
            RemoteOperationResult(e)
        }
    }

    private fun downloadFile(
        client: OwnCloudClient,
        getMethod: GetMethod,
    ): String? {
        try {
            val status = client.executeMethod(getMethod)
            if (isSuccess(status)) {
                val bytes = getMethod.responseBodyAsStream
                    .use {
                        it.readBytes()
                    }

                return LicenseResponseTransformer().transform(bytes)
            }
        } finally {
            getMethod.releaseConnection()
        }

        return null
    }

    private fun isSuccess(status: Int): Boolean {
        return status == 200
    }
}