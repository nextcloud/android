/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import com.nextcloud.utils.extensions.setVisibilityWithAnimation
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsSharingProcessFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment
import com.owncloud.android.ui.fragment.util.SharePermissionManager
import com.owncloud.android.ui.fragment.util.SharingMenuHelper
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
        const val SCREEN_TYPE_CUSTOM_PERMISSION = 3 // custom permissions screen

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
    private val sharePermissionManager = SharePermissionManager()

    private lateinit var capabilities: OCCapability

    private var expirationDatePickerFragment: ExpirationDatePickerDialogFragment? = null

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
        permission = sharePermissionManager.getMaximumPermission(isFolder())

        requireNotNull(fileActivity) { "FileActivity may not be null" }

        permission = capabilities.defaultPermissions ?: OCShare.NO_PERMISSION
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FileDetailsSharingProcessFragmentBinding.inflate(inflater, container, false)
        fileOperationsHelper = fileActivity?.fileOperationsHelper
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (shareProcessStep == SCREEN_TYPE_PERMISSION || shareProcessStep == SCREEN_TYPE_CUSTOM_PERMISSION) {
            showShareProcessFirst()
        } else {
            showShareProcessSecond()
        }

        implementClickEvents()
        setCheckboxStates()
        themeView()
    }

    private fun isFolder(): Boolean = file?.isFolder == true || share?.isFolder == true

    private fun themeView() {
        viewThemeUtils.platform.run {
            binding.run {
                colorTextView(shareProcessEditShareLink)
                colorTextView(shareCustomPermissionsText)

                themeRadioButton(viewOnlyRadioButton)
                themeRadioButton(editingRadioButton)
                themeRadioButton(fileDropRadioButton)
                themeRadioButton(customPermissionRadioButton)

                themeCheckbox(shareReadCheckbox)
                themeCheckbox(shareCreateCheckbox)
                themeCheckbox(shareEditCheckbox)
                themeCheckbox(shareCheckbox)
                themeCheckbox(shareDeleteCheckbox)

                themeCheckbox(shareProcessAllowResharingCheckbox)
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

    private fun showShareProcessFirst() {
        binding.shareProcessGroupOne.visibility = View.VISIBLE
        binding.shareProcessEditShareLink.visibility = View.VISIBLE
        binding.shareProcessGroupTwo.visibility = View.GONE

        if (share != null) {
            setupModificationUI()
        } else {
            setupUpdateUI()
        }

        if (isSecureShare) {
            binding.shareProcessAdvancePermissionTitle.visibility = View.GONE
        }

        // show or hide expiry date
        binding.shareProcessSetExpDateSwitch.setVisibleIf(isExpDateShown && !isSecureShare)
        shareProcessStep = SCREEN_TYPE_PERMISSION
    }

    private fun setupModificationUI() {
        if (share?.isFolder == true) updateViewForFolder() else updateViewForFile()

        // custom permissions / read only / allow upload and editing / file drop
        binding.run {
            when {
                SharingMenuHelper.isUploadAndEditingAllowed(share) -> editingRadioButton.isChecked = true
                SharingMenuHelper.isFileDrop(share) && share?.isFolder == true -> fileDropRadioButton.isChecked = true
                SharingMenuHelper.isReadOnly(share) -> viewOnlyRadioButton.isChecked = true
                else -> {
                    if (sharePermissionManager.isCustomPermission(share) ||
                        shareProcessStep == SCREEN_TYPE_CUSTOM_PERMISSION) {
                        customPermissionRadioButton.isChecked = true
                        customPermissionLayout.setVisibilityWithAnimation(true)
                    }
                }
            }
        }

        shareType = share?.shareType ?: ShareType.NO_SHARED

        // show different text for link share and other shares
        // because we have link to share in Public Link
        binding.shareProcessBtnNext.text = getString(
            if (shareType == ShareType.PUBLIC_LINK) {
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

    private fun setupUpdateUI() {
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
        binding.shareProcessChangeNameSwitch.visibility = View.GONE
        binding.shareProcessChangeNameContainer.visibility = View.GONE
        updateViewForExternalAndLinkShare()
    }

    private fun updateViewForLinkShare() {
        updateViewForExternalAndLinkShare()
        binding.shareProcessChangeNameSwitch.visibility = View.VISIBLE
        if (share != null) {
            binding.shareProcessChangeName.setText(share?.label)
            binding.shareProcessChangeNameSwitch.isChecked = !TextUtils.isEmpty(share?.label)
        }
        showChangeNameInput(binding.shareProcessChangeNameSwitch.isChecked)
    }

    private fun updateViewForInternalShare() {
        binding.shareProcessChangeNameSwitch.visibility = View.GONE
        binding.shareProcessChangeNameContainer.visibility = View.GONE
        binding.shareProcessHideDownloadCheckbox.visibility = View.GONE
        if (isSecureShare) {
            binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
        } else {
            binding.shareProcessAllowResharingCheckbox.visibility = View.VISIBLE
        }
        binding.shareProcessSetPasswordSwitch.visibility = View.GONE

        if (share != null) {
            if (!isReShareShown) {
                binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
            }
            binding.shareProcessAllowResharingCheckbox.isChecked = SharingMenuHelper.canReshare(share)
        }
    }

    /**
     * update views where share type external or link share
     */
    private fun updateViewForExternalAndLinkShare() {
        binding.shareProcessHideDownloadCheckbox.visibility = View.VISIBLE
        binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
        binding.shareProcessSetPasswordSwitch.visibility = View.VISIBLE

        if (share != null) {
            if (SharingMenuHelper.isFileDrop(share)) {
                binding.shareProcessHideDownloadCheckbox.visibility = View.GONE
            } else {
                binding.shareProcessHideDownloadCheckbox.visibility = View.VISIBLE
                binding.shareProcessHideDownloadCheckbox.isChecked = share?.isHideFileDownload == true
            }
        }
    }

    /**
     * update expiration date view while modifying the share
     */
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
        if (capabilities.filesDownloadLimit.isTrue && share?.isFolder == false) {
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
        binding.editingRadioButton.text = getString(R.string.link_share_editing)
        binding.fileDropRadioButton.visibility = View.GONE
    }

    private fun updateViewForFolder() {
        binding.editingRadioButton.text = getString(R.string.link_share_allow_upload_and_editing)
        binding.fileDropRadioButton.visibility = View.VISIBLE
        if (isSecureShare) {
            binding.fileDropRadioButton.visibility = View.GONE
            binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
            binding.shareProcessSetExpDateSwitch.visibility = View.GONE
        }
    }

    /**
     * update views for screen type Note
     */
    private fun showShareProcessSecond() {
        binding.shareProcessGroupOne.visibility = View.GONE
        binding.shareProcessEditShareLink.visibility = View.GONE
        binding.shareProcessGroupTwo.visibility = View.VISIBLE
        if (share != null) {
            binding.shareProcessBtnNext.text = getString(R.string.set_note)
            binding.noteText.setText(share?.note)
        } else {
            binding.shareProcessBtnNext.text = getString(R.string.send_share)
            binding.noteText.setText(R.string.empty)
        }
        shareProcessStep = SCREEN_TYPE_NOTE
        binding.shareProcessBtnNext.performClick()
    }

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
                    validateShareProcessSecond()
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
            shareProcessPermissionRadioGroup.setOnCheckedChangeListener { radioGroup, optionId ->
                run {
                    when (optionId) {
                        R.id.view_only_radio_button -> {
                            customPermissionLayout.visibility = View.GONE
                            permission = OCShare.READ_PERMISSION_FLAG
                        }

                        R.id.editing_radio_button -> {
                            customPermissionLayout.visibility = View.GONE
                            permission = sharePermissionManager.getMaximumPermission(isFolder())
                        }

                        R.id.file_drop_radio_button -> {
                            permission = OCShare.CREATE_PERMISSION_FLAG
                        }

                        R.id.custom_permission_radio_button -> {
                            val isChecked = customPermissionRadioButton.isChecked
                            customPermissionLayout.setVisibilityWithAnimation(isChecked)
                        }
                    }
                }
            }
            // endregion
        }
    }

    private fun togglePermission(permissionFlag: Int) {
        permission = sharePermissionManager.togglePermission(permission, permissionFlag)
    }

    private fun setCheckboxStates() {
        val currentPermissions = share?.permissions ?: permission

        binding.run {
            if (isFolder()) {
                // Only for the folder makes sense to have create permission
                // so that user can create files in the shared folder
                shareCreateCheckbox.isChecked =
                    sharePermissionManager.hasPermission(currentPermissions, OCShare.CREATE_PERMISSION_FLAG)
            } else {
                shareCreateCheckbox.visibility = View.GONE
            }

            shareReadCheckbox.isChecked =
                sharePermissionManager.hasPermission(currentPermissions, OCShare.READ_PERMISSION_FLAG)
            shareEditCheckbox.isChecked =
                sharePermissionManager.hasPermission(currentPermissions, OCShare.UPDATE_PERMISSION_FLAG)

            if (isFolder()) {
                shareDeleteCheckbox.isChecked =
                    sharePermissionManager.hasPermission(currentPermissions, OCShare.DELETE_PERMISSION_FLAG)
            } else {
                shareDeleteCheckbox.isChecked = false
                shareDeleteCheckbox.isEnabled = false
            }

            shareCheckbox.isChecked =
                sharePermissionManager.hasPermission(currentPermissions, OCShare.SHARE_PERMISSION_FLAG)
        }

        setCheckboxesListeners()
    }

    private fun setCheckboxesListeners() {
        binding.run {
            shareReadCheckbox.setOnCheckedChangeListener { _, isChecked ->
                togglePermission(OCShare.READ_PERMISSION_FLAG)
            }

            shareCreateCheckbox.setOnCheckedChangeListener { _, isChecked ->
                togglePermission(OCShare.CREATE_PERMISSION_FLAG)
            }

            shareEditCheckbox.setOnCheckedChangeListener { _, isChecked ->
                togglePermission(OCShare.UPDATE_PERMISSION_FLAG)
            }

            shareProcessAllowResharingCheckbox.setOnCheckedChangeListener { _, isChecked ->
                permission = sharePermissionManager.getReSharePermission()
            }

            shareCheckbox.setOnCheckedChangeListener { _, isChecked ->
                togglePermission(OCShare.SHARE_PERMISSION_FLAG)
            }

            shareDeleteCheckbox.setOnCheckedChangeListener { _, isChecked ->
                togglePermission(OCShare.DELETE_PERMISSION_FLAG)
            }
        }
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
                showShareProcessFirst()
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
            showShareProcessSecond()
        }
    }

    private fun updateShare() {
        fileOperationsHelper?.updateShareInformation(
            share,
            permission,
            binding.shareProcessHideDownloadCheckbox.isChecked,
            binding.shareProcessEnterPassword.text.toString().trim(),
            chosenExpDateInMills,
            binding.shareProcessChangeName.text.toString().trim()
        )

        if (capabilities.filesDownloadLimit.isTrue) {
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

    /**
     * method to validate step 2 (note screen) information
     */
    private fun validateShareProcessSecond() {
        val noteText = binding.noteText.text.toString().trim()
        // if modifying existing share then directly update the note and send email
        if (share != null && share?.note != noteText) {
            fileOperationsHelper?.updateNoteToShare(share, noteText)
        } else {
            // else create new share
            fileOperationsHelper?.shareFileWithSharee(
                file,
                shareeName,
                shareType,
                permission,
                binding
                    .shareProcessHideDownloadCheckbox.isChecked,
                binding.shareProcessEnterPassword.text.toString().trim(),
                chosenExpDateInMills,
                noteText,
                binding.shareProcessChangeName.text.toString().trim(),
                true
            )
        }
        removeCurrentFragment()
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
}
