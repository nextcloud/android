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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.ConflictResolveDialogBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.ui.adapter.localFileList.LocalFileThumbnailBinder
import com.owncloud.android.ui.dialog.conflict.model.ConflictDialogType
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import javax.inject.Inject

/**
 * Dialog which will be displayed to user upon keep-in-sync file conflict.
 */
class ConflictsResolveDialog :
    DialogFragment(),
    Injectable {
    private lateinit var binding: ConflictResolveDialogBinding

    var listener: OnConflictDecisionMadeListener? = null
    private var positiveButton: MaterialButton? = null

    private lateinit var dialogType: ConflictDialogType
    private var user: User? = null
    private var leftDataFile: File? = null
    private var rightDataFile: OCFile? = null

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    @Inject
    lateinit var thumbnailGenerator: ThumbnailGenerator

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
        } catch (_: ClassCastException) {
            throw ClassCastException("Activity of this dialog must implement OnConflictDecisionMadeListener")
        }
    }

    override fun onStart() {
        super.onStart()

        val alertDialog = dialog as AlertDialog?

        if (alertDialog == null) {
            activity?.let {
                DisplayUtils.showSnackMessage(it, R.string.failed_to_create_conflict_dialog)
            }
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

        val bundle = requireArguments()
        dialogType = requireNotNull(bundle.getParcelableArgument(ARG_CONFLICT_DATA, ConflictDialogType::class.java)) {
            "$ARG_CONFLICT_DATA is required, create this dialog through ConflictResolveDialogFactory"
        }
        leftDataFile = bundle.getSerializableArgument(ARG_LEFT_FILE, File::class.java)
        rightDataFile = bundle.getParcelableArgument(ARG_RIGHT_FILE, OCFile::class.java)
        user = bundle.getParcelableArgument(ARG_USER, User::class.java)
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

    private fun createDialogBuilder(): MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
        .setView(binding.root)
        .setPositiveButton(R.string.common_ok) { _: DialogInterface?, _: Int ->
            okButtonClick()
        }
        .setNegativeButton(R.string.common_cancel) { _: DialogInterface?, _: Int ->
            listener?.conflictDecisionMade(Decision.CANCEL)
        }
        .setTitle(dialogType.dialogTitle)

    private fun okButtonClick() {
        binding.run {
            val isDialogTypeOffline = (dialogType is ConflictDialogType.Offline)
            val decision = when {
                leftCheckbox.isChecked && rightCheckbox.isChecked ->
                    if (isDialogTypeOffline) Decision.KEEP_BOTH_FOLDER else Decision.KEEP_BOTH

                leftCheckbox.isChecked ->
                    if (isDialogTypeOffline) Decision.KEEP_OFFLINE_FOLDER else Decision.KEEP_LOCAL

                rightCheckbox.isChecked ->
                    if (isDialogTypeOffline) Decision.KEEP_SERVER_FOLDER else Decision.KEEP_SERVER

                else -> null
            }

            decision?.let { listener?.conflictDecisionMade(it) }
        }
    }

    private fun setupUI() {
        val data = dialogType.data

        binding.run {
            headline.text = data.headline
            description.text = data.description

            leftCheckbox.text = data.localFile.title
            leftTimestamp.text = data.localFile.timestamp
            leftFileSize.text = data.localFile.fileSize

            rightCheckbox.text = data.serverFile.title
            rightTimestamp.text = data.serverFile.timestamp
            rightFileSize.text = data.serverFile.fileSize

            if (leftDataFile != null && rightDataFile != null && user != null) {
                setThumbnailsForFileConflicts()
            } else {
                val folderIcon = MimeTypeUtil.getDefaultFolderIcon(requireContext(), viewThemeUtils)
                leftThumbnail.setImageDrawable(folderIcon)
                rightThumbnail.setImageDrawable(folderIcon)
            }
        }
    }

    private fun setThumbnailsForFileConflicts() {
        val localFile = leftDataFile ?: return
        val currentContext = context ?: return

        binding.leftThumbnail.tag = localFile.hashCode()
        binding.rightThumbnail.tag = rightDataFile.hashCode()

        LocalFileThumbnailBinder.setThumbnail(
            localFile,
            binding.leftThumbnail,
            currentContext,
            viewThemeUtils
        )

        thumbnailGenerator.setThumbnail(rightDataFile, binding.rightThumbnail)
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

    companion object {
        internal const val ARG_CONFLICT_DATA = "CONFLICT_DATA"
        internal const val ARG_LEFT_FILE = "LEFT_FILE"
        internal const val ARG_RIGHT_FILE = "RIGHT_FILE"
        internal const val ARG_USER = "USER"
    }
}
