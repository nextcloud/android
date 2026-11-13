/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.fileactivity

import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.operations.CreateShareViaLinkOperation
import com.owncloud.android.operations.CreateShareWithShareeOperation
import com.owncloud.android.operations.SetFilesDownloadLimitOperation
import com.owncloud.android.operations.UnshareOperation
import com.owncloud.android.operations.UpdateNoteForShareOperation
import com.owncloud.android.operations.UpdateShareInfoOperation
import com.owncloud.android.operations.UpdateSharePermissionsOperation
import com.owncloud.android.operations.UpdateShareViaLinkOperation
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.ShareActivity
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileDetailSharingFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.ErrorMessageAdapter

class ShareOperationResultHandler(private val activity: FileActivity) {

    private val fragmentManager
        get() = activity.supportFragmentManager

    private val sharingFragment: FileDetailSharingFragment?
        get() {
            val fragment = fragmentManager.findFragmentByTag(ShareActivity.TAG_SHARE_FRAGMENT)
                ?: fragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES)

            return when (fragment) {
                is FileDetailSharingFragment -> fragment
                is FileDetailFragment -> fragment.fileDetailSharingFragment
                else -> null
            }
        }

    fun onOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        when (operation) {
            is CreateShareViaLinkOperation -> onCreateShareViaLinkOperationFinish(operation, result)

            is CreateShareWithShareeOperation -> onUpdateShareInformation(result, R.string.sharee_add_failed)

            is UpdateShareViaLinkOperation,
            is UpdateShareInfoOperation,
            is UpdateSharePermissionsOperation -> onUpdateShareInformation(result, R.string.updating_share_failed)

            is SetFilesDownloadLimitOperation -> onUpdateShareInformation(result, R.string.set_download_limit_failed)

            is UnshareOperation -> onUpdateShareInformation(result, R.string.unsharing_failed)

            is UpdateNoteForShareOperation -> onUpdateNoteForShareOperationFinish(result)
        }
    }

    private fun onUpdateNoteForShareOperationFinish(result: RemoteOperationResult<*>) {
        if (!result.isSuccess) {
            SnackbarUtil.show(activity, R.string.note_could_not_sent)
            return
        }
        sharingFragment?.onUpdateShareInformation(result)
    }

    private fun onUpdateShareInformation(result: RemoteOperationResult<*>, @StringRes defaultError: Int) {
        val sharingFragment = sharingFragment

        if (result.isSuccess) {
            activity.updateFileFromDB()
            sharingFragment?.onUpdateShareInformation(result)
            return
        }

        val message = result.message.takeUnless { it.isNullOrEmpty() } ?: activity.getString(defaultError)
        val sharingFragmentView = sharingFragment?.view
        if (sharingFragmentView == null) {
            SnackbarUtil.show(activity, message)
            return
        }

        Snackbar.make(sharingFragmentView, message, Snackbar.LENGTH_LONG).showThemed()
    }

    private fun onCreateShareViaLinkOperationFinish(
        operation: CreateShareViaLinkOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            onShareViaLinkCreated(result)
        } else {
            onShareViaLinkFailed(operation, result)
        }
    }

    private fun onShareViaLinkCreated(result: RemoteOperationResult<*>) {
        val sharingFragment = sharingFragment
        val fileListFragment = fragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES)
        activity.updateFileFromDB()

        val publicLinkShare = result.data
            .filterIsInstance<OCShare>()
            .firstOrNull { FileDisplayActivity.TAG_PUBLIC_LINK.equals(it.shareType?.name, ignoreCase = true) }
        val sharedFile = publicLinkShare?.let { activity.storageManager.getFileByEncryptedRemotePath(it.path) }

        if (publicLinkShare != null && sharedFile != null) {
            ShareLinkHelper.copyAndShareFileLink(activity, sharedFile, publicLinkShare.shareLink)
        }
        sharingFragment?.onUpdateShareInformation(result, sharedFile)

        if (fileListFragment !is OCFileListFragment || sharedFile == null) return

        if (fileListFragment.adapterFiles.contains(sharedFile)) {
            fileListFragment.updateOCFile(sharedFile)
        } else {
            SnackbarUtil.show(activity, R.string.file_activity_shared_file_cannot_be_updated)
        }
    }

    private fun onShareViaLinkFailed(operation: CreateShareViaLinkOperation, result: RemoteOperationResult<*>) {
        val sharingFragment = sharingFragment
        val isForbiddenWithoutPassword =
            result.code == ResultCode.SHARE_FORBIDDEN && operation.password.isNullOrEmpty()

        if (!isForbiddenWithoutPassword) {
            sharingFragment?.refreshSharesFromDB()
            val errorMessage = ErrorMessageAdapter.getErrorCauseMessage(result, operation, activity.resources)
            Snackbar
                .make(activity.findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_LONG)
                .showThemed()
            return
        }

        val capability = activity.capabilities.orElse(null) ?: return
        if (!capability.filesSharingPublicEnabled.isUnknown) return
        if (sharingFragment == null || !sharingFragment.isAdded) return

        sharingFragment.requestPasswordForShareViaLink(true, capability.filesSharingPublicAskForOptionalPassword.isTrue)
    }

    private fun Snackbar.showThemed() {
        activity.viewThemeUtils.material.themeSnackbar(this)
        show()
    }
}
