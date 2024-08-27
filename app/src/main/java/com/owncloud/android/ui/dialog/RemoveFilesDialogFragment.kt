/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2018 Jessie Chatham Spencer <jessie@teainspace.com>
 * SPDX-FileCopyrightText: 2016-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.ActionMode
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import javax.inject.Inject

/**
 * Dialog requiring confirmation before removing a collection of given OCFiles.
 * Triggers the removal according to the user response.
 */
class RemoveFilesDialogFragment : ConfirmationDialogFragment(), ConfirmationDialogFragmentListener, Injectable {
    private var mTargetFiles: Collection<OCFile>? = null
    private var actionMode: ActionMode? = null

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    private var positiveButton: MaterialButton? = null

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog? ?: return

        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
        positiveButton?.let {
            viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(it)
        }

        val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton
        negativeButton?.let {
            viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(negativeButton)
        }

        val neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL) as? MaterialButton
        neutralButton?.let {
            viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(neutralButton)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        val arguments = arguments ?: return dialog

        mTargetFiles = arguments.getParcelableArrayList(ARG_TARGET_FILES)
        setOnConfirmationListener(this)
        return dialog
    }

    /**
     * Performs the removal of the target file, both locally and in the server and
     * finishes the supplied ActionMode if one was given.
     */
    override fun onConfirmation(callerTag: String?) {
        removeFiles(false)
    }

    /**
     * Performs the removal of the local copy of the target file
     */
    override fun onCancel(callerTag: String?) {
        removeFiles(true)
    }

    private fun removeFiles(onlyLocalCopy: Boolean) {
        val (offlineFiles, files) = mTargetFiles?.partition { it.isOfflineOperation } ?: Pair(emptyList(), emptyList())

        offlineFiles.forEach {
            fileDataStorageManager.deleteOfflineOperation(it)
        }

        if (files.isNotEmpty()) {
            val cg = activity as ComponentsGetter?
            cg?.fileOperationsHelper?.removeFiles(files, onlyLocalCopy, false)
        }

        if (offlineFiles.isNotEmpty()) {
            val activity = requireActivity() as? FileDisplayActivity
            activity?.refreshFolderWithDelay()
        }

        finishActionMode()
    }

    override fun onNeutral(callerTag: String?) {
        // nothing to do here
    }

    private fun setActionMode(actionMode: ActionMode?) {
        this.actionMode = actionMode
    }

    /**
     * This is used when finishing an actionMode,
     * for example if we want to exit the selection mode
     * after deleting the target files.
     */
    private fun finishActionMode() {
        actionMode?.finish()
    }

    companion object {
        private const val SINGLE_SELECTION = 1
        private const val ARG_TARGET_FILES = "TARGET_FILES"

        @JvmStatic
        fun newInstance(files: ArrayList<OCFile>, actionMode: ActionMode?): RemoveFilesDialogFragment {
            return newInstance(files).apply {
                setActionMode(actionMode)
            }
        }

        @JvmStatic
        fun newInstance(files: ArrayList<OCFile>): RemoveFilesDialogFragment {
            val messageStringId: Int

            var containsFolder = false
            var containsDown = false

            for (file in files) {
                containsFolder = containsFolder or file.isFolder
                containsDown = containsDown or file.isDown
            }

            if (files.size == SINGLE_SELECTION) {
                val file = files[0]
                messageStringId =
                    if (file.isFolder) {
                        R.string.confirmation_remove_folder_alert
                    } else {
                        R.string.confirmation_remove_file_alert
                    }
            } else {
                messageStringId =
                    if (containsFolder) {
                        R.string.confirmation_remove_folders_alert
                    } else {
                        R.string.confirmation_remove_files_alert
                    }
            }

            val bundle = Bundle().apply {
                putInt(ARG_MESSAGE_RESOURCE_ID, messageStringId)
                if (files.size == SINGLE_SELECTION) {
                    putStringArray(
                        ARG_MESSAGE_ARGUMENTS,
                        arrayOf(
                            files[0].fileName
                        )
                    )
                }

                putInt(ARG_POSITIVE_BTN_RES, R.string.file_delete)

                val isAnyFileOffline = files.any { it.isOfflineOperation }
                if ((containsFolder || containsDown) && !isAnyFileOffline) {
                    putInt(ARG_NEGATIVE_BTN_RES, R.string.confirmation_remove_local)
                }

                putInt(ARG_NEUTRAL_BTN_RES, R.string.file_keep)
                putParcelableArrayList(ARG_TARGET_FILES, files)
            }

            return RemoveFilesDialogFragment().apply {
                arguments = bundle
            }
        }

        @JvmStatic
        fun newInstance(file: OCFile): RemoveFilesDialogFragment {
            val list = ArrayList<OCFile>().apply {
                add(file)
            }
            return newInstance(list)
        }
    }
}
