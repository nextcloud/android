/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.fileactions

import androidx.annotation.IdRes
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.ShortcutUtil
import com.nextcloud.utils.extensions.handle
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.ui.activity.TextEditorWebView
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class FileActionHandler @Inject constructor(
    private val backgroundJobManager: BackgroundJobManager,
    private val accountManager: UserAccountManager,
    private val editorUtils: EditorUtils,
    private val shortcutUtil: ShortcutUtil,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val viewThemeUtils: ViewThemeUtils
) {
    fun handle(@IdRes itemId: Int, files: Collection<OCFile>, host: FileActionHost): Boolean =
        files.isNotEmpty() && if (files.size == 1) {
            file(itemId, files.first(), host)
        } else {
            files(itemId, files, host)
        }

    @Suppress("CyclomaticComplexMethod")
    private fun file(@IdRes itemId: Int, file: OCFile, host: FileActionHost): Boolean {
        when (itemId) {
            R.id.action_send_share_file -> host.containerActivity.showDetails(file, SHARING_TAB)

            R.id.action_rename_file -> RenameFileDialogFragment.newInstance(file, host.parentFolder)
                .show(host.fragmentManager, FileDetailFragment.FTAG_RENAME_FILE)

            R.id.action_open_in_web_editor -> TextEditorWebView.startTextEditor(file, host.context)

            R.id.action_edit -> edit(file, host)

            R.id.action_download_file, R.id.action_sync_file -> sync(file, host)

            R.id.action_sync_all_files -> syncFolder(file, host)

            R.id.action_pin_to_homescreen ->
                shortcutUtil.addShortcutToHomescreen(file, viewThemeUtils, accountManager.user, syncedFolderProvider)

            R.id.action_retry -> backgroundJobManager.startOfflineOperations()

            else -> return host.helper?.handle(itemId, file, host.view) == true || files(itemId, listOf(file), host)
        }

        return true
    }

    private fun files(@IdRes itemId: Int, files: Collection<OCFile>, host: FileActionHost): Boolean {
        when (itemId) {
            R.id.action_remove_file -> RemoveFilesDialogFragment.newInstance(ArrayList(files), host.actionMode)
                .show(host.fragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)

            R.id.action_cancel_sync -> files.forEach {
                host.fda?.cancelTransference(it)
            }

            R.id.action_favorite -> setFavorite(files, true, host)

            R.id.action_unset_favorite -> setFavorite(files, false, host)

            else -> return host.helper?.handle(itemId, files, host.view, backgroundJobManager) == true
        }

        return true
    }

    private fun setFavorite(files: Collection<OCFile>, isFavorite: Boolean, host: FileActionHost) {
        host.helper?.toggleFavoriteFiles(files, isFavorite)
        host.onFavoriteChanged(isFavorite)
    }

    private fun edit(file: OCFile, host: FileActionHost) {
        val context = host.context ?: return
        when {
            editorUtils.isEditorAvailable(accountManager.user, file.mimeType) ->
                TextEditorWebView.startTextEditor(file, context)

            EditImageActivity.canBePreviewed(file) ->
                host.fda?.startImageEditor(file)

            else -> host.helper?.openFileAsRichDocument(file, context)
        }
    }

    private fun sync(file: OCFile, host: FileActionHost) {
        host.fileActivity?.showSyncLoadingDialog(file.isFolder)
        host.helper?.syncFileOrFolder(file)
    }

    private fun syncFolder(folder: OCFile, host: FileActionHost) {
        if (!FileStorageUtils.checkIfEnoughSpace(folder)) {
            SyncFileNotEnoughSpaceDialogFragment
                .newInstance(folder, FileOperationsHelper.getAvailableSpaceOnDevice())
                .show(host.fragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
            return
        }

        ConfirmationDialogFragment.newInstance(
            R.string.sync_all_action_dialog_description,
            null,
            R.string.sync_all_action_dialog_title,
            R.drawable.ic_sync_all,
            R.string.common_ok,
            R.string.common_cancel,
            NO_NEUTRAL_BUTTON
        ).run {
            isCancelable = false
            setOnConfirmationListener(object : ConfirmationDialogFragment.ConfirmationDialogFragmentListener {
                override fun onConfirmation(callerTag: String?) {
                    host.helper?.syncFolderIncludingNestedFiles(folder)
                }
                override fun onNeutral(callerTag: String?) = Unit
                override fun onCancel(callerTag: String?) = Unit
            })
            show(host.fragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
        }
    }

    companion object {
        private const val SHARING_TAB = 1
        private const val NO_NEUTRAL_BUTTON = -1
    }
}
