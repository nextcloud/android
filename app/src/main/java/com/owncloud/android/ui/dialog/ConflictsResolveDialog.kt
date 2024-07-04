/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nextcloud.utils.extensions.logFileSize
import com.owncloud.android.R
import com.owncloud.android.databinding.ConflictResolveDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.LocalFileListAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import javax.inject.Inject

/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
class ConflictsResolveDialog : DialogFragment(), Injectable {
    private lateinit var binding: ConflictResolveDialogBinding

    private var existingFile: OCFile? = null
    private var newFile: File? = null
    var listener: OnConflictDecisionMadeListener? = null
    private var user: User? = null
    private val asyncTasks: MutableList<ThumbnailGenerationTask> = ArrayList()
    private var positiveButton: MaterialButton? = null

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    enum class Decision {
        CANCEL,
        KEEP_BOTH,
        KEEP_LOCAL,
        KEEP_SERVER
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as OnConflictDecisionMadeListener
        } catch (e: ClassCastException) {
            throw ClassCastException("Activity of this dialog must implement OnConflictDecisionMadeListener")
        }
    }

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog?

        if (alertDialog == null) {
            Toast.makeText(context, "Failed to create conflict dialog", Toast.LENGTH_LONG).show()
            return
        }

        positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE) as MaterialButton
        val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE) as MaterialButton

        positiveButton?.let {
            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(it)
        }

        viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(negativeButton)
        positiveButton?.isEnabled = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            existingFile = savedInstanceState.getParcelableArgument(KEY_EXISTING_FILE, OCFile::class.java)
            newFile = savedInstanceState.getSerializableArgument(KEY_NEW_FILE, File::class.java)
            user = savedInstanceState.getParcelableArgument(KEY_USER, User::class.java)
        } else if (arguments != null) {
            existingFile = arguments.getParcelableArgument(KEY_EXISTING_FILE, OCFile::class.java)
            newFile = arguments.getSerializableArgument(KEY_NEW_FILE, File::class.java)
            user = arguments.getParcelableArgument(KEY_USER, User::class.java)
        } else {
            Toast.makeText(context, "Failed to create conflict dialog", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        existingFile.logFileSize(TAG)
        newFile.logFileSize(TAG)
        outState.putParcelable(KEY_EXISTING_FILE, existingFile)
        outState.putSerializable(KEY_NEW_FILE, newFile)
        outState.putParcelable(KEY_USER, user)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = ConflictResolveDialogBinding.inflate(requireActivity().layoutInflater)

        viewThemeUtils.platform.themeCheckbox(binding.newCheckbox)
        viewThemeUtils.platform.themeCheckbox(binding.existingCheckbox)

        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setView(binding.root)
            .setPositiveButton(R.string.common_ok) { _: DialogInterface?, _: Int ->
                if (binding.newCheckbox.isChecked && binding.existingCheckbox.isChecked) {
                    listener?.conflictDecisionMade(Decision.KEEP_BOTH)
                } else if (binding.newCheckbox.isChecked) {
                    listener?.conflictDecisionMade(Decision.KEEP_LOCAL)
                } else if (binding.existingCheckbox.isChecked) {
                    listener?.conflictDecisionMade(Decision.KEEP_SERVER)
                }
            }
            .setNegativeButton(R.string.common_cancel) { _: DialogInterface?, _: Int ->
                listener?.conflictDecisionMade(Decision.CANCEL)
            }
            .setTitle(String.format(getString(R.string.conflict_file_headline), existingFile?.fileName))

        setupUI()
        setOnClickListeners()

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.existingFileContainer.context, builder)

        return builder.create()
    }

    private fun setupUI() {
        val parentFile = existingFile?.remotePath?.let { File(it).parentFile }
        if (parentFile != null) {
            binding.`in`.text = String.format(getString(R.string.in_folder), parentFile.absolutePath)
        } else {
            binding.`in`.visibility = View.GONE
        }

        // set info for new file
        binding.newSize.text = newFile?.length()?.let { DisplayUtils.bytesToHumanReadable(it) }
        binding.newTimestamp.text = newFile?.lastModified()?.let { DisplayUtils.getRelativeTimestamp(context, it) }
        binding.newThumbnail.tag = newFile.hashCode()
        LocalFileListAdapter.setThumbnail(
            newFile,
            binding.newThumbnail,
            context,
            viewThemeUtils
        )

        // set info for existing file
        binding.existingSize.text = existingFile?.fileLength?.let { DisplayUtils.bytesToHumanReadable(it) }
        binding.existingTimestamp.text = existingFile?.modificationTimestamp?.let {
            DisplayUtils.getRelativeTimestamp(
                context,
                it
            )
        }

        binding.existingThumbnail.tag = existingFile?.fileId
        DisplayUtils.setThumbnail(
            existingFile,
            binding.existingThumbnail,
            user,
            FileDataStorageManager(
                user,
                requireContext().contentResolver
            ),
            asyncTasks,
            false,
            context,
            null,
            syncedFolderProvider.preferences,
            viewThemeUtils,
            syncedFolderProvider
        )
    }

    private fun setOnClickListeners() {
        val checkBoxClickListener = View.OnClickListener {
            positiveButton?.isEnabled = binding.newCheckbox.isChecked || binding.existingCheckbox.isChecked
        }

        binding.newCheckbox.setOnClickListener(checkBoxClickListener)
        binding.existingCheckbox.setOnClickListener(checkBoxClickListener)

        binding.newFileContainer.setOnClickListener {
            binding.newCheckbox.isChecked = !binding.newCheckbox.isChecked
            positiveButton?.isEnabled = binding.newCheckbox.isChecked || binding.existingCheckbox.isChecked
        }
        binding.existingFileContainer.setOnClickListener {
            binding.existingCheckbox.isChecked = !binding.existingCheckbox.isChecked
            positiveButton?.isEnabled = binding.newCheckbox.isChecked || binding.existingCheckbox.isChecked
        }
    }

    fun showDialog(activity: AppCompatActivity) {
        val prev = activity.supportFragmentManager.findFragmentByTag("dialog")
        val ft = activity.supportFragmentManager.beginTransaction()
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        show(ft, "dialog")
    }

    override fun onCancel(dialog: DialogInterface) {
        listener?.conflictDecisionMade(Decision.CANCEL)
    }

    fun interface OnConflictDecisionMadeListener {
        fun conflictDecisionMade(decision: Decision?)
    }

    override fun onStop() {
        super.onStop()

        for (task in asyncTasks) {
            task.cancel(true)
            Log_OC.d(this, "cancel: abort get method directly")
            task.getMethod?.abort()
        }

        asyncTasks.clear()
    }

    companion object {
        private const val TAG = "ConflictsResolveDialog"
        private const val KEY_NEW_FILE = "file"
        private const val KEY_EXISTING_FILE = "ocfile"
        private const val KEY_USER = "user"

        @JvmStatic
        fun newInstance(existingFile: OCFile?, newFile: OCFile, user: User?): ConflictsResolveDialog {
            val file = File(newFile.storagePath)
            file.logFileSize(TAG)

            val bundle = Bundle().apply {
                putParcelable(KEY_EXISTING_FILE, existingFile)
                putSerializable(KEY_NEW_FILE, file)
                putParcelable(KEY_USER, user)
            }

            return ConflictsResolveDialog().apply {
                arguments = bundle
            }
        }
    }
}
