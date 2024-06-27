/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro.brey@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: GPL-3.0-or-later AND AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

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
class ChooseTemplateDialogFragment : DialogFragment(), View.OnClickListener, TemplateAdapter.ClickListener, Injectable {

    private lateinit var fileNames: List<String>

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

    enum class Type {
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION
    }

    private var _binding: ChooseTemplateBinding? = null
    val binding get() = _binding!!

    override fun onStart() {
        super.onStart()
        val alertDialog = dialog as AlertDialog

        val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton
        viewThemeUtils.material.colorMaterialButtonPrimaryTonal(positiveButton)

        val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton
        viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton)

        positiveButton.setOnClickListener(this)
        positiveButton.isEnabled = false
        positiveButton.isClickable = false

        this.positiveButton = positiveButton
        checkEnablingCreateButton()
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

        fileNames = fileDataStorageManager.getFolderContent(parentFolder, false).map { it.fileName }

        // Inflate the layout for the dialog
        val inflater = requireActivity().layoutInflater
        _binding = ChooseTemplateBinding.inflate(inflater, null, false)
        val view: View = binding.root

        viewThemeUtils.material.colorTextInputLayout(
            binding.filenameContainer
        )

        binding.filename.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // not needed
            }

            override fun afterTextChanged(s: Editable) {
                checkEnablingCreateButton()
            }
        })

        fetchTemplate()

        binding.list.setHasFixedSize(true)
        binding.list.layoutManager = GridLayoutManager(activity, 2)
        adapter = TemplateAdapter(
            creator!!.mimetype,
            this,
            context,
            currentAccount,
            clientFactory,
            viewThemeUtils
        )
        binding.list.adapter = adapter

        // Build the dialog
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setView(view)
            .setPositiveButton(R.string.create, null)
            .setNegativeButton(R.string.common_cancel, null)
            .setTitle(title)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.list.context, builder)

        return builder.create()
    }

    @Suppress("TooGenericExceptionCaught") // legacy code
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

    fun setTemplateList(templateList: TemplateList?) {
        adapter?.setTemplateList(templateList)
        adapter?.notifyDataSetChanged()
    }

    override fun onClick(template: Template) {
        onTemplateChosen(template)
    }

    private fun onTemplateChosen(template: Template) {
        adapter!!.setTemplateAsActive(template)
        prefillFilenameIfEmpty(template)
        checkEnablingCreateButton()
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

    override fun onClick(v: View) {
        val name = binding.filename.text.toString()
        val path = parentFolder!!.remotePath + name

        val selectedTemplate = adapter!!.selectedTemplate

        if (selectedTemplate == null) {
            DisplayUtils.showSnackMessage(binding.list, R.string.select_one_template)
        } else if (name.isEmpty() || name.equals(DOT + selectedTemplate.extension, ignoreCase = true)) {
            DisplayUtils.showSnackMessage(binding.list, R.string.enter_filename)
        } else if (!name.endsWith(selectedTemplate.extension)) {
            createFromTemplate(selectedTemplate, path + DOT + selectedTemplate.extension)
        } else {
            createFromTemplate(selectedTemplate, path)
        }
    }

    private fun checkEnablingCreateButton() {
        if (positiveButton != null) {
            val selectedTemplate = adapter!!.selectedTemplate
            val name = binding.filename.text.toString().trim()
            val isNameJustExtension = selectedTemplate != null && name.equals(
                DOT + selectedTemplate.extension,
                ignoreCase = true
            )
            val isNameEmpty = name.isEmpty() || isNameJustExtension
            val state = selectedTemplate != null && !isNameEmpty && !fileNames.contains(name)

            positiveButton?.isEnabled = state
            positiveButton?.isClickable = state
            binding.filenameContainer.isErrorEnabled = !state

            if (!state) {
                if (isNameEmpty) {
                    binding.filenameContainer.error = getText(R.string.filename_empty)
                } else {
                    binding.filenameContainer.error = getText(R.string.file_already_exists)
                }
            }
        }
    }

    @Suppress("LongParameterList") // legacy code
    private class CreateFileFromTemplateTask(
        chooseTemplateDialogFragment: ChooseTemplateDialogFragment,
        private val clientFactory: ClientFactory?,
        user: User,
        template: Template,
        path: String,
        creator: Creator?
    ) : AsyncTask<Void, Void, String>() {
        private val chooseTemplateDialogFragmentWeakReference: WeakReference<ChooseTemplateDialogFragment>
        private val template: Template
        private val path: String
        private val creator: Creator?
        private val user: User
        private var file: OCFile? = null

        @Suppress("ReturnCount") // legacy code
        override fun doInBackground(vararg params: Void): String {
            return try {
                val client = clientFactory!!.create(user)
                val nextcloudClient = clientFactory.createNextcloudClient(user)
                val result = DirectEditingCreateFileRemoteOperation(
                    path,
                    creator!!.editor,
                    creator.id,
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

        override fun onPostExecute(url: String) {
            val fragment = chooseTemplateDialogFragmentWeakReference.get()
            if (fragment != null && fragment.isAdded) {
                if (url.isEmpty()) {
                    DisplayUtils.showSnackMessage(fragment.binding.list, R.string.error_creating_file_from_template)
                } else {
                    val editorWebView = Intent(MainApp.getAppContext(), TextEditorWebView::class.java)
                    editorWebView.putExtra(ExternalSiteWebView.EXTRA_TITLE, "Text")
                    editorWebView.putExtra(ExternalSiteWebView.EXTRA_URL, url)
                    editorWebView.putExtra(ExternalSiteWebView.EXTRA_FILE, file)
                    editorWebView.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false)
                    fragment.startActivity(editorWebView)
                    fragment.dismiss()
                }
            } else {
                Log_OC.e(TAG, "Error creating file from template!")
            }
        }

        init {
            chooseTemplateDialogFragmentWeakReference = WeakReference(chooseTemplateDialogFragment)
            this.template = template
            this.path = path
            this.creator = creator
            this.user = user
        }
    }

    private class FetchTemplateTask(
        chooseTemplateDialogFragment: ChooseTemplateDialogFragment,
        private val clientFactory: ClientFactory?,
        private val user: User,
        creator: Creator?
    ) : AsyncTask<Void, Void, TemplateList>() {
        private val chooseTemplateDialogFragmentWeakReference: WeakReference<ChooseTemplateDialogFragment>
        private val creator: Creator?

        override fun doInBackground(vararg voids: Void): TemplateList {
            return try {
                val client = clientFactory!!.createNextcloudClient(user)
                val result = DirectEditingObtainListOfTemplatesRemoteOperation(
                    creator!!.editor,
                    creator.id
                )
                    .execute(client)
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

        override fun onPostExecute(templateList: TemplateList) {
            val fragment = chooseTemplateDialogFragmentWeakReference.get()
            if (fragment != null && fragment.isAdded) {
                if (templateList.templates.isEmpty()) {
                    DisplayUtils.showSnackMessage(fragment.binding.list, R.string.error_retrieving_templates)
                } else {
                    if (templateList.templates.size == SINGLE_TEMPLATE) {
                        fragment.onTemplateChosen(templateList.templates.values.iterator().next())
                        fragment.binding.list.visibility = View.GONE
                    } else {
                        val name = DOT + templateList.templates.values.iterator().next().extension
                        fragment.binding.filename.setText(name)
                        fragment.binding.helperText.visibility = View.VISIBLE
                    }
                    fragment.setTemplateList(templateList)
                }
            } else {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!")
            }
        }

        init {
            chooseTemplateDialogFragmentWeakReference = WeakReference(chooseTemplateDialogFragment)
            this.creator = creator
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
            val frag = ChooseTemplateDialogFragment()
            val args = Bundle()
            args.putParcelable(ARG_PARENT_FOLDER, parentFolder)
            args.putParcelable(ARG_CREATOR, creator)
            args.putString(ARG_HEADLINE, headline)
            frag.arguments = args
            return frag
        }
    }
}
