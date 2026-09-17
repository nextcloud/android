/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.conflict

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.utils.extensions.toFile
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.dialog.conflict.model.ConflictDialogData
import com.owncloud.android.ui.dialog.conflict.model.ConflictDialogType
import com.owncloud.android.ui.dialog.conflict.model.ConflictFileData
import com.owncloud.android.utils.DisplayUtils
import java.io.File

object ConflictResolveDialogFactory {
    private const val SECONDS_TO_MILLIS = 1000L

    fun forOffline(
        context: Context,
        leftFile: OfflineOperationEntity,
        rightFile: OCFile,
        user: User?
    ): ConflictsResolveDialog {
        val offlineCreatedLocalFile =
            if (leftFile.type is OfflineOperationType.CreateFile) {
                (leftFile.type as OfflineOperationType.CreateFile).localPath.toFile()
            } else {
                null
            }

        val isFolderConflict = leftFile.type is OfflineOperationType.CreateFolder

        val data = ConflictDialogData(
            headline = context.getString(
                if (isFolderConflict) R.string.conflict_folder_headline
                else R.string.choose_which_file
            ),
            description = context.getString(
                if (isFolderConflict) R.string.conflict_message_description_for_folder
                else R.string.conflict_message_description
            ),
            localFile = context.conflictFileData(
                titleId = R.string.conflict_local_file,
                timestamp = (offlineCreatedLocalFile?.lastModified()
                    ?: leftFile.createdAt?.times(SECONDS_TO_MILLIS) ?: 0L),
                fileLength = (offlineCreatedLocalFile?.length() ?: 0L)
            ),
            serverFile = context.conflictFileData(R.string.prefs_synced_folders_remote_path_title, rightFile)
        )

        return createDialog(ConflictDialogType.Offline(data)) {
            putSerializable(ConflictsResolveDialog.ARG_LEFT_FILE, offlineCreatedLocalFile)
            putParcelable(ConflictsResolveDialog.ARG_RIGHT_FILE, rightFile)
            putParcelable(ConflictsResolveDialog.ARG_USER, user)
        }
    }

    fun forNormal(
        title: String,
        context: Context,
        leftFile: OCFile,
        rightFile: OCFile,
        user: User?
    ): ConflictsResolveDialog {
        val localFile = File(leftFile.storagePath)
        val data = ConflictDialogData(
            headline = context.getString(R.string.choose_which_file),
            description = context.getString(R.string.conflict_message_description),
            localFile = context.conflictFileData(
                titleId = R.string.conflict_local_file,
                timestamp = localFile.lastModified(),
                fileLength = localFile.length()
            ),
            serverFile = context.conflictFileData(R.string.conflict_server_file, rightFile)
        )

        return createDialog(ConflictDialogType.Normal(title, data)) {
            putSerializable(ConflictsResolveDialog.ARG_LEFT_FILE, localFile)
            putParcelable(ConflictsResolveDialog.ARG_RIGHT_FILE, rightFile)
            putParcelable(ConflictsResolveDialog.ARG_USER, user)
        }
    }

    private fun Context.conflictFileData(@StringRes titleId: Int, file: OCFile): ConflictFileData =
        conflictFileData(titleId, file.modificationTimestamp, file.fileLength)

    private fun Context.conflictFileData(@StringRes titleId: Int, timestamp: Long, fileLength: Long): ConflictFileData =
        ConflictFileData(
            title = getString(titleId),
            timestamp = DisplayUtils.getRelativeTimestamp(this, timestamp).toString(),
            fileSize = DisplayUtils.bytesToHumanReadable(fileLength)
        )

    private fun createDialog(type: ConflictDialogType, putFiles: Bundle.() -> Unit): ConflictsResolveDialog =
        ConflictsResolveDialog().apply {
            arguments = Bundle().apply {
                putParcelable(ConflictsResolveDialog.ARG_CONFLICT_DATA, type)
                putFiles()
            }
        }
}
