/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro.brey@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later AND AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.annotation.SuppressLint
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
import com.nextcloud.android.lib.resources.directediting.DirectEditingCreateFileRemoteOperation
import com.nextcloud.android.lib.resources.directediting.DirectEditingObtainListOfTemplatesRemoteOperation
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
import com.owncloud.android.lib.common.Creator
import com.owncloud.android.lib.common.Template
import com.owncloud.android.lib.common.TemplateList
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.activity.TextEditorWebView
import com.owncloud.android.ui.adapter.TemplateAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.KeyboardUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Dialog to show templates for new documents/spreadsheets/presentations.
 */
class ChooseTemplateDialogFragment :
    DialogFragment(),
    View.OnClickListener,
    TemplateAdapter.ClickListener,
    Injectable {

    private lateinit var fileNames: MutableSet<String>

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var currentAccount: CurrentAccountProvider

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var keyboardUtils: KeyboardUtils

    private var adapter: TemplateAdapter? = null
    private var parentFolder: OCFile? = null
    private var title: String? = null
    private var positiveButton: MaterialButton? = null
    private var creator: Creator? = null

    private var _binding: ChooseTemplateBinding? = null
    val binding get() = _binding!!

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as AlertDialog

        val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton
        negativeButton?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton)
        }

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as? MaterialButton
        positiveButton?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton)
            positiveButton.setOnClickListener(this)
            positiveButton.isEnabled = false
            positiveButton.isClickable = false
            this.positiveButton = positiveButton
        }

        checkFileNameAfterEachType()
    }

    override fun onResume() {
        super.onResume()
        keyboardUtils.showKeyboardForEditText(dialog?.window, binding.filename)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = arguments ?: throw IllegalArgumentException("Arguments may not be null")
        val activity = activity ?: throw IllegalArgumentException("Activity may not be null")

        parentFolder = arguments.getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)
        creator = arguments.getParcelableArgument(ARG_CREATOR, Creator::class.java)

        title = arguments.getString(ARG_HEADLINE, getString(R.string.select_template))
        title = when (savedInstanceState) {
            null -> arguments.getString(ARG_HEADLINE)
            else -> savedInstanceState.getString(ARG_HEADLINE)
        }

        fileNames = fileDataStorageManager.getFolderContent(parentFolder, false).map { it.fileName }.toMutableSet()

        val inflater = requireActivity().layoutInflater
        _binding = ChooseTemplateBinding.inflate(inflater, null, false)

        viewThemeUtils.material.colorTextInputLayout(
            binding.filenameContainer
        )

        binding.filename.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable) {
                checkFileNameAfterEachType()
            }
        })

        fetchTemplate()

        binding.list.setHasFixedSize(true)
        binding.list.layoutManager = GridLayoutManager(activity, 2)
        adapter = TemplateAdapter(
            creator?.mimetype,
            this,
            context,
            currentAccount,
            clientFactory,
            viewThemeUtils
        )
        binding.list.adapter = adapter

        val builder = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.common_cancel, null)
            .setTitle(title)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.list.context, builder)

        return builder.create()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchTemplate() {
        try {
            val user = currentAccount.user
            FetchTemplateTask(this, clientFactory, user, creator).execute()
        } catch (e: Exception) {
            Log_OC.e(TAG, "Loading stream url not possible: $e")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString(ARG_HEADLINE, title)
    }

    private fun createFromTemplate(template: Template, path: String) {
        CreateFileFromTemplateTask(this, clientFactory, currentAccount.user, template, path, creator).execute()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setTemplateList(templateList: TemplateList?) {
        adapter?.setTemplateList(templateList)
        adapter?.notifyDataSetChanged()
    }

    override fun onClick(template: Template) {
        onTemplateChosen(template)
    }

    private fun onTemplateChosen(template: Template) {
        adapter?.setTemplateAsActive(template)
        prefillFilenameIfEmpty(template)
        checkFileNameAfterEachType()
    }

    private fun prefillFilenameIfEmpty(template: Template) {
        var name = binding.filename.text.toString()
        if (name.isEmpty() || name.equals(DOT + template.extension, ignoreCase = true)) {
            binding.filename.setText(String.format("%s.%s", template.title, template.extension))
            name = binding.filename.text.toString()
            val dotPos = name.lastIndexOf('.')
            binding.filename.setSelection(if (dotPos != -1) dotPos else name.length)
        }
    }

    private fun getOCCapability(): OCCapability = fileDataStorageManager.getCapability(currentAccount.user.accountName)

    override fun onClick(v: View) {
        val name = binding.filename.text.toString()
        val path = parentFolder?.remotePath + name
        val selectedTemplate = adapter?.selectedTemplate

        val errorMessage = FileNameValidator.checkFileName(name, getOCCapability(), requireContext())

        when {
            selectedTemplate == null -> {
                DisplayUtils.showSnackMessage(binding.list, R.string.select_one_template)
            }
            errorMessage != null -> {
                DisplayUtils.showSnackMessage(requireActivity(), errorMessage)
            }
            name.equals(DOT + selectedTemplate.extension, ignoreCase = true) -> {
                DisplayUtils.showSnackMessage(binding.list, R.string.enter_filename)
            }
            else -> {
                val fullPath = if (!name.endsWith(selectedTemplate.extension)) {
                    path + DOT + selectedTemplate.extension
                } else {
                    path
                }
                createFromTemplate(selectedTemplate, fullPath)
            }
        }
    }

    private fun checkFileNameAfterEachType() {
        if (positiveButton == null) return

        val selectedTemplate = adapter?.selectedTemplate
        val name = binding.filename.text.toString().trim()
        val isNameJustExtension = selectedTemplate != null &&
            name.equals(
                DOT + selectedTemplate.extension,
                ignoreCase = true
            )
        val fileNameValidatorResult =
            FileNameValidator.checkFileName(name, getOCCapability(), requireContext(), fileNames)

        val errorMessage = when {
            isNameJustExtension -> null
            fileNameValidatorResult != null -> fileNameValidatorResult
            else -> null
        }

        val isNameValid = (errorMessage == null) && !name.equals(DOT + selectedTemplate?.extension, ignoreCase = true)
        val isHiddenFileName = FileNameValidator.isFileHidden(name)

        binding.filenameContainer.isErrorEnabled = !isNameValid || isHiddenFileName
        binding.filenameContainer.error = when {
            !isNameValid -> errorMessage ?: getString(R.string.enter_filename)
            isHiddenFileName -> getText(R.string.hidden_file_name_warning)
            else -> null
        }

        positiveButton?.apply {
            isEnabled = isNameValid && !isHiddenFileName
            isClickable = isEnabled
        }
    }

    @Suppress("LongParameterList", "DEPRECATION")
    private class CreateFileFromTemplateTask(
        chooseTemplateDialogFragment: ChooseTemplateDialogFragment,
        private val clientFactory: ClientFactory?,
        private val user: User,
        private val template: Template,
        private val path: String,
        private val creator: Creator?
    ) : AsyncTask<Void, Void, String>() {
        private val chooseTemplateDialogFragmentWeakReference: WeakReference<ChooseTemplateDialogFragment> =
            WeakReference(chooseTemplateDialogFragment)
        private var file: OCFile? = null

        @Deprecated("Deprecated in Java")
        @Suppress("ReturnCount") // legacy code
        override fun doInBackground(vararg params: Void): String {
            return try {
                val client = clientFactory?.create(user) ?: return ""
                val nextcloudClient = clientFactory.createNextcloudClient(user)
                val result = DirectEditingCreateFileRemoteOperation(
                    path,
                    creator?.editor,
                    creator?.id,
                    template.title
                ).execute(nextcloudClient)
                if (!result.isSuccess) {
                    return ""
                }
                val newFileResult = ReadFileRemoteOperation(path).execute(client)
                if (!newFileResult.isSuccess) {
                    return ""
                }
                val fragment = chooseTemplateDialogFragmentWeakReference.get() ?: return ""
                val context = fragment.context
                    ?: // fragment has been detached
                    return ""
                val storageManager = FileDataStorageManager(
                    user,
                    context.contentResolver
                )
                val temp = FileStorageUtils.fillOCFile(newFileResult.data[0] as RemoteFile)
                storageManager.saveFile(temp)
                file = storageManager.getFileByPath(path)
                result.resultData
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Error creating file from template!", e)
                ""
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(url: String) {
            val fragment = chooseTemplateDialogFragmentWeakReference.get()
            if (fragment == null || !fragment.isAdded) {
                Log_OC.e(TAG, "Error creating file from template!")
                return
            }

            if (url.isEmpty()) {
                DisplayUtils.showSnackMessage(fragment.binding.list, R.string.error_creating_file_from_template)
                return
            }

            val editorWebView = Intent(MainApp.getAppContext(), TextEditorWebView::class.java).apply {
                putExtra(ExternalSiteWebView.EXTRA_TITLE, "Text")
                putExtra(ExternalSiteWebView.EXTRA_URL, url)
                putExtra(ExternalSiteWebView.EXTRA_FILE, file)
                putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
            }

            fragment.run {
                startActivity(editorWebView)
                dismiss()
            }
        }
    }

    @Suppress("DEPRECATION")
    private class FetchTemplateTask(
        chooseTemplateDialogFragment: ChooseTemplateDialogFragment,
        private val clientFactory: ClientFactory?,
        private val user: User,
        private val creator: Creator?
    ) : AsyncTask<Void, Void, TemplateList>() {
        private val chooseTemplateDialogFragmentWeakReference: WeakReference<ChooseTemplateDialogFragment> =
            WeakReference(chooseTemplateDialogFragment)

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void): TemplateList {
            return try {
                val client = clientFactory?.createNextcloudClient(user) ?: return TemplateList()
                val result = DirectEditingObtainListOfTemplatesRemoteOperation(
                    creator?.editor,
                    creator?.id
                ).execute(client)
                if (!result.isSuccess) {
                    TemplateList()
                } else {
                    result.resultData
                }
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Could not fetch template", e)
                TemplateList()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(templateList: TemplateList) {
            val fragment = chooseTemplateDialogFragmentWeakReference.get()
            if (fragment == null || !fragment.isAdded) {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!")
                return
            }

            if (templateList.templates.isEmpty()) {
                DisplayUtils.showSnackMessage(fragment.binding.list, R.string.error_retrieving_templates)
                return
            }

            fragment.run {
                if (templateList.templates.size == SINGLE_TEMPLATE) {
                    onTemplateChosen(templateList.templates.values.iterator().next())
                    binding.list.visibility = View.GONE
                } else {
                    val name = DOT + templateList.templates.values.iterator().next().extension
                    binding.filename.setText(name)
                    binding.helperText.visibility = View.VISIBLE
                }

                setTemplateList(templateList)
            }
        }
    }

    companion object {
        private const val ARG_PARENT_FOLDER = "PARENT_FOLDER"
        private const val ARG_CREATOR = "CREATOR"
        private const val ARG_HEADLINE = "HEADLINE"
        private val TAG = ChooseTemplateDialogFragment::class.java.simpleName
        private const val DOT = "."
        const val SINGLE_TEMPLATE = 1

        @JvmStatic
        fun newInstance(parentFolder: OCFile?, creator: Creator?, headline: String?): ChooseTemplateDialogFragment {
            val bundle = Bundle().apply {
                putParcelable(ARG_PARENT_FOLDER, parentFolder)
                putParcelable(ARG_CREATOR, creator)
                putString(ARG_HEADLINE, headline)
            }

            return ChooseTemplateDialogFragment().apply {
                arguments = bundle
            }
        }
    }
}
