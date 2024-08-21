/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.operation

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactoryImpl
import com.nextcloud.utils.extensions.getErrorMessage
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RemoveFileOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class FileOperationHelper(
    private val user: User,
    private val context: Context,
    private val fileDataStorageManager: FileDataStorageManager
) {

    companion object {
        private val TAG = FileOperationHelper::class.java.simpleName
    }

    private val clientFactory = ClientFactoryImpl(context)
    private val client = clientFactory.create(user)

    @Suppress("TooGenericExceptionCaught", "Deprecation")
    suspend fun removeFile(
        file: OCFile,
        onlyLocalCopy: Boolean,
        inBackground: Boolean
    ): Boolean =
        coroutineScope {
            try {
                val operation = async(Dispatchers.IO) {
                    RemoveFileOperation(
                        file,
                        onlyLocalCopy,
                        user,
                        inBackground,
                        context,
                        fileDataStorageManager
                    )
                }
                val operationResult = operation.await()
                val result = operationResult.execute(client)

                return@coroutineScope if (result.isSuccess) {
                    true
                } else {
                    val reason = (result to operationResult).getErrorMessage()
                    Log_OC.e(TAG, "Error occurred while removing file: $reason")
                    false
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Error occurred while removing file: $e")
                false
            }
        }
}
