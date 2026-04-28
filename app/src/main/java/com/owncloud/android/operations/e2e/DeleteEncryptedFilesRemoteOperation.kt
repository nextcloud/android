/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import android.util.Log
import com.nextcloud.common.NextcloudClient
import com.nextcloud.operations.DeleteMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import org.apache.commons.httpclient.HttpStatus

class DeleteEncryptedFilesRemoteOperation : RemoteOperation<Unit>() {

    @Deprecated("Deprecated in Java")
    @Suppress("Detekt.TooGenericExceptionCaught", "DEPRECATION")
    override fun run(client: NextcloudClient): RemoteOperationResult<Unit> {
        val method =
            DeleteMethod(uri = client.baseUri.toString() + ENCRYPTED_FILES_URL, useOcsApiRequestHeader = true)

        return try {
            val status = client.execute(method)

            if (status == HttpStatus.SC_OK) {
                RemoteOperationResult<Unit>(true, method)
            } else {
                RemoteOperationResult<Unit>(false, method).also {
                    Log.e(
                        TAG,
                        "Deleting encrypted files failed: ${method.getResponseBodyAsString()}",
                        it.exception
                    )
                }
            }
        } catch (e: Exception) {
            RemoteOperationResult<Unit>(e).also {
                Log.e(TAG, "Deleting encrypted files failed: ${it.logMessage}", it.exception)
            }
        } finally {
            method.releaseConnection()
        }
    }

    companion object {
        private val TAG = DeleteEncryptedFilesRemoteOperation::class.java.simpleName
        private const val ENCRYPTED_FILES_URL = "/ocs/v2.php/apps/end_to_end_encryption/api/v1/encrypted-files"
    }
}
