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
import com.nextcloud.client.database.entity.OfflineOperationEntity
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
import com.owncloud.android.utils.MimeTypeUtil
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
    private var offlineOperation: OfflineOperationEntity? = null
    private var serverFile: OCFile? = null

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    enum class Decision {
        CANCEL,
        KEEP_BOTH,
        KEEP_LOCAL,
        KEEP_SERVER,
        KEEP_OFFLINE_FOLDER,
        KEEP_SERVER_FOLDER,
        KEEP_BOTH_FOLDER
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

            val offlineOperationPath = savedInstanceState.getString(KEY_OFFLINE_OPERATION_PATH) ?: return
            offlineOperation = fileDataStorageManager.offlineOperationDao.getByPath(offlineOperationPath)

            val serverFileRemoteId = savedInstanceState.getString(KEY_OFFLINE_SERVER_FILE_REMOTE_ID) ?: return
            serverFile = fileDataStorageManager.getFileByRemoteId(serverFileRemoteId)
        } else if (arguments != null) {
            existingFile = arguments.getParcelableArgument(KEY_EXISTING_FILE, OCFile::class.java)
            newFile = arguments.getSerializableArgument(KEY_NEW_FILE, File::class.java)
            user = arguments.getParcelableArgument(KEY_USER, User::class.java)

            val offlineOperationPath = arguments?.getString(KEY_OFFLINE_OPERATION_PATH) ?: return
            offlineOperation = fileDataStorageManager.offlineOperationDao.getByPath(offlineOperationPath)

            val serverFileRemoteId = arguments?.getString(KEY_OFFLINE_SERVER_FILE_REMOTE_ID) ?: return
            serverFile = fileDataStorageManager.getFileByRemoteId(serverFileRemoteId)
        } else {
            Toast.makeText(context, "Failed to create conflict dialog", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        existingFile.logFileSize(TAG)
        newFile.logFileSize(TAG)

        outState.run {
            putParcelable(KEY_EXISTING_FILE, existingFile)
            putSerializable(KEY_NEW_FILE, newFile)
            putParcelable(KEY_USER, user)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = ConflictResolveDialogBinding.inflate(requireActivity().layoutInflater)

        viewThemeUtils.platform.themeCheckbox(binding.newCheckbox)
        viewThemeUtils.platform.themeCheckbox(binding.existingCheckbox)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.common_ok) { _: DialogInterface?, _: Int ->
                val decision = when {
                    binding.newCheckbox.isChecked && binding.existingCheckbox.isChecked ->
                        if (offlineOperation != null && serverFile != null) Decision.KEEP_BOTH_FOLDER else Decision.KEEP_BOTH

                    binding.newCheckbox.isChecked ->
                        if (offlineOperation != null && serverFile != null) Decision.KEEP_OFFLINE_FOLDER else Decision.KEEP_LOCAL

                    binding.existingCheckbox.isChecked ->
                        if (offlineOperation != null && serverFile != null) Decision.KEEP_SERVER_FOLDER else Decision.KEEP_SERVER

                    else -> null
                }

                decision?.let { listener?.conflictDecisionMade(it) }
            }
            .setNegativeButton(R.string.common_cancel) { _: DialogInterface?, _: Int ->
                listener?.conflictDecisionMade(Decision.CANCEL)
            }

        if (existingFile != null && newFile != null) {
            builder.setTitle(String.format(getString(R.string.conflict_file_headline), existingFile?.fileName))
            setupUI()
        } else if (offlineOperation != null && serverFile != null) {
            builder.setTitle(getString(R.string.conflict_folder_headline))
            setupUIForFolderConflict()
        }

        setOnClickListeners()

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.existingFileContainer.context, builder)

        return builder.create()
    }

    private fun setupUIForFolderConflict() {
        binding.run {
            folderName.visibility = View.GONE
            title.visibility = View.GONE
            description.text = getString(R.string.conflict_message_description_for_folder)
            newCheckbox.text = getString(R.string.prefs_synced_folders_local_path_title)
            existingCheckbox.text = getString(R.string.prefs_synced_folders_remote_path_title)

            val folderIcon = MimeTypeUtil.getDefaultFolderIcon(requireContext(), viewThemeUtils)
            newThumbnail.setImageDrawable(folderIcon)
            newTimestamp.text =
                DisplayUtils.getRelativeTimestamp(requireContext(), offlineOperation?.createdAt?.times(1000L) ?: 0)
            newSize.text = DisplayUtils.bytesToHumanReadable(0)

            existingThumbnail.setImageDrawable(folderIcon)
            existingTimestamp.text =
                DisplayUtils.getRelativeTimestamp(requireContext(), serverFile?.modificationTimestamp ?: 0)
            existingSize.text = DisplayUtils.bytesToHumanReadable(serverFile?.fileLength ?: 0)
        }
    }

    private fun setupUI() {
        val parentFile = existingFile?.remotePath?.let { File(it).parentFile }
        if (parentFile != null) {
            binding.folderName.text = String.format(getString(R.string.in_folder), parentFile.absolutePath)
        } else {
            binding.folderName.visibility = View.GONE
        }

        // set info for new file
        binding.newSize.text = newFile?.length()?.let { DisplayUtils.bytesToHumanReadable(it) }
        binding.newTimestamp.text = newFile?.lastModified()?.let { DisplayUtils.getRelativeTimestamp(context, it) }
        binding.newThumbnail.tag = newFile?.hashCode()
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
        binding.run {
            val checkBoxClickListener = View.OnClickListener {
                positiveButton?.isEnabled = newCheckbox.isChecked || existingCheckbox.isChecked
            }

            newCheckbox.setOnClickListener(checkBoxClickListener)
            existingCheckbox.setOnClickListener(checkBoxClickListener)

            newFileContainer.setOnClickListener {
                newCheckbox.isChecked = !newCheckbox.isChecked
                positiveButton?.isEnabled = newCheckbox.isChecked || existingCheckbox.isChecked
            }

            existingFileContainer.setOnClickListener {
                existingCheckbox.isChecked = !existingCheckbox.isChecked
                positiveButton?.isEnabled = newCheckbox.isChecked || existingCheckbox.isChecked
            }
        }
    }

    fun showDialog(activity: AppCompatActivity) {
        val prev = activity.supportFragmentManager.findFragmentByTag("dialog")
        activity.supportFragmentManager.beginTransaction().run {
            if (prev != null) {
                this.remove(prev)
            }
            addToBackStack(null)
            show(this, "dialog")
        }
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

        private const val KEY_OFFLINE_SERVER_FILE_REMOTE_ID = "KEY_OFFLINE_SERVER_FILE_REMOTE_ID"
        private const val KEY_OFFLINE_OPERATION_PATH = "KEY_OFFLINE_OPERATION_PATH"

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

        @JvmStatic
        fun newInstance(ocFile: OCFile, user: User, operationPath: String): ConflictsResolveDialog {
            val bundle = Bundle().apply {
                putString(KEY_OFFLINE_SERVER_FILE_REMOTE_ID, ocFile.remoteId)
                putString(KEY_OFFLINE_OPERATION_PATH, operationPath)
                putParcelable(KEY_USER, user)
            }

            return ConflictsResolveDialog().apply {
                arguments = bundle
            }
        }
    }
}
