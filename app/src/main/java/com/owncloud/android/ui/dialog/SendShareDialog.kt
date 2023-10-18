package com.owncloud.android.ui.dialog

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.utils.IntentUtil.createSendIntent
import com.owncloud.android.R
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

/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Andy Scherzinger
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
class SendShareDialog : BottomSheetDialogFragment(), Injectable {
    private lateinit var view: View
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

        file = arguments.getParcelable(KEY_OCFILE)
        hideNcSharingOptions = arguments.getBoolean(KEY_HIDE_NCSHARING_OPTIONS, false)
        sharingPublicPasswordEnforced = arguments.getBoolean(KEY_SHARING_PUBLIC_PASSWORD_ENFORCED, false)
        sharingPublicAskForPassword = arguments.getBoolean(KEY_SHARING_PUBLIC_ASK_FOR_PASSWORD)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.send_share_fragment, container, false)
        val sendShareButtons = view.findViewById<LinearLayout>(R.id.send_share_buttons)
        val divider = view.findViewById<View>(R.id.divider)

        // Share with people
        val sharePeopleText = view.findViewById<TextView>(R.id.share_people_button)
        sharePeopleText.setOnClickListener { shareFile(file) }
        val sharePeopleImageView = view.findViewById<ImageView>(R.id.share_people_icon)
        themeShareButtonImage(sharePeopleImageView)
        sharePeopleImageView.setOnClickListener { shareFile(file) }

        // Share via link button
        val shareLinkText = view.findViewById<TextView>(R.id.share_link_button)
        shareLinkText.setOnClickListener { shareByLink() }
        val shareLinkImageView = view.findViewById<ImageView>(R.id.share_link_icon)
        themeShareButtonImage(shareLinkImageView)
        shareLinkImageView.setOnClickListener { shareByLink() }

        if (hideNcSharingOptions) {
            sendShareButtons.visibility = View.GONE
            divider.visibility = View.GONE
        } else if (file!!.isSharedWithMe && !file!!.canReshare()) {
            showResharingNotAllowedSnackbar()
            if (file!!.isFolder) {
                shareLinkText.visibility = View.GONE
                shareLinkImageView.visibility = View.GONE
                sharePeopleText.visibility = View.GONE
                sharePeopleImageView.visibility = View.GONE
                dialog!!.hide()
            } else {
                shareLinkText.isEnabled = false
                shareLinkText.alpha = 0.3f
                shareLinkImageView.isEnabled = false
                shareLinkImageView.alpha = 0.3f
                sharePeopleText.isEnabled = false
                sharePeopleText.alpha = 0.3f
                sharePeopleImageView.isEnabled = false
                sharePeopleImageView.alpha = 0.3f
            }
        }

        // populate send apps
        val sendIntent = createSendIntent(requireContext(), file!!)
        val sendButtonDataList = setupSendButtonData(sendIntent)
        if ("off".equals(requireContext().getString(R.string.send_files_to_other_apps), ignoreCase = true)) {
            sharePeopleText.visibility = View.GONE
        }
        val clickListener = setupSendButtonClickListener(sendIntent)
        val sendButtonsView = view.findViewById<RecyclerView>(R.id.send_button_recycler_view)
        sendButtonsView.layoutManager = GridLayoutManager(activity, 4)
        sendButtonsView.adapter = SendButtonAdapter(sendButtonDataList, clickListener)
        return view
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    private fun shareByLink() {
        if (file!!.isSharedViaLink) {
            (requireActivity() as FileActivity).fileOperationsHelper.getFileWithLink(file!!, viewThemeUtils)
        } else if (sharingPublicPasswordEnforced || sharingPublicAskForPassword) {
            // password enforced by server, request to the user before trying to create
            requestPasswordForShareViaLink()
        } else {
            // create without password if not enforced by server or we don't know if enforced;
            (requireActivity() as FileActivity).fileOperationsHelper.shareFileViaPublicShare(file, null)
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

    private fun themeShareButtonImage(shareImageView: ImageView) {
        viewThemeUtils?.files?.themeAvatarButton(shareImageView)
    }

    private fun showResharingNotAllowedSnackbar() {
        val snackbar = Snackbar.make(view, R.string.resharing_is_not_allowed, Snackbar.LENGTH_LONG)
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar, event: Int) {
                super.onDismissed(transientBottomBar, event)
                if (file!!.isFolder) {
                    dismiss()
                }
            }
        })
        snackbar.show()
    }

    private fun setupSendButtonClickListener(sendIntent: Intent): SendButtonAdapter.ClickListener {
        return SendButtonAdapter.ClickListener { sendButtonDataData: SendButtonData ->
            val packageName = sendButtonDataData.packageName
            val activityName = sendButtonDataData.activityName

            if (MimeTypeUtil.isImage(file) && !file!!.isDown) {
                fileOperationsHelper!!.sendCachedImage(file, packageName, activityName)
            } else {
                // Obtain the file
                if (file!!.isDown) {
                    sendIntent.component = ComponentName(packageName, activityName)
                    requireActivity().startActivity(Intent.createChooser(sendIntent, getString(R.string.send)))
                } else {  // Download the file
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
                icon, label,
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