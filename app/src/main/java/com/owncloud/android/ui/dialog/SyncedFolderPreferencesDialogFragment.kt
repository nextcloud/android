/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.SyncedFoldersSettingsLayoutBinding
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.dialog.DurationPickerDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.TimeUtils.getDurationParts
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import javax.inject.Inject

/**
 * Dialog to show the preferences/configuration of a synced folder allowing the user to change the different
 * parameters.
 */
class SyncedFolderPreferencesDialogFragment : DialogFragment(), Injectable {
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    private var mUploadBehaviorItemStrings: Array<CharSequence>
    private var mNameCollisionPolicyItemStrings: Array<CharSequence>
    private var mEnabledSwitch: SwitchCompat? = null
    private var mUploadOnWifiCheckbox: AppCompatCheckBox? = null
    private var mUploadOnChargingCheckbox: AppCompatCheckBox? = null
    private var mUploadExistingCheckbox: AppCompatCheckBox? = null
    private var mUploadUseSubfoldersCheckbox: AppCompatCheckBox? = null
    private var mUploadBehaviorSummary: TextView? = null
    private var mNameCollisionPolicySummary: TextView? = null
    private var mLocalFolderPath: TextView? = null
    private var mLocalFolderSummary: TextView? = null
    private var mRemoteFolderSummary: TextView? = null
    private var mUploadDelaySummary: TextView? = null

    private var mSyncedFolder: SyncedFolderParcelable? = null
    private var mCancel: MaterialButton? = null
    private var mSave: MaterialButton? = null
    private var behaviourDialogShown = false
    private var nameCollisionPolicyDialogShown = false
    private var behaviourDialog: AlertDialog? = null
    private var binding: SyncedFoldersSettingsLayoutBinding? = null

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        require(activity is OnSyncedFolderPreferenceListener) {
            ("The host activity must implement "
                + OnSyncedFolderPreferenceListener::class.java.canonicalName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // keep the state of the fragment on configuration changes
        retainInstance = true

        binding = null

        mSyncedFolder = arguments!!.getParcelable(SYNCED_FOLDER_PARCELABLE)
        mUploadBehaviorItemStrings = resources.getTextArray(R.array.pref_behaviour_entries)
        mNameCollisionPolicyItemStrings = resources.getTextArray(R.array.pref_name_collision_policy_entries)
    }

    /**
     * find all relevant UI elements and set their values.
     *
     * @param binding the parent binding
     */
    private fun setupDialogElements(binding: SyncedFoldersSettingsLayoutBinding) {
        if (mSyncedFolder!!.type.id > MediaFolderType.CUSTOM.id) {
            // hide local folder chooser and delete for non-custom folders
            binding.localFolderContainer.visibility = View.GONE
            binding.delete.visibility = View.GONE
        } else if (mSyncedFolder!!.id <= SyncedFolder.UNPERSISTED_ID) {
            // Hide delete/enabled for unpersisted custom folders
            binding.delete.visibility = View.GONE
            binding.syncEnabled.visibility = View.GONE

            // auto set custom folder to enabled
            mSyncedFolder!!.isEnabled = true

            // switch text to create headline
            binding.syncedFoldersSettingsTitle.setText(R.string.autoupload_create_new_custom_folder)

            // disable save button
            binding.save.isEnabled = false
        } else {
            binding.localFolderContainer.visibility = View.GONE
        }

        // find/saves UI elements
        mEnabledSwitch = binding.syncEnabled
        viewThemeUtils!!.androidx.colorSwitchCompat(mEnabledSwitch!!)

        mLocalFolderPath = binding.syncedFoldersSettingsLocalFolderPath

        mLocalFolderSummary = binding.localFolderSummary
        mRemoteFolderSummary = binding.remoteFolderSummary

        mUploadOnWifiCheckbox = binding.settingInstantUploadOnWifiCheckbox

        mUploadOnChargingCheckbox = binding.settingInstantUploadOnChargingCheckbox

        mUploadExistingCheckbox = binding.settingInstantUploadExistingCheckbox

        mUploadUseSubfoldersCheckbox = binding.settingInstantUploadPathUseSubfoldersCheckbox

        viewThemeUtils!!.platform.themeCheckbox(
            mUploadOnWifiCheckbox!!,
            mUploadOnChargingCheckbox!!,
            mUploadExistingCheckbox!!,
            mUploadUseSubfoldersCheckbox!!
        )

        mUploadBehaviorSummary = binding.settingInstantBehaviourSummary

        mNameCollisionPolicySummary = binding.settingInstantNameCollisionPolicySummary

        mUploadDelaySummary = binding.settingInstantUploadDelaySummary

        mCancel = binding.cancel
        mSave = binding.save

        viewThemeUtils!!.platform.colorTextButtons(mCancel!!, mSave!!)

        // Set values
        setEnabled(mSyncedFolder!!.isEnabled)

        if (!TextUtils.isEmpty(mSyncedFolder!!.localPath)) {
            mLocalFolderPath!!.text = DisplayUtils.createTextWithSpan(
                String.format(
                    getString(R.string.synced_folders_preferences_folder_path),
                    mSyncedFolder!!.localPath
                ),
                mSyncedFolder!!.folderName,
                StyleSpan(Typeface.BOLD)
            )
            mLocalFolderSummary!!.text = FileStorageUtils.pathToUserFriendlyDisplay(
                mSyncedFolder!!.localPath,
                activity,
                resources
            )
        } else {
            mLocalFolderSummary!!.setText(R.string.choose_local_folder)
        }

        if (!TextUtils.isEmpty(mSyncedFolder!!.localPath)) {
            mRemoteFolderSummary!!.text = mSyncedFolder!!.remotePath
        } else {
            mRemoteFolderSummary!!.setText(R.string.choose_remote_folder)
        }

        mUploadOnWifiCheckbox!!.isChecked = mSyncedFolder!!.isWifiOnly
        mUploadOnChargingCheckbox!!.isChecked = mSyncedFolder!!.isChargingOnly

        mUploadExistingCheckbox!!.isChecked = mSyncedFolder!!.isExisting
        mUploadUseSubfoldersCheckbox!!.isChecked = mSyncedFolder!!.isSubfolderByDate

        mUploadBehaviorSummary!!.text = mUploadBehaviorItemStrings[mSyncedFolder!!.uploadActionInteger]

        val nameCollisionPolicyIndex =
            getSelectionIndexForNameCollisionPolicy(mSyncedFolder!!.nameCollisionPolicy)
        mNameCollisionPolicySummary!!.text = mNameCollisionPolicyItemStrings[nameCollisionPolicyIndex]

        mUploadDelaySummary!!.text = getDelaySummary(mSyncedFolder!!.uploadDelayTimeMs)
    }

    /**
     * set correct icon/flag.
     *
     * @param enabled if enabled or disabled
     */
    private fun setEnabled(enabled: Boolean) {
        mSyncedFolder!!.isEnabled = enabled
        mEnabledSwitch!!.isChecked = enabled

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
        mSyncedFolder!!.remotePath = path
        mRemoteFolderSummary!!.text = path
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
        mSyncedFolder!!.localPath = path
        mLocalFolderSummary!!.text = FileStorageUtils.pathToUserFriendlyDisplay(path, activity, resources)
        mLocalFolderPath!!.text = DisplayUtils.createTextWithSpan(
            String.format(
                getString(R.string.synced_folders_preferences_folder_path),
                mSyncedFolder!!.localPath
            ),
            File(mSyncedFolder!!.localPath).name,
            StyleSpan(Typeface.BOLD)
        )
        checkAndUpdateSaveButtonState()
    }

    private fun checkAndUpdateSaveButtonState() {
        if (mSyncedFolder!!.localPath != null && mSyncedFolder!!.remotePath != null) {
            binding!!.save.isEnabled = true
        } else {
            binding!!.save.isEnabled = false
        }

        checkWritableFolder()
    }

    private fun checkWritableFolder() {
        if (!mSyncedFolder!!.isEnabled) {
            binding!!.settingInstantBehaviourContainer.isEnabled = false
            binding!!.settingInstantBehaviourContainer.alpha = alphaDisabled
            return
        }

        if (mSyncedFolder!!.localPath != null && File(mSyncedFolder!!.localPath).canWrite()) {
            binding!!.settingInstantBehaviourContainer.isEnabled = true
            binding!!.settingInstantBehaviourContainer.alpha = alphaEnabled
            mUploadBehaviorSummary!!.text = mUploadBehaviorItemStrings[mSyncedFolder!!.uploadActionInteger]
        } else {
            binding!!.settingInstantBehaviourContainer.isEnabled = false
            binding!!.settingInstantBehaviourContainer.alpha = alphaDisabled

            mSyncedFolder!!.setUploadAction(
                resources.getTextArray(R.array.pref_behaviour_entryValues)[0].toString()
            )

            mUploadBehaviorSummary!!.setText(R.string.auto_upload_file_behaviour_kept_in_folder)
        }
    }

    private fun setupViews(binding: SyncedFoldersSettingsLayoutBinding?, enable: Boolean) {
        val alpha = if (enable) {
            alphaEnabled
        } else {
            alphaDisabled
        }
        binding!!.settingInstantUploadOnWifiContainer.isEnabled = enable
        binding.settingInstantUploadOnWifiContainer.alpha = alpha

        binding.settingInstantUploadOnChargingContainer.isEnabled = enable
        binding.settingInstantUploadOnChargingContainer.alpha = alpha

        binding.settingInstantUploadExistingContainer.isEnabled = enable
        binding.settingInstantUploadExistingContainer.alpha = alpha

        binding.settingInstantUploadPathUseSubfoldersContainer.isEnabled = enable
        binding.settingInstantUploadPathUseSubfoldersContainer.alpha = alpha

        binding.remoteFolderContainer.isEnabled = enable
        binding.remoteFolderContainer.alpha = alpha

        binding.localFolderContainer.isEnabled = enable
        binding.localFolderContainer.alpha = alpha

        binding.settingInstantNameCollisionPolicyContainer.isEnabled = enable
        binding.settingInstantNameCollisionPolicyContainer.alpha = alpha

        binding.settingInstantUploadDelayContainer.isEnabled = enable
        binding.settingInstantUploadDelayContainer.alpha = alpha

        mUploadOnWifiCheckbox!!.isEnabled = enable
        mUploadOnChargingCheckbox!!.isEnabled = enable
        mUploadExistingCheckbox!!.isEnabled = enable
        mUploadUseSubfoldersCheckbox!!.isEnabled = enable

        checkWritableFolder()
    }

    /**
     * setup all listeners.
     *
     * @param binding the parent binding
     */
    private fun setupListeners(binding: SyncedFoldersSettingsLayoutBinding) {
        mSave!!.setOnClickListener(OnSyncedFolderSaveClickListener())
        mCancel!!.setOnClickListener(OnSyncedFolderCancelClickListener())
        binding.delete.setOnClickListener(OnSyncedFolderDeleteClickListener())

        binding.settingInstantUploadOnWifiContainer.setOnClickListener {
            mSyncedFolder!!.isWifiOnly = !mSyncedFolder!!.isWifiOnly
            mUploadOnWifiCheckbox!!.toggle()
        }

        binding.settingInstantUploadOnChargingContainer.setOnClickListener {
            mSyncedFolder!!.isChargingOnly = !mSyncedFolder!!.isChargingOnly
            mUploadOnChargingCheckbox!!.toggle()
        }

        binding.settingInstantUploadExistingContainer.setOnClickListener {
            mSyncedFolder!!.isExisting = !mSyncedFolder!!.isExisting
            mUploadExistingCheckbox!!.toggle()
        }

        binding.settingInstantUploadPathUseSubfoldersContainer.setOnClickListener {
            mSyncedFolder!!.isSubfolderByDate = !mSyncedFolder!!.isSubfolderByDate
            mUploadUseSubfoldersCheckbox!!.toggle()
        }

        binding.remoteFolderContainer.setOnClickListener { v: View? ->
            val action = Intent(activity, FolderPickerActivity::class.java)
            activity!!.startActivityForResult(action, REQUEST_CODE__SELECT_REMOTE_FOLDER)
        }

        binding.localFolderContainer.setOnClickListener { v: View? ->
            val action = Intent(activity, UploadFilesActivity::class.java)
            action.putExtra(UploadFilesActivity.KEY_LOCAL_FOLDER_PICKER_MODE, true)
            action.putExtra(UploadFilesActivity.REQUEST_CODE_KEY, REQUEST_CODE__SELECT_LOCAL_FOLDER)
            activity!!.startActivityForResult(action, REQUEST_CODE__SELECT_LOCAL_FOLDER)
        }

        binding.syncEnabled.setOnClickListener { setEnabled(!mSyncedFolder!!.isEnabled) }

        binding.settingInstantBehaviourContainer.setOnClickListener { showBehaviourDialog() }

        binding.settingInstantNameCollisionPolicyContainer.setOnClickListener { showNameCollisionPolicyDialog() }

        binding.settingInstantUploadDelayContainer.setOnClickListener { showUploadDelayDialog() }
    }

    private fun showBehaviourDialog() {
        val builder = MaterialAlertDialogBuilder(activity!!)
        builder.setTitle(R.string.prefs_instant_behaviour_dialogTitle)
            .setSingleChoiceItems(
                resources.getTextArray(R.array.pref_behaviour_entries),
                mSyncedFolder!!.uploadActionInteger
            ) { dialog, which ->
                mSyncedFolder!!.setUploadAction(
                    resources.getTextArray(
                        R.array.pref_behaviour_entryValues
                    )[which].toString()
                )
                mUploadBehaviorSummary!!.text = mUploadBehaviorItemStrings[which]
                behaviourDialogShown = false
                dialog.dismiss()
            }
            .setOnCancelListener { behaviourDialogShown = false }
        behaviourDialogShown = true

        viewThemeUtils!!.dialog.colorMaterialAlertDialogBackground(activity!!, builder)

        behaviourDialog = builder.create()
        behaviourDialog!!.show()
    }

    private fun showNameCollisionPolicyDialog() {
        val builder = MaterialAlertDialogBuilder(activity!!)

        builder.setTitle(R.string.pref_instant_name_collision_policy_dialogTitle)
            .setSingleChoiceItems(
                resources.getTextArray(R.array.pref_name_collision_policy_entries),
                getSelectionIndexForNameCollisionPolicy(mSyncedFolder!!.nameCollisionPolicy),
                OnNameCollisionDialogClickListener()
            )
            .setOnCancelListener { dialog: DialogInterface? -> nameCollisionPolicyDialogShown = false }

        nameCollisionPolicyDialogShown = true

        viewThemeUtils!!.dialog.colorMaterialAlertDialogBackground(activity!!, builder)

        behaviourDialog = builder.create()
        behaviourDialog!!.show()
    }

    private fun showUploadDelayDialog() {
        val dialog = newInstance(
            mSyncedFolder!!.uploadDelayTimeMs,
            getString(R.string.pref_instant_upload_delay_dialogTitle),
            getString(R.string.pref_instant_upload_delay_hint)
        )

        dialog.setListener(DurationPickerDialogFragment.Listener { resultCode: Int, duration: Long ->
            if (resultCode == Activity.RESULT_OK) {
                mSyncedFolder!!.uploadDelayTimeMs = duration
                mUploadDelaySummary!!.text = getDelaySummary(duration)
            }
            dialog.dismiss()
        })
        dialog.show(parentFragmentManager, "UPLOAD_DELAY_PICKER_DIALOG")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is $savedInstanceState")

        binding = SyncedFoldersSettingsLayoutBinding.inflate(requireActivity().layoutInflater, null, false)

        setupDialogElements(binding!!)
        setupListeners(binding!!)

        val builder = MaterialAlertDialogBuilder(binding!!.getRoot().context)
        builder.setView(binding!!.getRoot())

        viewThemeUtils!!.dialog.colorMaterialAlertDialogBackground(binding!!.getRoot().context, builder)

        return builder.create()
    }

    override fun onDestroyView() {
        Log_OC.d(TAG, "destroy SyncedFolderPreferencesDialogFragment view")
        if (dialog != null && retainInstance) {
            dialog!!.setDismissMessage(null)
        }

        if (behaviourDialog != null && behaviourDialog!!.isShowing) {
            behaviourDialog!!.dismiss()
        }

        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(BEHAVIOUR_DIALOG_STATE, behaviourDialogShown)
        outState.putBoolean(NAME_COLLISION_POLICY_DIALOG_STATE, nameCollisionPolicyDialogShown)

        super.onSaveInstanceState(outState)
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
            (activity as OnSyncedFolderPreferenceListener?)!!.onSaveSyncedFolderPreference(mSyncedFolder)
        }
    }

    private inner class OnSyncedFolderCancelClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            dismiss()
            (activity as OnSyncedFolderPreferenceListener?)!!.onCancelSyncedFolderPreference()
        }
    }

    private inner class OnSyncedFolderDeleteClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            dismiss()
            (activity as OnSyncedFolderPreferenceListener?)!!.onDeleteSyncedFolderPreference(mSyncedFolder)
        }
    }

    private inner class OnNameCollisionDialogClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            mSyncedFolder!!.nameCollisionPolicy =
                getNameCollisionPolicyForSelectionIndex(which)

            mNameCollisionPolicySummary!!.text = mNameCollisionPolicyItemStrings[which]
            nameCollisionPolicyDialogShown = false
            dialog.dismiss()
        }
    }

    private fun getDelaySummary(duration: Long): String {
        if (duration == 0L) {
            return getString(R.string.pref_instant_upload_delay_disabled)
        }
        val durationParts = getDurationParts(duration)
        val durationSummary = StringBuilder()
        if (durationParts.days > 0) {
            durationSummary.append(durationParts.days)
            durationSummary.append(getString(R.string.common_days_short))
            durationSummary.append(' ')
        }
        if (durationParts.hours > 0) {
            durationSummary.append(durationParts.hours)
            durationSummary.append(getString(R.string.common_hours_short))
            durationSummary.append(' ')
        }
        if (durationParts.minutes > 0) {
            durationSummary.append(durationParts.minutes)
            durationSummary.append(getString(R.string.common_minutes_short))
        }
        return getString(R.string.pref_instant_upload_delay_enabled, durationSummary.toString().trim { it <= ' ' })
    }

    companion object {
        const val SYNCED_FOLDER_PARCELABLE: String = "SyncedFolderParcelable"
        const val REQUEST_CODE__SELECT_REMOTE_FOLDER: Int = 0
        const val REQUEST_CODE__SELECT_LOCAL_FOLDER: Int = 1

        private val TAG: String = SyncedFolderPreferencesDialogFragment::class.java.simpleName
        private const val BEHAVIOUR_DIALOG_STATE = "BEHAVIOUR_DIALOG_STATE"
        private const val NAME_COLLISION_POLICY_DIALOG_STATE = "NAME_COLLISION_POLICY_DIALOG_STATE"
        private const val alphaEnabled = 1.0f
        private const val alphaDisabled = 0.7f

        fun newInstance(syncedFolder: SyncedFolderDisplayItem?, section: Int): SyncedFolderPreferencesDialogFragment {
            requireNotNull(syncedFolder) { "SyncedFolder is mandatory but NULL!" }

            val args = Bundle()
            args.putParcelable(SYNCED_FOLDER_PARCELABLE, SyncedFolderParcelable(syncedFolder, section))

            val dialogFragment = SyncedFolderPreferencesDialogFragment()
            dialogFragment.arguments = args
            dialogFragment.setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog)

            return dialogFragment
        }

        /**
         * Get index for name collision selection dialog.
         *
         * @return 0 if ASK_USER, 1 if OVERWRITE, 2 if RENAME, 3 if SKIP, Otherwise: 0
         */
        private fun getSelectionIndexForNameCollisionPolicy(nameCollisionPolicy: NameCollisionPolicy): Int {
            return when (nameCollisionPolicy) {
                NameCollisionPolicy.OVERWRITE -> 1
                NameCollisionPolicy.RENAME -> 2
                NameCollisionPolicy.CANCEL -> 3
                NameCollisionPolicy.ASK_USER -> 0
                else -> 0
            }
        }

        /**
         * Get index for name collision selection dialog. Inverse of getSelectionIndexForNameCollisionPolicy.
         *
         * @return ASK_USER if 0, OVERWRITE if 1, RENAME if 2, SKIP if 3. Otherwise: ASK_USER
         */
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
