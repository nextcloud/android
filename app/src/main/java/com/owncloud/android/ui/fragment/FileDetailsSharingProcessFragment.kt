/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2023 TSI-mc
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nextcloud.client.di.Injectable
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
import com.owncloud.android.utils.KeyboardUtils
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

        // types of screens to be displayed
        const val SCREEN_TYPE_PERMISSION = 1 // permissions screen
        const val SCREEN_TYPE_NOTE = 2 // note screen

        /**
         * fragment instance to be called while creating new share for internal and external share
         */
        @JvmStatic
        fun newInstance(file: OCFile, shareeName: String, shareType: ShareType): FileDetailsSharingProcessFragment {
            val args = Bundle()
            args.putParcelable(ARG_OCFILE, file)
            args.putSerializable(ARG_SHARE_TYPE, shareType)
            args.putString(ARG_SHAREE_NAME, shareeName)
            val fragment = FileDetailsSharingProcessFragment()
            fragment.arguments = args
            return fragment
        }

        /**
         * fragment instance to be called while modifying existing share information
         */
        @JvmStatic
        fun newInstance(share: OCShare, screenType: Int, isReshareShown: Boolean, isExpirationDateShown: Boolean):
            FileDetailsSharingProcessFragment {
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
    @Inject
    lateinit var keyboardUtils: KeyboardUtils

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
    private var isReshareShown: Boolean = true // show or hide reshare option
    private var isExpDateShown: Boolean = true // show or hide expiry date option
    private var isDownloadCountFetched: Boolean = false

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
            file = it.getParcelable(ARG_OCFILE)
            shareeName = it.getString(ARG_SHAREE_NAME)
            share = it.getParcelable(ARG_OCSHARE)
            if (it.containsKey(ARG_SHARE_TYPE)) {
                shareType = it.getSerializable(ARG_SHARE_TYPE) as ShareType
            } else if (share != null) {
                shareType = share!!.shareType!!
            }

            shareProcessStep = it.getInt(ARG_SCREEN_TYPE, SCREEN_TYPE_PERMISSION)
            isReshareShown = it.getBoolean(ARG_RESHARE_SHOWN, true)
            isExpDateShown = it.getBoolean(ARG_EXP_DATE_SHOWN, true)
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
        viewThemeUtils.platform.colorPrimaryTextViewElement(binding.shareProcessEditShareLink)
        viewThemeUtils.platform.colorPrimaryTextViewElement(binding.shareProcessAdvancePermissionTitle)

        viewThemeUtils.platform.themeRadioButton(binding.shareProcessPermissionReadOnly)
        viewThemeUtils.platform.themeRadioButton(binding.shareProcessPermissionUploadEditing)
        viewThemeUtils.platform.themeRadioButton(binding.shareProcessPermissionFileDrop)

        viewThemeUtils.platform.themeCheckbox(binding.shareProcessAllowResharingCheckbox)

        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessSetPasswordSwitch)
        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessSetExpDateSwitch)
        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessHideDownloadCheckbox)
        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessChangeNameSwitch)
        viewThemeUtils.androidx.colorSwitchCompat(binding.shareProcessDownloadLimitSwitch)

        viewThemeUtils.material.colorTextInputLayout(binding.shareProcessEnterPasswordContainer)
        viewThemeUtils.material.colorTextInputLayout(binding.shareProcessChangeNameContainer)
        viewThemeUtils.material.colorTextInputLayout(binding.shareProcessDownloadLimitContainer)
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

        // show or hide expiry date
        if (isExpDateShown) {
            binding.shareProcessSetExpDateSwitch.visibility = View.VISIBLE
        } else {
            binding.shareProcessSetExpDateSwitch.visibility = View.GONE
        }
        shareProcessStep = SCREEN_TYPE_PERMISSION
    }

    private fun setupModificationUI() {
        if (share?.isFolder == true) {
            updateViewForFolder()
        } else {
            updateViewForFile()
        }

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
        if (shareType == ShareType.PUBLIC_LINK) {
            binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.share_copy_link)
        } else {
            binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.common_confirm)
        }
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

    /**
     * method to update views on the basis of Share type
     */
    private fun updateViewForShareType() {
        // external share
        if (shareType == ShareType.EMAIL) {
            hideLinkLabelViews()
            updateViewForExternalAndLinkShare()
        }
        // link share
        else if (shareType == ShareType.PUBLIC_LINK) {
            updateViewForExternalAndLinkShare()
            binding.shareProcessChangeNameSwitch.visibility = View.VISIBLE
            if (share != null) {
                binding.shareProcessChangeName.setText(share?.label)
                binding.shareProcessChangeNameSwitch.isChecked = !TextUtils.isEmpty(share?.label)
            }
            showChangeNameInput(binding.shareProcessChangeNameSwitch.isChecked)

            //download limit will only be available for files
            if (share?.isFolder == false || file?.isFolder == false) {
                binding.shareProcessDownloadLimitSwitch.visibility = View.VISIBLE

                //fetch the download limit for link share
                fetchDownloadLimitForShareLink()
            } else {
                binding.shareProcessDownloadLimitSwitch.visibility = View.GONE
            }

            //the input for download limit will be hidden initially
            //and can be visible back or no depending on the api result
            //from the download limit api
            binding.shareProcessDownloadLimitContainer.visibility = View.GONE
            binding.shareProcessRemainingDownloadCountTv.visibility = View.GONE

        }
        // internal share
        else {
            hideLinkLabelViews()
            binding.shareProcessHideDownloadCheckbox.visibility = View.GONE
            binding.shareProcessAllowResharingCheckbox.visibility = View.VISIBLE
            binding.shareProcessSetPasswordSwitch.visibility = View.GONE
            if (share != null) {
                if (!isReshareShown) {
                    binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
                }
                binding.shareProcessAllowResharingCheckbox.isChecked = SharingMenuHelper.canReshare(share)
            }
        }
    }

    private fun hideLinkLabelViews() {
        binding.shareProcessChangeNameSwitch.visibility = View.GONE
        binding.shareProcessChangeNameContainer.visibility = View.GONE

        binding.shareProcessDownloadLimitSwitch.visibility = View.GONE
        binding.shareProcessDownloadLimit.visibility = View.GONE
        binding.shareProcessRemainingDownloadCountTv.visibility = View.GONE
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
            if (share?.expirationDate ?: 0 > 0) {
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
            binding.noteText.setText("")
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
        binding.shareProcessDownloadLimitSwitch.setOnCheckedChangeListener { _, isChecked ->
            showDownloadLimitInput(isChecked)
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
            binding.shareProcessChangeName.setText("")
        }
    }

    private fun showDownloadLimitInput(isChecked: Boolean) {
        binding.shareProcessDownloadLimitContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        binding.shareProcessRemainingDownloadCountTv.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (!isChecked) {
            binding.shareProcessDownloadLimit.setText("")
            if (!isDownloadCountFetched) {
                binding.shareProcessRemainingDownloadCountTv.text =
                    String.format(resources.getString(R.string.download_text), "0")
            }
            //hide keyboard when user unchecks
            hideKeyboard()
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
            binding.shareProcessSelectExpDate.text = ""
        }
    }

    private fun showPasswordInput(isChecked: Boolean) {
        binding.shareProcessEnterPasswordContainer.visibility = if (isChecked) View.VISIBLE else View.GONE

        // reset the password if switch is unchecked
        if (!isChecked) {
            binding.shareProcessEnterPassword.setText("")
        }
    }

    private fun hideKeyboard() {
        if (this::binding.isInitialized) {
            keyboardUtils.hideKeyboardFrom(requireContext(), binding.root)
        }
    }

    private fun removeCurrentFragment() {
        onEditShareListener.onShareProcessClosed()
        fileActivity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    private fun getResharePermission(): Int {
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

        if (binding.shareProcessDownloadLimitSwitch.isChecked) {
            val downloadLimit = binding.shareProcessDownloadLimit.text?.trim()
            if (downloadLimit.isNullOrEmpty()) {
                DisplayUtils.showSnackMessage(binding.root, R.string.download_limit_empty)
                return
            } else if (downloadLimit.toString().toLong() <= 0) {
                DisplayUtils.showSnackMessage(binding.root, R.string.download_limit_zero)
                return
            }
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
        binding.shareProcessAllowResharingCheckbox.isChecked -> getResharePermission()
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
            binding.shareProcessChangeName.text.toString().trim(),
            binding.shareProcessDownloadLimit.text.toString().trim()
        )
        // copy the share link if available
        if (!TextUtils.isEmpty(share?.shareLink)) {
            ClipboardUtil.copyToClipboard(activity, share?.shareLink)
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
                binding.shareProcessChangeName.text.toString().trim()
            )
        }
        removeCurrentFragment()
    }

    /**
     * fetch the download limit for the link share
     * the response will be received in FileActivity --> onRemoteOperationFinish() method
     */
    private fun fetchDownloadLimitForShareLink() {
        //need to call this method in handler else to show progress dialog it will throw exception
        Handler(Looper.getMainLooper()).post {
            share?.let {
                fileOperationsHelper?.getShareDownloadLimit(it.token)
            }
        }
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

    /**
     * will be called when download limit is fetched
     */
    fun onLinkShareDownloadLimitFetched(downloadLimit: Long, downloadCount: Long) {
        binding.shareProcessDownloadLimitSwitch.isChecked = downloadLimit > 0
        showDownloadLimitInput(binding.shareProcessDownloadLimitSwitch.isChecked)
        binding.shareProcessDownloadLimit.setText(if (downloadLimit > 0) downloadLimit.toString() else "")
        binding.shareProcessRemainingDownloadCountTv.text =
            String.format(resources.getString(R.string.download_text), downloadCount.toString())
        isDownloadCountFetched = true
    }
}
