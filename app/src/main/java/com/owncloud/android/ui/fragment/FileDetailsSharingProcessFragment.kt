/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2021 TSI-mc
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
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RadioGroup.OnCheckedChangeListener
import androidx.fragment.app.Fragment
import com.nmc.android.utils.KeyboardUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsSharingProcessFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment
import com.owncloud.android.ui.fragment.util.SharingMenuHelper
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ThemeButtonUtils
import com.owncloud.android.utils.theme.ThemeCheckableUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment class to show share permission options, set expiration date, change label, set password, send note
 *
 * This fragment handles following:
 * 1. This will be shown while creating new internal and external share. So that user can set every share
 * configuration at one time.
 * 2. This will handle both Advanced Permissions and Send New Email functionality for existing shares to modify them.
 */
@Suppress("TooManyFunctions")
class FileDetailsSharingProcessFragment : Fragment(), ExpirationDatePickerDialogFragment.OnExpiryDateListener, OnCheckedChangeListener {

    companion object {
        const val TAG = "FileDetailsSharingProcessFragment"
        private const val ARG_OCFILE = "arg_sharing_oc_file"
        private const val ARG_SHAREE_NAME = "arg_sharee_name"
        private const val ARG_SHARE_TYPE = "arg_share_type"
        private const val ARG_OCSHARE = "arg_ocshare"
        private const val ARG_SCREEN_TYPE = "arg_screen_type"
        private const val ARG_RESHARE_SHOWN = "arg_reshare_shown"
        private const val ARG_EXP_DATE_SHOWN = "arg_exp_date_shown"
        private const val ARG_NO_TEXT_FILE = "is_file_with_no_text_file"

        //types of screens to be displayed
        const val SCREEN_TYPE_PERMISSION = 1 // permissions screen
        const val SCREEN_TYPE_NOTE = 2 //note screen

        /**
         * fragment instance to be called while creating new share for internal and external share
         */
        @JvmStatic
        fun newInstance(file: OCFile, shareeName: String, shareType: ShareType, isFileWithNoTextFile: Boolean):
            FileDetailsSharingProcessFragment {
            val args = Bundle()
            args.putParcelable(ARG_OCFILE, file)
            args.putSerializable(ARG_SHARE_TYPE, shareType)
            args.putString(ARG_SHAREE_NAME, shareeName)
            args.putBoolean(ARG_NO_TEXT_FILE, isFileWithNoTextFile)
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
            isExpirationDateShown: Boolean,
            isFileWithNoTextFile: Boolean
        ):
            FileDetailsSharingProcessFragment {
            val args = Bundle()
            args.putParcelable(ARG_OCSHARE, share)
            args.putInt(ARG_SCREEN_TYPE, screenType)
            args.putBoolean(ARG_RESHARE_SHOWN, isReshareShown)
            args.putBoolean(ARG_EXP_DATE_SHOWN, isExpirationDateShown)
            args.putBoolean(ARG_NO_TEXT_FILE, isFileWithNoTextFile)
            val fragment = FileDetailsSharingProcessFragment()
            fragment.arguments = args
            return fragment
        }
    }

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
    private var isFileWithNoTextFile: Boolean = false

    private var share: OCShare? = null
    private var isReshareShown: Boolean = true // show or hide reshare option
    private var isExpDateShown: Boolean = true // show or hide expiry date option
    private var isHideDownloadCheckedReadOnly: Boolean = false
    private var isHideDownloadCheckedUploadEdit: Boolean = false

    private var isFileDropSelected: Boolean = false
    private var isReadOnlySelected: Boolean = false
    private var isUploadEditingSelected: Boolean = false
    private var isDownloadCountFetched: Boolean = false


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
            if (it.containsKey(ARG_SHARE_TYPE)) {
                shareType = it.getSerializable(ARG_SHARE_TYPE) as ShareType
            }
            share = it.getParcelable(ARG_OCSHARE)
            shareProcessStep = it.getInt(ARG_SCREEN_TYPE, SCREEN_TYPE_PERMISSION)
            isReshareShown = it.getBoolean(ARG_RESHARE_SHOWN, true)
            isExpDateShown = it.getBoolean(ARG_EXP_DATE_SHOWN, true)
            isFileWithNoTextFile = it.getBoolean(ARG_NO_TEXT_FILE, false)
        }

        fileActivity = activity as FileActivity?

        requireNotNull(fileActivity) { "FileActivity may not be null" }
    }

    //Updating Hide Download enable/disable on selection of FileDrop
    override fun onCheckedChanged(group: RadioGroup?, checkId: Int) {
        if (binding.shareProcessPermissionFileDrop.id == checkId) {
            isFileDropSelected = true
            binding.shareProcessHideDownloadCheckbox.isChecked = true
            binding.shareProcessHideDownloadCheckbox.isEnabled = false
        } else {
            isFileDropSelected = false
            binding.shareProcessHideDownloadCheckbox.isEnabled = true
            if (binding.shareProcessPermissionReadOnly.id == checkId) {
                isReadOnlySelected = true;
                isUploadEditingSelected = false;
                binding.shareProcessHideDownloadCheckbox.isChecked = isHideDownloadCheckedReadOnly
            } else if (binding.shareProcessPermissionUploadEditing.id == checkId) {
                isReadOnlySelected = false
                isUploadEditingSelected = true
                binding.shareProcessHideDownloadCheckbox.isChecked = isHideDownloadCheckedUploadEdit
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FileDetailsSharingProcessFragmentBinding.inflate(inflater, container, false)
        fileOperationsHelper = fileActivity?.fileOperationsHelper
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (shareProcessStep == SCREEN_TYPE_PERMISSION) {
            binding.shareProcessPermissionReadOnly.isChecked = true
            showShareProcessFirst()
        } else {
            showShareProcessSecond()
        }
        //Set default value to 0 for download count
         if(!isDownloadCountFetched) {
                binding.shareProcessRemainingDownloadCountTv.text = getString(R.string.download_text) + "0"
            }
        //  ThemeCheckableUtils.tintSwitch(binding.shareProcessHideDownloadCheckbox, 0)
        binding.shareProcessPermissionRadioGroup.setOnCheckedChangeListener(this)
        ThemeButtonUtils.colorPrimaryButton(binding.shareProcessBtnNext, requireContext())
        implementClickEvents()

        binding.shareProcessHideDownloadCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (!isFileDropSelected) {
                if (isReadOnlySelected) {
                    isHideDownloadCheckedReadOnly = isChecked
                } else if (isUploadEditingSelected) {
                    isHideDownloadCheckedUploadEdit = isChecked
                }
            }
        }
    }

    private fun scrollTopShowToolbar() {
        //show the toolbar if it is hidden due to scrolling
        if (requireActivity() is ToolbarActivity) {
            (requireActivity() as ToolbarActivity).expandToolbar()
        }
    }

    private fun showShareProcessFirst() {
        scrollTopShowToolbar()
        binding.shareProcessGroupOne.visibility = View.VISIBLE
        binding.shareProcessGroupTwo.visibility = View.GONE
        binding.tvSetPasswordEmailWarning.visibility = View.GONE

        //set up UI for modifying share
        if (share != null) {
            binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.common_confirm)

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
            updateViewForShareType()
            binding.shareProcessSetPasswordSwitch.isChecked = share?.isPasswordProtected == true
            showPasswordInput(binding.shareProcessSetPasswordSwitch.isChecked)
            updateExpirationDateView()
            showExpirationDateInput(binding.shareProcessSetExpDateSwitch.isChecked)
        }

        //update UI for creating new share
        else {
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

        //show or hide expiry date
        if (isExpDateShown) {
            binding.shareProcessSetExpDateSwitch.visibility = View.VISIBLE
            binding.dividerSharingExpDate.visibility = View.VISIBLE
        } else {
            binding.shareProcessSetExpDateSwitch.visibility = View.GONE
            binding.dividerSharingExpDate.visibility = View.GONE
        }
        shareProcessStep = SCREEN_TYPE_PERMISSION
    }

    /**
     * method to update views on the basis of Share type
     */
    private fun updateViewForShareType() {
        //external share
        if (shareType == ShareType.EMAIL) {
            hideLinkLabelViews()
            updateViewForExternalAndLinkShare()
            updateFileEditingRadioButton()
        }
        //link share
        else if (shareType == ShareType.PUBLIC_LINK) {
            updateViewForExternalAndLinkShare()
            binding.shareProcessChangeNameSwitch.visibility = View.VISIBLE
            binding.dividerSharingChangeName.visibility = View.VISIBLE
            if (share != null) {
                binding.shareProcessChangeNameEt.setText(share?.label)
                binding.shareProcessChangeNameSwitch.isChecked = !TextUtils.isEmpty(share?.label)
            }
            showChangeNameInput(binding.shareProcessChangeNameSwitch.isChecked)

            //download limit will only be available for Files
            if (share?.isFolder == false || file?.isFolder == false) {
                binding.shareProcessDownloadLimitSwitch.visibility = View.VISIBLE
                binding.dividerSharingDownloadLimit.visibility = View.VISIBLE

                //fetch the download limit for link share
                fetchDownloadLimitForShareLink()
            } else {
                binding.shareProcessDownloadLimitSwitch.visibility = View.GONE
                binding.dividerSharingDownloadLimit.visibility = View.GONE
            }

            //the input for download limit will be hidden initially
            //and can be visible back or no depending on the api result
            //from the download limit api
            binding.shareProcessDownloadLimitEt.visibility = View.GONE
            binding.shareProcessRemainingDownloadCountTv.visibility = View.GONE

            updateFileEditingRadioButton()

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
                if (share?.isFolder == true) {
                    hideFileDropView()
                }
            } else if (file?.isFolder == true) {
                hideFileDropView()
            }
        }
    }

    private fun hideFileDropView() {
        //no file drop for internal share due to 403 bad request api issue
        binding.shareProcessPermissionFileDrop.visibility = View.GONE
        binding.shareFileDropInfo.visibility = View.GONE
    }

    private fun hideLinkLabelViews() {
        binding.shareProcessChangeNameSwitch.visibility = View.GONE
        binding.shareProcessChangeNameEt.visibility = View.GONE
        binding.dividerSharingChangeName.visibility = View.GONE

        binding.shareProcessDownloadLimitSwitch.visibility = View.GONE
        binding.shareProcessDownloadLimitEt.visibility = View.GONE
        binding.shareProcessRemainingDownloadCountTv.visibility = View.GONE
        binding.dividerSharingDownloadLimit.visibility = View.GONE
    }

    private fun updateFileEditingRadioButton() {
        if (isFileWithNoTextFile) {
            binding.shareProcessPermissionUploadEditing.isEnabled = false
            binding.shareProcessPermissionUploadEditing.setTextColor(resources.getColor(R.color.share_disabled_txt_color))
        }
    }

    /**
     * update views where share type external or link share
     */
    private fun updateViewForExternalAndLinkShare() {
        binding.shareProcessHideDownloadCheckbox.isEnabled = true;
        binding.dividerSharingHideDownload.visibility = View.VISIBLE
        binding.shareProcessAllowResharingCheckbox.visibility = View.GONE
        binding.shareProcessAllowResharingInfo.visibility = View.GONE
        binding.dividerSharingAllowResharing.visibility = View.GONE
        binding.shareProcessSetPasswordSwitch.visibility = View.VISIBLE
        binding.dividerSharingEnterPassword.visibility = View.VISIBLE

        if (share != null) {
            if (SharingMenuHelper.isFileDrop(share)) {
                binding.shareProcessHideDownloadCheckbox.isChecked = true;
                binding.shareProcessHideDownloadCheckbox.isEnabled = false;
                binding.dividerSharingHideDownload.visibility = View.GONE
            } else {
                binding.shareProcessHideDownloadCheckbox.isEnabled = true;
                binding.dividerSharingHideDownload.visibility = View.VISIBLE
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
        binding.shareFileDropInfo.visibility = View.GONE
    }

    private fun updateViewForFolder() {
        binding.shareProcessPermissionUploadEditing.text =
            requireContext().resources.getString(R.string.link_share_allow_upload_and_editing)
        binding.shareProcessPermissionFileDrop.visibility = View.VISIBLE
        binding.shareFileDropInfo.visibility = View.VISIBLE
    }

    /**
     * update views for screen type Note
     */
    private fun showShareProcessSecond() {
        scrollTopShowToolbar()
        binding.shareProcessGroupOne.visibility = View.GONE
        binding.shareProcessGroupTwo.visibility = View.VISIBLE
        if (share != null) {
            binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.send_email)
            binding.noteText.setText(share?.note)

            //show the warning label if password protection is enabled
            /* binding.tvSetPasswordEmailWarning.visibility =
                 if (share?.isPasswordProtected == true) View.VISIBLE
                 else View.GONE*/
        } else {
            binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.send_share)
            binding.noteText.setText("")

            //show the warning label if password protection is enabled
            /* binding.tvSetPasswordEmailWarning.visibility =
                 if (binding.shareProcessSetPasswordSwitch.isChecked) View.VISIBLE
                 else View.GONE*/
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
        binding.noteText.setOnTouchListener { view, event ->
            view.parent.requestDisallowInterceptTouchEvent(true)
            if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                view.parent.requestDisallowInterceptTouchEvent(false)
            }
            return@setOnTouchListener false
        }

    }

    private fun showExpirationDateDialog() {
        val dialog = ExpirationDatePickerDialogFragment.newInstance(chosenExpDateInMills)
        dialog.setOnExpiryDateListener(this)
        fileActivity?.let { it1 ->
            dialog.show(
                it1.supportFragmentManager,
                ExpirationDatePickerDialogFragment.DATE_PICKER_DIALOG
            )
        }
    }

    private fun showChangeNameInput(isChecked: Boolean) {
        binding.shareProcessChangeNameEt.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (!isChecked) {
            binding.shareProcessChangeNameEt.setText("")
            //hide keyboard when user unchecks
            hideKeyboard()
        }
    }

    private fun showDownloadLimitInput(isChecked: Boolean) {
        binding.shareProcessDownloadLimitEt.visibility = if (isChecked) View.VISIBLE else View.GONE
        binding.shareProcessRemainingDownloadCountTv.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (!isChecked) {
            binding.shareProcessDownloadLimitEt.setText("")
            if(!isDownloadCountFetched) {
                binding.shareProcessRemainingDownloadCountTv.text = getString(R.string.download_text) + "0"
            }
            //hide keyboard when user unchecks
            hideKeyboard()
        }
    }

    private fun onCancelClick() {
        //hide keyboard when user clicks cancel button
        hideKeyboard()
        //if modifying the existing share then on back press remove the current fragment
        if (share != null) {
            removeCurrentFragment()
        }
        //else we have to check if user is in step 2(note screen) then show step 1 (permission screen)
        //and if user is in step 1 (permission screen) then remove the fragment
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

        //update margin of divider when switch is enabled/disabled
        val margin = if (isChecked) requireContext().resources.getDimensionPixelOffset(R.dimen.standard_half_margin)
        else 0
        val param = binding.dividerSharingExpDate.layoutParams as ViewGroup.MarginLayoutParams
        param.setMargins(0, margin, 0, 0)
        binding.dividerSharingExpDate.layoutParams = param

        //reset the expiration date if switch is unchecked
        if (!isChecked) {
            chosenExpDateInMills = -1
            binding.shareProcessSelectExpDate.text = ""
        }
    }

    private fun showPasswordInput(isChecked: Boolean) {
        binding.shareProcessEnterPassword.visibility = if (isChecked) View.VISIBLE else View.GONE

        //reset the password if switch is unchecked
        if (!isChecked) {
            binding.shareProcessEnterPassword.setText("")
            //hide keyboard when user unchecks
            hideKeyboard()
        }
    }

    private fun hideKeyboard() {
        KeyboardUtils.hideKeyboardFrom(requireContext(), binding.root)
    }

    /**
     * remove the fragment and pop it from backstack because we are adding it to backstack
     */
    private fun removeCurrentFragment() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun getResharePermission(): Int {
        val spb = SharePermissionsBuilder()
        spb.setSharePermission(true)
        return spb.build()
    }

    /**
     * method to validate the step 1 screen information
     */
    private fun validateShareProcessFirst() {
        hideKeyboard()
        //get the permissions on the basis of selection
        when {
            binding.shareProcessPermissionReadOnly.isChecked -> {
                permission = OCShare.READ_PERMISSION_FLAG
            }
            binding.shareProcessPermissionUploadEditing.isChecked -> {
                permission = if (file?.isFolder == true || share?.isFolder == true) {
                    SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER
                } else {
                    SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
                }
            }
            binding.shareProcessPermissionFileDrop.isChecked -> {
                permission = OCShare.CREATE_PERMISSION_FLAG
            }
        }

        if (binding.shareProcessAllowResharingCheckbox.isChecked) {
            permission = getResharePermission()
        }

        if (permission == OCShare.NO_PERMISSION) {
            DisplayUtils.showSnackMessage(binding.root, R.string.no_share_permission_selected)
            return
        }

        if (binding.shareProcessSetPasswordSwitch.isChecked && TextUtils.isEmpty(
                binding.shareProcessEnterPassword
                    .text.toString().trim()
            )
        ) {
            DisplayUtils.showSnackMessage(binding.root, R.string.share_link_empty_password)
            return
        }

        if (binding.shareProcessSetExpDateSwitch.isChecked && TextUtils.isEmpty(
                binding.shareProcessSelectExpDate
                    .text.toString().trim()
            )
        ) {
            showExpirationDateDialog()
            return
        }

        if (binding.shareProcessChangeNameSwitch.isChecked && TextUtils.isEmpty(
                binding.shareProcessChangeNameEt
                    .text.toString().trim()
            )
        ) {
            DisplayUtils.showSnackMessage(binding.root, R.string.label_empty)
            return
        }

        val downloadLimit = binding.shareProcessDownloadLimitEt
            .text.toString().trim()

        if (binding.shareProcessDownloadLimitSwitch.isChecked) {
            if (TextUtils.isEmpty(downloadLimit)) {
                DisplayUtils.showSnackMessage(binding.root, R.string.download_limit_empty)
                return
            } else if (downloadLimit.toLong() <= 0) {
                DisplayUtils.showSnackMessage(binding.root, R.string.download_limit_zero)
                return
            }
        }

        //if modifying existing share information then execute the process
        if (share != null) {
            fileOperationsHelper?.updateShareInformation(
                share, permission, binding
                .shareProcessHideDownloadCheckbox.isChecked,
                binding.shareProcessEnterPassword.text.toString().trim(),
                chosenExpDateInMills, binding.shareProcessChangeNameEt.text.toString().trim(),
                downloadLimit
            )
            removeCurrentFragment()
        } else {
            //else show step 2 (note screen)
            showShareProcessSecond()
        }
    }

    /**
     * method to validate step 2 (note screen) information
     */
    private fun validateShareProcessSecond() {
        hideKeyboard()
        //if modifying existing share then directly update the note and send email
        if (share != null) {
            if (TextUtils.isEmpty(binding.noteText.text.toString().trim())) {
                DisplayUtils.showSnackMessage(binding.root, R.string.share_link_empty_note_message)
                return
            }
            fileOperationsHelper?.updateNoteToShare(share, binding.noteText.text.toString().trim())
        } else {
            //else create new share
            fileOperationsHelper?.shareFileWithSharee(
                file,
                shareeName,
                shareType,
                permission,
                binding
                    .shareProcessHideDownloadCheckbox.isChecked,
                binding.shareProcessEnterPassword.text.toString().trim(),
                chosenExpDateInMills,
                binding.noteText.text.toString().trim(),
                binding.shareProcessChangeNameEt.text.toString().trim()
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
        val uiHandler = Handler()
        uiHandler.post {
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
        binding.shareProcessSelectExpDate.text = (resources.getString(
            R.string.share_expiration_date_format,
            SimpleDateFormat.getDateInstance().format(Date(chosenDateInMillis))
        ))
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
        binding.shareProcessDownloadLimitEt.setText(if (downloadLimit > 0) downloadLimit.toString() else "")
        binding.shareProcessRemainingDownloadCountTv.text = getString(R.string.download_text) + downloadCount.toString()
        isDownloadCountFetched = true
    }
}