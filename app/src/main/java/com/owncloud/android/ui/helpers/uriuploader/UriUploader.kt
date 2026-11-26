/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.helpers.uriuploader

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcelable
import androidx.core.util.Function
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.model.OCUploadLocalPathData
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener
import com.owncloud.android.ui.fragment.TaskRetainerFragment
import com.owncloud.android.ui.helpers.uriuploader.model.UriUploaderResultCode
import com.owncloud.android.utils.UriUtils.getDisplayNameForUri

class UriUploader @JvmOverloads constructor(
    private val activity: FileActivity,
    private val urisToUpload: List<Parcelable?>,
    private val uploadPath: String,
    private val user: User,
    private val behaviour: Int,
    private val showWaitingDialog: Boolean,
    private val copyTmpTaskListener: OnCopyTmpFilesTaskListener?,
    private val fileDisplayNameTransformer: Function<Uri, String?>? = null,
    private val albumName: String? = null
) {

    /**
     * The read permission for content:// URIs belongs to the calling Activity and is revoked when it is destroyed,
     * so callers must stay alive until [OnCopyTmpFilesTaskListener.onTmpFilesCopied] while this is true.
     */
    var isTmpCopyInProgress: Boolean = false
        private set

    private val uriChecker = UriChecker(activity)

    fun uploadUris(): UriUploaderResultCode = try {
        upload()
    } catch (e: SecurityException) {
        Log_OC.e(TAG, "Permissions fail", e)
        UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED
    } catch (e: Exception) {
        Log_OC.e(TAG, "Unexpected error", e)
        UriUploaderResultCode.ERROR_UNKNOWN
    }

    private fun upload(): UriUploaderResultCode {
        val uris = urisToUpload.filterIsInstance<Uri>()
        if (uris.any(uriChecker::pointsToAppPrivateData)) {
            Log_OC.e(TAG, "Sensitive URI detected, aborting upload.")
            return UriUploaderResultCode.ERROR_SENSITIVE_PATH
        }

        val fileUploads = uris
            .filter { it.scheme == ContentResolver.SCHEME_FILE }
            .mapNotNull { uri -> uri.path?.let { localPath -> localPath to getRemotePath(uri) } }
        val contentUploads = uris
            .filter { it.scheme == ContentResolver.SCHEME_CONTENT }
            .map { uri -> uri to getRemotePath(uri) }

        if (fileUploads.isEmpty() && contentUploads.isEmpty()) {
            return UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD
        }

        if (fileUploads.isNotEmpty()) {
            requestUpload(fileUploads)
        }
        if (contentUploads.isNotEmpty()) {
            copyThenUpload(contentUploads)
        }

        return UriUploaderResultCode.OK
    }

    private fun getRemotePath(uri: Uri): String {
        val displayName = fileDisplayNameTransformer?.apply(uri) ?: getDisplayNameForUri(uri, activity)
        requireNotNull(displayName) { "Display name cannot be null" }

        return uploadPath + displayName
    }

    private fun requestUpload(uploads: List<Pair<String, String>>) {
        val (localPaths, remotePaths) = uploads.unzip()
        val localPathArray = localPaths.toTypedArray()
        val remotePathArray = remotePaths.toTypedArray()
        val uploadHelper = FileUploadHelper.instance()

        if (albumName.isNullOrEmpty()) {
            uploadHelper.uploadNewFiles(OCUploadLocalPathData.forFile(user, localPathArray, remotePathArray, behaviour))
            return
        }

        val data = OCUploadLocalPathData.forAlbum(user, localPathArray, remotePathArray, behaviour)
        uploadHelper.uploadAndCopyNewFilesForAlbum(data, albumName)
    }

    private fun copyThenUpload(uploads: List<Pair<Uri, String>>) {
        if (showWaitingDialog) {
            activity.showLoadingDialog(activity.getString(R.string.wait_for_tmp_copy_from_private_storage))
        }

        val (sourceUris, remotePaths) = uploads.unzip()
        val copyTask = CopyAndUploadContentUrisTask(copyTmpTaskListener, activity, activity.lifecycleScope, albumName)
        val taskRetainer = activity.supportFragmentManager
            .findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT) as? TaskRetainerFragment
        taskRetainer?.setTask(copyTask)
        isTmpCopyInProgress = true

        copyTask.execute(
            user,
            sourceUris.toTypedArray(),
            remotePaths.toTypedArray(),
            behaviour,
            activity.contentResolver
        )
    }

    companion object {
        private val TAG = UriUploader::class.java.simpleName
    }
}
