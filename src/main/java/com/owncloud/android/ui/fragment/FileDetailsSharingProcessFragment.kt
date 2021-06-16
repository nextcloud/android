package com.owncloud.android.ui.fragment

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.fragment.app.Fragment
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsSharingProcessFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.SharePermissionsBuilder
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.fragment.util.SharingMenuHelper
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.DisplayUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class FileDetailsSharingProcessFragment : Fragment(), OnDateSetListener {

    companion object {
        const val TAG = "FileDetailsSharingProcessFragment"
        private const val ARG_OCFILE = "arg_sharing_oc_file"
        private const val ARG_SHAREE_NAME = "arg_sharee_name"
        private const val ARG_SHARE_TYPE = "arg_share_type"
        private const val ARG_OCSHARE = "arg_ocshare"
        private const val ARG_SCREEN_TYPE = "arg_screen_type"
        private const val ARG_RESHARE_SHOWN = "arg_reshare_shown"
        private const val ARG_EXP_DATE_SHOWN = "arg_exp_date_shown"

        const val SCREEN_TYPE_PERMISSION = 1
        const val SCREEN_TYPE_NOTE = 2

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

    private lateinit var binding: FileDetailsSharingProcessFragmentBinding
    private var fileOperationsHelper: FileOperationsHelper? = null

    private var file: OCFile? = null
    private var shareeName: String? = null
    private lateinit var shareType: ShareType
    private var shareProcessStep = SCREEN_TYPE_PERMISSION
    private var permission = -1
    private var fileActivity: FileActivity? = null
    private var chosenExpDateInMills: Long = -1 //for no expiry date

    private var share: OCShare? = null
    private var isReshareShown: Boolean = true
    private var isExpDateShown: Boolean = true

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
    }

    private fun showShareProcessFirst() {
        binding.shareProcessGroupOne.visibility = View.VISIBLE
        binding.shareProcessGroupTwo.visibility = View.GONE
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
            } else if (SharingMenuHelper.isFileDrop(share) && share!!.isFolder) {
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
        } else {
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
        if (isExpDateShown) {
            binding.shareProcessSetExpDateSwitch.visibility = View.VISIBLE
        } else {
            binding.shareProcessSetExpDateSwitch.visibility = View.GONE
        }
        shareProcessStep = SCREEN_TYPE_PERMISSION
    }

    private fun updateViewForShareType() {
        //external share
        if (shareType == ShareType.EMAIL) {
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
        //internal share
        else {
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

    private fun showShareProcessSecond() {
        binding.shareProcessGroupOne.visibility = View.GONE
        binding.shareProcessGroupTwo.visibility = View.VISIBLE
        if (share != null) {
            binding.shareProcessBtnNext.text = requireContext().resources.getString(R.string.send_email)
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
        binding.shareProcessSelectExpDate.setOnClickListener {
            showExpirationDateDialog()
        }
    }

    private fun onCancelClick() {
        if (share != null) {
            removeCurrentFragment()
        } else {
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
        if (!isChecked) {
            chosenExpDateInMills = -1
            binding.shareProcessSelectExpDate.text = ""
        }
    }

    private fun showPasswordInput(isChecked: Boolean) {
        binding.shareProcessEnterPassword.visibility = if (isChecked) View.VISIBLE else View.GONE
        if (!isChecked) {
            binding.shareProcessEnterPassword.setText("")
        }
    }

    private fun removeCurrentFragment() {
        fileActivity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    private fun getResharePermission(): Int {
        val spb = SharePermissionsBuilder()
        spb.setSharePermission(true)
        return spb.build()
    }

    private fun validateShareProcessFirst() {
        when {
            binding.shareProcessPermissionReadOnly.isChecked -> {
                permission = OCShare.READ_PERMISSION_FLAG
            }
            binding.shareProcessPermissionUploadEditing.isChecked -> {
                permission = if (file?.isFolder == true || share?.isFolder == true) {
                    OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER
                } else {
                    OCShare.MAXIMUM_PERMISSIONS_FOR_FILE
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
            DisplayUtils.showSnackMessage(binding.root, R.string.share_link_empty_exp_date)
            return
        }

        if (share != null) {
            fileOperationsHelper?.updateShareInformation(
                share, permission, binding
                    .shareProcessHideDownloadCheckbox.isChecked,
                binding.shareProcessEnterPassword.text.toString().trim(),
                chosenExpDateInMills
            )
            removeCurrentFragment()
        } else {
            showShareProcessSecond()
        }
    }

    private fun validateShareProcessSecond() {
        if (TextUtils.isEmpty(binding.noteText.text.toString().trim())) {
            DisplayUtils.showSnackMessage(binding.root, R.string.share_link_empty_note_message)
            return
        }
        if (share != null) {
            fileOperationsHelper?.updateNoteToShare(share, binding.noteText.text.toString().trim())
        } else {
            fileOperationsHelper?.shareFileWithSharee(
                file,
                shareeName,
                shareType,
                permission,
                binding
                    .shareProcessHideDownloadCheckbox.isChecked,
                binding.shareProcessEnterPassword.text.toString().trim(),
                chosenExpDateInMills,
                binding.noteText.text.toString().trim()
            )
        }
        removeCurrentFragment()
    }

    private fun showExpirationDateDialog() {
        // Chosen date received as an argument must be later than tomorrow ; default to tomorrow in other case
        val chosenDate = Calendar.getInstance()
        val tomorrowInMillis = chosenDate.timeInMillis + DateUtils.DAY_IN_MILLIS
        chosenDate.timeInMillis = Math.max(chosenExpDateInMills, tomorrowInMillis)
        // Create a new instance of DatePickerDialog
        val dialog = DatePickerDialog(
            requireActivity(),
            this,
            chosenDate[Calendar.YEAR],
            chosenDate[Calendar.MONTH],
            chosenDate[Calendar.DAY_OF_MONTH]
        )
        dialog.show()

        // Prevent days in the past may be chosen
        val picker = dialog.datePicker
        picker.minDate = tomorrowInMillis - 1000

        // Enforce spinners view; ignored by MD-based theme in Android >=5, but calendar is REALLY buggy
        // in Android < 5, so let's be sure it never appears (in tablets both spinners and calendar are
        // shown by default)
        picker.calendarViewShown = false
    }

    /**
     * Called when the user chooses an expiration date.
     *
     * @param view        View instance where the date was chosen
     * @param year        Year of the date chosen.
     * @param monthOfYear Month of the date chosen [0, 11]
     * @param dayOfMonth  Day of the date chosen
     */
    override fun onDateSet(view: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val chosenDate = Calendar.getInstance()
        chosenDate[Calendar.YEAR] = year
        chosenDate[Calendar.MONTH] = monthOfYear
        chosenDate[Calendar.DAY_OF_MONTH] = dayOfMonth
        val chosenDateInMillis = chosenDate.timeInMillis
        binding.shareProcessSelectExpDate.text = (resources.getString(
            R.string.share_expiration_date_format,
            SimpleDateFormat.getDateInstance().format(Date(chosenDateInMillis))
        ))
        this.chosenExpDateInMills = chosenDateInMillis
    }

    fun onBackPressed() {
        onCancelClick()
    }
}