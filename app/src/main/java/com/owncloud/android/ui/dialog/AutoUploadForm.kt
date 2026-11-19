/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import com.nextcloud.client.core.Clock
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.text.SpanFormatter
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogAutoUploadPreferenceBinding
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.ui.dialog.extensions.nameCollisionPolicyForDialogSelectionIndex
import com.owncloud.android.ui.dialog.extensions.toDialogSelectionIndex
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.FileUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

class AutoUploadForm(
    private val binding: DialogAutoUploadPreferenceBinding,
    private val syncedFolder: SyncedFolderParcelable,
    private val clock: Clock,
    private val lifecycle: Lifecycle
) {

    private val context = binding.root.context

    val uploadBehaviourEntries: Array<CharSequence> = context.resources.getTextArray(R.array.pref_behaviour_entries)
    val nameCollisionPolicyEntries: Array<CharSequence> =
        context.resources.getTextArray(R.array.pref_name_collision_policy_entries)
    private val uploadBehaviourEntryValues = context.resources.getTextArray(R.array.pref_behaviour_entryValues)

    private val isMediaFolder: Boolean
        get() = syncedFolder.type != MediaFolderType.CUSTOM

    private val isNewCustomFolder: Boolean
        get() = !isMediaFolder && syncedFolder.id == SyncedFolder.UNPERSISTED_ID

    val canDelete: Boolean
        get() = !isMediaFolder && !isNewCustomFolder

    val uploadBehaviourIndex: Int
        get() = syncedFolder.uploadActionInteger

    val nameCollisionPolicyIndex: Int
        get() = syncedFolder.nameCollisionPolicy.toDialogSelectionIndex()

    private val settingContainers: List<View>
        get() = with(binding) {
            listOf(
                settingInstantUploadOnWifiContainer,
                settingInstantUploadOnChargingContainer,
                settingInstantUploadExistingContainer,
                settingInstantUploadPathUseSubfoldersContainer,
                settingInstantUploadExcludeHiddenContainer,
                remoteFolderContainer,
                localFolderContainer,
                settingInstantNameCollisionPolicyContainer
            )
        }

    private val checkboxes: List<CompoundButton>
        get() = with(binding) {
            listOf(
                settingInstantUploadOnWifiCheckbox,
                settingInstantUploadOnChargingCheckbox,
                settingInstantUploadExistingCheckbox,
                settingInstantUploadPathUseSubfoldersCheckbox,
                settingInstantUploadExcludeHiddenCheckbox
            )
        }

    fun setup(viewThemeUtils: ViewThemeUtils) {
        setupLayout()
        applyUserColor(viewThemeUtils)
        render()
        setupValueListeners()
    }

    fun setRemotePath(path: String) {
        syncedFolder.remotePath = path
        binding.remoteFolderSummary.text = path
        updateSaveButtonState()
    }

    fun setLocalPath(path: String) {
        syncedFolder.localPath = path
        renderLocalFolder()
        updateSaveButtonState()
        updateBehaviourAvailability()
    }

    fun selectUploadBehaviour(index: Int) {
        syncedFolder.setUploadAction(uploadBehaviourEntryValues[index].toString())
        binding.settingInstantBehaviourSummary.text = uploadBehaviourEntries[index]
    }

    fun selectNameCollisionPolicy(index: Int) {
        syncedFolder.nameCollisionPolicy = nameCollisionPolicyForDialogSelectionIndex(index)
        binding.settingInstantNameCollisionPolicySummary.text = nameCollisionPolicyEntries[index]
    }

    private fun setupLayout() {
        binding.localFolderContainer.isVisible = isNewCustomFolder
        binding.settingInstantUploadExcludeHiddenContainer.isVisible = !isMediaFolder
        binding.syncEnabled.isVisible = !isNewCustomFolder

        if (isNewCustomFolder) {
            binding.syncedFoldersSettingsTitle.setText(R.string.autoupload_create_new_custom_folder)
        }
    }

    private fun applyUserColor(viewThemeUtils: ViewThemeUtils) {
        viewThemeUtils.androidx.colorSwitchCompat(binding.syncEnabled)
        viewThemeUtils.platform.themeCheckbox(
            binding.settingInstantUploadOnWifiCheckbox,
            binding.settingInstantUploadOnChargingCheckbox,
            binding.settingInstantUploadExistingCheckbox,
            binding.settingInstantUploadPathUseSubfoldersCheckbox,
            binding.settingInstantUploadExcludeHiddenCheckbox
        )
        viewThemeUtils.material.colorMaterialButtonPrimaryTonal(binding.btnPositive)
        viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(binding.btnNegative)
        viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(binding.btnNeutral)
    }

    private fun setupValueListeners() = with(binding) {
        bindCheckbox(settingInstantUploadOnWifiContainer, settingInstantUploadOnWifiCheckbox) {
            syncedFolder.isWifiOnly = it
        }
        bindCheckbox(settingInstantUploadOnChargingContainer, settingInstantUploadOnChargingCheckbox) {
            syncedFolder.isChargingOnly = it
        }
        bindCheckbox(settingInstantUploadExistingContainer, settingInstantUploadExistingCheckbox) {
            syncedFolder.isExisting = it
        }
        bindCheckbox(settingInstantUploadPathUseSubfoldersContainer, settingInstantUploadPathUseSubfoldersCheckbox) {
            syncedFolder.isSubfolderByDate = it
            settingInstantUploadSubfolderRuleContainer.isVisible = it
        }
        bindCheckbox(settingInstantUploadExcludeHiddenContainer, settingInstantUploadExcludeHiddenCheckbox) {
            syncedFolder.isExcludeHidden = it
        }
        settingInstantUploadSubfolderRuleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                syncedFolder.subFolderRule = SubFolderRule.entries[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                syncedFolder.subFolderRule = SubFolderRule.YEAR_MONTH
            }
        }
        syncEnabled.setOnClickListener {
            syncedFolder.setEnabled(!syncedFolder.isEnabled, clock.currentTime)
            renderEnabledState()
        }
    }

    private fun bindCheckbox(container: View, checkbox: CompoundButton, onChanged: (Boolean) -> Unit) {
        container.setOnClickListener {
            checkbox.toggle()
            onChanged(checkbox.isChecked)
        }
    }

    private fun render() {
        renderLocalFolder()

        with(binding) {
            remoteFolderSummary.text = syncedFolder.remotePath ?: context.getString(R.string.choose_remote_folder)
            settingInstantUploadOnWifiCheckbox.isChecked = syncedFolder.isWifiOnly
            settingInstantUploadOnChargingCheckbox.isChecked = syncedFolder.isChargingOnly
            settingInstantUploadExistingCheckbox.isChecked = syncedFolder.isExisting
            settingInstantUploadPathUseSubfoldersCheckbox.isChecked = syncedFolder.isSubfolderByDate
            settingInstantUploadExcludeHiddenCheckbox.isChecked = syncedFolder.isExcludeHidden
            settingInstantUploadSubfolderRuleSpinner.setSelection(syncedFolder.subFolderRule.ordinal)
            settingInstantUploadSubfolderRuleContainer.isVisible = syncedFolder.isSubfolderByDate
            settingInstantBehaviourSummary.text = uploadBehaviourEntries[uploadBehaviourIndex]
            settingInstantNameCollisionPolicySummary.text = nameCollisionPolicyEntries[nameCollisionPolicyIndex]
        }

        renderEnabledState()
        updateSaveButtonState()
    }

    private fun renderLocalFolder() {
        val localPath = syncedFolder.localPath
        if (localPath.isNullOrEmpty()) {
            binding.localFolderSummary.setText(R.string.choose_local_folder)
            return
        }

        binding.syncedFoldersSettingsLocalFolderPath.text = SpanFormatter.styleLast(
            context.getString(R.string.synced_folders_preferences_folder_path, localPath),
            File(localPath).name,
            StyleSpan(Typeface.BOLD)
        )
        binding.localFolderSummary.text =
            FileStorageUtils.pathToUserFriendlyDisplay(localPath, context, context.resources)
    }

    private fun renderEnabledState() {
        val enabled = syncedFolder.isEnabled
        val alpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED

        binding.syncEnabled.isChecked = enabled
        settingContainers.forEach {
            it.isEnabled = enabled
            it.alpha = alpha
        }
        checkboxes.forEach { it.isEnabled = enabled }

        updateBehaviourAvailability()
    }

    private fun updateBehaviourAvailability() {
        if (!syncedFolder.isEnabled) {
            setBehaviourEnabled(false)
            return
        }

        val localFolder = syncedFolder.localPath?.let(::File)
        FileUtil.isFolderWritable(localFolder, lifecycle) { writable ->
            setBehaviourEnabled(writable)

            if (writable) {
                binding.settingInstantBehaviourSummary.text = uploadBehaviourEntries[uploadBehaviourIndex]
                return@isFolderWritable
            }

            syncedFolder.setUploadAction(uploadBehaviourEntryValues[KEPT_IN_FOLDER_BEHAVIOUR_INDEX].toString())
            binding.settingInstantBehaviourSummary.setText(R.string.auto_upload_file_behaviour_kept_in_folder)
        }
    }

    private fun setBehaviourEnabled(enabled: Boolean) {
        binding.settingInstantBehaviourContainer.isEnabled = enabled
        binding.settingInstantBehaviourContainer.alpha = if (enabled) ALPHA_ENABLED else ALPHA_DISABLED
    }

    private fun updateSaveButtonState() {
        binding.btnPositive.isEnabled = syncedFolder.localPath != null && syncedFolder.remotePath != null
    }

    companion object {
        private const val KEPT_IN_FOLDER_BEHAVIOUR_INDEX = 0
        private const val ALPHA_ENABLED = 1.0f
        private const val ALPHA_DISABLED = 0.7f
    }
}
