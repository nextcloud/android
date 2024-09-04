/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.collect.Sets
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.fileNameValidator.FileNameValidator
import com.owncloud.android.R
import com.owncloud.android.databinding.EditBoxDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dialog to input the name for a new folder to create.
 *
 *
 * Triggers the folder creation when name is confirmed.
 */
class CreateFolderDialogFragment : DialogFragment(), DialogInterface.OnClickListener, Injectable {

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var keyboardUtils: KeyboardUtils

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var accountProvider: CurrentAccountProvider

    private var parentFolder: OCFile? = null
    private var positiveButton: MaterialButton? = null

    private lateinit var binding: EditBoxDialogBinding

    override fun onStart() {
        super.onStart()
        bindButton()
    }

    private fun bindButton() {
        val dialog = dialog

        if (dialog is AlertDialog) {
            positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
            positiveButton?.let {
                it.isEnabled = false
                viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
            }

            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton
            negativeButton?.let {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bindButton()
        keyboardUtils.showKeyboardForEditText(requireDialog().window, binding.userInput)
    }

    @Suppress("EmptyFunctionBlock")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        parentFolder = arguments?.getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)

        val inflater = requireActivity().layoutInflater
        binding = EditBoxDialogBinding.inflate(inflater, null, false)

        binding.userInput.setText(R.string.empty)
        viewThemeUtils.material.colorTextInputLayout(binding.userInputContainer)

        val parentFolder = requireArguments().getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)

        val folderContent = fileDataStorageManager.getFolderContent(parentFolder, false)
        val fileNames: MutableSet<String> = Sets.newHashSetWithExpectedSize(folderContent.size)
        for (file in folderContent) {
            fileNames.add(file.fileName)
        }

        binding.userInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                checkFileNameAfterEachType(fileNames)
            }
        })

        val builder = buildMaterialAlertDialog(binding.root)
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.userInputContainer.context, builder)
        return builder.create()
    }

    private fun getOCCapability(): OCCapability = fileDataStorageManager.getCapability(accountProvider.user.accountName)

    private fun checkFileNameAfterEachType(fileNames: MutableSet<String>) {
        val newFileName = binding.userInput.text?.toString()?.trim() ?: ""

        val fileNameValidatorResult: String? =
            FileNameValidator.checkFileName(newFileName, getOCCapability(), requireContext(), fileNames)

        val errorMessage = when {
            newFileName.isEmpty() -> getString(R.string.folder_name_empty)
            fileNameValidatorResult != null -> fileNameValidatorResult
            else -> null
        }

        if (errorMessage != null) {
            binding.userInputContainer.error = errorMessage
            positiveButton?.isEnabled = false
            if (positiveButton == null) {
                bindButton()
            }
        } else if (FileNameValidator.isFileHidden(newFileName)) {
            binding.userInputContainer.error = requireContext().getString(R.string.hidden_file_name_warning)
            binding.userInputContainer.isErrorEnabled = true
            positiveButton?.isEnabled = true
        } else {
            binding.userInputContainer.error = null
            binding.userInputContainer.isErrorEnabled = false
            positiveButton?.isEnabled = true
        }
    }

    private fun buildMaterialAlertDialog(view: View): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(requireActivity())
            .setView(view)
            .setPositiveButton(R.string.folder_confirm_create, this)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(R.string.uploader_info_dirname)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            val newFolderName = (getDialog()?.findViewById<View>(R.id.user_input) as TextView)
                .text.toString().trim { it <= ' ' }

            val errorMessage: String? =
                FileNameValidator.checkFileName(newFolderName, getOCCapability(), requireContext())

            if (errorMessage != null) {
                DisplayUtils.showSnackMessage(requireActivity(), errorMessage)
                return
            }

            val path = parentFolder?.decryptedRemotePath + newFolderName + OCFile.PATH_SEPARATOR
            lifecycleScope.launch(Dispatchers.IO) {
                if (connectivityService.isNetworkAndServerAvailable()) {
                    (requireActivity() as ComponentsGetter).fileOperationsHelper.createFolder(path)
                } else {
                    Log_OC.d(TAG, "Network not available, creating offline operation")
                    fileDataStorageManager.addCreateFolderOfflineOperation(
                        path,
                        newFolderName,
                        parentFolder?.offlineOperationParentPath,
                        parentFolder?.fileId
                    )

                    launch(Dispatchers.Main) {
                        val fileDisplayActivity = requireActivity() as? FileDisplayActivity
                        fileDisplayActivity?.syncAndUpdateFolder(true)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CreateFolderDialogFragment"
        private const val ARG_PARENT_FOLDER = "PARENT_FOLDER"
        const val CREATE_FOLDER_FRAGMENT = "CREATE_FOLDER_FRAGMENT"

        /**
         * Public factory method to create new CreateFolderDialogFragment instances.
         *
         * @param parentFolder Folder to create
         * @return Dialog ready to show.
         */
        @JvmStatic
        fun newInstance(parentFolder: OCFile?): CreateFolderDialogFragment {
            val bundle = Bundle().apply {
                putParcelable(ARG_PARENT_FOLDER, parentFolder)
            }

            return CreateFolderDialogFragment().apply {
                arguments = bundle
            }
        }
    }
}
