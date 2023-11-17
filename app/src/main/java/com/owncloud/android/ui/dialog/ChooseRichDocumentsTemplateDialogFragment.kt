/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 * Copyright (C) 2023 TSI-mc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
import com.google.common.collect.Sets
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.utils.extensions.getParcelableArgument
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
import java.util.Objects
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

    @JvmField
    @Inject
    var currentAccount: CurrentAccountProvider? = null

    @JvmField
    @Inject
    var clientFactory: ClientFactory? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    @JvmField
    @Inject
    var fileDataStorageManager: FileDataStorageManager? = null

    @JvmField
    @Inject
    var keyboardUtils: KeyboardUtils? = null

    private var adapter: RichDocumentsTemplateAdapter? = null
    private var parentFolder: OCFile? = null
    private var client: OwnCloudClient? = null
    private var positiveButton: MaterialButton? = null
    private var waitDialog: DialogFragment? = null

    enum class Type {
        DOCUMENT, SPREADSHEET, PRESENTATION
    }

    var binding: ChooseTemplateBinding? = null

    override fun onStart() {
        super.onStart()
        setupAlertDialogButtons()
        checkEnablingCreateButton()
    }

    private fun setupAlertDialogButtons() {
        (dialog as AlertDialog?)?.run {
            positiveButton = getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton
            positiveButton?.let {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(it)
            }
            positiveButton?.setOnClickListener(this@ChooseRichDocumentsTemplateDialogFragment)
            positiveButton?.isEnabled = false

            val negativeButton = getButton(AlertDialog.BUTTON_NEGATIVE) as? MaterialButton
            negativeButton?.let {
                viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding?.let {
            keyboardUtils?.showKeyboardForEditText(requireDialog().window, it.filename)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bundle = arguments ?: throw IllegalArgumentException("Arguments may not be null")
        val activity = activity ?: throw IllegalArgumentException("Activity may not be null")

        setupClient()
        setupParentFolder(bundle)

        val inflater = requireActivity().layoutInflater
        binding = ChooseTemplateBinding.inflate(inflater, null, false)

        viewThemeUtils?.material?.colorTextInputLayout(binding!!.filenameContainer)

        val type = bundle.getString(ARG_TYPE)?.let { Type.valueOf(it) }
        setupList(type)

        setupFileName()

        // Build the dialog
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setView(view)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.common_cancel, null)

        type?.let {
            builder.setTitle(getTitle(it))
        }

        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(activity, builder)
        return builder.create()
    }

    @Suppress("TooGenericExceptionThrown")
    private fun setupClient() {
        client = try {
            clientFactory?.create(currentAccount?.user)
        } catch (e: CreationException) {
            // we'll NPE without the client
            throw RuntimeException(e)
        }
    }

    private fun setupParentFolder(bundle: Bundle?) {
        parentFolder = bundle?.getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)
        val folderContent = fileDataStorageManager?.getFolderContent(parentFolder, false)
        fileNames = folderContent?.size?.let { Sets.newHashSetWithExpectedSize(it) }
        if (folderContent != null) {
            for (file in folderContent) {
                fileNames?.add(file.fileName)
            }
        }
    }

    private fun setupList(type: Type?) {
        FetchTemplateTask(this, client).execute(type)
        binding?.list?.setHasFixedSize(true)
        binding?.list?.layoutManager = GridLayoutManager(activity, 2)
        adapter = RichDocumentsTemplateAdapter(
            type,
            this,
            context,
            currentAccount,
            clientFactory,
            viewThemeUtils
        )
        binding?.list?.adapter = adapter
    }

    @Suppress("EmptyFunctionBlock")
    private fun setupFileName() {
        binding?.filename?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                checkEnablingCreateButton()
            }
        })
    }

    @Suppress("ReturnCount")
    private fun getTitle(type: Type): Int {
        when (type) {
            Type.DOCUMENT -> {
                return R.string.create_new_document
            }
            Type.SPREADSHEET -> {
                return R.string.create_new_spreadsheet
            }
            Type.PRESENTATION -> {
                return R.string.create_new_presentation
            }
            else -> return R.string.select_template
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun createFromTemplate(template: Template, path: String) {
        waitDialog = newInstance(R.string.wait_a_moment, false)
        waitDialog?.show(parentFragmentManager, WAIT_DIALOG_TAG)
        CreateFileFromTemplateTask(this, client, template, path, currentAccount!!.user).execute()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setTemplateList(templateList: List<Template>?) {
        adapter?.setTemplateList(templateList)
        adapter?.notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        val name = binding?.filename?.text.toString()
        val path = parentFolder?.remotePath + name
        val selectedTemplate = adapter?.selectedTemplate

        if (selectedTemplate == null) {
            DisplayUtils.showSnackMessage(binding?.list, R.string.select_one_template)
        } else if (name.isEmpty() || name.equals(DOT + selectedTemplate.extension, ignoreCase = true)) {
            DisplayUtils.showSnackMessage(binding?.list, R.string.enter_filename)
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
        val name = binding?.filename?.text.toString()
        if (name.isEmpty() || name.equals(DOT + template.extension, ignoreCase = true)) {
            binding?.filename?.setText(String.format("%s.%s", template.name, template.extension))
        }
        val dotIndex = binding?.filename?.text.toString().lastIndexOf('.')
        if (dotIndex >= 0) {
            binding?.filename?.setSelection(dotIndex)
        }
    }

    private fun checkEnablingCreateButton() {
        if (positiveButton != null) {
            val selectedTemplate = adapter!!.selectedTemplate
            val name = Objects.requireNonNull(binding?.filename?.text).toString()
            val isNameJustExtension = selectedTemplate != null && name.equals(
                DOT + selectedTemplate.extension, ignoreCase = true
            )
            val isNameEmpty = name.isEmpty() || isNameJustExtension
            val state = selectedTemplate != null && !isNameEmpty && !fileNames!!.contains(name)
            positiveButton?.isEnabled = selectedTemplate != null && name.isNotEmpty() && !name.equals(
                DOT + selectedTemplate.extension,
                ignoreCase = true
            )
            positiveButton?.isEnabled = state
            positiveButton?.isClickable = state
            binding?.filenameContainer?.isErrorEnabled = !state
            if (!state) {
                if (isNameEmpty) {
                    binding?.filenameContainer?.error = getText(R.string.filename_empty)
                } else {
                    binding?.filenameContainer?.error = getText(R.string.file_already_exists)
                }
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
        private val chooseTemplateDialogFragmentWeakReference: WeakReference<ChooseRichDocumentsTemplateDialogFragment?>
        private var file: OCFile? = null

        init {
            chooseTemplateDialogFragmentWeakReference = WeakReference(chooseTemplateDialogFragment)
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void?): String {
            val result = CreateFileFromTemplateOperation(path, template.id).execute(client)
            return if (result.isSuccess) {
                // get file
                val newFileResult = ReadFileRemoteOperation(path).execute(client)
                if (newFileResult.isSuccess) {
                    val temp = FileStorageUtils.fillOCFile(newFileResult.data[0] as RemoteFile)
                    if (chooseTemplateDialogFragmentWeakReference.get() != null) {
                        val storageManager = FileDataStorageManager(
                            user,
                            chooseTemplateDialogFragmentWeakReference.get()!!.requireContext().contentResolver
                        )
                        storageManager.saveFile(temp)
                        file = storageManager.getFileByPath(path)
                        result.data[0].toString()
                    } else {
                        ""
                    }
                } else {
                    ""
                }
            } else {
                ""
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(url: String) {
            val fragment = chooseTemplateDialogFragmentWeakReference.get()
            if (fragment != null && fragment.isAdded) {
                if (fragment.waitDialog != null) {
                    fragment.waitDialog!!.dismiss()
                }
                if (url.isEmpty()) {
                    fragment.dismiss()
                    DisplayUtils.showSnackMessage(
                        fragment.requireActivity(),
                        R.string.error_creating_file_from_template
                    )
                } else {
                    val collaboraWebViewIntent = Intent(MainApp.getAppContext(), RichDocumentsEditorWebView::class.java)
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, "Collabora")
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_URL, url)
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_FILE, file)
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
                    collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TEMPLATE, template)
                    fragment.startActivity(collaboraWebViewIntent)
                    fragment.dismiss()
                }
            } else {
                Log_OC.e(TAG, "Error creating file from template!")
            }
        }
    }

    @Suppress("DEPRECATION")
    private class FetchTemplateTask(
        chooseTemplateDialogFragment: ChooseRichDocumentsTemplateDialogFragment,
        private val client: OwnCloudClient?
    ) : AsyncTask<Type?, Void?, List<Template>>() {
        private val chooseTemplateDialogFragmentWeakReference: WeakReference<ChooseRichDocumentsTemplateDialogFragment>

        init {
            chooseTemplateDialogFragmentWeakReference = WeakReference(chooseTemplateDialogFragment)
        }

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
            if (fragment != null) {
                if (templateList.isEmpty()) {
                    fragment.dismiss()
                    DisplayUtils.showSnackMessage(fragment.requireActivity(), R.string.error_retrieving_templates)
                } else {
                    if (templateList.size == SINGLE_TEMPLATE) {
                        fragment.onTemplateChosen(templateList[0])
                        fragment.binding?.list?.visibility = View.GONE
                    } else {
                        val name = DOT + templateList[0].extension
                        fragment.binding?.filename?.setText(name)
                        fragment.binding?.helperText?.visibility = View.VISIBLE
                    }
                    fragment.setTemplateList(templateList)
                }
            } else {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!")
            }
        }
    }

    companion object {
        private const val ARG_PARENT_FOLDER = "PARENT_FOLDER"
        private const val ARG_TYPE = "TYPE"
        private val TAG = ChooseRichDocumentsTemplateDialogFragment::class.java.simpleName
        private const val DOT = "."
        const val SINGLE_TEMPLATE = 1
        private const val WAIT_DIALOG_TAG = "WAIT"

        @JvmStatic
        @NextcloudServer(max = 18) // will be removed in favor of generic direct editing
        fun newInstance(parentFolder: OCFile?, type: Type): ChooseRichDocumentsTemplateDialogFragment {
            val frag = ChooseRichDocumentsTemplateDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_PARENT_FOLDER, parentFolder)
            args.putString(ARG_TYPE, type.name)
            frag.arguments = args
            return frag
        }
    }
}
