/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@nextcloud.com>
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import androidx.core.view.isNotEmpty
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.documentscan.AppScanOptionalFeature
import com.nextcloud.utils.BuildHelper.isFlavourGPlay
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.extensions.isNetworkAndServerAvailableSuspended
import com.nextcloud.utils.extensions.isTemplateAvailable
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.FileListActionsBottomSheetCreatorBoxBinding
import com.owncloud.android.databinding.FileListActionsBottomSheetCreatorRowBinding
import com.owncloud.android.databinding.FileListActionsBottomSheetFragmentBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.DirectEditing
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OCFileListBottomSheetDialog(
    private val fileActivity: FileActivity,
    private val actions: OCFileListBottomSheetActions,
    private val deviceInfo: DeviceInfo,
    private val user: User,
    private val file: OCFile,
    private val viewThemeUtils: ViewThemeUtils,
    private val editorUtils: EditorUtils,
    private val appScanOptionalFeature: AppScanOptionalFeature
) : BottomSheetDialog(fileActivity),
    Injectable {

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    companion object {
        // Number of items to show in document creators overview
        private const val CREATORS_OVERVIEW_ITEMS = 3
    }

    private val templateActions = listOf(
        CreatorAction.getNewDocument(context, action = {
            actions.newDocument()
            dismiss()
        }),
        CreatorAction.getNewSpreadsheet(context, action = {
            actions.newSpreadsheet()
            dismiss()
        }),
        CreatorAction.getNewPresentation(context, action = {
            actions.newPresentation()
            dismiss()
        })
    )

    private lateinit var binding: FileListActionsBottomSheetFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FileListActionsBottomSheetFragmentBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        applyBranding()

        initCreatorContainer()

        if (!deviceInfo.hasCamera(context)) {
            binding.menuDirectCameraUpload.visibility = View.GONE
        }

        createRichWorkspace()
        setupClickListener()
        filterActionsForOfflineOperations()
        checkCreateEncryptedFolderVisibility()

        if (MainApp.isClientBranded && isFlavourGPlay()) {
            // this way we can have branded clients with that permission
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionUtil.manifestHasAllFilesPermission(context)
            } else {
                true
            }

            if (!hasPermission) {
                binding.menuUploadFiles.visibility = View.GONE
                binding.menuUploadFromApp.text = context.getString(R.string.upload_files)
            }
        }
    }

    private fun checkCreateEncryptedFolderVisibility() {
        fileActivity.capabilities.ifPresent { capabilities ->
            binding.menuEncryptedMkdir.setVisibleIf(!file.isEncrypted && capabilities.endToEndEncryption.isTrue)
        }
    }

    private fun applyBranding() {
        viewThemeUtils.material.run {
            binding.run {
                colorMaterialButtonContent(menuUploadFiles, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuUploadFromApp, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuDirectCameraUpload, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuScanDocUpload, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuMkdir, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuEncryptedMkdir, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuCreateRichWorkspace, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuMoreDocuments, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuBack, ColorRole.PRIMARY)

                viewThemeUtils.platform.tintDrawable(
                    context,
                    binding.menuMoreDocumentsExpand.drawable,
                    ColorRole.PRIMARY
                )
            }
        }

        viewThemeUtils.platform.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)

        val textColor = ContextCompat.getColor(context, R.color.text_color)

        binding.run {
            menuUploadFiles.setTextColor(textColor)
            menuUploadFromApp.setTextColor(textColor)
            menuDirectCameraUpload.setTextColor(textColor)
            menuScanDocUpload.setTextColor(textColor)
            menuMkdir.setTextColor(textColor)
            menuEncryptedMkdir.setTextColor(textColor)
            menuCreateRichWorkspace.setTextColor(textColor)
            menuMoreDocuments.setTextColor(textColor)
        }
    }

    @Suppress("DEPRECATION", "LongMethod", "MagicNumber")
    private fun initCreatorContainer() {
        if (file.isEncrypted) {
            return
        }

        // Create a list of supported creators, in the order to be shown (direct editing, then collabora)
        val creatorsActions = ArrayList<CreatorAction>()

        // Check direct editing
        val directEditing = creatorsActionsFromDirectEditing()

        // Check capabilities (e.g. collabora)
        val capabilities = creatorsActionsFromCollabora()

        // First Direct Editing Office (if any)
        creatorsActions.addAll(directEditing.filter { it.type == CreatorType.Office })
        // Then Capabilities Office (if any). Capabilities entry are all type office, no need to filter.
        creatorsActions.addAll(capabilities)
        // Then all the rest
        creatorsActions.addAll(directEditing.filter { it.type != CreatorType.Office })

        displayCreatorsActions(creatorsActions)
    }

    private fun displayCreatorsActions(creatorsActions: List<CreatorAction>) {
        if (creatorsActions.isEmpty()) {
            // If no creators at all, hide whole container (comprising separator)
            binding.creatorsOverviewContainer.visibility = View.GONE
            return
        }

        binding.creatorsOverviewContainer.visibility = View.VISIBLE

        with(binding) {
            creatorsOverview.removeAllViews()
            creators.removeAllViews()

            creatorsActions.forEachIndexed { index, action ->
                if (creatorsActions.size == 1) {
                    // Single element is shown as row in the overview
                    creatorsOverview.addView(buildCreatorButton(action, false, creatorsOverview))
                } else if (index < CREATORS_OVERVIEW_ITEMS) {
                    // First x elements shown as boxes in the overview
                    creatorsOverview.addView(buildCreatorButton(action, true, creatorsOverview))
                } else {
                    // Other elements shown as rows in the overflow view
                    creators.addView(buildCreatorButton(action, false, creators))
                }
            }

            if (creatorsOverview.isNotEmpty()) {
                creatorsOverview.visibility = View.VISIBLE
            }

            menuMoreDocumentsContainer.visibility = if (creators.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun creatorsActionsFromDirectEditing(): List<CreatorAction> {
        val creatorsActions = ArrayList<CreatorAction>()
        arbitraryDataProvider
            .getValue(user, ArbitraryDataProvider.DIRECT_EDITING)
            .takeIf(String::isNotEmpty)
            ?.let { Gson().fromJson(it, DirectEditing::class.java) }
            ?.creators
            ?.values
            ?.forEach { creator ->
                creatorsActions.add(CreatorAction.getAction(context, viewThemeUtils, creator, action = {
                    actions.showTemplate(creator, creator.name)
                    dismiss()
                }))
            }
        return creatorsActions
    }

    private fun creatorsActionsFromCollabora(): List<CreatorAction> {
        // richdocuments 11+ serves its creators through DirectEditing, so the legacy entries would be duplicates.
        if (editorUtils.isRichDocumentsDirectEditingAvailable(user)) {
            return emptyList()
        }

        val creatorsActions = ArrayList<CreatorAction>()

        fileActivity.capabilities
            .filter { it.isTemplateAvailable() }
            .ifPresent { creatorsActions.addAll(templateActions) }

        return creatorsActions
    }

    /**
     * @param creatorAction Action for which the button is created
     * @param showAsBox If true, creates a squarish view with weight to be displayed horizontally, otherwise a row
     * @param parent Container the button is added to, used to resolve its layout params
     */
    private fun buildCreatorButton(
        creatorAction: CreatorAction,
        showAsBox: Boolean,
        parent: ViewGroup
    ): MaterialButton {
        val creatorButton = if (showAsBox) {
            FileListActionsBottomSheetCreatorBoxBinding.inflate(layoutInflater, parent, false).root
        } else {
            FileListActionsBottomSheetCreatorRowBinding.inflate(layoutInflater, parent, false).root
        }

        return creatorButton.apply {
            text = creatorAction.text
            icon = creatorAction.icon
            setOnClickListener { creatorAction.action() }
        }
    }

    private fun createRichWorkspace() {
        if (editorUtils.isEditorAvailable(user, MimeType.TEXT_MARKDOWN) && !file.isEncrypted) {
            // richWorkspace
            // == "": no info set -> show button
            // == null: disabled on server side -> hide button
            // != "": info set -> hide button
            if (file.richWorkspace == null || "" != file.richWorkspace) {
                binding.menuCreateRichWorkspace.visibility = View.GONE
            } else {
                binding.menuCreateRichWorkspace.visibility = View.VISIBLE
            }
        } else {
            binding.menuCreateRichWorkspace.visibility = View.GONE
        }
    }

    private fun setupClickListener() {
        binding.run {
            menuCreateRichWorkspace.setOnClickListener {
                actions.createRichWorkspace()
                dismiss()
            }

            menuMkdir.setOnClickListener {
                actions.createFolder(encrypted = false)
                dismiss()
            }

            menuEncryptedMkdir.setOnClickListener {
                actions.createFolder(encrypted = true)
                dismiss()
            }

            menuUploadFromApp.setOnClickListener {
                actions.uploadFromApp()
                dismiss()
            }

            menuDirectCameraUpload.setOnClickListener {
                actions.directCameraUpload()
                dismiss()
            }

            if (appScanOptionalFeature.isAvailable) {
                menuScanDocUpload.setOnClickListener {
                    actions.scanDocUpload()
                    dismiss()
                }
            } else if (actions.isScanDocUploadFromAppAvailable) {
                menuScanDocUpload.setOnClickListener {
                    actions.scanDocUploadFromApp()
                    dismiss()
                }
            } else {
                menuScanDocUpload.visibility = View.GONE
                menuScanDocUploadDivider.visibility = View.GONE
            }

            menuUploadFiles.setOnClickListener {
                actions.uploadFiles()
                dismiss()
            }

            menuMoreDocuments.setOnClickListener {
                bottomSheetViewSwitcher.showNext()
            }

            menuBack.setOnClickListener {
                // Invert animation
                bottomSheetViewSwitcher.showPrevious()
            }
        }
    }

    private fun filterActionsForOfflineOperations() {
        lifecycleScope.launch {
            val available = fileActivity.connectivityService.isNetworkAndServerAvailableSuspended()
            if (available && (!file.isOfflineOperation || file.isRootDirectory)) {
                return@launch
            }
            hideCreationOptions()
        }
    }

    private suspend fun hideCreationOptions() = withContext(Dispatchers.Main) {
        binding.run {
            menuCreateRichWorkspace.visibility = View.GONE
            creatorsOverviewContainer.visibility = View.GONE
            menuEncryptedMkdir.visibility = View.GONE
        }
    }
}
