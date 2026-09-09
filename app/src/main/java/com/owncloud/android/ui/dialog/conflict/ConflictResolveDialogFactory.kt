/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.conflict

import android.content.Context
import android.os.Bundle
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.dialog.ConflictsResolveDialog
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Companion.ARG_CONFLICT_DATA
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Companion.ARG_LEFT_FILE
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Companion.ARG_RIGHT_FILE
import com.owncloud.android.ui.dialog.ConflictsResolveDialog.Companion.ARG_USER
import com.owncloud.android.ui.dialog.parcel.ConflictDialogData
import com.owncloud.android.ui.dialog.parcel.ConflictDialogType
import com.owncloud.android.ui.dialog.parcel.ConflictFileData
import com.owncloud.android.utils.DisplayUtils
import java.io.File

class ConflictResolveDialogFactory {
    fun forOffline(context: Context, leftFile: OfflineOperationEntity, rightFile: OCFile): ConflictsResolveDialog {
        val leftTitle = context.getString(R.string.prefs_synced_folders_local_path_title)
        val leftTimestamp =
            DisplayUtils.getRelativeTimestamp(context, leftFile.createdAt?.times(1000L) ?: 0)
        val leftFileSize = DisplayUtils.bytesToHumanReadable(0)
        val leftCheckBoxData = ConflictFileData(leftTitle, leftTimestamp.toString(), leftFileSize)

        val rightTitle = context.getString(R.string.prefs_synced_folders_remote_path_title)
        val rightTimestamp = DisplayUtils.getRelativeTimestamp(context, rightFile.modificationTimestamp)
        val rightFileSize = DisplayUtils.bytesToHumanReadable(rightFile.fileLength)
        val rightCheckBoxData = ConflictFileData(rightTitle, rightTimestamp.toString(), rightFileSize)

        val title = context.getString(R.string.conflict_folder_headline)
        val description = context.getString(R.string.conflict_message_description_for_folder)
        val data = ConflictDialogType.Offline(
            ConflictDialogData(
                null,
                title,
                description,
                leftCheckBoxData,
                rightCheckBoxData
            )
        )

        val bundle = Bundle().apply {
            putParcelable(ARG_CONFLICT_DATA, data)
            putParcelable(ARG_RIGHT_FILE, rightFile)
        }

        return ConflictsResolveDialog().apply {
            arguments = bundle
        }
    }

    fun forNormal(
        title: String,
        context: Context,
        leftFile: OCFile,
        rightFile: OCFile,
        user: User?
    ): ConflictsResolveDialog {
        val file = File(leftFile.storagePath)
        val leftTitle = context.getString(R.string.conflict_local_file)
        val leftTimestamp = DisplayUtils.getRelativeTimestamp(context, file.lastModified())
        val leftFileSize = DisplayUtils.bytesToHumanReadable(file.length())
        val leftCheckBoxData = ConflictFileData(leftTitle, leftTimestamp.toString(), leftFileSize)

        val rightTitle = context.getString(R.string.conflict_server_file)
        val rightTimestamp = DisplayUtils.getRelativeTimestamp(context, rightFile.modificationTimestamp)
        val rightFileSize = DisplayUtils.bytesToHumanReadable(rightFile.fileLength)
        val rightCheckBoxData = ConflictFileData(rightTitle, rightTimestamp.toString(), rightFileSize)

        val headline = context.getString(R.string.choose_which_file)
        val description = context.getString(R.string.conflict_message_description)
        val data = ConflictDialogType.Normal(
            ConflictDialogData(
                title,
                headline,
                description,
                leftCheckBoxData,
                rightCheckBoxData
            )
        )

        val bundle = Bundle().apply {
            putParcelable(ARG_CONFLICT_DATA, data)
            putSerializable(ARG_LEFT_FILE, file)
            putParcelable(ARG_RIGHT_FILE, rightFile)
            putParcelable(ARG_USER, user)
        }

        return ConflictsResolveDialog().apply {
            arguments = bundle
        }
    }
}
