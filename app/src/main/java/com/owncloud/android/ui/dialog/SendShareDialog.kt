/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2018 Andy Scherzinger
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.utils.IntentUtil.createSendIntent
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.databinding.SendShareFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.SendButtonAdapter
import com.owncloud.android.ui.components.SendButtonData
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class SendShareDialog : BottomSheetDialogFragment(R.layout.send_share_fragment), Injectable {

    private lateinit var binding: SendShareFragmentBinding

    private var file: OCFile? = null
    private var hideNcSharingOptions = false
    private var sharingPublicPasswordEnforced = false
    private var sharingPublicAskForPassword = false
    private var fileOperationsHelper: FileOperationsHelper? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // keep the state of the fragment on configuration changes
        retainInstance = true
        val arguments = requireArguments()

        file = arguments.getParcelableArgument(KEY_OCFILE, OCFile::class.java)
        hideNcSharingOptions = arguments.getBoolean(KEY_HIDE_NCSHARING_OPTIONS, false)
        sharingPublicPasswordEnforced = arguments.getBoolean(KEY_SHARING_PUBLIC_PASSWORD_ENFORCED, false)
        sharingPublicAskForPassword = arguments.getBoolean(KEY_SHARING_PUBLIC_ASK_FOR_PASSWORD)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SendShareFragmentBinding.inflate(inflater, container, false)

        binding.btnShare.setOnClickListener { shareFile(file) }
        binding.btnLink.setOnClickListener { shareByLink() }

        applyTintColor()
        setupBottomSheetBehaviour()
        checkButtonVisibilities()
        setupSendButtonRecyclerView()

        return binding.root
    }

    @Suppress("MagicNumber")
    private fun setupSendButtonRecyclerView() {
        val sendIntent = createSendIntent(requireContext(), file!!)
        val sendButtonDataList = setupSendButtonData(sendIntent)
        val clickListener = setupSendButtonClickListener(sendIntent)

        binding.sendButtonRecyclerView.layoutManager = GridLayoutManager(activity, 4)
        binding.sendButtonRecyclerView.adapter = SendButtonAdapter(sendButtonDataList, clickListener)
    }

    private fun setupBottomSheetBehaviour() {
        val bottomSheetDialog = dialog as BottomSheetDialog
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true
    }

    private fun applyTintColor() {
        viewThemeUtils?.material?.colorMaterialButtonPrimaryFilled(binding.btnLink)
        viewThemeUtils?.material?.colorMaterialButtonPrimaryFilled(binding.btnShare)
        viewThemeUtils?.platform?.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)
    }

    @Suppress("MagicNumber")
    private fun checkButtonVisibilities() {
        if (hideNcSharingOptions) {
            binding.sendShareButtons.visibility = View.GONE
            binding.divider.visibility = View.GONE
        } else if (file?.isSharedWithMe == true && file?.canReshare() == false) {
            showSharingNotAllowedMessage()

            if (file?.isFolder == true) {
                binding.btnShare.visibility = View.GONE
                binding.btnLink.visibility = View.GONE
                dialog!!.hide()
            } else {
                binding.btnLink.isEnabled = false
                binding.btnLink.alpha = 0.3f

                binding.btnShare.isEnabled = false
                binding.btnShare.alpha = 0.3f
            }
        }
    }

    private fun shareByLink() {
        val fileOperationsHelper = (requireActivity() as FileActivity).fileOperationsHelper

        if (file?.isSharedViaLink == true) {
            fileOperationsHelper.getFileWithLink(file!!, viewThemeUtils)
        } else if (sharingPublicPasswordEnforced || sharingPublicAskForPassword) {
            // password enforced by server, request to the user before trying to create
            requestPasswordForShareViaLink()
        } else {
            // create without password if not enforced by server or we don't know if enforced;
            fileOperationsHelper.shareFileViaPublicShare(file, null)
        }

        dismiss()
    }

    private fun requestPasswordForShareViaLink() {
        val dialog = SharePasswordDialogFragment.newInstance(
            file,
            true,
            sharingPublicAskForPassword
        )

        dialog.show(parentFragmentManager, SharePasswordDialogFragment.PASSWORD_FRAGMENT)
    }

    private fun showSharingNotAllowedMessage() {
        val message = Snackbar.make(binding.root, R.string.resharing_is_not_allowed, Snackbar.LENGTH_LONG)

        message.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (file!!.isFolder) {
                    dismiss()
                }
            }
        })

        message.show()
    }

    private fun setupSendButtonClickListener(sendIntent: Intent): SendButtonAdapter.ClickListener {
        return SendButtonAdapter.ClickListener { sendButtonDataData: SendButtonData ->
            val packageName = sendButtonDataData.packageName
            val activityName = sendButtonDataData.activityName

            if (MimeTypeUtil.isImage(file) && !file!!.isDown) {
                fileOperationsHelper?.sendCachedImage(file, packageName, activityName)
            } else {
                // Obtain the file
                if (file!!.isDown) {
                    sendIntent.component = ComponentName(packageName, activityName)
                    requireActivity().startActivity(Intent.createChooser(sendIntent, getString(R.string.send)))
                } else { // Download the file
                    Log_OC.d(TAG, file!!.remotePath + ": File must be downloaded")
                    (requireActivity() as SendShareDialogDownloader)
                        .downloadFile(file, packageName, activityName)
                }
            }

            dismiss()
        }
    }

    private fun setupSendButtonData(sendIntent: Intent): List<SendButtonData> {
        var icon: Drawable
        var sendButtonData: SendButtonData
        var label: CharSequence
        val matches = requireActivity().packageManager.queryIntentActivities(sendIntent, 0)
        val sendButtonDataList: MutableList<SendButtonData> = ArrayList(matches.size)
        for (match in matches) {
            icon = match.loadIcon(requireActivity().packageManager)
            label = match.loadLabel(requireActivity().packageManager)
            sendButtonData = SendButtonData(
                icon,
                label,
                match.activityInfo.packageName,
                match.activityInfo.name
            )
            sendButtonDataList.add(sendButtonData)
        }
        return sendButtonDataList
    }

    private fun shareFile(file: OCFile?) {
        dismiss()

        if (activity is FileDisplayActivity) {
            (activity as FileDisplayActivity?)?.showDetails(file, 1)
        } else {
            fileOperationsHelper?.showShareFile(file)
        }
    }

    fun setFileOperationsHelper(fileOperationsHelper: FileOperationsHelper?) {
        this.fileOperationsHelper = fileOperationsHelper
    }

    interface SendShareDialogDownloader {
        fun downloadFile(file: OCFile?, packageName: String?, activityName: String?)
    }

    companion object {
        private const val KEY_OCFILE = "KEY_OCFILE"
        private const val KEY_SHARING_PUBLIC_PASSWORD_ENFORCED = "KEY_SHARING_PUBLIC_PASSWORD_ENFORCED"
        private const val KEY_SHARING_PUBLIC_ASK_FOR_PASSWORD = "KEY_SHARING_PUBLIC_ASK_FOR_PASSWORD"
        private const val KEY_HIDE_NCSHARING_OPTIONS = "KEY_HIDE_NCSHARING_OPTIONS"
        private val TAG = SendShareDialog::class.java.simpleName
        const val PACKAGE_NAME = "PACKAGE_NAME"
        const val ACTIVITY_NAME = "ACTIVITY_NAME"

        @JvmStatic
        fun newInstance(file: OCFile?, hideNcSharingOptions: Boolean, capability: OCCapability): SendShareDialog {
            val dialogFragment = SendShareDialog()
            val args = Bundle()
            args.putParcelable(KEY_OCFILE, file)
            args.putBoolean(KEY_HIDE_NCSHARING_OPTIONS, hideNcSharingOptions)
            args.putBoolean(
                KEY_SHARING_PUBLIC_PASSWORD_ENFORCED,
                capability.filesSharingPublicPasswordEnforced.isTrue
            )
            args.putBoolean(
                KEY_SHARING_PUBLIC_ASK_FOR_PASSWORD,
                capability.filesSharingPublicAskForOptionalPassword.isTrue
            )
            dialogFragment.arguments = args
            return dialogFragment
        }
    }
}
