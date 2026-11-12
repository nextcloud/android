/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.uploadList

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.jobs.utils.UploadErrorNotificationManager
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.webDavParentPath
import com.owncloud.android.R
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.operations.factory.UploadFileOperationFactory
import com.owncloud.android.operations.upload.EncryptedUploadConflictChecker
import com.owncloud.android.ui.adapter.uploadList.UploadListAdapter
import com.owncloud.android.ui.adapter.uploadList.helper.ConflictHandlingResult
import com.owncloud.android.ui.adapter.uploadList.helper.UploadListAdapterHelper
import com.owncloud.android.ui.navigation.NavigatorActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadListConflictHandler(
    private val fragment: Fragment,
    private val uploadFileOperationFactory: UploadFileOperationFactory,
    private val adapterHelper: UploadListAdapterHelper,
    private val uploadListAdapter: UploadListAdapter
) {
    private var conflictSnackbar: Snackbar? = null

    fun handleConflict(upload: OCUpload) {
        val rootView = fragment.view ?: return
        val activity = fragment.getTypedActivity(NavigatorActivity::class.java) ?: return

        conflictSnackbar = Snackbar.make(
            rootView,
            R.string.upload_sync_conflict_checking,
            Snackbar.LENGTH_INDEFINITE
        ).apply { show() }

        fragment.lifecycleScope.launch {
            val result = FileUploadHelper.instance().checkConflict(upload, requirePreview = true)
            var handedOff = false
            try {
                if (!fragment.isAdded || fragment.view !== rootView) return@launch
                when (result) {
                    is ConflictHandlingResult.ConflictNotExistsRemoteFileNotFound -> {
                        showConflictSnackbar(R.string.upload_sync_conflict_not_exists)
                        retryUpload(activity, upload)
                    }

                    is ConflictHandlingResult.CannotCheckConflict -> {
                        showConflictSnackbar(R.string.upload_sync_conflict_check_error)
                    }

                    is ConflictHandlingResult.ShowConflictResolveDialog -> {
                        conflictSnackbar?.dismiss()
                        adapterHelper.openConflictActivity(result.file, result.upload, result.encryptedRemoteVersion)
                        handedOff = true
                    }

                    is ConflictHandlingResult.ConflictNotExistsSameFile -> {
                        showConflictSnackbar(R.string.upload_sync_conflict_same_file)
                        uploadListAdapter.loadUploadItemsFromDb()
                    }
                }
            } finally {
                if (!handedOff && result is ConflictHandlingResult.ShowConflictResolveDialog) {
                    EncryptedUploadConflictChecker.releasePreview(activity, result.file)
                }
            }
        }
    }

    private fun retryUpload(activity: NavigatorActivity, upload: OCUpload) {
        fragment.lifecycleScope.launch(Dispatchers.IO) {
            val user = activity.userAccountManager.getUser(upload.accountName).orElse(null) ?: return@launch
            val client = activity.clientRepository.getOwncloudClient(user) ?: return@launch
            val operation = uploadFileOperationFactory.create(activity, upload, user = user)

            val file = operation.storageManager.getFileByEncryptedRemotePath(upload.remotePath)
            val parentPath = (file?.parentRemotePath ?: upload.remotePath?.webDavParentPath())

            parentPath?.takeUnless { operation.isAutoUpload }?.let {
                val checkResult = ExistenceCheckRemoteOperation(it, false).execute(client)

                if (!checkResult.isSuccess &&
                    checkResult.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND
                ) {
                    withContext(Dispatchers.Main) {
                        showConflictSnackbar(R.string.uploader_file_not_found_message)
                    }
                    return@launch
                }
            }

            val result = operation.execute(client)

            if (result.isSuccess) {
                withContext(Dispatchers.Main) {
                    UploadErrorNotificationManager.dismissConflictResolveNotification(activity, upload.uploadId)
                    uploadListAdapter.loadUploadItemsFromDb()
                }
            }
        }
    }

    private fun showConflictSnackbar(@StringRes messageId: Int) {
        conflictSnackbar?.apply {
            setText(messageId)
            duration = Snackbar.LENGTH_LONG
            show()
        }
    }
}
