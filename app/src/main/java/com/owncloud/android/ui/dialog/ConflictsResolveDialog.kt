/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
import com.owncloud.android.R
import com.owncloud.android.databinding.ConflictResolveDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.LocalFileListAdapter
import com.owncloud.android.ui.dialog.parcel.ConflictDialogData
import com.owncloud.android.ui.dialog.parcel.ConflictFileData
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

    var listener: OnConflictDecisionMadeListener? = null
    private val asyncTasks: MutableList<ThumbnailGenerationTask> = ArrayList()
    private var positiveButton: MaterialButton? = null

    private var data: ConflictDialogData? = null
    private var user: User? = null
    private var leftDataFile: File? = null
    private var rightDataFile: OCFile? = null

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

        val bundle = savedInstanceState ?: arguments

        if (bundle != null) {
            data = bundle.getParcelableArgument(ARG_CONFLICT_DATA, ConflictDialogData::class.java)
            leftDataFile = bundle.getSerializableArgument(ARG_LEFT_FILE, File::class.java)
            rightDataFile = bundle.getParcelableArgument(ARG_RIGHT_FILE, OCFile::class.java)
            user = bundle.getParcelableArgument(ARG_USER, User::class.java)
        } else {
            Toast.makeText(context, "Failed to create conflict dialog", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putParcelable(ARG_CONFLICT_DATA, data)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = ConflictResolveDialogBinding.inflate(requireActivity().layoutInflater)

        val builder = createDialogBuilder()

        setupUI()
        setOnClickListeners()

        viewThemeUtils.run {
            platform.themeCheckbox(binding.leftCheckbox)
            platform.themeCheckbox(binding.rightCheckbox)
            dialog.colorMaterialAlertDialogBackground(requireContext(), builder)
        }

        return builder.create()
    }

    private fun createDialogBuilder(): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.common_ok) { _: DialogInterface?, _: Int ->
                okButtonClick()
            }
            .setNegativeButton(R.string.common_cancel) { _: DialogInterface?, _: Int ->
                listener?.conflictDecisionMade(Decision.CANCEL)
            }
            .setTitle(data?.folderName)
    }

    private fun okButtonClick() {
        binding.run {
            val isFolderNameNotExists = (data?.folderName == null)
            val decision = when {
                leftCheckbox.isChecked && rightCheckbox.isChecked ->
                    if (isFolderNameNotExists) Decision.KEEP_BOTH_FOLDER else Decision.KEEP_BOTH

                leftCheckbox.isChecked ->
                    if (isFolderNameNotExists) Decision.KEEP_OFFLINE_FOLDER else Decision.KEEP_LOCAL

                rightCheckbox.isChecked ->
                    if (isFolderNameNotExists) Decision.KEEP_SERVER_FOLDER else Decision.KEEP_SERVER

                else -> null
            }

            decision?.let { listener?.conflictDecisionMade(it) }
        }
    }

    private fun setupUI() {
        binding.run {
            data?.let {
                val (leftData, rightData) = it.checkboxData

                folderName.visibility = if (it.folderName == null) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                folderName.text = it.folderName

                title.visibility = if (it.title == null) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                title.text = it.title

                description.text = it.description

                leftCheckbox.text = leftData.title
                leftTimestamp.text = leftData.timestamp
                leftFileSize.text = leftData.fileSize

                rightCheckbox.text = rightData.title
                rightTimestamp.text = rightData.timestamp
                rightFileSize.text = rightData.fileSize

                if (leftDataFile != null && rightDataFile != null && user != null) {
                    setThumbnailsForFileConflicts()
                } else {
                    val folderIcon = MimeTypeUtil.getDefaultFolderIcon(requireContext(), viewThemeUtils)
                    leftThumbnail.setImageDrawable(folderIcon)
                    rightThumbnail.setImageDrawable(folderIcon)
                }
            }
        }
    }

    private fun setThumbnailsForFileConflicts() {
        binding.leftThumbnail.tag = leftDataFile.hashCode()
        binding.rightThumbnail.tag = rightDataFile.hashCode()

        LocalFileListAdapter.setThumbnail(
            leftDataFile,
            binding.leftThumbnail,
            context,
            viewThemeUtils
        )

        DisplayUtils.setThumbnail(
            rightDataFile,
            binding.rightThumbnail,
            user,
            fileDataStorageManager,
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
                positiveButton?.isEnabled = (leftCheckbox.isChecked || rightCheckbox.isChecked)
            }

            leftCheckbox.setOnClickListener(checkBoxClickListener)
            rightCheckbox.setOnClickListener(checkBoxClickListener)

            leftFileContainer.setOnClickListener {
                leftCheckbox.toggle()
                positiveButton?.isEnabled = (leftCheckbox.isChecked || rightCheckbox.isChecked)
            }

            rightFileContainer.setOnClickListener {
                rightCheckbox.toggle()
                positiveButton?.isEnabled = (leftCheckbox.isChecked || rightCheckbox.isChecked)
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

        asyncTasks.forEach {
            it.cancel(true)
            Log_OC.d(this, "cancel: abort get method directly")
            it.getMethod?.abort()
        }

        asyncTasks.clear()
    }

    companion object {
        private const val ARG_CONFLICT_DATA = "CONFLICT_DATA"
        private const val ARG_LEFT_FILE = "LEFT_FILE"
        private const val ARG_RIGHT_FILE = "RIGHT_FILE"
        private const val ARG_USER = "USER"

        @JvmStatic
        fun newInstance(context: Context, leftFile: OCFile, rightFile: OCFile, user: User?): ConflictsResolveDialog {
            val file = File(leftFile.storagePath)
            val conflictData = getFileConflictData(file, rightFile, context)

            val bundle = Bundle().apply {
                putParcelable(ARG_CONFLICT_DATA, conflictData)
                putSerializable(ARG_LEFT_FILE, file)
                putParcelable(ARG_RIGHT_FILE, rightFile)
                putParcelable(ARG_USER, user)
            }

            return ConflictsResolveDialog().apply {
                arguments = bundle
            }
        }

        @JvmStatic
        fun newInstance(
            context: Context,
            offlineOperation: OfflineOperationEntity,
            rightFile: OCFile
        ): ConflictsResolveDialog {
            val conflictData = getFolderConflictData(offlineOperation, rightFile, context)

            val bundle = Bundle().apply {
                putParcelable(ARG_CONFLICT_DATA, conflictData)
                putParcelable(ARG_RIGHT_FILE, rightFile)
            }

            return ConflictsResolveDialog().apply {
                arguments = bundle
            }
        }

        @Suppress("MagicNumber")
        @JvmStatic
        private fun getFolderConflictData(
            offlineOperation: OfflineOperationEntity,
            rightFile: OCFile,
            context: Context
        ): ConflictDialogData {
            val folderName = null

            val leftTitle = context.getString(R.string.prefs_synced_folders_local_path_title)
            val leftTimestamp =
                DisplayUtils.getRelativeTimestamp(context, offlineOperation.createdAt?.times(1000L) ?: 0)
            val leftFileSize = DisplayUtils.bytesToHumanReadable(0)
            val leftCheckBoxData = ConflictFileData(leftTitle, leftTimestamp.toString(), leftFileSize)

            val rightTitle = context.getString(R.string.prefs_synced_folders_remote_path_title)
            val rightTimestamp = DisplayUtils.getRelativeTimestamp(context, rightFile.modificationTimestamp)
            val rightFileSize = DisplayUtils.bytesToHumanReadable(rightFile.fileLength)
            val rightCheckBoxData = ConflictFileData(rightTitle, rightTimestamp.toString(), rightFileSize)

            val title = context.getString(R.string.conflict_folder_headline)
            val description = context.getString(R.string.conflict_message_description_for_folder)
            return ConflictDialogData(folderName, title, description, Pair(leftCheckBoxData, rightCheckBoxData))
        }

        @JvmStatic
        private fun getFileConflictData(file: File, rightFile: OCFile, context: Context): ConflictDialogData {
            val parentFile = File(rightFile.remotePath).parentFile
            val folderName = if (parentFile != null) {
                String.format(context.getString(R.string.in_folder), parentFile.absolutePath)
            } else {
                null
            }

            val leftTitle = context.getString(R.string.conflict_local_file)
            val leftTimestamp = DisplayUtils.getRelativeTimestamp(context, file.lastModified())
            val leftFileSize = DisplayUtils.bytesToHumanReadable(file.length())
            val leftCheckBoxData = ConflictFileData(leftTitle, leftTimestamp.toString(), leftFileSize)

            val rightTitle = context.getString(R.string.conflict_server_file)
            val rightTimestamp = DisplayUtils.getRelativeTimestamp(context, rightFile.modificationTimestamp)
            val rightFileSize = DisplayUtils.bytesToHumanReadable(rightFile.fileLength)
            val rightCheckBoxData = ConflictFileData(rightTitle, rightTimestamp.toString(), rightFileSize)

            val title = context.getString(R.string.choose_which_file)
            val description = context.getString(R.string.conflict_message_description)
            return ConflictDialogData(folderName, title, description, Pair(leftCheckBoxData, rightCheckBoxData))
        }
    }
}
