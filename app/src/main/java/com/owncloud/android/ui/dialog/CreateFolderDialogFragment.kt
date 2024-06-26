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
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.collect.Sets
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.EditBoxDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

/**
 * Dialog to input the name for a new folder to create.
 *
 *
 * Triggers the folder creation when name is confirmed.
 */
class CreateFolderDialogFragment : DialogFragment(), DialogInterface.OnClickListener, Injectable {
    @JvmField
    @Inject
    var fileDataStorageManager: FileDataStorageManager? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    @JvmField
    @Inject
    var keyboardUtils: KeyboardUtils? = null
    private var mParentFolder: OCFile? = null
    private var positiveButton: MaterialButton? = null

    private lateinit var binding: EditBoxDialogBinding

    override fun onStart() {
        super.onStart()
        bindButton()
    }

    private fun bindButton() {
        val dialog = dialog

        if (dialog is AlertDialog) {
            positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton

            viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(positiveButton!!)
            viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(negativeButton)
        }
    }

    override fun onResume() {
        super.onResume()
        bindButton()
        keyboardUtils!!.showKeyboardForEditText(requireDialog().window, binding.userInput)
    }

    @Suppress("EmptyFunctionBlock")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mParentFolder = arguments?.getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)

        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        binding = EditBoxDialogBinding.inflate(inflater, null, false)
        val view: View = binding.root

        // Setup layout
        binding.userInput.setText(R.string.empty)
        viewThemeUtils?.material?.colorTextInputLayout(binding.userInputContainer)

        val parentFolder = requireArguments().getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)

        val folderContent = fileDataStorageManager!!.getFolderContent(parentFolder, false)
        val fileNames: MutableSet<String> = Sets.newHashSetWithExpectedSize(folderContent.size)
        for (file in folderContent) {
            fileNames.add(file.fileName)
        }

        // Add TextChangedListener to handle showing/hiding the input warning message
        binding.userInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            /**
             * When user enters a hidden file name, the 'hidden file' message is shown. Otherwise,
             * the message is ensured to be hidden.
             */
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                var newFileName = ""
                if (binding.userInput.text != null) {
                    newFileName = binding.userInput.text.toString().trim { it <= ' ' }
                }
                if (!TextUtils.isEmpty(newFileName) && newFileName[0] == '.') {
                    binding.userInputContainer.error = getText(R.string.hidden_file_name_warning)
                } else if (TextUtils.isEmpty(newFileName)) {
                    binding.userInputContainer.error = getString(R.string.filename_empty)
                    if (positiveButton == null) {
                        bindButton()
                    }
                    positiveButton!!.isEnabled = false
                } else if (!FileUtils.isValidName(newFileName)) {
                    binding.userInputContainer.error = getString(R.string.filename_forbidden_charaters_from_server)
                    positiveButton!!.isEnabled = false
                } else if (fileNames.contains(newFileName)) {
                    binding.userInputContainer.error = getText(R.string.file_already_exists)
                    positiveButton!!.isEnabled = false
                } else if (binding.userInputContainer.error != null) {
                    binding.userInputContainer.error = null
                    // Called to remove extra padding
                    binding.userInputContainer.isErrorEnabled = false
                    positiveButton!!.isEnabled = true
                }
            }
        })

        // Build the dialog
        val builder = buildMaterialAlertDialog(view)
        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(binding.userInputContainer.context, builder)
        return builder.create()
    }

    private fun buildMaterialAlertDialog(view: View): MaterialAlertDialogBuilder {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder
            .setView(view)
            .setPositiveButton(R.string.folder_confirm_create, this)
            .setNegativeButton(R.string.common_cancel, this)
            .setTitle(R.string.uploader_info_dirname)
        return builder
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            val newFolderName = (getDialog()!!.findViewById<View>(R.id.user_input) as TextView)
                .text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(newFolderName)) {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_empty)
                return
            }
            if (!FileUtils.isValidName(newFolderName)) {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.filename_forbidden_charaters_from_server)
                return
            }
            val path = mParentFolder!!.decryptedRemotePath + newFolderName + OCFile.PATH_SEPARATOR
            (requireActivity() as ComponentsGetter).fileOperationsHelper.createFolder(path)
        }
    }

    companion object {
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
            val frag = CreateFolderDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_PARENT_FOLDER, parentFolder)
            frag.arguments = args
            return frag
        }
    }
}
