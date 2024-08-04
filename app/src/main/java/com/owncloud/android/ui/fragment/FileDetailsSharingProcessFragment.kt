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
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsSharingProcessFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment
import com.owncloud.android.ui.fragment.util.SharingMenuHelper
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.DisplayUtils
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
            val args = Bundle()
            args.putParcelable(ARG_OCFILE, file)
            args.putSerializable(ARG_SHARE_TYPE, shareType)
            args.putString(ARG_SHAREE_NAME, shareeName)
            args.putBoolean(ARG_SECURE_SHARE, secureShare)
            val fragment = FileDetailsSharingProcessFragment()
            fragment.arguments = args
            return fragment
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
            val args = Bundle()
            args.putParcelable(ARG_OCSHARE, share)
            args.putInt(ARG_SCREEN_TYPE, screenType)
            args.putBoolean(ARG_RESHARE_SHOWN, isReshareShown)
            args.putBoolean(ARG_EXP_DATE_SHOWN, isExpirationDateShown)
            val fragment = FileDetailsSharingProcessFragment()
            fragment.arguments = args
            return fragment
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

    private var expirationDatePickerFragment: ExpirationDatePickerDialogFragment? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onEditShareListener = context as FileDetailSharingFragment.OnEditShareListener
        } catch (e: ClassCastException) {
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

        requireNotNull(fileActivity) { "FileActivity may not be null" }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FileDetailsSharingProcessFragmentBinding.inflate(inflater, container, false)
        fileOperationsHelper = fileActivity?.fileOperationsHelper
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (shareProcessStep == SCREEN_TYPE_PERMISSION) {
            showShareProcessFirst()
        } else {
            showShareProcessSecond()
        }
        implementClickEvents()

        themeView()
    }

    private fun themeView() {
        viewThemeUtils.platform.colorTextView(binding.shareProcessEditShareLink)
        viewThemeUtils.platform.colorTextView(binding.shareProcessAdvancePermissionTitle)

        viewThemeUtils.platform.themeRadioButton(binding.shareProcessPermissionReadOnly)
        viewThemeUtils.platform.themeRadioButton(binding.shareProcessPermissionUploadEditing)
        viewThemeUtils.platform.themeRadioButton(binding.shareProcessPermissionFileDrop)

        viewThemeUtils.platform.themeCheckbox(binding.shareProcessAllowResharingCheckbox)

        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessSetPasswordSwitch)
        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessSetExpDateSwitch)
        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessHideDownloadCheckbox)
        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessChangeNameSwitch)

        viewThemeUtils.material.colorTextInputLayout(binding.shareProcessEnterPasswordContainer)
        viewThemeUtils.material.colorTextInputLayout(binding.shareProcessChangeNameContainer)
        viewThemeUtils.material.colorTextInputLayout(binding.noteContainer)

        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(binding.shareProcessBtnNext)
        viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(binding.shareProcessBtnCancel)
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
        if (isExpDateShown && !isSecureShare) {
            binding.shareProcessSetExpDateSwitch.visibility = View.VISIBLE
        } else {
            binding.shareProcessSetExpDateSwitch.visibility = View.GONE
        }
        shareProcessStep = SCREEN_TYPE_PERMISSION
    }

    private fun setupModificationUI() {
        if (share?.isFolder == true) updateViewForFolder() else updateViewForFile()

        // read only / allow upload and editing / file drop
        if (SharingMenuHelper.isUploadAndEditingAllowed(share)) {
            binding.shareProcessPermissionUploadEditing.isChecked = true
        } else if (SharingMenuHelper.isFileDrop(share) && share?.isFolder == true) {
            binding.shareProcessPermissionFileDrop.isChecked = true
        } else if (SharingMenuHelper.isReadOnly(share)) {
            binding.shareProcessPermissionReadOnly.isChecked = true
        }

        shareType = share?.shareType ?: ShareType.NO_SHARED

        // show different text for link share and other shares
        // because we have link to share in Public Link
        val resources = requireContext().resources

        binding.shareProcessBtnNext.text = resources.getString(
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
    }

    private fun setupUpdateUI() {
        binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.common_next)
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
        if (share != null) {
            if ((share?.expirationDate ?: 0) > 0) {
                chosenExpDateInMills = share?.expirationDate ?: -1
                binding.shareProcessSetExpDateSwitch.isChecked = true
                binding.shareProcessSelectExpDate.text = (
                    resources.getString(
                        R.string.share_expiration_date_format,
                        SimpleDateFormat.getDateInstance().format(Date(share?.expirationDate ?: 0))
                    )
                    )
            }
        }
    }

    private fun updateViewForFile() {
        binding.shareProcessPermissionUploadEditing.text =
            requireContext().resources.getString(R.string.link_share_editing)
        binding.shareProcessPermissionFileDrop.visibility = View.GONE
    }

    private fun updateViewForFolder() {
        binding.shareProcessPermissionUploadEditing.text =
            requireContext().resources.getString(R.string.link_share_allow_upload_and_editing)
        binding.shareProcessPermissionFileDrop.visibility = View.VISIBLE
        if (isSecureShare) {
            binding.shareProcessPermissionFileDrop.visibility = View.GONE
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
            binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.set_note)
            binding.noteText.setText(share?.note)
        } else {
            binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.send_share)
            binding.noteText.setText(R.string.empty)
        }
        shareProcessStep = SCREEN_TYPE_NOTE
    }

    private fun implementClickEvents() {
        binding.shareProcessBtnCancel.setOnClickListener {
            onCancelClick()
        }
        binding.shareProcessBtnNext.setOnClickListener {
            if (shareProcessStep == SCREEN_TYPE_PERMISSION) {
                validateShareProcessFirst()
            } else {
                validateShareProcessSecond()
            }
        }
        binding.shareProcessSetPasswordSwitch.setOnCheckedChangeListener { _, isChecked ->
            showPasswordInput(isChecked)
        }
        binding.shareProcessSetExpDateSwitch.setOnCheckedChangeListener { _, isChecked ->
            showExpirationDateInput(isChecked)
        }
        binding.shareProcessChangeNameSwitch.setOnCheckedChangeListener { _, isChecked ->
            showChangeNameInput(isChecked)
        }
        binding.shareProcessSelectExpDate.setOnClickListener {
            showExpirationDateDialog()
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
        binding.shareProcessChangeNameContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
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
        binding.shareProcessSelectExpDate.visibility = if (isChecked) View.VISIBLE else View.GONE
        binding.shareProcessExpDateDivider.visibility = if (isChecked) View.VISIBLE else View.GONE

        // reset the expiration date if switch is unchecked
        if (!isChecked) {
            chosenExpDateInMills = -1
            binding.shareProcessSelectExpDate.text = getString(R.string.empty)
        }
    }

    private fun showPasswordInput(isChecked: Boolean) {
        binding.shareProcessEnterPasswordContainer.visibility = if (isChecked) View.VISIBLE else View.GONE

        // reset the password if switch is unchecked
        if (!isChecked) {
            binding.shareProcessEnterPassword.setText(R.string.empty)
        }
    }

    private fun removeCurrentFragment() {
        onEditShareListener.onShareProcessClosed()
        fileActivity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    private fun getReSharePermission(): Int {
        val spb = SharePermissionsBuilder()
        spb.setSharePermission(true)
        return spb.build()
    }

    /**
     * method to validate the step 1 screen information
     */
    @Suppress("ReturnCount")
    private fun validateShareProcessFirst() {
        permission = getSelectedPermission()
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

    /**
     *  get the permissions on the basis of selection
     */
    private fun getSelectedPermission() = when {
        binding.shareProcessAllowResharingCheckbox.isChecked -> getReSharePermission()
        binding.shareProcessPermissionReadOnly.isChecked -> OCShare.READ_PERMISSION_FLAG
        binding.shareProcessPermissionUploadEditing.isChecked -> when {
            file?.isFolder == true || share?.isFolder == true -> OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
            else -> OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
        }

        binding.shareProcessPermissionFileDrop.isChecked -> OCShare.CREATE_PERMISSION_FLAG
        else -> permission
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
        binding.shareProcessSelectExpDate.text = (
            resources.getString(
                R.string.share_expiration_date_format,
                SimpleDateFormat.getDateInstance().format(Date(chosenDateInMillis))
            )
            )
        this.chosenExpDateInMills = chosenDateInMillis
    }

    override fun onDateUnSet() {
        binding.shareProcessSetExpDateSwitch.isChecked = false
    }
}
