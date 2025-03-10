/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 ZetaTom <70907959+ZetaTom@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogDataStorageLocationBinding
import com.owncloud.android.datastorage.DataStorageProvider
import com.owncloud.android.datastorage.StoragePoint
import com.owncloud.android.datastorage.StoragePoint.PrivacyType
import com.owncloud.android.datastorage.StoragePoint.StorageType
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import javax.inject.Inject

class ChooseStorageLocationDialogFragment : DialogFragment(), Injectable {

    private lateinit var binding: DialogDataStorageLocationBinding

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private val storagePoints = DataStorageProvider.getInstance().availableStoragePoints

    private val selectedStorageType
        get() = if (!binding.storageExternalRadio.isChecked) StorageType.INTERNAL else StorageType.EXTERNAL
    private val selectedPrivacyType
        get() = if (binding.allowMediaIndexSwitch.isChecked) PrivacyType.PUBLIC else PrivacyType.PRIVATE

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogDataStorageLocationBinding.inflate(layoutInflater)

        viewThemeUtils.material.colorMaterialSwitch(binding.allowMediaIndexSwitch)
        viewThemeUtils.platform.themeRadioButton(binding.storageInternalRadio)
        viewThemeUtils.platform.themeRadioButton(binding.storageExternalRadio)

        val builder = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.storage_choose_location)
            .setPositiveButton(R.string.common_ok) { dialog: DialogInterface, _ ->
                notifyResult()
                dialog.dismiss()
            }.setView(binding.root)

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(requireContext(), builder)

        binding.storageRadioGroup.setOnCheckedChangeListener { _, _ ->
            updateMediaIndexSwitch()
        }

        binding.allowMediaIndexSwitch.setOnCheckedChangeListener { _, _ ->
            updateStorageTypeSelection()
        }

        return builder.create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupLocationSelection()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

    private fun setupLocationSelection() {
        updateStorageTypeSelection()
        val currentStorageLocation = getCurrentStorageLocation() ?: return

        val radioButton = when (currentStorageLocation.storageType) {
            StorageType.EXTERNAL -> binding.storageExternalRadio
            else -> binding.storageInternalRadio
        }

        radioButton.isChecked = true
        updateMediaIndexSwitch()
    }

    private fun getStoragePointLabel(storageType: StorageType, privacyType: PrivacyType): String {
        val typeString = when (storageType) {
            StorageType.INTERNAL -> getString(R.string.storage_internal_storage)
            StorageType.EXTERNAL -> getString(R.string.storage_external_storage)
        }

        val storagePath =
            storagePoints.find { it.storageType == storageType && it.privacyType == privacyType }?.path

        return storagePath?.let {
            val file = File(it)
            val totalSpace = file.totalSpace
            val usedSpace = totalSpace - file.freeSpace
            return String.format(
                getString(R.string.file_migration_free_space),
                typeString,
                DisplayUtils.bytesToHumanReadable(usedSpace),
                DisplayUtils.bytesToHumanReadable(totalSpace)
            )
        } ?: typeString
    }

    private fun updateMediaIndexSwitch() {
        val privacyTypes =
            storagePoints.filter { it.storageType == selectedStorageType }.map { it.privacyType }.distinct()
        binding.allowMediaIndexSwitch.isEnabled = privacyTypes.size > 1
        binding.allowMediaIndexSwitch.isChecked = privacyTypes.contains(PrivacyType.PUBLIC)
    }

    private fun updateStorageTypeSelection() {
        val hasInternalStorage = storagePoints.any { it.storageType == StorageType.INTERNAL }
        val hasExternalStorage = storagePoints.any { it.storageType == StorageType.EXTERNAL }

        binding.storageInternalRadio.isEnabled = hasInternalStorage
        binding.storageInternalRadio.text = getStoragePointLabel(StorageType.INTERNAL, selectedPrivacyType)

        binding.storageExternalRadio.isEnabled = hasExternalStorage
        binding.storageExternalRadio.text = getStoragePointLabel(StorageType.EXTERNAL, selectedPrivacyType)
    }

    private fun getCurrentStorageLocation(): StoragePoint? {
        val appContext = MainApp.getAppContext()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        val storagePath = sharedPreferences.getString(AppPreferencesImpl.STORAGE_PATH, appContext.filesDir.absolutePath)
        return storagePoints.find { it.path == storagePath }
    }

    private fun notifyResult() {
        val newPath =
            storagePoints.find { it.storageType == selectedStorageType && it.privacyType == selectedPrivacyType }
                ?: return

        val resultBundle = Bundle().apply {
            putString(KEY_RESULT_STORAGE_LOCATION, newPath.path)
        }

        parentFragmentManager.setFragmentResult(KEY_RESULT_STORAGE_LOCATION, resultBundle)
    }

    companion object {
        const val KEY_RESULT_STORAGE_LOCATION = "KEY_RESULT_STORAGE_LOCATION"
        const val STORAGE_LOCATION_RESULT_CODE = 100

        @JvmStatic
        fun newInstance() = ChooseStorageLocationDialogFragment()

        @JvmStatic
        val TAG: String = Companion::class.java.simpleName
    }
}
