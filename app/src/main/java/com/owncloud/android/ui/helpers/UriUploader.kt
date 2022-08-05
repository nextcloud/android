/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2022 Nextcloud GmbH
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.helpers

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcelable
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask
import com.owncloud.android.ui.asynctasks.CopyAndUploadContentUrisTask.OnCopyTmpFilesTaskListener
import com.owncloud.android.ui.fragment.TaskRetainerFragment
import com.owncloud.android.utils.UriUtils.getDisplayNameForUri

/**
 * This class examines URIs pointing to files to upload and then requests [FileUploader] to upload them.
 *
 *
 * URIs with scheme file:// do not require any previous processing, their path is sent to [FileUploader] to find
 * the source file.
 *
 *
 * URIs with scheme content:// are handling assuming that file is in private storage owned by a different app, and that
 * persistence permission is not granted. Due to this, contents of the file are temporary copied by the OC app, and then
 * passed [FileUploader].
 */
@Suppress(
    "Detekt.LongParameterList",
    "Detekt.SpreadOperator",
    "Detekt.TooGenericExceptionCaught"
) // legacy code
class UriUploader(
    private val mActivity: FileActivity,
    private val mUrisToUpload: List<Parcelable?>,
    private val mUploadPath: String,
    private val user: User,
    private val mBehaviour: Int,
    private val mShowWaitingDialog: Boolean,
    private val mCopyTmpTaskListener: OnCopyTmpFilesTaskListener?
) {

    enum class UriUploaderResultCode {
        OK, ERROR_UNKNOWN, ERROR_NO_FILE_TO_UPLOAD, ERROR_READ_PERMISSION_NOT_GRANTED, ERROR_SENSITIVE_PATH
    }

    fun uploadUris(): UriUploaderResultCode {
        var code = UriUploaderResultCode.OK
        try {
            val anySensitiveUri = mUrisToUpload
                .filterNotNull()
                .any { isSensitiveUri((it as Uri)) }
            if (anySensitiveUri) {
                Log_OC.e(TAG, "Sensitive URI detected, aborting upload.")
                code = UriUploaderResultCode.ERROR_SENSITIVE_PATH
            } else {
                val uris = mUrisToUpload.filterNotNull()
                    .map { it as Uri }
                    .map { Pair(it, getRemotePathForUri(it)) }

                val fileUris = uris
                    .filter { it.first.scheme == ContentResolver.SCHEME_FILE }
                fileUris.forEach {
                    requestUpload(it.first.path, it.second)
                }

                val contentUrisNew = uris
                    .filter { it.first.scheme == ContentResolver.SCHEME_CONTENT }

                if (contentUrisNew.isNotEmpty()) {
                    val (contentUris, contentRemotePaths) = contentUrisNew.unzip()
                    copyThenUpload(contentUris.toTypedArray(), contentRemotePaths.toTypedArray())
                } else if (fileUris.isEmpty()) {
                    code = UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD
                }
            }
        } catch (e: SecurityException) {
            code = UriUploaderResultCode.ERROR_READ_PERMISSION_NOT_GRANTED
            Log_OC.e(TAG, "Permissions fail", e)
        } catch (e: Exception) {
            code = UriUploaderResultCode.ERROR_UNKNOWN
            Log_OC.e(TAG, "Unexpected error", e)
        }
        return code
    }

    private fun getRemotePathForUri(sourceUri: Uri): String {
        val displayName = getDisplayNameForUri(sourceUri, mActivity)
        require(displayName != null) { "Display name cannot be null" }
        return mUploadPath + displayName
    }

    private fun isSensitiveUri(uri: Uri): Boolean = uri.toString().contains(mActivity.packageName)

    /**
     * Requests the upload of a file in the local file system to [FileUploader] service.
     *
     * The original file will be left in its original location, and will not be duplicated.
     * As a side effect, the user will see the file as not uploaded when accesses to the OC app.
     * This is considered as acceptable, since when a file is shared from another app to OC,
     * the usual workflow will go back to the original app.
     *
     * @param localPath     Absolute path in the local file system to the file to upload.
     * @param remotePath    Absolute path in the current OC account to set to the uploaded file.
     */
    private fun requestUpload(localPath: String?, remotePath: String) {
        FileUploader.uploadNewFile(
            mActivity,
            user,
            localPath,
            remotePath,
            mBehaviour,
            null, // MIME type will be detected from file name
            false, // do not create parent folder if not existent
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.ASK_USER
        )
    }

    /**
     *
     * @param sourceUris        Array of content:// URIs to the files to upload
     * @param remotePaths       Array of absolute paths to set to the uploaded files
     */
    private fun copyThenUpload(sourceUris: Array<Uri>, remotePaths: Array<String>) {
        if (mShowWaitingDialog) {
            mActivity.showLoadingDialog(mActivity.resources.getString(R.string.wait_for_tmp_copy_from_private_storage))
        }
        val copyTask = CopyAndUploadContentUrisTask(mCopyTmpTaskListener, mActivity)
        val fm = mActivity.supportFragmentManager

        // Init Fragment without UI to retain AsyncTask across configuration changes
        val taskRetainerFragment =
            fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT) as TaskRetainerFragment?
        taskRetainerFragment?.setTask(copyTask)
        copyTask.execute(
            *CopyAndUploadContentUrisTask.makeParamsToExecute(
                user,
                sourceUris,
                remotePaths,
                mBehaviour,
                mActivity.contentResolver
            )
        )
    }

    companion object {
        private val TAG = UriUploader::class.java.simpleName
    }
}
