/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.collect.Sets
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.fileNameValidator.FileNameValidator
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.ChooseTemplateBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.Template
import com.owncloud.android.files.CreateFileFromTemplateOperation
import com.owncloud.android.files.FetchTemplateOperation
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.activity.RichDocumentsEditorWebView
import com.owncloud.android.ui.adapter.RichDocumentsTemplateAdapter
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog.Companion.newInstance
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.NextcloudServer
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Dialog to show templates for new documents/spreadsheets/presentations.
 */
class ChooseRichDocumentsTemplateDialogFragment :
    DialogFragment(),
    View.OnClickListener,
    RichDocumentsTemplateAdapter.ClickListener,
    Injectable {
    private var fileNames: MutableSet<String>? = null

    @Inject
    lateinit var currentAccount: CurrentAccountProvider

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    @Inject
    lateinit var keyboardUtils: KeyboardUtils

    private var adapter: RichDocumentsTemplateAdapter? = null
    private var parentFolder: OCFile? = null
    private var client: OwnCloudClient? = null
    private var positiveButton: MaterialButton? = null
    private var waitDialog: DialogFragment? = null

    enum class Type {
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION
    }

    private lateinit var binding: ChooseTemplateBinding

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog?

        alertDialog?.let {
            positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
            positiveButton?.let {
                viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
                it.setOnClickListener(this)
                it.isEnabled = false
            }

            val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton
            negativeButton?.let {
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton)
            }
        }

        checkEnablingCreateButton()
    }

    override fun onResume() {
        super.onResume()
        keyboardUtils.showKeyboardForEditText(requireDialog().window, binding.filename)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = requireActivity().layoutInflater
        binding = ChooseTemplateBinding.inflate(inflater, null, false)
    }

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = arguments ?: throw IllegalArgumentException("Arguments may not be null")
        val activity = activity ?: throw IllegalArgumentException("Activity may not be null")

        initClient()
        initFilenames(arguments)
        viewThemeUtils.material.colorTextInputLayout(binding.filenameContainer)

        val type = Type.valueOf(arguments.getString(ARG_TYPE) ?: "")
        FetchTemplateTask(this, client).execute(type)

        initList(type)
        addTextChangeListener()

        val titleTextId = getTitle(type)
        val builder = getDialogBuilder(activity, titleTextId)
        return builder.create()
    }

    @Suppress("DEPRECATION", "TooGenericExceptionThrown")
    private fun initClient() {
        try {
            client = clientFactory.create(currentAccount.user)
        } catch (e: CreationException) {
            throw RuntimeException(e)
        }
    }

    private fun initFilenames(arguments: Bundle) {
        parentFolder = arguments.getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)
        val folderContent = fileDataStorageManager.getFolderContent(parentFolder, false)
        fileNames = Sets.newHashSetWithExpectedSize(folderContent.size)

        for (file in folderContent) {
            fileNames?.add(file.fileName)
        }
    }

    private fun initList(type: Type) {
        binding.list.setHasFixedSize(true)
        binding.list.layoutManager = GridLayoutManager(activity, 2)
        adapter = RichDocumentsTemplateAdapter(
            type,
            this,
            context,
            viewThemeUtils
        )
        binding.list.adapter = adapter
    }

    private fun addTextChangeListener() {
        binding.filename.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable) {
                checkEnablingCreateButton()
            }
        })
    }

    private fun getDialogBuilder(activity: Activity, titleTextId: Int): MaterialAlertDialogBuilder {
        val builder = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.common_cancel, null)
            .setTitle(titleTextId)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(activity, builder)

        return builder
    }

    private fun getTitle(type: Type): Int {
        return when (type) {
            Type.DOCUMENT -> {
                R.string.create_new_document
            }

            Type.SPREADSHEET -> {
                R.string.create_new_spreadsheet
            }

            Type.PRESENTATION -> {
                R.string.create_new_presentation
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun createFromTemplate(template: Template, path: String) {
        waitDialog = newInstance(R.string.wait_a_moment, false).also {
            it.show(parentFragmentManager, WAIT_DIALOG_TAG)
        }
        CreateFileFromTemplateTask(this, client, template, path, currentAccount.user).execute()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setTemplateList(templateList: List<Template>?) {
        adapter?.let {
            it.setTemplateList(templateList)
            it.notifyDataSetChanged()
        }
    }

    private val fileNameText: String
        get() {
            var result = ""

            val text = binding.filename.text

            if (text != null) {
                result = text.toString()
            }

            return result
        }

    override fun onClick(v: View) {
        val name = fileNameText
        val path = parentFolder?.remotePath + name

        val selectedTemplate = adapter?.selectedTemplate

        val errorMessage = FileNameValidator.checkFileName(
            name,
            fileDataStorageManager.getCapability(currentAccount.user),
            requireContext(),
            fileNames
        )

        if (selectedTemplate == null) {
            DisplayUtils.showSnackMessage(binding.list, R.string.select_one_template)
        } else if (errorMessage != null) {
            DisplayUtils.showSnackMessage(requireActivity(), errorMessage)
        } else if (name.equals(DOT + selectedTemplate.extension, ignoreCase = true)) {
            DisplayUtils.showSnackMessage(binding.list, R.string.enter_filename)
        } else if (!name.endsWith(selectedTemplate.extension)) {
            createFromTemplate(selectedTemplate, path + DOT + selectedTemplate.extension)
        } else {
            createFromTemplate(selectedTemplate, path)
        }
    }

    override fun onClick(template: Template) {
        onTemplateChosen(template)
    }

    private fun onTemplateChosen(template: Template) {
        adapter?.setTemplateAsActive(template)
        prefillFilenameIfEmpty(template)
        checkEnablingCreateButton()
    }

    private fun prefillFilenameIfEmpty(template: Template) {
        val name = fileNameText

        if (name.isEmpty() || name.equals(DOT + template.extension, ignoreCase = true)) {
            binding.filename.setText(String.format("%s.%s", template.name, template.extension))
        }

        val dotIndex = fileNameText.lastIndexOf('.')
        if (dotIndex >= 0) {
            binding.filename.setSelection(dotIndex)
        }
    }

    private fun checkEnablingCreateButton() {
        if (positiveButton == null) {
            return
        }

        val selectedTemplate = adapter?.selectedTemplate
        val name = fileNameText
        val errorMessage = FileNameValidator.checkFileName(
            name,
            fileDataStorageManager.getCapability(currentAccount.user),
            requireContext(),
            fileNames
        )
        val isExtension = (
            selectedTemplate == null || !name.equals(
                DOT + selectedTemplate.extension,
                ignoreCase = true
            )
            )
        val isEnable = isExtension && errorMessage == null

        positiveButton?.let {
            it.isEnabled = isEnable
            it.isClickable = isEnable
        }

        binding.filenameContainer.run {
            isErrorEnabled = !isEnable
            error = if (!isEnable) {
                errorMessage ?: getText(R.string.filename_empty)
            } else {
                null
            }
        }
    }

    @Suppress("DEPRECATION")
    private class CreateFileFromTemplateTask(
        chooseTemplateDialogFragment: ChooseRichDocumentsTemplateDialogFragment?,
        private val client: OwnCloudClient?,
        private val template: Template,
        private val path: String,
        private val user: User
    ) : AsyncTask<Void?, Void?, String>() {
        private val chooseTemplateDialogFragmentWeakReference = WeakReference(chooseTemplateDialogFragment)
        private var file: OCFile? = null

        @Suppress("ReturnCount")
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void?): String {
            val result = CreateFileFromTemplateOperation(path, template.id).execute(client)

            if (!result.isSuccess) {
                return ""
            }

            // get file
            val newFileResult = ReadFileRemoteOperation(path).execute(client)

            if (!newFileResult.isSuccess) {
                return ""
            }

            val temp = FileStorageUtils.fillOCFile(newFileResult.data[0] as RemoteFile)

            if (chooseTemplateDialogFragmentWeakReference.get() == null) {
                return ""
            }

            val storageManager = FileDataStorageManager(
                user,
                chooseTemplateDialogFragmentWeakReference.get()!!.requireContext().contentResolver
            )
            storageManager.saveFile(temp)
            file = storageManager.getFileByPath(path)

            return result.data[0].toString()
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(url: String) {
            val fragment = chooseTemplateDialogFragmentWeakReference.get()

            if (fragment == null || !fragment.isAdded) {
                Log_OC.e(TAG, "Error creating file from template!")
                return
            }

            fragment.waitDialog?.dismiss()

            if (url.isEmpty()) {
                fragment.dismiss()
                DisplayUtils.showSnackMessage(
                    fragment.requireActivity(),
                    R.string.error_creating_file_from_template
                )

                return
            }

            val intent = Intent(MainApp.getAppContext(), RichDocumentsEditorWebView::class.java).apply {
                putExtra(ExternalSiteWebView.EXTRA_TITLE, "Collabora")
                putExtra(ExternalSiteWebView.EXTRA_URL, url)
                putExtra(ExternalSiteWebView.EXTRA_FILE, file)
                putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
                putExtra(ExternalSiteWebView.EXTRA_TEMPLATE, template)
            }

            fragment.run {
                startActivity(intent)
                dismiss()
            }
        }
    }

    @Suppress("DEPRECATION")
    private class FetchTemplateTask(
        chooseTemplateDialogFragment: ChooseRichDocumentsTemplateDialogFragment,
        private val client: OwnCloudClient?
    ) : AsyncTask<Type?, Void?, List<Template>>() {
        private val chooseTemplateDialogFragmentWeakReference = WeakReference(chooseTemplateDialogFragment)

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg type: Type?): List<Template> {
            val fetchTemplateOperation = FetchTemplateOperation(type[0])
            val result = fetchTemplateOperation.execute(client)

            if (!result.isSuccess) {
                return ArrayList()
            }

            val templateList: MutableList<Template> = ArrayList()
            for (`object` in result.data) {
                templateList.add(`object` as Template)
            }

            return templateList
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(templateList: List<Template>) {
            val fragment = chooseTemplateDialogFragmentWeakReference.get()
            if (fragment == null || !fragment.isAdded) {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!")
                return
            }

            fragment.run {
                if (templateList.isEmpty()) {
                    dismiss()
                    DisplayUtils.showSnackMessage(requireActivity(), R.string.error_retrieving_templates)
                    return
                }

                if (templateList.size == SINGLE_TEMPLATE) {
                    onTemplateChosen(templateList[0])
                    binding.list.visibility = View.GONE
                } else {
                    val name = DOT + templateList[0].extension
                    binding.filename.setText(name)
                    binding.helperText.visibility = View.VISIBLE
                }

                setTemplateList(templateList)
            }
        }
    }

    companion object {
        private const val ARG_PARENT_FOLDER = "PARENT_FOLDER"
        private const val ARG_TYPE = "TYPE"
        private val TAG: String = ChooseRichDocumentsTemplateDialogFragment::class.java.simpleName
        private const val DOT = "."
        const val SINGLE_TEMPLATE: Int = 1
        private const val WAIT_DIALOG_TAG = "WAIT"

        @JvmStatic
        @NextcloudServer(max = 18) // will be removed in favor of generic direct editing
        fun newInstance(parentFolder: OCFile?, type: Type): ChooseRichDocumentsTemplateDialogFragment {
            val bundle = Bundle().apply {
                putParcelable(ARG_PARENT_FOLDER, parentFolder)
                putString(ARG_TYPE, type.name)
            }

            return ChooseRichDocumentsTemplateDialogFragment().apply {
                arguments = bundle
            }
        }
    }
}
