/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.filedisplayactivity

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.core.util.Function
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.model.OCUploadLocalPathData
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.fileNameValidator.FileNameValidator.checkFolderPath
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask.CheckAvailableSpaceListener
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.ui.helpers.UriUploader
import com.owncloud.android.utils.StringUtils
import com.owncloud.android.utils.UriUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import java.io.File

class FileDisplayUploadHandler(
    private val activity: FileDisplayActivity,
    private val connectivityService: ConnectivityService,
    private val storageManager: FileDataStorageManager
) {

    // region Content from other apps
    fun uploadContentFromApps(contentIntent: Intent, resultCode: Int, autoRename: Boolean) {
        val user = activity.user.orElse(null) ?: return

        val clipData = contentIntent.clipData
        val urisToUpload: List<Parcelable?> = if (clipData != null && clipData.itemCount > 0) {
            List(clipData.itemCount) { clipData.getItemAt(it)?.uri }
        } else {
            listOf(contentIntent.data)
        }

        val behaviour = if (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE) {
            FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
        } else {
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY
        }

        val remotePath = activity.getCurrentDir()?.remotePath ?: OCFile.ROOT_PATH
        val displayNameTransformer = if (autoRename) Function<Uri, String?> { timestampedFileName(it) } else null

        UriUploader(
            activity,
            urisToUpload,
            remotePath,
            user,
            behaviour,
            showWaitingDialog = false,
            copyTmpTaskListener = null,
            fileDisplayNameTransformer = displayNameTransformer
        ).uploadUris()
    }

    private fun timestampedFileName(uri: Uri): String? {
        val displayName = UriUtils.getDisplayNameForUri(uri, activity.applicationContext)
        if (displayName.isNullOrEmpty()) return null

        return FileOperationsHelper.getTimestampedFileName("." + FilenameUtils.getExtension(displayName))
    }
    // endregion

    // region Camera
    fun uploadFromCamera(isVideo: Boolean) {
        val cameraFilePath = FileOperationsHelper.createCameraFile(activity, isVideo).absolutePath

        val listener = object : CheckAvailableSpaceListener {
            override fun onCheckAvailableSpaceStart() {
                Log_OC.d(this, "onCheckAvailableSpaceStart")
            }

            override fun onCheckAvailableSpaceFinish(hasEnoughSpaceAvailable: Boolean, vararg filesToUpload: String?) {
                Log_OC.d(this, "onCheckAvailableSpaceFinish")
                if (!hasEnoughSpaceAvailable) return

                val capturedFilePath = filesToUpload.firstOrNull() ?: return
                renameAndUploadCapturedFile(File(capturedFilePath), isVideo)
            }
        }

        CheckAvailableSpaceTask(listener, cameraFilePath).execute()
    }

    private fun renameAndUploadCapturedFile(capturedFile: File, isVideo: Boolean) {
        val capturedFileName = if (isVideo) {
            FileOperationsHelper.getCapturedVideoName()
        } else {
            FileOperationsHelper.getCapturedImageName()
        }
        val renamedFile = File(capturedFile.parent + OCFile.PATH_SEPARATOR + capturedFileName)

        if (!capturedFile.renameTo(renamedFile)) {
            SnackbarUtil.show(activity, R.string.error_uploading_direct_camera_upload)
            return
        }

        val localBasePath = renamedFile.parentFile?.absolutePath ?: return showNoFileSelected()
        uploadFiles(localBasePath, arrayOf(renamedFile.absolutePath), FileUploadWorker.LOCAL_BEHAVIOUR_DELETE)
    }
    // endregion

    // region Files from the file system
    fun uploadFilesFromFileSystem(data: Intent, resultCode: Int) {
        val filePaths = data.getStringArrayExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES) ?: return
        val localBasePath = data.getStringExtra(UploadFilesActivity.LOCAL_BASE_PATH) ?: return showNoFileSelected()

        val behaviour = when (resultCode) {
            UploadFilesActivity.RESULT_OK_AND_MOVE -> FileUploadWorker.LOCAL_BEHAVIOUR_MOVE
            UploadFilesActivity.RESULT_OK_AND_DELETE -> FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
            else -> FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
        }
        uploadFiles(localBasePath, filePaths, behaviour)
    }

    private fun showNoFileSelected() {
        Log_OC.d(TAG, "User clicked on 'Update' with no selection")
        SnackbarUtil.show(activity, R.string.filedisplay_no_file_selected)
    }

    private fun uploadFiles(localBasePath: String, filePaths: Array<String>, behaviour: Int) {
        val normalizedLocalBasePath = if (localBasePath.endsWith("/")) localBasePath else "$localBasePath/"
        val remotePathBase = activity.getCurrentDir()?.remotePath
        val remotePaths = filePaths
            .map { (remotePathBase ?: "") + StringUtils.removePrefix(it, normalizedLocalBasePath) }
            .toTypedArray()

        connectivityService.isNetworkAndServerAvailable { isAvailable ->
            if (isAvailable) {
                uploadNewFiles(remotePathBase, filePaths, remotePaths, behaviour)
            } else {
                addCreateFileOfflineOperation(filePaths, remotePaths)
            }
        }
    }

    private fun uploadNewFiles(
        remotePathBase: String?,
        filePaths: Array<String>,
        remotePaths: Array<String>,
        behaviour: Int
    ) {
        val capability = activity.capabilities.orElse(null) ?: return

        val isValidFolderPath = remotePathBase?.let { checkFolderPath(it, capability, activity) }
        if (isValidFolderPath == false) {
            SnackbarUtil.show(
                activity,
                R.string.file_name_validator_error_contains_reserved_names_or_invalid_characters
            )
            return
        }

        val user = activity.user.orElse(null) ?: return
        val data = OCUploadLocalPathData.forFile(user, filePaths, remotePaths, behaviour, createRemoteFolder = true)
        FileUploadHelper.instance().uploadNewFiles(data)
    }

    private fun addCreateFileOfflineOperation(filePaths: Array<String>, remotePaths: Array<String>) {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                storageManager.addCreateFileOfflineOperation(filePaths, remotePaths)
            }
            activity.refreshCurrentDirectory()
        }
    }
    // endregion

    companion object {
        private const val TAG = "FileDisplayUploadHandler"
    }
}
