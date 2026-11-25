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
import androidx.core.view.isNotEmpty
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.device.DeviceInfo
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
import kotlinx.coroutines.launch

class OCFileListBottomSheetDialog(
    private val fileActivity: FileActivity,
    private val actions: OCFileListBottomSheetActions,
    private val deviceInfo: DeviceInfo,
    private val user: User,
    private val file: OCFile,
    private val viewThemeUtils: ViewThemeUtils,
    private val editorUtils: EditorUtils,
    private val appScanOptionalFeature: AppScanOptionalFeature,
    private val arbitraryDataProvider: ArbitraryDataProvider
) : BottomSheetDialog(fileActivity) {

    companion object {
        private const val CREATORS_OVERVIEW_ITEMS = 3
    }

    private lateinit var binding: FileListActionsBottomSheetFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FileListActionsBottomSheetFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyBranding()
        initCreatorContainer()

        if (!deviceInfo.hasCamera(context)) {
            binding.menuDirectCameraUpload.visibility = View.GONE
        }

        updateRichWorkspaceVisibility()
        setupClickListener()
        filterActionsForOfflineOperations()
        checkCreateEncryptedFolderVisibility()
        hideUploadFilesIfAllFilesPermissionMissing()
    }

    private fun hideUploadFilesIfAllFilesPermissionMissing() {
        if (!MainApp.isClientBranded || !isFlavourGPlay()) return

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            PermissionUtil.manifestHasAllFilesPermission(context)
        if (hasPermission) return

        binding.menuUploadFiles.visibility = View.GONE
        binding.menuUploadFromApp.text = context.getString(R.string.upload_files)
    }

    private fun checkCreateEncryptedFolderVisibility() {
        fileActivity.capabilities.ifPresent { capabilities ->
            binding.menuEncryptedMkdir.setVisibleIf(!file.isEncrypted && capabilities.endToEndEncryption.isTrue)
        }
    }

    private fun applyBranding() = with(binding) {
        val textColor = ContextCompat.getColor(context, R.color.text_color)
        val menuItems = listOf(
            menuUploadFiles,
            menuUploadFromApp,
            menuDirectCameraUpload,
            menuScanDocUpload,
            menuMkdir,
            menuEncryptedMkdir,
            menuCreateRichWorkspace,
            menuMoreDocuments
        )

        menuItems.forEach {
            viewThemeUtils.material.colorMaterialButtonContent(it, ColorRole.PRIMARY)
            it.setTextColor(textColor)
        }
        viewThemeUtils.material.colorMaterialButtonContent(menuBack, ColorRole.PRIMARY)
        viewThemeUtils.platform.tintDrawable(context, menuMoreDocumentsExpand.drawable, ColorRole.PRIMARY)

        viewThemeUtils.platform.colorViewBackground(bottomSheet, ColorRole.SURFACE)
    }

    private fun initCreatorContainer() {
        if (file.isEncrypted) return

        val (officeActions, otherActions) = creatorsActionsFromDirectEditing().partition {
            it.type == CreatorType.Office
        }

        displayCreatorsActions(officeActions + creatorsActionsFromCollabora() + otherActions)
    }

    private fun displayCreatorsActions(creatorsActions: List<CreatorAction>) = with(binding) {
        creatorsOverviewContainer.setVisibleIf(creatorsActions.isNotEmpty())
        if (creatorsActions.isEmpty()) return@with

        creatorsOverview.removeAllViews()
        creators.removeAllViews()

        sendToOverview(creatorsActions.take(CREATORS_OVERVIEW_ITEMS))
        sendToMoreDocuments(creatorsActions.drop(CREATORS_OVERVIEW_ITEMS))

        creatorsOverview.visibility = View.VISIBLE
        menuMoreDocumentsContainer.setVisibleIf(creators.isNotEmpty())
    }

    private fun sendToOverview(overviewActions: List<CreatorAction>) {
        val isSingleCreator = overviewActions.size == 1

        overviewActions.forEach { addCreatorButton(binding.creatorsOverview, it, showAsBox = !isSingleCreator) }
    }

    private fun sendToMoreDocuments(moreDocumentsActions: List<CreatorAction>) {
        moreDocumentsActions.forEach { addCreatorButton(binding.creators, it, showAsBox = false) }
    }

    private fun creatorsActionsFromDirectEditing(): List<CreatorAction> = arbitraryDataProvider
        .getValue(user, ArbitraryDataProvider.DIRECT_EDITING)
        .takeIf(String::isNotEmpty)
        ?.let { Gson().fromJson(it, DirectEditing::class.java) }
        ?.creators
        ?.values
        .orEmpty()
        .map { creator ->
            CreatorAction.getAction(context, viewThemeUtils, creator) {
                actions.showTemplate(creator, creator.name)
            }
        }

    private fun creatorsActionsFromCollabora(): List<CreatorAction> {
        // richdocuments 11+ serves its creators through DirectEditing, so the legacy entries would be duplicates.
        if (editorUtils.isRichDocumentsDirectEditingAvailable(user)) return emptyList()
        if (!fileActivity.capabilities.filter { it.isTemplateAvailable() }.isPresent) return emptyList()

        return listOf(
            CreatorAction.getNewDocument(context, actions::newDocument),
            CreatorAction.getNewSpreadsheet(context, actions::newSpreadsheet),
            CreatorAction.getNewPresentation(context, actions::newPresentation)
        )
    }

    private fun addCreatorButton(parent: ViewGroup, creatorAction: CreatorAction, showAsBox: Boolean) {
        val button = if (showAsBox) {
            FileListActionsBottomSheetCreatorBoxBinding.inflate(layoutInflater, parent, true).root
        } else {
            FileListActionsBottomSheetCreatorRowBinding.inflate(layoutInflater, parent, true).root
        }

        button.text = creatorAction.text
        button.icon = creatorAction.icon
        button.setOnClickListenerAndDismiss(creatorAction.action)
    }

    private fun updateRichWorkspaceVisibility() {
        val canCreateRichWorkspace = !file.isEncrypted &&
            file.richWorkspace == "" &&
            editorUtils.isEditorAvailable(user, MimeType.TEXT_MARKDOWN)

        binding.menuCreateRichWorkspace.setVisibleIf(canCreateRichWorkspace)
    }

    private fun setupClickListener() = with(binding) {
        menuCreateRichWorkspace.setOnClickListenerAndDismiss(actions::createRichWorkspace)
        menuMkdir.setOnClickListenerAndDismiss { actions.createFolder(encrypted = false) }
        menuEncryptedMkdir.setOnClickListenerAndDismiss { actions.createFolder(encrypted = true) }
        menuUploadFromApp.setOnClickListenerAndDismiss(actions::uploadFromApp)
        menuDirectCameraUpload.setOnClickListenerAndDismiss(actions::directCameraUpload)
        menuUploadFiles.setOnClickListenerAndDismiss(actions::uploadFiles)

        when {
            appScanOptionalFeature.isAvailable -> menuScanDocUpload.setOnClickListenerAndDismiss(actions::scanDocUpload)

            actions.isScanDocUploadFromAppAvailable ->
                menuScanDocUpload.setOnClickListenerAndDismiss(actions::scanDocUploadFromApp)

            else -> {
                menuScanDocUpload.visibility = View.GONE
                menuScanDocUploadDivider.visibility = View.GONE
            }
        }

        menuMoreDocuments.setOnClickListener { bottomSheetViewSwitcher.showNext() }
        menuBack.setOnClickListener { bottomSheetViewSwitcher.showPrevious() }
    }

    private fun View.setOnClickListenerAndDismiss(action: () -> Unit) {
        setOnClickListener {
            action()
            dismiss()
        }
    }

    private fun filterActionsForOfflineOperations() {
        lifecycleScope.launch {
            val isAvailable = fileActivity.connectivityService.isNetworkAndServerAvailableSuspended()
            if (isAvailable && (!file.isOfflineOperation || file.isRootDirectory)) return@launch

            hideCreationOptions()
        }
    }

    private fun hideCreationOptions() = with(binding) {
        menuCreateRichWorkspace.visibility = View.GONE
        creatorsOverviewContainer.visibility = View.GONE
        menuEncryptedMkdir.visibility = View.GONE
    }
}
