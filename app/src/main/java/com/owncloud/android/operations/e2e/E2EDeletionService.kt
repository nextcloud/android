/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.DeleteEncryptedFilesRemoteOperation
import com.owncloud.android.lib.resources.users.DeletePrivateKeyRemoteOperation
import com.owncloud.android.lib.resources.users.DeletePublicKeyRemoteOperation

@Suppress("MagicNumber")
class E2EDeletionService(private val clientFactory: ClientFactory) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showRemoveE2EKeysAndFilesAlertDialog(context: Context, user: User, onResult: (Boolean) -> Unit) {
        MaterialAlertDialogBuilder(context, R.style.FallbackTheming_Dialog)
            .setTitle(R.string.prefs_remove_e2e_keys_and_files)
            .setMessage(R.string.remove_e2e_keys_and_files_dialog_warning)
            .setCancelable(true)
            .setNegativeButton(R.string.common_cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.common_ok) { dialog, _ ->
                deleteKeysAndFiles(user) {
                    dialog.dismiss()
                    onResult(it)
                }
            }
            .show()
    }

    private fun deleteKeysAndFiles(user: User, onResult: (Boolean) -> Unit) {
        Thread {
            val result = runCatching {
                val client = clientFactory.createNextcloudClient(user)
                var successfulOperationResultCount = 3

                if (!DeletePrivateKeyRemoteOperation().execute(client).isSuccess) {
                    successfulOperationResultCount -= 1
                }

                Log_OC.i(TAG, "🔑" + "private key is deleted")

                if (!DeletePublicKeyRemoteOperation().execute(client).isSuccess) {
                    successfulOperationResultCount -= 1
                }

                Log_OC.i(TAG, "🗝" + "public key is deleted")

                if (!DeleteEncryptedFilesRemoteOperation().execute(client).isSuccess) {
                    successfulOperationResultCount -= 1
                }

                Log_OC.i(TAG, "🗂️" + "encrypted files are deleted")

                successfulOperationResultCount == 3
            }.getOrElse { e ->
                Log.e(TAG, "Cannot delete E2E keys and files", e)
                false
            }

            mainHandler.post { onResult(result) }
        }.start()
    }

    companion object {
        private val TAG = E2EDeletionService::class.java.simpleName
    }
}
