/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.core.Clock
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogAutoUploadPreferenceBinding
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class AutoUploadPreferenceDialogFragment :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var clock: Clock

    private lateinit var syncedFolder: SyncedFolderParcelable
    private lateinit var form: AutoUploadForm

    private var openOptionDialog: AutoUploadOption? = null
    private var optionDialog: AlertDialog? = null

    private val listener: AutoUploadFolderConfigurationListener
        get() = parentFragment as? AutoUploadFolderConfigurationListener
            ?: requireActivity() as AutoUploadFolderConfigurationListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = savedInstanceState ?: requireArguments()
        syncedFolder = requireNotNull(
            state.getParcelableArgument(SYNCED_FOLDER_PARCELABLE, SyncedFolderParcelable::class.java)
        )
        openOptionDialog = savedInstanceState?.getString(OPEN_OPTION_DIALOG)?.let(AutoUploadOption::valueOf)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAutoUploadPreferenceBinding.inflate(requireActivity().layoutInflater)
        form = AutoUploadForm(binding, syncedFolder, clock, lifecycle)
        form.setup(viewThemeUtils)
        setupButtons(binding)
        setupNavigationListeners(binding)

        val builder = MaterialAlertDialogBuilder(requireContext()).setView(binding.root)
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(SYNCED_FOLDER_PARCELABLE, syncedFolder)
        outState.putString(OPEN_OPTION_DIALOG, openOptionDialog?.name)
    }

    override fun onStart() {
        super.onStart()
        val pendingOptionDialog = openOptionDialog ?: return
        if (optionDialog?.isShowing == true) {
            return
        }

        showOptionDialog(pendingOptionDialog)
    }

    override fun onDestroyView() {
        optionDialog?.dismiss()
        super.onDestroyView()
    }

    fun setRemoteFolderSummary(path: String) {
        form.setRemotePath(path)
    }

    fun setLocalFolderSummary(path: String) {
        form.setLocalPath(path)
    }

    private fun setupButtons(binding: DialogAutoUploadPreferenceBinding) {
        binding.btnPositive.setOnClickListener {
            dismiss()
            listener.onSaveAutoUploadFolderConfiguration(syncedFolder)
        }

        val cancelButton = if (form.canDelete) binding.btnNeutral else binding.btnNegative
        cancelButton.setText(R.string.common_cancel)
        cancelButton.setOnClickListener {
            dismiss()
            listener.onCancelAutoUploadFolderConfiguration()
        }

        if (!form.canDelete) {
            binding.btnNeutral.isVisible = false
            return
        }

        binding.btnNegative.setText(R.string.common_delete)
        binding.btnNegative.setOnClickListener {
            dismiss()
            listener.onDeleteAutoUploadFolderConfiguration(syncedFolder)
        }
    }

    private fun setupNavigationListeners(binding: DialogAutoUploadPreferenceBinding) {
        binding.remoteFolderContainer.setOnClickListener { listener.onRemoteFolderPickerRequested() }
        binding.localFolderContainer.setOnClickListener { listener.onLocalFolderPickerRequested() }
        binding.settingInstantBehaviourContainer.setOnClickListener {
            showOptionDialog(AutoUploadOption.UPLOAD_BEHAVIOUR)
        }
        binding.settingInstantNameCollisionPolicyContainer.setOnClickListener {
            showOptionDialog(AutoUploadOption.NAME_COLLISION_POLICY)
        }
    }

    private fun showOptionDialog(type: AutoUploadOption) {
        when (type) {
            AutoUploadOption.UPLOAD_BEHAVIOUR -> showOptionDialog(
                type,
                R.string.prefs_instant_behaviour_dialogTitle,
                form.uploadBehaviourEntries,
                form.uploadBehaviourIndex,
                form::selectUploadBehaviour
            )

            AutoUploadOption.NAME_COLLISION_POLICY -> showOptionDialog(
                type,
                R.string.pref_instant_name_collision_policy_dialogTitle,
                form.nameCollisionPolicyEntries,
                form.nameCollisionPolicyIndex,
                form::selectNameCollisionPolicy
            )
        }
    }

    private fun showOptionDialog(
        type: AutoUploadOption,
        @StringRes title: Int,
        entries: Array<CharSequence>,
        checkedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        openOptionDialog = type
        val builder = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(title)
            .setSingleChoiceItems(entries, checkedIndex) { dialog, which ->
                onSelected(which)
                openOptionDialog = null
                dialog.dismiss()
            }
            .setOnCancelListener { openOptionDialog = null }
        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireActivity(), builder)

        optionDialog = builder.create().apply { show() }
    }

    companion object {
        const val REQUEST_CODE__SELECT_LOCAL_FOLDER = 1
        private const val SYNCED_FOLDER_PARCELABLE = "SyncedFolderParcelable"
        private const val OPEN_OPTION_DIALOG = "OPEN_OPTION_DIALOG"

        @JvmStatic
        fun newInstance(syncedFolder: SyncedFolderDisplayItem): AutoUploadPreferenceDialogFragment =
            AutoUploadPreferenceDialogFragment().apply {
                arguments = bundleOf(SYNCED_FOLDER_PARCELABLE to SyncedFolderParcelable(syncedFolder))
                setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog)
            }
    }
}
