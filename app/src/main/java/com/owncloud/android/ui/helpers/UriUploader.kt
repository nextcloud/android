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
package com.owncloud.android.ui.helpers

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
import com.owncloud.android.utils.UriUtils.getDisplayNameForUri

/**
 * This class examines URIs pointing to files to upload and then requests [FileUploadHelper] to upload them.
 *
 *
 * URIs with scheme file:// do not require any previous processing, their path is sent to [FileUploadHelper] to find
 * the source file.
 *
 *
 * URIs with scheme content:// are handling assuming that file is in private storage owned by a different app, and that
 * persistence permission is not granted. Due to this, contents of the file are temporary copied by the OC app, and then
 * passed [FileUploadHelper].
 */
@Suppress(
    "Detekt.LongParameterList",
    "Detekt.SpreadOperator",
    "Detekt.TooGenericExceptionCaught"
)
class UriUploader @JvmOverloads constructor(
    private val activity: FileActivity,
    private val urisToUpload: List<Parcelable?>,
    private val uploadPath: String,
    private val user: User,
    private val behaviour: Int,
    private val showWaitingDialog: Boolean,
    private val copyTmpTaskListener: OnCopyTmpFilesTaskListener?,
    /** If non-null, this function is called to determine the desired display name (i.e. filename) after upload**/
    private val fileDisplayNameTransformer: Function<Uri, String?>? = null,
    private var albumName: String? = null
) {

    enum class UriUploaderResultCode {
        OK,
        ERROR_UNKNOWN,
        ERROR_NO_FILE_TO_UPLOAD,
        ERROR_READ_PERMISSION_NOT_GRANTED,
        ERROR_SENSITIVE_PATH
    }

    @Suppress("NestedBlockDepth")
    fun uploadUris(): UriUploaderResultCode {
        var code = UriUploaderResultCode.OK
        try {
            val anySensitiveUri = urisToUpload
                .filterNotNull()
                .any { isSensitiveUri((it as Uri)) }
            if (anySensitiveUri) {
                Log_OC.e(TAG, "Sensitive URI detected, aborting upload.")
                code = UriUploaderResultCode.ERROR_SENSITIVE_PATH
            } else {
                val uris = urisToUpload
                    .filterNotNull()
                    .map { it as Uri }
                    .map { Pair(it, getRemotePathForUri(it)) }

                val fileUris = uris.filter { it.first.scheme == ContentResolver.SCHEME_FILE }
                if (fileUris.isNotEmpty()) {
                    val localPaths = Array<String>(fileUris.size) { "" }
                    val remotePaths = Array<String>(fileUris.size) { "" }

                    for (i in 0..<fileUris.size) {
                        val uri = fileUris[i]
                        uri.first.path?.let { path ->
                            localPaths[i] = path
                            remotePaths[i] = uri.second
                        }
                    }

                    requestUpload(localPaths, remotePaths)
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
        val displayName = fileDisplayNameTransformer?.apply(sourceUri)
            ?: getDisplayNameForUri(sourceUri, activity)
        require(displayName != null) { "Display name cannot be null" }
        return uploadPath + displayName
    }

    private fun isSensitiveUri(uri: Uri): Boolean = uri.toString().contains(activity.packageName)

    /**
     * Requests the upload of a file in the local file system to [FileUploadHelper] service.
     *
     * The original file will be left in its original location, and will not be duplicated.
     * As a side effect, the user will see the file as not uploaded when accesses to the OC app.
     * This is considered as acceptable, since when a file is shared from another app to OC,
     * the usual workflow will go back to the original app.
     *
     * @param localPaths     Absolute paths in the local file system to the file to upload.
     * @param remotePaths    Absolute paths in the current OC account to set to the uploaded file.
     */
    private fun requestUpload(localPaths: Array<String>, remotePaths: Array<String>) {
        FileUploadHelper.instance().run {
            if (albumName.isNullOrEmpty()) {
                val data = OCUploadLocalPathData.forFile(user, localPaths, remotePaths, behaviour)
                uploadNewFiles(data)
            } else {
                val data = OCUploadLocalPathData.forAlbum(user, localPaths, remotePaths, behaviour)
                uploadAndCopyNewFilesForAlbum(data, albumName!!)
            }
        }
    }

    /**
     *
     * @param sourceUris        Array of content:// URIs to the files to upload
     * @param remotePaths       Array of absolute paths to set to the uploaded files
     */
    private fun copyThenUpload(sourceUris: Array<Uri>, remotePaths: Array<String>) {
        if (showWaitingDialog) {
            activity.showLoadingDialog(activity.resources.getString(R.string.wait_for_tmp_copy_from_private_storage))
        }
        val copyTask =
            CopyAndUploadContentUrisTask(copyTmpTaskListener, activity, activity.lifecycleScope, albumName)
        val fm = activity.supportFragmentManager

        // Init Fragment without UI to retain AsyncTask across configuration changes
        val taskRetainerFragment =
            fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT) as TaskRetainerFragment?
        taskRetainerFragment?.setTask(copyTask)
        copyTask.execute(
            user,
            sourceUris,
            remotePaths,
            behaviour,
            activity.contentResolver
        )
    }

    companion object {
        private val TAG = UriUploader::class.java.simpleName
    }
}
