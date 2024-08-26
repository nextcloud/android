/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2014 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.collect.Sets
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.fileNameValidator.FileNameValidator.checkFileName
import com.nextcloud.utils.fileNameValidator.FileNameValidator.isFileHidden
import com.owncloud.android.R
import com.owncloud.android.databinding.EditBoxDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Dialog to input a new name for an [OCFile] being renamed.
 * Triggers the rename operation.
 */
class RenameFileDialogFragment : DialogFragment(), DialogInterface.OnClickListener, TextWatcher, Injectable {
    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    @Inject
    lateinit var keyboardUtils: KeyboardUtils

    @Inject
    lateinit var currentAccount: CurrentAccountProvider

    private lateinit var binding: EditBoxDialogBinding
    private var mTargetFile: OCFile? = null
    private var positiveButton: MaterialButton? = null
    private var fileNames: MutableSet<String>? = null

    override fun onStart() {
        super.onStart()
        initAlertDialog()
    }

    override fun onResume() {
        super.onResume()
        keyboardUtils.showKeyboardForEditText(requireDialog().window, binding.userInput)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mTargetFile = requireArguments().getParcelableArgument(ARG_TARGET_FILE, OCFile::class.java)

        val inflater = requireActivity().layoutInflater
        binding = EditBoxDialogBinding.inflate(inflater, null, false)

        val currentName = mTargetFile?.fileName
        binding.userInput.setText(currentName)
        viewThemeUtils.material.colorTextInputLayout(binding.userInputContainer)
        val extensionStart = if (mTargetFile?.isFolder == true) -1 else currentName?.lastIndexOf('.')
        val selectionEnd = if ((extensionStart ?: -1) >= 0) extensionStart else currentName?.length
        if (selectionEnd != null) {
            binding.userInput.setSelection(0, selectionEnd)
        }

        val parentFolder = arguments.getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)
        val folderContent = fileDataStorageManager.getFolderContent(parentFolder, false)
        fileNames = Sets.newHashSetWithExpectedSize(folderContent.size)

        for (file in folderContent) {
            fileNames?.add(file.fileName)
        }

        binding.userInput.addTextChangedListener(this)

        val builder = buildMaterialAlertDialog(binding.root)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.userInputContainer.context, builder)

        return builder.create()
    }

    private fun buildMaterialAlertDialog(view: View): MaterialAlertDialogBuilder {
        val builder = MaterialAlertDialogBuilder(requireActivity())

        builder
            .setView(view)
            .setPositiveButton(R.string.file_rename, this)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(R.string.rename_dialog_title)

        return builder
    }

    private fun initAlertDialog() {
        val alertDialog = dialog as AlertDialog?

        if (alertDialog != null) {
            positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton
            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton

            positiveButton?.let {
                viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
            }
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton)
        }
    }

    private val oCCapability: OCCapability
        get() = fileDataStorageManager.getCapability(currentAccount.user.accountName)

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            var newFileName = ""

            if (binding.userInput.text != null) {
                newFileName = binding.userInput.text.toString().trim { it <= ' ' }
            }

            val errorMessage = checkFileName(newFileName, oCCapability, requireContext(), null)
            if (errorMessage != null) {
                DisplayUtils.showSnackMessage(requireActivity(), errorMessage)
                return
            }

            if (mTargetFile?.isOfflineOperation == true) {
                fileDataStorageManager.renameCreateFolderOfflineOperation(mTargetFile, newFileName)
                if (requireActivity() is FileDisplayActivity) {
                    val activity = requireActivity() as FileDisplayActivity
                    activity.refreshFolderWithDelay()
                }
            } else {
                (requireActivity() as ComponentsGetter).fileOperationsHelper.renameFile(mTargetFile, newFileName)
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

    /**
     * When user enters a hidden file name, the 'hidden file' message is shown.
     * Otherwise, the message is ensured to be hidden.
     */
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        var newFileName = ""
        if (binding.userInput.text != null) {
            newFileName = binding.userInput.text.toString().trim { it <= ' ' }
        }

        val errorMessage = checkFileName(newFileName, oCCapability, requireContext(), fileNames)

        if (isFileHidden(newFileName)) {
            binding.userInputContainer.error = getText(R.string.hidden_file_name_warning)
        } else if (errorMessage != null) {
            binding.userInputContainer.error = errorMessage
            positiveButton?.isEnabled = false
        } else if (binding.userInputContainer.error != null) {
            binding.userInputContainer.error = null
            // Called to remove extra padding
            binding.userInputContainer.isErrorEnabled = false
            positiveButton?.isEnabled = true
        }
    }

    override fun afterTextChanged(s: Editable) = Unit

    companion object {
        private const val ARG_TARGET_FILE = "TARGET_FILE"
        private const val ARG_PARENT_FOLDER = "PARENT_FOLDER"

        /**
         * Public factory method to create new RenameFileDialogFragment instances.
         *
         * @param file File to rename.
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(file: OCFile?, parentFolder: OCFile?): RenameFileDialogFragment {
            val bundle = Bundle().apply {
                putParcelable(ARG_TARGET_FILE, file)
                putParcelable(ARG_PARENT_FOLDER, parentFolder)
            }

            return RenameFileDialogFragment().apply {
                arguments = bundle
            }
        }
    }
}
