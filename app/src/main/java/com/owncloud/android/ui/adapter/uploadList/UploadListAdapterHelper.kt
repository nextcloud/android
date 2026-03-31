/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.uploadList

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

class UploadListAdapterHelper(private val activity: FileActivity) {

    companion object {
        private const val TAG = "UploadListAdapterHelper"
    }

    fun openConflictActivity(file: OCFile, upload: OCUpload) {
        file.setStoragePath(upload.localPath)
        val user = activity.accountManager.getUser(upload.accountName)
        if (user.isPresent) {
            val intent = ConflictsResolveActivity.createIntent(
                file,
                user.get(),
                upload.uploadId,
                Intent.FLAG_ACTIVITY_NEW_TASK,
                activity
            )
            activity.startActivity(intent)
        }
    }

    fun onUploadingItemClick(file: OCUpload) {
        val f = File(file.localPath)
        if (!f.exists()) {
            DisplayUtils.showSnackMessage(activity, R.string.local_file_not_found_message)
        } else {
            openFileWithDefault(file.localPath)
        }
    }

    fun onUploadedItemClick(upload: OCUpload) {
        val file = activity.storageManager.getFileByEncryptedRemotePath(upload.remotePath)
        if (file == null) {
            DisplayUtils.showSnackMessage(activity, R.string.error_retrieving_file)
            Log_OC.i(TAG, "Could not find uploaded file on remote.")
            return
        }

        val optionalUser = activity.user
        if (PreviewImageFragment.canBePreviewed(file) && optionalUser.isPresent) {
            // show image preview and stay in uploads tab
            val intent = FileDisplayActivity.openFileIntent(activity, optionalUser.get(), file)
            activity.startActivity(intent)
            return
        }

        val intent = Intent(activity, FileDisplayActivity::class.java).apply {
            setAction(Intent.ACTION_VIEW)
            putExtra(FileDisplayActivity.KEY_FILE_PATH, upload.remotePath)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        activity.startActivity(intent)
    }

    fun openFileWithDefault(localPath: String) {
        var mimetype = MimeTypeUtil.getBestMimeTypeByFilename(localPath)
        if (mimetype == "application/octet-stream") mimetype = "*/*"
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(File(localPath)), mimetype)
                }
            )
        } catch (e: ActivityNotFoundException) {
            DisplayUtils.showSnackMessage(activity, R.string.file_list_no_app_for_file_type)
            Log_OC.i(TAG, "Could not find app for sending log history: $e")
        }
    }
}
