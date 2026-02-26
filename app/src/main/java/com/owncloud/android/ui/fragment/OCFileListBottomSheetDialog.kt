/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
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
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.FileListActionsBottomSheetFragmentBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.DirectEditing
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ThemeUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

@Suppress("LongParameterList")
class OCFileListBottomSheetDialog(
    private val fileActivity: FileActivity,
    private val actions: OCFileListBottomSheetActions,
    private val deviceInfo: DeviceInfo,
    private val user: User,
    private val file: OCFile,
    private val themeUtils: ThemeUtils,
    private val viewThemeUtils: ViewThemeUtils,
    private val editorUtils: EditorUtils,
    private val appScanOptionalFeature: AppScanOptionalFeature
) : BottomSheetDialog(fileActivity),
    Injectable {

    private lateinit var binding: FileListActionsBottomSheetFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FileListActionsBottomSheetFragmentBinding.inflate(layoutInflater)
        setContentView(binding.getRoot())

        applyBranding()
        binding.addToCloud.text = context.resources.getString(
            R.string.add_to_cloud,
            themeUtils.getDefaultDisplayNameForRootFolder(context)
        )

        checkTemplateVisibility()
        initCreatorContainer()

        if (!deviceInfo.hasCamera(context)) {
            binding.menuDirectCameraUpload.visibility = View.GONE
        }

        createRichWorkspace()
        setupClickListener()
        filterActionsForOfflineOperations()

        if (MainApp.isClientBranded() && isFlavourGPlay()) {
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

    private fun applyBranding() {
        viewThemeUtils.material.run {
            binding.run {
                colorMaterialButtonContent(menuUploadFiles, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuUploadFromApp, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuDirectCameraUpload, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuScanDocUpload, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuMkdir, ColorRole.PRIMARY)
                colorMaterialButtonContent(menuCreateRichWorkspace, ColorRole.PRIMARY)
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
            menuCreateRichWorkspace.setTextColor(textColor)
        }
    }

    @Suppress("ComplexCondition")
    private fun checkTemplateVisibility() {
        val capability = fileActivity.capabilities
        if (capability != null &&
            capability.richDocuments.isTrue &&
            capability.richDocumentsDirectEditing.isTrue &&
            capability.richDocumentsTemplatesAvailable.isTrue &&
            !file.isEncrypted
        ) {
            binding.menuNewDocument.visibility = View.VISIBLE
            binding.menuNewSpreadsheet.visibility = View.VISIBLE
            binding.menuNewPresentation.visibility = View.VISIBLE
        }
    }

    @Suppress("DEPRECATION", "LongMethod", "MagicNumber")
    private fun initCreatorContainer() {
        val json = ArbitraryDataProviderImpl(context)
            .getValue(user, ArbitraryDataProvider.DIRECT_EDITING)

        if (json.isNotEmpty() && !file.isEncrypted) {
            val directEditing = Gson().fromJson(json, DirectEditing::class.java)
            if (directEditing.creators.isEmpty()) {
                return
            }

            binding.creatorsContainer.visibility = View.VISIBLE
            binding.creators.removeAllViews()

            val itemHeight = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_item_height)
            val standardPadding = context.resources.getDimensionPixelSize(R.dimen.standard_padding)
            val iconSize = context.resources.getDimensionPixelSize(R.dimen.iconized_single_line_item_icon_size)
            val startPadding = context.resources.getDimensionPixelSize(R.dimen.creator_container_start_padding)

            for (creator in directEditing.creators.values) {
                val creatorButton = MaterialButton(
                    ContextThemeWrapper(
                        context,
                        com.google.android.material.R.style.Widget_Material3_Button_IconButton
                    ),
                    null,
                    com.google.android.material.R.attr.materialButtonStyle
                ).apply {
                    id = View.generateViewId()
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        itemHeight
                    )

                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setPaddingRelative(startPadding, 0, standardPadding, 0)
                    insetTop = 0
                    insetBottom = 0

                    val buttonText = String.format(
                        fileActivity.getString(R.string.editor_placeholder),
                        fileActivity.getString(R.string.create_new),
                        creator.name
                    )
                    text = buttonText
                    setTextColor(ContextCompat.getColor(context, R.color.text_color))
                    textSize = 16f
                    isAllCaps = false

                    icon = MimeTypeUtil.getFileTypeIcon(
                        creator.mimetype,
                        creator.extension,
                        context,
                        viewThemeUtils
                    )
                    this.iconSize = iconSize
                    this.iconPadding = standardPadding
                    iconGravity = MaterialButton.ICON_GRAVITY_START
                    iconTint = null

                    setOnClickListener {
                        actions.showTemplate(creator, buttonText)
                        dismiss()
                    }
                }

                binding.creators.addView(creatorButton)
            }
        }
    }

    private fun createRichWorkspace() {
        if (editorUtils.isEditorAvailable(user, MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN) && !file.isEncrypted) {
            // richWorkspace
            // == "": no info set -> show button
            // == null: disabled on server side -> hide button
            // != "": info set -> hide button
            if (file.richWorkspace == null || "" != file.richWorkspace) {
                binding.menuCreateRichWorkspace.visibility = View.GONE
                binding.menuCreateRichWorkspaceDivider.visibility = View.GONE
            } else {
                binding.menuCreateRichWorkspace.visibility = View.VISIBLE
                binding.menuCreateRichWorkspaceDivider.visibility = View.VISIBLE
            }
        } else {
            binding.menuCreateRichWorkspace.visibility = View.GONE
            binding.menuCreateRichWorkspaceDivider.visibility = View.GONE
        }
    }

    private fun setupClickListener() {
        binding.run {
            menuCreateRichWorkspace.setOnClickListener {
                actions.createRichWorkspace()
                dismiss()
            }

            menuMkdir.setOnClickListener {
                actions.createFolder()
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
            } else {
                menuScanDocUpload.visibility = View.GONE
            }

            menuUploadFiles.setOnClickListener {
                actions.uploadFiles()
                dismiss()
            }

            menuNewDocument.setOnClickListener {
                actions.newDocument()
                dismiss()
            }

            menuNewSpreadsheet.setOnClickListener {
                actions.newSpreadsheet()
                dismiss()
            }

            menuNewPresentation.setOnClickListener {
                actions.newPresentation()
                dismiss()
            }
        }
    }

    private fun filterActionsForOfflineOperations() {
        fileActivity.connectivityService.isNetworkAndServerAvailable { result: Boolean? ->
            if (file.isRootDirectory) {
                return@isNetworkAndServerAvailable
            }

            if (!result!! || file.isOfflineOperation) {
                binding.run {
                    menuCreateRichWorkspace.visibility = View.GONE
                    menuUploadFromApp.visibility = View.GONE
                    menuDirectCameraUpload.visibility = View.GONE
                    menuScanDocUpload.visibility = View.GONE
                    menuNewDocument.visibility = View.GONE
                    menuNewSpreadsheet.visibility = View.GONE
                    menuNewPresentation.visibility = View.GONE
                    creatorsContainer.visibility = View.GONE
                }
            }
        }
    }
}
