/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 ZetaTom <70907959+ZetaTom@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.dialog.chooseStorage

import android.app.Dialog
import android.os.Bundle
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.utils.HumanReadableFormatter
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogDataStorageLocationBinding
import com.owncloud.android.datastorage.DataStorageProvider
import com.owncloud.android.datastorage.StoragePoint
import com.owncloud.android.datastorage.StoragePoint.PrivacyType
import com.owncloud.android.datastorage.StoragePoint.StorageType
import com.owncloud.android.ui.dialog.extensions.themeButtons
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import javax.inject.Inject

class ChooseStorageLocationDialogFragment :
    DialogFragment(),
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: DialogDataStorageLocationBinding

    private val storageProvider = DataStorageProvider.instance
    private val storagePoints = storageProvider.availableStoragePoints.toList()
    private var currentLocation: StoragePoint? = null
    private var selectedStorageType: StorageType? = null
    private var allowAccessFromOtherApps = true

    private val selectablePrivacyTypes: Set<PrivacyType>
        get() = storagePoints
            .filter { it.storageType == selectedStorageType }
            .map { it.privacyType }
            .toSet()

    private val selectedPrivacyType: PrivacyType?
        get() {
            val privacyTypes = selectablePrivacyTypes
            if (privacyTypes.size > 1) {
                return if (allowAccessFromOtherApps) PrivacyType.PUBLIC else PrivacyType.PRIVATE
            }

            return privacyTypes.firstOrNull()
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDataStorageLocationBinding.inflate(layoutInflater)
        currentLocation = storageProvider.findStoragePoint(readStoragePath())
        restoreSelection(savedInstanceState)

        viewThemeUtils.material.colorMaterialSwitch(binding.allowMediaIndexSwitch)
        viewThemeUtils.platform.themeRadioButton(binding.storageInternalRadio)
        viewThemeUtils.platform.themeRadioButton(binding.storageExternalRadio)

        render()
        setupListeners()

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.storage_choose_location)
            .setView(binding.root)
            .setPositiveButton(R.string.common_ok) { _, _ -> notifyResult() }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)

        return builder.create()
    }

    override fun onStart() {
        super.onStart()

        dialog?.themeButtons(viewThemeUtils)
        updatePositiveButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(KEY_STORAGE_TYPE, selectedStorageType?.name)
        outState.putBoolean(KEY_ALLOW_ACCESS, allowAccessFromOtherApps)
    }

    private fun readStoragePath(): String {
        val context = requireContext()
        val defaultPath = context.filesDir.absolutePath

        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(AppPreferencesImpl.STORAGE_PATH, defaultPath) ?: defaultPath
    }

    private fun restoreSelection(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            selectedStorageType = savedInstanceState.getString(KEY_STORAGE_TYPE)?.let(StorageType::valueOf)
            allowAccessFromOtherApps = savedInstanceState.getBoolean(KEY_ALLOW_ACCESS, true)
            return
        }

        val location = currentLocation ?: return
        selectedStorageType = location.storageType.takeIf(::isAvailable)

        allowAccessFromOtherApps = location.privacyType == PrivacyType.PUBLIC
    }

    private fun setupListeners() {
        binding.storageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedStorageType = when (checkedId) {
                R.id.storage_internal_radio -> StorageType.INTERNAL
                R.id.storage_external_radio -> StorageType.EXTERNAL
                else -> null
            }

            renderAccessSwitch()
            updatePositiveButton()
        }

        binding.allowMediaIndexSwitch.setOnCheckedChangeListener { switch, isChecked ->
            if (switch.isEnabled) {
                allowAccessFromOtherApps = isChecked
            }
        }
    }

    private fun render() {
        renderStorageOption(binding.storageInternalRadio, StorageType.INTERNAL)
        renderStorageOption(binding.storageExternalRadio, StorageType.EXTERNAL)

        when (selectedStorageType) {
            StorageType.INTERNAL -> binding.storageRadioGroup.check(R.id.storage_internal_radio)
            StorageType.EXTERNAL -> binding.storageRadioGroup.check(R.id.storage_external_radio)
            null -> binding.storageRadioGroup.clearCheck()
        }

        renderAccessSwitch()
    }

    private fun renderStorageOption(radioButton: RadioButton, storageType: StorageType) {
        val storagePoint = storagePoints.firstOrNull { it.storageType == storageType }

        radioButton.isEnabled = storagePoint != null
        radioButton.text = storagePoint?.let { createStorageLabel(storageType, it) }
            ?: getString(storageType.labelResId)
    }

    private fun createStorageLabel(storageType: StorageType, storagePoint: StoragePoint): String {
        val volume = File(storagePoint.path)
        val totalSpace = volume.totalSpace
        val usedSpace = totalSpace - volume.freeSpace

        return getString(
            R.string.file_migration_free_space,
            getString(storageType.labelResId),
            HumanReadableFormatter.formatBytes(usedSpace),
            HumanReadableFormatter.formatBytes(totalSpace)
        )
    }

    private fun renderAccessSwitch() {
        binding.allowMediaIndexSwitch.isEnabled = selectablePrivacyTypes.size > 1
        binding.allowMediaIndexSwitch.isChecked = selectedPrivacyType == PrivacyType.PUBLIC
    }

    private fun updatePositiveButton() {
        val positiveButton = (dialog as? AlertDialog)?.getButton(AlertDialog.BUTTON_POSITIVE) ?: return

        positiveButton.isEnabled = selectedStorageType != null
    }

    private fun isAvailable(storageType: StorageType): Boolean = storagePoints.any { it.storageType == storageType }

    private fun notifyResult() {
        val storageType = selectedStorageType ?: return
        val privacyType = selectedPrivacyType ?: return
        val path = resolveSelectedPath(storageType, privacyType) ?: return

        parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(RESULT_STORAGE_PATH to path))
    }

    private fun resolveSelectedPath(storageType: StorageType, privacyType: PrivacyType): String? {
        val location = currentLocation
        if (location != null && location.storageType == storageType && location.privacyType == privacyType) {
            return location.path
        }

        return storagePoints.firstOrNull { it.storageType == storageType && it.privacyType == privacyType }?.path
    }

    companion object {
        const val TAG = "choose_storage_location"
        const val REQUEST_KEY = "choose_storage_location_request"
        const val RESULT_STORAGE_PATH = "storage_path"

        private const val KEY_STORAGE_TYPE = "storage_type"
        private const val KEY_ALLOW_ACCESS = "allow_access"
    }
}
