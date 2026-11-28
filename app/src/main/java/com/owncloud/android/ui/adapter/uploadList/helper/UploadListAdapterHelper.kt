/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.uploadList.helper

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.upload.EncryptedUploadConflictChecker
import com.owncloud.android.operations.upload.UploadFileLink
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.preview.media.PreviewMediaFragment
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

class UploadListAdapterHelper(private val activity: DrawerActivity) {

    companion object {
        private const val TAG = "UploadListAdapterHelper"
        private const val ANY_MIME_TYPE = "*/*"
    }

    fun openConflictActivity(file: OCFile, upload: OCUpload, encryptedRemoteVersion: String? = null) {
        val user = activity.userAccountManager.getUser(upload.accountName)
        user.ifPresent {
            val local = if (file.isEncrypted) {
                FileDataStorageManager(it, activity.contentResolver).getFileByEncryptedRemotePath(file.remotePath)
                    ?: OCFile(file.remotePath).apply {
                        isEncrypted = true
                        decryptedRemotePath = file.decryptedRemotePath
                        mimeType = file.mimeType
                        parentId = file.parentId
                    }
            } else {
                file
            }
            local.storagePath = upload.localPath
            val intent = ConflictsResolveActivity.createIntent(
                local,
                it,
                upload.uploadId,
                Intent.FLAG_ACTIVITY_NEW_TASK,
                activity
            )
            if (file.isEncrypted) intent.putExtra(ConflictsResolveActivity.EXTRA_EXISTING_FILE, file)
            intent.putExtra(ConflictsResolveActivity.EXTRA_ENCRYPTED_REMOTE_VERSION, encryptedRemoteVersion)
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                EncryptedUploadConflictChecker.releasePreview(activity, file)
                throw e
            }
        }
        if (user.isEmpty) {
            EncryptedUploadConflictChecker.releasePreview(
                activity,
                file
            )
        }
    }

    fun onUploadingItemClick(file: OCUpload) {
        if (File(file.localPath).exists()) {
            openFileWithDefault(file.localPath)
            return
        }

        SnackbarUtil.show(activity, R.string.local_file_not_found_message)
    }

    fun onUploadedItemClick(upload: OCUpload) {
        val user = activity.userAccountManager.getUser(upload.accountName).orElse(null) ?: return
        val storage = FileDataStorageManager(user, activity.contentResolver)
        val file = UploadFileLink.findUploadedFile(storage, upload.remotePath)
        if (file == null) {
            SnackbarUtil.show(activity, R.string.error_retrieving_file)
            Log_OC.i(TAG, "Could not find uploaded file on remote.")
            return
        }

        if (PreviewMediaFragment.canBePreviewed(file)) {
            // show image preview and stay in uploads tab
            val intent = FileDisplayActivity.openFileIntent(activity, user, file)
            activity.startActivity(intent)
            return
        }

        val intent = Intent(activity, FileDisplayActivity::class.java).apply {
            setAction(Intent.ACTION_VIEW)
            putExtra(FileDisplayActivity.KEY_FILE_PATH, file.remotePath)
            putExtra(FileActivity.EXTRA_USER, user)
            putExtra(FileDisplayActivity.KEY_ACCOUNT, user.accountName)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        activity.startActivity(intent)
    }

    fun openFileWithDefault(localPath: String) {
        val uri = contentUriFor(localPath)
        if (uri == null) {
            SnackbarUtil.show(activity, R.string.error_retrieving_file)
            return
        }

        var mimetype = MimeTypeUtil.getBestMimeTypeByFilename(localPath)
        if (mimetype == MimeType.FILE) mimetype = ANY_MIME_TYPE

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimetype)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            SnackbarUtil.show(activity, R.string.file_list_no_app_for_file_type)
            Log_OC.i(TAG, "Could not find app for opening the local file: $e")
        }
    }

    private fun contentUriFor(localPath: String): Uri? = try {
        FileProvider.getUriForFile(activity, activity.getString(R.string.file_provider_authority), File(localPath))
    } catch (e: IllegalArgumentException) {
        Log_OC.e(TAG, "Local file is outside the paths supported by the file provider: $e")
        null
    }
}
