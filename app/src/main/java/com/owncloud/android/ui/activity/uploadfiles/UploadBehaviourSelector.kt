/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.uploadfiles

import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadFilesLayoutBinding
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.utils.FileUtil
import java.io.File

class UploadBehaviourSelector(
    private val activity: AppCompatActivity,
    private val binding: UploadFilesLayoutBinding,
    private val preferences: AppPreferences
) {
    private val spinner get() = binding.uploadFilesSpinnerBehaviour

    val isMoveSelected: Boolean
        get() = spinner.selectedItemPosition == POSITION_MOVE

    val resultCode: Int?
        get() = when (spinner.selectedItemPosition) {
            POSITION_MOVE -> UploadFilesActivity.RESULT_OK_AND_MOVE
            POSITION_ONLY_UPLOAD -> UploadFilesActivity.RESULT_OK_AND_DO_NOTHING
            POSITION_DELETE -> UploadFilesActivity.RESULT_OK_AND_DELETE
            else -> null
        }

    fun setup(rootFolderName: String) {
        val behaviours = listOf(
            activity.getString(R.string.uploader_upload_files_behaviour_move_to_nextcloud_folder, rootFolderName),
            activity.getString(R.string.uploader_upload_files_behaviour_only_upload),
            activity.getString(R.string.uploader_upload_files_behaviour_upload_and_delete_from_source)
        )

        spinner.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, behaviours).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.setSelection(preferences.uploaderBehaviour)
    }

    fun saveSelection() {
        preferences.uploaderBehaviour = spinner.selectedItemPosition
    }

    fun checkWritableFolder(folder: File?) {
        FileUtil.isFolderWritable(folder, activity.lifecycle) { canWriteIntoFolder ->
            spinner.isEnabled = canWriteIntoFolder

            val behaviourLabel = activity.getString(R.string.uploader_upload_files_behaviour)
            if (canWriteIntoFolder) {
                binding.uploadFilesUploadFilesBehaviourText.text = behaviourLabel
                spinner.setSelection(preferences.uploaderBehaviour)
            } else {
                spinner.setSelection(POSITION_ONLY_UPLOAD)
                val notWritableLabel = activity.getString(R.string.uploader_upload_files_behaviour_not_writable)
                binding.uploadFilesUploadFilesBehaviourText.text =
                    listOf(behaviourLabel, notWritableLabel).joinToString(LABEL_SEPARATOR)
            }
        }
    }

    companion object {
        private const val POSITION_MOVE = 0
        private const val POSITION_ONLY_UPLOAD = 1
        private const val POSITION_DELETE = 2
        private const val LABEL_SEPARATOR = " "
    }
}
