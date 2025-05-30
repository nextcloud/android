/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nextcloud.utils.extensions.isPublicOrMail
import com.nextcloud.utils.extensions.setVisibilityWithAnimation
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsSharingProcessFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.quickPermission.QuickPermissionType
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment
import com.owncloud.android.ui.fragment.util.SharePermissionManager
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

/**
 * Fragment class to show share permission options, set expiration date, change label, set password, send note
 *
 * This fragment handles following:
 * 1. This will be shown while creating new internal and external share. So that user can set every share
 * configuration at one time.
 * 2. This will handle both Advanced Permissions and Send New Email functionality for existing shares to modify them.
 */
@Suppress("TooManyFunctions")
class FileDetailsSharingProcessFragment :
    Fragment(),
    Injectable,
    ExpirationDatePickerDialogFragment.OnExpiryDateListener {

    companion object {
        const val TAG = "FileDetailsSharingProcessFragment"
        private const val ARG_OCFILE = "arg_sharing_oc_file"
        private const val ARG_SHAREE_NAME = "arg_sharee_name"
        private const val ARG_SHARE_TYPE = "arg_share_type"
        private const val ARG_OCSHARE = "arg_ocshare"
        private const val ARG_SCREEN_TYPE = "arg_screen_type"
        private const val ARG_RESHARE_SHOWN = "arg_reshare_shown"
        private const val ARG_EXP_DATE_SHOWN = "arg_exp_date_shown"
        private const val ARG_SECURE_SHARE = "secure_share"

        // types of screens to be displayed
        const val SCREEN_TYPE_PERMISSION = 1 // permissions screen
        const val SCREEN_TYPE_NOTE = 2 // note screen

        /**
         * fragment instance to be called while creating new share for internal and external share
         */
        @JvmStatic
        fun newInstance(
            file: OCFile,
            shareeName: String,
            shareType: ShareType,
            secureShare: Boolean
        ): FileDetailsSharingProcessFragment {
            val bundle = Bundle().apply {
                putParcelable(ARG_OCFILE, file)
                putSerializable(ARG_SHARE_TYPE, shareType)
                putString(ARG_SHAREE_NAME, shareeName)
                putBoolean(ARG_SECURE_SHARE, secureShare)
            }

            return FileDetailsSharingProcessFragment().apply {
                arguments = bundle
            }
        }

        /**
         * fragment instance to be called while modifying existing share information
         */
        @JvmStatic
        fun newInstance(
            share: OCShare,
            screenType: Int,
            isReshareShown: Boolean,
            isExpirationDateShown: Boolean
        ): FileDetailsSharingProcessFragment {
            val bundle = Bundle().apply {
                putParcelable(ARG_OCSHARE, share)
                putInt(ARG_SCREEN_TYPE, screenType)
                putBoolean(ARG_RESHARE_SHOWN, isReshareShown)
                putBoolean(ARG_EXP_DATE_SHOWN, isExpirationDateShown)
            }

            return FileDetailsSharingProcessFragment().apply {
                arguments = bundle
            }
        }
    }

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var onEditShareListener: FileDetailSharingFragment.OnEditShareListener

    private lateinit var binding: FileDetailsSharingProcessFragmentBinding
    private var fileOperationsHelper: FileOperationsHelper? = null
    private var fileActivity: FileActivity? = null

    private var file: OCFile? = null // file to be share
    private var shareeName: String? = null
    private lateinit var shareType: ShareType
    private var shareProcessStep = SCREEN_TYPE_PERMISSION // default screen type
    private var permission = OCShare.NO_PERMISSION // no permission
    private var chosenExpDateInMills: Long = -1 // for no expiry date

    private var share: OCShare? = null
    private var isReShareShown: Boolean = true // show or hide reShare option
    private var isExpDateShown: Boolean = true // show or hide expiry date option
    private var isSecureShare: Boolean = false

    private lateinit var capabilities: OCCapability

    private var expirationDatePickerFragment: ExpirationDatePickerDialogFragment? = null
    private var downloadAttribute: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onEditShareListener = context as FileDetailSharingFragment.OnEditShareListener
        } catch (_: ClassCastException) {
            throw IllegalStateException("Calling activity must implement the interface")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            file = it.getParcelableArgument(ARG_OCFILE, OCFile::class.java)
            shareeName = it.getString(ARG_SHAREE_NAME)
            share = it.getParcelableArgument(ARG_OCSHARE, OCShare::class.java)

            if (it.containsKey(ARG_SHARE_TYPE)) {
                shareType = it.getSerializableArgument(ARG_SHARE_TYPE, ShareType::class.java)!!
            } else if (share != null) {
                shareType = share!!.shareType!!
            }

            shareProcessStep = it.getInt(ARG_SCREEN_TYPE, SCREEN_TYPE_PERMISSION)
            isReShareShown = it.getBoolean(ARG_RESHARE_SHOWN, true)
            isExpDateShown = it.getBoolean(ARG_EXP_DATE_SHOWN, true)
            isSecureShare = it.getBoolean(ARG_SECURE_SHARE, false)
        }

        fileActivity = activity as FileActivity?
        capabilities = CapabilityUtils.getCapability(context)

        requireNotNull(fileActivity) { "FileActivity may not be null" }

        permission = share?.permissions
            ?: capabilities.defaultPermissions
            ?: SharePermissionManager.getMaximumPermission(isFolder())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FileDetailsSharingProcessFragmentBinding.inflate(inflater, container, false)
        fileOperationsHelper = fileActivity?.fileOperationsHelper
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (shareProcessStep == SCREEN_TYPE_PERMISSION) {
            setupUI()
        } else {
            updateViewForNoteScreenType()
        }

        implementClickEvents()
        setCheckboxStates()
        themeView()
        setVisibilitiesOfShareOption()
        toggleNextButtonAvailability(isAnyShareOptionChecked())
        logShareInfo()
    }

    private fun logShareInfo() {
        share?.run {
            Log_OC.i(TAG, "-----BEFORE UPDATE SHARE-----")
            Log_OC.i(TAG, "ID: $id")
            Log_OC.i(TAG, "Permission: $permissions")
            Log_OC.i(TAG, "Hide File Download: $isHideFileDownload")
            Log_OC.i(TAG, "Label: $label")
            Log_OC.i(TAG, "Attributes: $attributes")
        }
    }

    private fun setVisibilitiesOfShareOption() {
        binding.run {
            shareAllowDownloadAndSyncCheckbox.setVisibleIf(!isPublicShare())
            fileRequestRadioButton.setVisibleIf(canSetFileRequest())
        }
    }

    private fun themeView() {
        viewThemeUtils.platform.run {
            binding.run {
                colorTextView(shareProcessEditShareLink)
                colorTextView(shareCustomPermissionsText)

                themeRadioButton(viewOnlyRadioButton)
                themeRadioButton(canEditRadioButton)
                themeRadioButton(customPermissionRadioButton)

                if (!isPublicShare()) {
                    themeCheckbox(shareAllowDownloadAndSyncCheckbox)
                }

                if (canSetFileRequest()) {
                    themeRadioButton(fileRequestRadioButton)
                }

                themeCheckbox(shareReadCheckbox)
                themeCheckbox(shareCreateCheckbox)
                themeCheckbox(shareEditCheckbox)
                themeCheckbox(shareCheckbox)
                themeCheckbox(shareDeleteCheckbox)
            }
        }

        viewThemeUtils.androidx.run {
            binding.run {
                colorSwitchCompat(shareProcessSetPasswordSwitch)
                colorSwitchCompat(shareProcessSetExpDateSwitch)
                colorSwitchCompat(shareProcessSetDownloadLimitSwitch)
                colorSwitchCompat(shareProcessHideDownloadCheckbox)
                colorSwitchCompat(shareProcessChangeNameSwitch)
            }
        }

        viewThemeUtils.material.run {
            binding.run {
                colorTextInputLayout(shareProcessEnterPasswordContainer)
                colorTextInputLayout(shareProcessSetDownloadLimitInputContainer)
                colorTextInputLayout(shareProcessChangeNameContainer)
                colorTextInputLayout(noteContainer)
                colorMaterialButtonPrimaryFilled(shareProcessBtnNext)
                colorMaterialButtonPrimaryOutlined(shareProcessBtnCancel)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Force recreation of dialog fragment when screen rotates
        // This is needed because the calendar layout should be different in portrait and landscape,
        // but as FDA persists through config changes, the dialog is not recreated automatically
        val datePicker = expirationDatePickerFragment
        if (datePicker?.dialog?.isShowing == true) {
            val currentSelectionMillis = datePicker.currentSelectionMillis
            datePicker.dismiss()
            showExpirationDateDialog(currentSelectionMillis)
        }
    }

    private fun setupUI() {
        binding.shareProcessGroupOne.visibility = View.VISIBLE
        binding.shareProcessEditShareLink.visibility = View.VISIBLE
        binding.shareProcessGroupTwo.visibility = View.GONE

        if (share != null) {
            updateViewForUpdate()
        } else {
            updateViewForCreate()
        }

        // show or hide expiry date
        binding.shareProcessSetExpDateSwitch.setVisibleIf(isExpDateShown && !isSecureShare)
        shareProcessStep = SCREEN_TYPE_PERMISSION
    }

    private fun setMaxPermissionsIfDefaultPermissionExists() {
        if (capabilities.defaultPermissions != null) {
            binding.canEditRadioButton.isChecked = true
            permission = SharePermissionManager.getMaximumPermission(isFolder())
        }
    }

    // region ViewUpdates
    private fun updateViewForCreate() {
        binding.shareProcessBtnNext.text = getString(R.string.common_next)
        file.let {
            if (file?.isFolder == true) {
                updateViewForFolder()
            } else {
                updateViewForFile()
            }
            updateViewForShareType()
        }
        showPasswordInput(binding.shareProcessSetPasswordSwitch.isChecked)
        showExpirationDateInput(binding.shareProcessSetExpDateSwitch.isChecked)
        showFileDownloadLimitInput(binding.shareProcessSetDownloadLimitSwitch.isChecked)
        setMaxPermissionsIfDefaultPermissionExists()
    }

    private fun updateViewForUpdate() {
        if (share?.isFolder == true) updateViewForFolder() else updateViewForFile()

        // custom permissions / read only / allow upload and editing / file request
        val selectedType = SharePermissionManager.getSelectedType(share, encrypted = file?.isEncrypted == true)
        binding.run {
            when(selectedType) {
                QuickPermissionType.VIEW_ONLY -> {
                    viewOnlyRadioButton.isChecked = true
                }
                QuickPermissionType.CAN_EDIT -> {
                    canEditRadioButton.isChecked = true
                }
                QuickPermissionType.FILE_REQUEST -> {
                    fileRequestRadioButton.isChecked = true
                }
                QuickPermissionType.CUSTOM_PERMISSIONS -> {
                    customPermissionRadioButton.isChecked = true
                    customPermissionLayout.setVisibilityWithAnimation(true)
                }
                else -> {

                }
            }
        }

        shareType = share?.shareType ?: ShareType.NO_SHARED

        // show different text for link share and other shares
        // because we have link to share in Public Link
        binding.shareProcessBtnNext.text = getString(
            if (isPublicShare()) {
                R.string.share_copy_link
            } else {
                R.string.common_confirm
            }
        )

        updateViewForShareType()
        binding.shareProcessSetPasswordSwitch.isChecked = share?.isPasswordProtected == true
        showPasswordInput(binding.shareProcessSetPasswordSwitch.isChecked)
        updateExpirationDateView()
        showExpirationDateInput(binding.shareProcessSetExpDateSwitch.isChecked)
        updateFileDownloadLimitView()
        showFileDownloadLimitInput(binding.shareProcessSetDownloadLimitSwitch.isChecked)
    }

    private fun updateViewForShareType() {
        when (shareType) {
            ShareType.EMAIL -> {
                updateViewForExternalShare()
            }

            ShareType.PUBLIC_LINK -> {
                updateViewForLinkShare()
            }

            else -> {
                updateViewForInternalShare()
            }
        }
    }

    private fun updateViewForExternalShare() {
        binding.run {
            shareProcessChangeNameSwitch.visibility = View.GONE
            shareProcessChangeNameContainer.visibility = View.GONE
            updateViewForExternalAndLinkShare()
        }
    }

    private fun updateViewForLinkShare() {
        updateViewForExternalAndLinkShare()
        binding.run {
            shareProcessChangeNameSwitch.visibility = View.VISIBLE
            if (share != null) {
                shareProcessChangeName.setText(share?.label)
                shareProcessChangeNameSwitch.isChecked = !TextUtils.isEmpty(share?.label)
            }
            shareReadCheckbox.isEnabled = isFolder()
            showChangeNameInput(shareProcessChangeNameSwitch.isChecked)
        }
    }

    private fun updateViewForInternalShare() {
        binding.run {
            shareProcessChangeNameSwitch.visibility = View.GONE
            shareProcessChangeNameContainer.visibility = View.GONE
            shareProcessHideDownloadCheckbox.visibility = View.GONE
            shareCheckbox.setVisibleIf(!isSecureShare)
            shareProcessSetPasswordSwitch.visibility = View.GONE

            if (share != null) {
                if (!isReShareShown) {
                    shareCheckbox.visibility = View.GONE
                }
                shareCheckbox.isChecked = SharePermissionManager.canReshare(share)
            }
        }
    }

    private fun updateViewForExternalAndLinkShare() {
        binding.run {
            shareProcessHideDownloadCheckbox.visibility = View.VISIBLE
            shareCheckbox.visibility = View.GONE
            shareProcessSetPasswordSwitch.visibility = View.VISIBLE

            if (share != null) {
                if (SharePermissionManager.isFileRequest(share)) {
                    shareProcessHideDownloadCheckbox.visibility = View.GONE
                } else {
                    shareProcessHideDownloadCheckbox.visibility = View.VISIBLE
                    shareProcessHideDownloadCheckbox.isChecked = share?.isHideFileDownload == true
                }
            }
        }
    }

    private fun updateExpirationDateView() {
        share?.let { share ->
            if (share.expirationDate > 0) {
                chosenExpDateInMills = share.expirationDate
                binding.shareProcessSetExpDateSwitch.isChecked = true
                binding.shareProcessSelectExpDate.text = getString(
                    R.string.share_expiration_date_format,
                    SimpleDateFormat.getDateInstance().format(Date(share.expirationDate))
                )
            }
        }
    }

    private fun updateFileDownloadLimitView() {
        if (canSetDownloadLimit()) {
            binding.shareProcessSetDownloadLimitSwitch.visibility = View.VISIBLE

            val currentDownloadLimit = share?.fileDownloadLimit?.limit ?: capabilities.filesDownloadLimitDefault
            if (currentDownloadLimit > 0) {
                binding.shareProcessSetDownloadLimitSwitch.isChecked = true
                showFileDownloadLimitInput(true)
                binding.shareProcessSetDownloadLimitInput.setText("$currentDownloadLimit")
            }
        }
    }

    private fun updateViewForFile() {
        binding.run {
            canEditRadioButton.text = getString(R.string.link_share_editing)
        }
    }

    private fun updateViewForFolder() {
        binding.run {
            canEditRadioButton.text = getString(R.string.share_permission_can_edit)

            if (isSecureShare) {
                shareCheckbox.visibility = View.GONE
                shareProcessSetExpDateSwitch.visibility = View.GONE
            }
        }
    }

    private fun updateViewForNoteScreenType() {
        binding.run {
            shareProcessGroupOne.visibility = View.GONE
            shareProcessEditShareLink.visibility = View.GONE
            shareProcessGroupTwo.visibility = View.VISIBLE
            if (share != null) {
                shareProcessBtnNext.text = getString(R.string.set_note)
                noteText.setText(share?.note)
            } else {
                shareProcessBtnNext.text = getString(R.string.send_share)
                noteText.setText(R.string.empty)
            }
            shareProcessStep = SCREEN_TYPE_NOTE
            shareProcessBtnNext.performClick()
        }
    }
    // endregion

    @Suppress("LongMethod")
    private fun implementClickEvents() {
        binding.run {
            shareProcessBtnCancel.setOnClickListener {
                onCancelClick()
            }
            shareProcessBtnNext.setOnClickListener {
                if (shareProcessStep == SCREEN_TYPE_PERMISSION) {
                    validateShareProcessFirst()
                } else {
                    createShareOrUpdateNoteShare()
                }
            }
            shareProcessSetPasswordSwitch.setOnCheckedChangeListener { _, isChecked ->
                showPasswordInput(isChecked)
            }
            shareProcessSetExpDateSwitch.setOnCheckedChangeListener { _, isChecked ->
                showExpirationDateInput(isChecked)
            }
            shareProcessSetDownloadLimitSwitch.setOnCheckedChangeListener { _, isChecked ->
                showFileDownloadLimitInput(isChecked)
            }
            shareProcessChangeNameSwitch.setOnCheckedChangeListener { _, isChecked ->
                showChangeNameInput(isChecked)
            }
            shareProcessSelectExpDate.setOnClickListener {
                showExpirationDateDialog()
            }

            // region RadioButtons
            shareRadioGroup.setOnCheckedChangeListener { _, optionId ->
                when (optionId) {
                    R.id.view_only_radio_button -> {
                        permission = OCShare.READ_PERMISSION_FLAG
                    }

                    R.id.can_edit_radio_button -> {
                        permission = SharePermissionManager.getMaximumPermission(isFolder())
                    }

                    R.id.file_request_radio_button -> {
                        permission = OCShare.CREATE_PERMISSION_FLAG
                    }
                }

                val isCustomPermissionSelected = (optionId == R.id.custom_permission_radio_button)
                if (isCustomPermissionSelected) {
                    permission = SharePermissionManager.getMaximumPermission(isFolder())
                }
                customPermissionLayout.setVisibilityWithAnimation(isCustomPermissionSelected)
                toggleNextButtonAvailability(true)
            }
            // endregion
        }
    }

    private fun isAnyShareOptionChecked(): Boolean {
        return binding.run {
            val isCustomPermissionChecked = customPermissionRadioButton.isChecked &&
                (
                    shareReadCheckbox.isChecked ||
                        shareCreateCheckbox.isChecked ||
                        shareEditCheckbox.isChecked ||
                        shareCheckbox.isChecked ||
                        shareDeleteCheckbox.isChecked
                    )

            viewOnlyRadioButton.isChecked ||
                canEditRadioButton.isChecked ||
                fileRequestRadioButton.isChecked ||
                isCustomPermissionChecked
        }
    }

    private fun toggleNextButtonAvailability(value: Boolean) {
        binding.run {
            shareProcessBtnNext.isEnabled = value
            shareProcessBtnNext.isClickable = value
        }
    }

    @Suppress("NestedBlockDepth")
    private fun setCheckboxStates() {
        val currentPermissions = share?.permissions ?: permission

        binding.run {
            SharePermissionManager.run {
                shareReadCheckbox.isChecked = hasPermission(currentPermissions, OCShare.READ_PERMISSION_FLAG)
                shareEditCheckbox.isChecked = hasPermission(currentPermissions, OCShare.UPDATE_PERMISSION_FLAG)
                shareCheckbox.isChecked = hasPermission(currentPermissions, OCShare.SHARE_PERMISSION_FLAG)

                if (isFolder()) {
                    // Only for the folder makes sense to have create permission
                    // so that user can create files in the shared folder
                    shareCreateCheckbox.isChecked = hasPermission(currentPermissions, OCShare.CREATE_PERMISSION_FLAG)
                    shareDeleteCheckbox.isChecked = hasPermission(currentPermissions, OCShare.DELETE_PERMISSION_FLAG)
                } else {
                    shareCreateCheckbox.visibility = View.GONE
                    shareDeleteCheckbox.apply {
                        isChecked = false
                        isEnabled = false
                    }
                }

                if (!isPublicShare()) {
                    shareAllowDownloadAndSyncCheckbox.isChecked = isAllowDownloadAndSyncEnabled(share)
                }
            }
        }

        setCheckboxesListeners()
    }

    private fun setCheckboxesListeners() {
        val checkboxes = mapOf(
            binding.shareReadCheckbox to OCShare.READ_PERMISSION_FLAG,
            binding.shareCreateCheckbox to OCShare.CREATE_PERMISSION_FLAG,
            binding.shareEditCheckbox to OCShare.UPDATE_PERMISSION_FLAG,
            binding.shareCheckbox to OCShare.SHARE_PERMISSION_FLAG,
            binding.shareDeleteCheckbox to OCShare.DELETE_PERMISSION_FLAG
        )

        checkboxes.forEach { (checkbox, flag) ->
            checkbox.setOnCheckedChangeListener { _, isChecked -> togglePermission(isChecked, flag) }
        }

        if (!isPublicShare()) {
            binding.shareAllowDownloadAndSyncCheckbox.setOnCheckedChangeListener { _, isChecked ->
                val result = SharePermissionManager.toggleAllowDownloadAndSync(isChecked, share)
                share?.attributes = result
                downloadAttribute = result
            }
        }
    }

    private fun togglePermission(isChecked: Boolean, permissionFlag: Int) {
        permission = SharePermissionManager.togglePermission(isChecked, permission, permissionFlag)
        toggleNextButtonAvailability(true)
    }

    private fun showExpirationDateDialog(chosenDateInMillis: Long = chosenExpDateInMills) {
        val dialog = ExpirationDatePickerDialogFragment.newInstance(chosenDateInMillis)
        dialog.setOnExpiryDateListener(this)
        expirationDatePickerFragment = dialog
        fileActivity?.let {
            dialog.show(
                it.supportFragmentManager,
                ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
            )
        }
    }

    private fun showChangeNameInput(isChecked: Boolean) {
        binding.shareProcessChangeNameContainer.setVisibleIf(isChecked)

        if (!isChecked) {
            binding.shareProcessChangeName.setText(R.string.empty)
        }
    }

    private fun onCancelClick() {
        // if modifying the existing share then on back press remove the current fragment
        if (share != null) {
            removeCurrentFragment()
        }
        // else we have to check if user is in step 2(note screen) then show step 1 (permission screen)
        // and if user is in step 1 (permission screen) then remove the fragment
        else {
            if (shareProcessStep == SCREEN_TYPE_NOTE) {
                setupUI()
            } else {
                removeCurrentFragment()
            }
        }
    }

    private fun showExpirationDateInput(isChecked: Boolean) {
        binding.shareProcessSelectExpDate.setVisibleIf(isChecked)
        binding.shareProcessExpDateDivider.setVisibleIf(isChecked)

        // reset the expiration date if switch is unchecked
        if (!isChecked) {
            chosenExpDateInMills = -1
            binding.shareProcessSelectExpDate.text = getString(R.string.empty)
        }
    }

    private fun showFileDownloadLimitInput(isChecked: Boolean) {
        binding.shareProcessSetDownloadLimitInputContainer.setVisibleIf(isChecked)

        // reset download limit if switch is unchecked
        if (!isChecked) {
            binding.shareProcessSetDownloadLimitInput.setText(R.string.empty)
        }
    }

    private fun showPasswordInput(isChecked: Boolean) {
        binding.shareProcessEnterPasswordContainer.setVisibleIf(isChecked)

        // reset the password if switch is unchecked
        if (!isChecked) {
            binding.shareProcessEnterPassword.setText(R.string.empty)
        }
    }

    private fun removeCurrentFragment() {
        onEditShareListener.onShareProcessClosed()
        fileActivity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    /**
     * method to validate the step 1 screen information
     */
    @Suppress("ReturnCount")
    private fun validateShareProcessFirst() {
        if (permission == OCShare.NO_PERMISSION) {
            DisplayUtils.showSnackMessage(binding.root, R.string.no_share_permission_selected)
            return
        }

        if (binding.shareProcessSetPasswordSwitch.isChecked &&
            binding.shareProcessEnterPassword.text?.trim().isNullOrEmpty()
        ) {
            DisplayUtils.showSnackMessage(binding.root, R.string.share_link_empty_password)
            return
        }

        if (binding.shareProcessSetExpDateSwitch.isChecked &&
            binding.shareProcessSelectExpDate.text?.trim().isNullOrEmpty()
        ) {
            showExpirationDateDialog()
            return
        }

        if (binding.shareProcessChangeNameSwitch.isChecked &&
            binding.shareProcessChangeName.text?.trim().isNullOrEmpty()
        ) {
            DisplayUtils.showSnackMessage(binding.root, R.string.label_empty)
            return
        }

        // if modifying existing share information then execute the process
        if (share != null) {
            updateShare()
            removeCurrentFragment()
        } else {
            // else show step 2 (note screen)
            updateViewForNoteScreenType()
        }
    }

    @Suppress("ReturnCount")
    private fun createShareOrUpdateNoteShare() {
        if (!isAnyShareOptionChecked()) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.share_option_required)
            return
        }

        val noteText = binding.noteText.text.toString().trim()
        if (file == null && (share != null && share?.note == noteText)) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.share_cannot_update_empty_note)
            return
        }

        when {
            // if modifying existing share then directly update the note and send email
            share != null && share?.note != noteText -> {
                fileOperationsHelper?.updateNoteToShare(share, noteText)
            }
            file == null -> {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.file_not_found_cannot_share)
                return
            }
            else -> {
                createShare(noteText)
            }
        }

        removeCurrentFragment()
    }

    private fun updateShare() {
        // empty string causing fails
        if (share?.attributes?.isEmpty() == true) {
            share?.attributes = null
        }

        fileOperationsHelper?.updateShareInformation(
            share,
            permission,
            binding.shareProcessHideDownloadCheckbox.isChecked,
            binding.shareProcessEnterPassword.text.toString().trim(),
            chosenExpDateInMills,
            binding.shareProcessChangeName.text.toString().trim()
        )

        if (canSetDownloadLimit()) {
            val downloadLimitInput = binding.shareProcessSetDownloadLimitInput.text.toString().trim()
            val downloadLimit =
                if (binding.shareProcessSetDownloadLimitSwitch.isChecked && downloadLimitInput.isNotEmpty()) {
                    downloadLimitInput.toInt()
                } else {
                    0
                }

            fileOperationsHelper?.updateFilesDownloadLimit(share, downloadLimit)
        }

        // copy the share link if available
        if (!TextUtils.isEmpty(share?.shareLink)) {
            ClipboardUtil.copyToClipboard(requireActivity(), share?.shareLink)
        }
    }

    private fun createShare(noteText: String) {
        fileOperationsHelper?.shareFileWithSharee(
            file,
            shareeName,
            shareType,
            permission,
            binding.shareProcessHideDownloadCheckbox.isChecked,
            binding.shareProcessEnterPassword.text.toString().trim(),
            chosenExpDateInMills,
            noteText,
            downloadAttribute,
            binding.shareProcessChangeName.text.toString().trim(),
            true
        )
    }

    /**
     * method will be called from DrawerActivity on back press to handle screen backstack
     */
    fun onBackPressed() {
        onCancelClick()
    }

    override fun onDateSet(year: Int, monthOfYear: Int, dayOfMonth: Int, chosenDateInMillis: Long) {
        binding.shareProcessSelectExpDate.text = getString(
            R.string.share_expiration_date_format,
            SimpleDateFormat.getDateInstance().format(Date(chosenDateInMillis))
        )
        this.chosenExpDateInMills = chosenDateInMillis
    }

    override fun onDateUnSet() {
        binding.shareProcessSetExpDateSwitch.isChecked = false
    }

    // region Helpers
    private fun isFolder(): Boolean = (file?.isFolder == true || share?.isFolder == true)

    private fun canSetFileRequest(): Boolean = isFolder() && shareType.isPublicOrMail()

    private fun canSetDownloadLimit(): Boolean =
        (isPublicShare() && capabilities.filesDownloadLimit.isTrue && share?.isFolder == false)

    private fun isPublicShare(): Boolean = (shareType == ShareType.PUBLIC_LINK)
    // endregion
}
