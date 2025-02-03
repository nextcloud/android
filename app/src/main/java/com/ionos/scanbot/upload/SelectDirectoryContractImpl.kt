/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.upload

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.ionos.scanbot.screens.save.SelectDirectoryContract
import com.ionos.scanbot.upload.target_provider.ScanbotUploadTarget
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FolderPickerActivity
import javax.inject.Inject

class SelectDirectoryContractImpl @Inject constructor(
) : SelectDirectoryContract() {

    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, FolderPickerActivity::class.java)
            .apply {
                putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SelectDirectoryResult {
        if (resultCode != Activity.RESULT_OK || intent == null || FolderPickerActivity.EXTRA_FOLDER == null)
            return SelectDirectoryResult.Canceled

        return intent.getParcelableArgument(FolderPickerActivity.EXTRA_FOLDER, OCFile::class.java)
            ?.remotePath?.let {
                SelectDirectoryResult.Success(
                    ScanbotUploadTarget(it)
                )
            } ?: SelectDirectoryResult.Canceled
    }

}