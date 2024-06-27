/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.text.style.StyleSpan
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.SyncedFoldersSettingsLayoutBinding
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import javax.inject.Inject

/**
 * Dialog to show the preferences/configuration of a synced folder allowing the user to change the different
 * parameters.
 */
class SyncedFolderPreferencesDialogFragment : DialogFragment(), Injectable {

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    private lateinit var uploadBehaviorItemStrings: Array<CharSequence>
    private lateinit var nameCollisionPolicyItemStrings: Array<CharSequence>

    private var syncedFolder: SyncedFolderParcelable? = null
    private var behaviourDialogShown = false
    private var nameCollisionPolicyDialogShown = false
    private var behaviourDialog: AlertDialog? = null
    private var binding: SyncedFoldersSettingsLayoutBinding? = null
    private var isNeutralButtonActive = true

    @Deprecated("Deprecated in Java")
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        require(activity is OnSyncedFolderPreferenceListener) {
            (
                "The host activity must implement " +
                    OnSyncedFolderPreferenceListener::class.java.canonicalName
                )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // keep the state of the fragment on configuration changes
        retainInstance = true
        binding = null

        val arguments = arguments
        if (arguments != null) {
            syncedFolder = arguments.getParcelableArgument(SYNCED_FOLDER_PARCELABLE, SyncedFolderParcelable::class.java)
        }

        uploadBehaviorItemStrings = resources.getTextArray(R.array.pref_behaviour_entries)
        nameCollisionPolicyItemStrings = resources.getTextArray(R.array.pref_name_collision_policy_entries)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is $savedInstanceState")
        binding = SyncedFoldersSettingsLayoutBinding.inflate(requireActivity().layoutInflater, null, false)

        setupDialogElements(binding!!)
        setupListeners(binding!!)

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setView(binding!!.getRoot())

        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    /**
     * find all relevant UI elements and set their values.
     *
     * @param binding the parent binding
     */
    private fun setupDialogElements(binding: SyncedFoldersSettingsLayoutBinding) {
        setupLayout(binding)
        applyUserColor(binding)
        setButtonOrder(binding)
        setValuesViaSyncedFolder(binding)
    }

    private fun setupLayout(binding: SyncedFoldersSettingsLayoutBinding) {
        if (syncedFolder!!.type.id > MediaFolderType.CUSTOM.id) {
            // hide local folder chooser and delete for non-custom folders
            binding.localFolderContainer.visibility = View.GONE
            isNeutralButtonActive = false
            binding.settingInstantUploadExcludeHiddenContainer.visibility = View.GONE
        } else if (syncedFolder!!.id <= SyncedFolder.UNPERSISTED_ID) {
            isNeutralButtonActive = false

            // Hide delete/enabled for unpersisted custom folders
            binding.syncEnabled.visibility = View.GONE

            // Show exclude hidden checkbox when {@link MediaFolderType#CUSTOM}
            binding.settingInstantUploadExcludeHiddenContainer.visibility = View.VISIBLE

            // auto set custom folder to enabled
            syncedFolder?.isEnabled = true

            // switch text to create headline
            binding.syncedFoldersSettingsTitle.setText(R.string.autoupload_create_new_custom_folder)

            // disable save button
            binding.btnPositive.isEnabled = false
        } else {
            binding.localFolderContainer.visibility = View.GONE
            if (MediaFolderType.CUSTOM.id == syncedFolder!!.type.id) {
                // Show exclude hidden checkbox when {@link MediaFolderType#CUSTOM}
                binding.settingInstantUploadExcludeHiddenContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun applyUserColor(binding: SyncedFoldersSettingsLayoutBinding) {
        viewThemeUtils?.androidx?.colorSwitchCompat(binding.syncEnabled)

        viewThemeUtils?.platform?.themeCheckbox(
            binding.settingInstantUploadOnWifiCheckbox,
            binding.settingInstantUploadOnChargingCheckbox,
            binding.settingInstantUploadExistingCheckbox,
            binding.settingInstantUploadPathUseSubfoldersCheckbox,
            binding.settingInstantUploadExcludeHiddenCheckbox
        )

        viewThemeUtils?.material?.colorMaterialButtonPrimaryTonal(binding.btnPositive)
        viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(binding.btnNegative)
        viewThemeUtils?.material?.colorMaterialButtonPrimaryBorderless(binding.btnNeutral)
    }

    private fun setButtonOrder(binding: SyncedFoldersSettingsLayoutBinding) {
        // btnNeutral  btnNegative btnPositive
        if (isNeutralButtonActive) {
            // Cancel   Delete Save
            binding.btnNeutral.setText(R.string.common_cancel)
            binding.btnNegative.setText(R.string.common_delete)
        } else {
            //          Cancel Save
            binding.btnNeutral.visibility = View.GONE
            binding.btnNegative.setText(R.string.common_cancel)
        }
    }

    private fun setValuesViaSyncedFolder(binding: SyncedFoldersSettingsLayoutBinding) {
        syncedFolder?.let {
            setEnabled(it.isEnabled)

            if (!TextUtils.isEmpty(it.localPath)) {
                binding.syncedFoldersSettingsLocalFolderPath.text = DisplayUtils.createTextWithSpan(
                    String.format(
                        getString(R.string.synced_folders_preferences_folder_path),
                        it.localPath
                    ),
                    it.folderName,
                    StyleSpan(Typeface.BOLD)
                )
                binding.localFolderSummary.text = FileStorageUtils.pathToUserFriendlyDisplay(
                    it.localPath,
                    activity,
                    resources
                )
            } else {
                binding.localFolderSummary.setText(R.string.choose_local_folder)
            }

            if (!TextUtils.isEmpty(it.localPath)) {
                binding.remoteFolderSummary.text = it.remotePath
            } else {
                binding.remoteFolderSummary.setText(R.string.choose_remote_folder)
            }

            binding.settingInstantUploadOnWifiCheckbox.isChecked = it.isWifiOnly
            binding.settingInstantUploadOnChargingCheckbox.isChecked = it.isChargingOnly
            binding.settingInstantUploadExistingCheckbox.isChecked = it.isExisting
            binding.settingInstantUploadPathUseSubfoldersCheckbox.isChecked = it.isSubfolderByDate
            binding.settingInstantUploadExcludeHiddenCheckbox.isChecked = it.isExcludeHidden

            binding.settingInstantUploadSubfolderRuleSpinner.setSelection(it.subFolderRule.ordinal)

            binding.settingInstantUploadSubfolderRuleContainer.visibility =
                if (binding.settingInstantUploadPathUseSubfoldersCheckbox.isChecked) View.VISIBLE else View.GONE

            binding.settingInstantBehaviourSummary.text = uploadBehaviorItemStrings[it.uploadActionInteger]
            val nameCollisionPolicyIndex = getSelectionIndexForNameCollisionPolicy(
                it.nameCollisionPolicy
            )
            binding.settingInstantNameCollisionPolicySummary.text =
                nameCollisionPolicyItemStrings[nameCollisionPolicyIndex]
        }
    }

    /**
     * set correct icon/flag.
     *
     * @param enabled if enabled or disabled
     */
    private fun setEnabled(enabled: Boolean) {
        syncedFolder?.isEnabled = enabled
        binding?.syncEnabled?.isChecked = enabled
        setupViews(binding, enabled)
    }

    /**
     * set (new) remote path on activity result of the folder picker activity. The result gets originally propagated to
     * the underlying activity since the picker is an activity and the result can't get passed to the dialog fragment
     * directly.
     *
     * @param path the remote path to be set
     */
    fun setRemoteFolderSummary(path: String?) {
        syncedFolder?.remotePath = path
        binding?.remoteFolderSummary?.text = path
        checkAndUpdateSaveButtonState()
    }

    /**
     * set (new) local path on activity result of the folder picker activity. The result gets originally propagated to
     * the underlying activity since the picker is an activity and the result can't get passed to the dialog fragment
     * directly.
     *
     * @param path the local path to be set
     */
    fun setLocalFolderSummary(path: String?) {
        syncedFolder?.localPath = path
        binding?.localFolderSummary?.text = FileStorageUtils.pathToUserFriendlyDisplay(path, activity, resources)
        binding?.syncedFoldersSettingsLocalFolderPath?.text = DisplayUtils.createTextWithSpan(
            String.format(
                getString(R.string.synced_folders_preferences_folder_path),
                syncedFolder!!.localPath
            ),
            File(syncedFolder!!.localPath).name,
            StyleSpan(Typeface.BOLD)
        )
        checkAndUpdateSaveButtonState()
    }

    private fun checkAndUpdateSaveButtonState() {
        binding?.btnPositive?.isEnabled = syncedFolder!!.localPath != null && syncedFolder!!.remotePath != null
        checkWritableFolder()
    }

    private fun checkWritableFolder() {
        if (!syncedFolder!!.isEnabled) {
            binding?.settingInstantBehaviourContainer?.isEnabled = false
            binding?.settingInstantBehaviourContainer?.alpha = ALPHA_DISABLED
            return
        }
        if (syncedFolder!!.localPath != null && File(syncedFolder!!.localPath).canWrite()) {
            binding?.settingInstantBehaviourContainer?.isEnabled = true
            binding?.settingInstantBehaviourContainer?.alpha = ALPHA_ENABLED
            binding?.settingInstantBehaviourSummary?.text =
                uploadBehaviorItemStrings[syncedFolder!!.uploadActionInteger]
        } else {
            binding?.settingInstantBehaviourContainer?.isEnabled = false
            binding?.settingInstantBehaviourContainer?.alpha = ALPHA_DISABLED
            syncedFolder?.setUploadAction(
                resources.getTextArray(R.array.pref_behaviour_entryValues)[0].toString()
            )
            binding?.settingInstantBehaviourSummary?.setText(R.string.auto_upload_file_behaviour_kept_in_folder)
        }
    }

    private fun setupViews(optionalBinding: SyncedFoldersSettingsLayoutBinding?, enable: Boolean) {
        val alpha: Float = if (enable) {
            ALPHA_ENABLED
        } else {
            ALPHA_DISABLED
        }

        optionalBinding?.let { binding ->
            binding.settingInstantUploadOnWifiContainer.isEnabled = enable
            binding.settingInstantUploadOnWifiContainer.alpha = alpha
            binding.settingInstantUploadOnChargingContainer.isEnabled = enable
            binding.settingInstantUploadOnChargingContainer.alpha = alpha
            binding.settingInstantUploadExistingContainer.isEnabled = enable
            binding.settingInstantUploadExistingContainer.alpha = alpha
            binding.settingInstantUploadPathUseSubfoldersContainer.isEnabled = enable
            binding.settingInstantUploadPathUseSubfoldersContainer.alpha = alpha
            binding.settingInstantUploadExcludeHiddenContainer.isEnabled = enable
            binding.settingInstantUploadExcludeHiddenContainer.alpha = alpha
            binding.remoteFolderContainer.isEnabled = enable
            binding.remoteFolderContainer.alpha = alpha
            binding.localFolderContainer.isEnabled = enable
            binding.localFolderContainer.alpha = alpha
            binding.settingInstantNameCollisionPolicyContainer.isEnabled = enable
            binding.settingInstantNameCollisionPolicyContainer.alpha = alpha
            binding.settingInstantUploadOnWifiCheckbox.isEnabled = enable
            binding.settingInstantUploadOnChargingCheckbox.isEnabled = enable
            binding.settingInstantUploadExistingCheckbox.isEnabled = enable
            binding.settingInstantUploadPathUseSubfoldersCheckbox.isEnabled = enable
            binding.settingInstantUploadExcludeHiddenCheckbox.isEnabled = enable
        }

        checkWritableFolder()
    }

    /**
     * setup all listeners.
     *
     * @param binding the parent binding
     */
    private fun setupListeners(binding: SyncedFoldersSettingsLayoutBinding) {
        binding.btnPositive.setOnClickListener(OnSyncedFolderSaveClickListener())
        if (isNeutralButtonActive) {
            binding.btnNeutral.setOnClickListener(OnSyncedFolderCancelClickListener())
            binding.btnNegative.setOnClickListener(OnSyncedFolderDeleteClickListener())
        } else {
            binding.btnNegative.setOnClickListener(OnSyncedFolderCancelClickListener())
        }

        syncedFolder?.let { syncedFolder ->
            binding.settingInstantUploadOnWifiContainer.setOnClickListener {
                syncedFolder.isWifiOnly = !syncedFolder.isWifiOnly
                binding.settingInstantUploadOnWifiCheckbox.toggle()
            }
            binding.settingInstantUploadOnChargingContainer.setOnClickListener {
                syncedFolder.isChargingOnly = !syncedFolder.isChargingOnly
                binding.settingInstantUploadOnChargingCheckbox.toggle()
            }
            binding.settingInstantUploadExistingContainer.setOnClickListener {
                syncedFolder.isExisting = !syncedFolder.isExisting
                binding.settingInstantUploadExistingCheckbox.toggle()
            }
            binding.settingInstantUploadPathUseSubfoldersContainer.setOnClickListener {
                syncedFolder.isSubfolderByDate = !syncedFolder.isSubfolderByDate
                binding.settingInstantUploadPathUseSubfoldersCheckbox.toggle()

                // Only allow setting subfolder rule if subfolder is allowed
                if (binding.settingInstantUploadPathUseSubfoldersCheckbox.isChecked) {
                    binding.settingInstantUploadSubfolderRuleContainer.visibility = View.VISIBLE
                } else {
                    binding.settingInstantUploadSubfolderRuleContainer.visibility = View.GONE
                }
            }
            binding.settingInstantUploadExcludeHiddenContainer.setOnClickListener {
                syncedFolder.isExcludeHidden = !syncedFolder.isExcludeHidden
                binding.settingInstantUploadExcludeHiddenCheckbox.toggle()
            }
            binding.settingInstantUploadSubfolderRuleSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                        syncedFolder.subFolderRule = SubFolderRule.values()[i]
                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>?) {
                        syncedFolder.subFolderRule = SubFolderRule.YEAR_MONTH
                    }
                }

            binding.syncEnabled.setOnClickListener { setEnabled(!syncedFolder.isEnabled) }
        }

        binding.remoteFolderContainer.setOnClickListener {
            val action = Intent(activity, FolderPickerActivity::class.java).apply {
                putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.CHOOSE_LOCATION)
            }
            requireActivity().startActivityForResult(action, REQUEST_CODE__SELECT_REMOTE_FOLDER)
        }
        binding.localFolderContainer.setOnClickListener {
            val action = Intent(activity, UploadFilesActivity::class.java)
            action.putExtra(UploadFilesActivity.KEY_LOCAL_FOLDER_PICKER_MODE, true)
            action.putExtra(UploadFilesActivity.REQUEST_CODE_KEY, REQUEST_CODE__SELECT_LOCAL_FOLDER)
            requireActivity().startActivityForResult(action, REQUEST_CODE__SELECT_LOCAL_FOLDER)
        }

        binding.settingInstantBehaviourContainer.setOnClickListener { showBehaviourDialog() }
        binding.settingInstantNameCollisionPolicyContainer.setOnClickListener { showNameCollisionPolicyDialog() }
    }

    private fun showBehaviourDialog() {
        val builder = MaterialAlertDialogBuilder(requireActivity())

        syncedFolder?.let {
            val behaviourEntries = resources.getTextArray(R.array.pref_behaviour_entries)
            val behaviourEntryValues = resources.getTextArray(R.array.pref_behaviour_entryValues)
            builder.setTitle(R.string.prefs_instant_behaviour_dialogTitle)
                .setSingleChoiceItems(behaviourEntries, it.uploadActionInteger) { dialog: DialogInterface, which: Int ->
                    it.setUploadAction(behaviourEntryValues[which].toString())
                    binding?.settingInstantBehaviourSummary?.text = uploadBehaviorItemStrings[which]
                    behaviourDialogShown = false
                    dialog.dismiss()
                }
                .setOnCancelListener { behaviourDialogShown = false }
        }

        behaviourDialogShown = true
        viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireActivity(), builder)

        behaviourDialog = builder.create()
        behaviourDialog?.show()
    }

    private fun showNameCollisionPolicyDialog() {
        syncedFolder?.let {
            val builder = MaterialAlertDialogBuilder(requireActivity())
            builder.setTitle(R.string.pref_instant_name_collision_policy_dialogTitle)
                .setSingleChoiceItems(
                    resources.getTextArray(R.array.pref_name_collision_policy_entries),
                    getSelectionIndexForNameCollisionPolicy(it.nameCollisionPolicy),
                    OnNameCollisionDialogClickListener()
                )
                .setOnCancelListener { nameCollisionPolicyDialogShown = false }

            nameCollisionPolicyDialogShown = true

            viewThemeUtils?.dialog?.colorMaterialAlertDialogBackground(requireActivity(), builder)
            behaviourDialog = builder.create()
            behaviourDialog?.show()
        }
    }

    override fun onDestroyView() {
        Log_OC.d(TAG, "destroy SyncedFolderPreferencesDialogFragment view")
        if (dialog != null && retainInstance) {
            dialog?.setDismissMessage(null)
        }
        if (behaviourDialog != null && behaviourDialog!!.isShowing) {
            behaviourDialog?.dismiss()
        }

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(BEHAVIOUR_DIALOG_STATE, behaviourDialogShown)
        outState.putBoolean(NAME_COLLISION_POLICY_DIALOG_STATE, nameCollisionPolicyDialogShown)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        behaviourDialogShown = savedInstanceState != null &&
            savedInstanceState.getBoolean(BEHAVIOUR_DIALOG_STATE, false)
        nameCollisionPolicyDialogShown = savedInstanceState != null &&
            savedInstanceState.getBoolean(NAME_COLLISION_POLICY_DIALOG_STATE, false)
        if (behaviourDialogShown) {
            showBehaviourDialog()
        }
        if (nameCollisionPolicyDialogShown) {
            showNameCollisionPolicyDialog()
        }

        super.onViewStateRestored(savedInstanceState)
    }

    interface OnSyncedFolderPreferenceListener {
        fun onSaveSyncedFolderPreference(syncedFolder: SyncedFolderParcelable?)
        fun onCancelSyncedFolderPreference()
        fun onDeleteSyncedFolderPreference(syncedFolder: SyncedFolderParcelable?)
    }

    private inner class OnSyncedFolderSaveClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            dismiss()
            (activity as OnSyncedFolderPreferenceListener?)?.onSaveSyncedFolderPreference(syncedFolder)
        }
    }

    private inner class OnSyncedFolderCancelClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            dismiss()
            (activity as OnSyncedFolderPreferenceListener?)?.onCancelSyncedFolderPreference()
        }
    }

    private inner class OnSyncedFolderDeleteClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            dismiss()
            (activity as OnSyncedFolderPreferenceListener?)?.onDeleteSyncedFolderPreference(syncedFolder)
        }
    }

    private inner class OnNameCollisionDialogClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            syncedFolder!!.nameCollisionPolicy =
                getNameCollisionPolicyForSelectionIndex(which)
            binding?.settingInstantNameCollisionPolicySummary?.text =
                nameCollisionPolicyItemStrings[which]
            nameCollisionPolicyDialogShown = false
            dialog.dismiss()
        }
    }

    companion object {
        const val SYNCED_FOLDER_PARCELABLE = "SyncedFolderParcelable"
        const val REQUEST_CODE__SELECT_REMOTE_FOLDER = 0
        const val REQUEST_CODE__SELECT_LOCAL_FOLDER = 1
        private val TAG = SyncedFolderPreferencesDialogFragment::class.java.simpleName
        private const val BEHAVIOUR_DIALOG_STATE = "BEHAVIOUR_DIALOG_STATE"
        private const val NAME_COLLISION_POLICY_DIALOG_STATE = "NAME_COLLISION_POLICY_DIALOG_STATE"
        private const val ALPHA_ENABLED = 1.0f
        private const val ALPHA_DISABLED = 0.7f

        @JvmStatic
        fun newInstance(syncedFolder: SyncedFolderDisplayItem?, section: Int): SyncedFolderPreferencesDialogFragment? {
            if (syncedFolder == null) {
                return null
            }

            val args = Bundle().apply {
                putParcelable(SYNCED_FOLDER_PARCELABLE, SyncedFolderParcelable(syncedFolder, section))
            }

            return SyncedFolderPreferencesDialogFragment().apply {
                arguments = args
                setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog)
            }
        }

        /**
         * Get index for name collision selection dialog.
         *
         * @return 0 if ASK_USER, 1 if OVERWRITE, 2 if RENAME, 3 if SKIP, Otherwise: 0
         */
        @Suppress("MagicNumber")
        private fun getSelectionIndexForNameCollisionPolicy(nameCollisionPolicy: NameCollisionPolicy): Int {
            return when (nameCollisionPolicy) {
                NameCollisionPolicy.OVERWRITE -> 1
                NameCollisionPolicy.RENAME -> 2
                NameCollisionPolicy.CANCEL -> 3
                NameCollisionPolicy.ASK_USER -> 0
            }
        }

        /**
         * Get index for name collision selection dialog. Inverse of getSelectionIndexForNameCollisionPolicy.
         *
         * @return ASK_USER if 0, OVERWRITE if 1, RENAME if 2, SKIP if 3. Otherwise: ASK_USER
         */
        @Suppress("MagicNumber")
        private fun getNameCollisionPolicyForSelectionIndex(index: Int): NameCollisionPolicy {
            return when (index) {
                1 -> NameCollisionPolicy.OVERWRITE
                2 -> NameCollisionPolicy.RENAME
                3 -> NameCollisionPolicy.CANCEL
                0 -> NameCollisionPolicy.ASK_USER
                else -> NameCollisionPolicy.ASK_USER
            }
        }
    }
}
