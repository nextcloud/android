/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * @author TSI-mc
 *
 * Copyright (C) 2026 Alper Öztürk
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2023 TSI-mc
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.InputType
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.mergeDistinctByToken
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.mdm.MDMConfig.shareViaUser
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsSharingFragmentBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SharesType
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.providers.UsersAndGroupsSearchConfig
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.adapter.ShareeListAdapter
import com.owncloud.android.ui.adapter.ShareeListAdapterListener
import com.owncloud.android.ui.asynctasks.RetrieveHoverCardAsyncTask
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment.Companion.newInstance
import com.owncloud.android.ui.fragment.QuickSharingPermissionsBottomSheetDialog.QuickPermissionSharingBottomSheetActions
import com.owncloud.android.ui.fragment.share.RemoteShareRepository
import com.owncloud.android.ui.fragment.share.ShareRepository
import com.owncloud.android.ui.fragment.util.FileDetailSharingFragmentHelper
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.ClipboardUtil.copyToClipboard
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.PermissionUtil.checkSelfPermission
import com.owncloud.android.utils.theme.ViewThemeUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.launch
import javax.inject.Inject

class FileDetailSharingFragment : Fragment(), ShareeListAdapterListener, AvatarGenerationListener, Injectable,
    FileDetailsSharingMenuBottomSheetActions, QuickPermissionSharingBottomSheetActions {
    private var file: OCFile? = null
    private var user: User? = null
    private var capabilities: OCCapability? = null

    private var fileOperationsHelper: FileOperationsHelper? = null
    private var fileActivity: FileActivity? = null
    private var fileDataStorageManager: FileDataStorageManager? = null

    private var binding: FileDetailsSharingFragmentBinding? = null

    private var onEditShareListener: OnEditShareListener? = null

    private var internalShareeListAdapter: ShareeListAdapter? = null

    private var externalShareeListAdapter: ShareeListAdapter? = null

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var searchConfig: UsersAndGroupsSearchConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelableArgument(ARG_FILE, OCFile::class.java)
            user = savedInstanceState.getParcelableArgument(ARG_USER, User::class.java)
        } else {
            val arguments = getArguments()

            if (arguments != null) {
                file = arguments.getParcelableArgument(ARG_FILE, OCFile::class.java)
                user = arguments.getParcelableArgument(ARG_USER, User::class.java)
            }
        }

        requireNotNull(file) { "File may not be null" }
        requireNotNull(user) { "Account may not be null" }

        fileActivity = activity as FileActivity?
        requireNotNull(fileActivity) { "FileActivity may not be null" }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (fileActivity == null) {
            return
        }

        fileDataStorageManager = fileActivity?.storageManager
        fileOperationsHelper = fileActivity?.fileOperationsHelper

        // start animation before loading process
        val blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink)
        binding?.shimmerLayout?.getRoot()?.startAnimation(blinkAnimation)

        val accountManager = AccountManager.get(requireContext())
        val userId = accountManager.getUserData(
            user?.toPlatformAccount(),
            AccountUtils.Constants.KEY_USER_ID
        )

        // internal shares
        internalShareeListAdapter = ShareeListAdapter(
            fileActivity!!,
            ArrayList(),
            this,
            userId,
            user,
            viewThemeUtils,
            file?.isEncrypted == true,
            SharesType.INTERNAL
        )
        internalShareeListAdapter?.setHasStableIds(true)
        binding?.sharesListInternal?.setAdapter(internalShareeListAdapter)
        binding?.sharesListInternal?.setLayoutManager(LinearLayoutManager(requireContext()))

        // external shares
        externalShareeListAdapter = ShareeListAdapter(
            fileActivity!!,
            ArrayList(),
            this,
            userId,
            user,
            viewThemeUtils,
            file?.isEncrypted == true,
            SharesType.EXTERNAL
        )
        externalShareeListAdapter!!.setHasStableIds(true)
        binding?.sharesListExternal?.setAdapter(externalShareeListAdapter)
        binding?.sharesListExternal?.setLayoutManager(LinearLayoutManager(requireContext()))
        binding?.pickContactEmailBtn?.setOnClickListener { checkContactPermission() }

        // start loading process
        fetchSharees()

        setupView()
    }

    private fun fetchSharees() {
        val activity = fileActivity ?: return
        val clientRepository = activity.clientRepository ?: return
        val storageManager = fileDataStorageManager ?: return
        val remotePath = file?.remotePath ?: return

        val shareRepository: ShareRepository = RemoteShareRepository(clientRepository, storageManager)
        lifecycleScope.launch {
            val result =  shareRepository.fetchSharees(remotePath)
            if (binding == null) {
                return@launch
            }

            // success
            if (result) {
                refreshCapabilitiesFromDB()
                refreshSharesFromDB()
                stopLoadingAnimationAndShowShareContainer()
                return@launch
            }

            // fail
            stopLoadingAnimationAndShowShareContainer()
            DisplayUtils.showSnackMessage(this@FileDetailSharingFragment, R.string.error_fetching_sharees)
        }
    }

    // stop loading animation
    private fun stopLoadingAnimationAndShowShareContainer() {
        if (binding == null) {
            return
        }

        val shimmerLayout = binding!!.shimmerLayout.getRoot()
        shimmerLayout.clearAnimation()
        shimmerLayout.visibility = View.GONE

        binding?.shareContainer?.visibility = View.VISIBLE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FileDetailsSharingFragmentBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        require(activity is FileActivity) { "Calling activity must be of type FileActivity" }

        try {
            onEditShareListener = context as OnEditShareListener
        } catch (e: Exception) {
            throw IllegalArgumentException("Calling activity must implement the interface$e")
        }
    }

    override fun onStart() {
        super.onStart()
        searchConfig.searchOnlyUsers = (file?.isEncrypted == true)
    }

    override fun onStop() {
        super.onStop()
        searchConfig.reset()
    }

    private fun resetSearchView() {
        binding?.run {
            toggleSearchViewEnable(searchView, true)
            searchView.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            searchView.setQueryHint(null)
            searchView.setQuery("", false)
            pickContactEmailBtn.setVisibility(View.VISIBLE)
        }
    }

    private fun setupView() {
        resetSearchView()
        setShareWithYou()

        binding?.run {
            val parentFile = file?.parentId?.let { fileDataStorageManager?.getFileById(it) }

            FileDetailSharingFragmentHelper.setupSearchView(
                fileActivity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager?,
                searchView,
                fileActivity?.componentName
            )

            viewThemeUtils.material.themeSearchCardView(searchCardWrapper)
            viewThemeUtils.files.themeContentSearchView(searchView)
            viewThemeUtils.platform.colorImageView(searchViewIcon, ColorRole.ON_SURFACE_VARIANT)
            viewThemeUtils.platform.colorImageView(pickContactEmailBtn, ColorRole.ON_SURFACE_VARIANT)

            viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(sendCopyBtn)

            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(sharesListInternalShowAll)
            viewThemeUtils.material.colorMaterialTextButton(sharesListInternalShowAll)
            sharesListInternalShowAll.setOnClickListener {
                internalShareeListAdapter?.toggleShowAll()
                val textRes = if (internalShareeListAdapter?.isShowAll == true) R.string.show_less else R.string.show_all
                sharesListInternalShowAll.setText(textRes)
            }

            viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(createLink)

            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(sharesListExternalShowAll)
            sharesListExternalShowAll.let { viewThemeUtils.material.colorMaterialTextButton(it) }
            sharesListExternalShowAll.setOnClickListener {
                externalShareeListAdapter!!.toggleShowAll()
                val textRes = if (externalShareeListAdapter?.isShowAll == true) R.string.show_less else R.string.show_all
                sharesListExternalShowAll.setText(textRes)
            }

            if (file?.canReshare() == true && !FileDetailSharingFragmentHelper.isPublicShareDisabled(capabilities)) {
                if (file?.isEncrypted == true || (parentFile != null && parentFile.isEncrypted)) {
                    internalShareHeadline.text = resources.getString(R.string.internal_share_headline_end_to_end_encrypted)
                    internalShareDescription.visibility = View.VISIBLE
                    externalSharesHeadline.text = resources.getString(R.string.create_end_to_end_encrypted_share_title)

                    fetchE2EECounter {
                        if (binding == null) {
                            return@fetchE2EECounter
                        }
                        if (file?.e2eCounter == -1L) {
                            // V1 cannot share
                            searchContainer.visibility = View.GONE
                            createLink.visibility = View.GONE
                        } else {
                            createLink.setText(R.string.add_new_secure_file_drop)
                            searchView.setQueryHint(resources.getString(R.string.secure_share_search))

                            if (file?.isSharedViaLink == true) {
                                searchView.setQueryHint(resources.getString(R.string.share_not_allowed_when_file_drop))
                                searchView.inputType = InputType.TYPE_NULL
                                toggleSearchViewEnable(searchView, false)
                            }
                        }
                    }
                } else {
                    createLink.setText(R.string.create_link)
                    searchView.setQueryHint(getResources().getString(R.string.share_search_internal))
                }

                createLink.setOnClickListener(View.OnClickListener { v: View? -> createPublicShareLink() })
            } else {
                searchView.setQueryHint(getResources().getString(R.string.resharing_is_not_allowed))
                createLink.visibility = View.GONE
                externalSharesHeadline.visibility = View.GONE
                searchView.inputType = InputType.TYPE_NULL
                pickContactEmailBtn.setVisibility(View.GONE)
                toggleSearchViewEnable(searchView, false)
                createLink.setOnClickListener(null)
            }

            checkShareViaUser()

            if (file?.isFolder == true) {
                sendCopyBtn.visibility = View.GONE
            }
            sendCopyBtn.setOnClickListener {
                startActivity(
                    Intent.createChooser(
                        IntentUtil.createSendIntent(requireContext(), file!!),
                        requireContext().getString(R.string.activity_chooser_send_file_title)
                    )
                )
            }
        }


    }

    private fun fetchE2EECounter(onComplete: Runnable?) {
        val context = requireContext()

        Thread {
            try {
                val client = clientFactory.create(user)
                val metadata = RefreshFolderOperation.getDecryptedFolderMetadata(true, file, client, user, context)
                if (metadata is DecryptedFolderMetadataFile) {
                    file?.setE2eCounter(metadata.metadata.counter)
                    fileDataStorageManager?.saveFile(file)
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Error refreshing E2E counter: " + e.message)
            }
            val activity = getActivity()
            activity?.runOnUiThread(onComplete)
        }.start()
    }

    private fun checkShareViaUser() {
        if (!shareViaUser(requireContext())) {
            binding?.searchContainer?.visibility = View.GONE
        }
    }

    private fun toggleSearchViewEnable(view: View, enable: Boolean) {
        view.setEnabled(enable)
        if (view is ViewGroup) {
            for (i in 0..<view.size) {
                toggleSearchViewEnable(view.getChildAt(i), enable)
            }
        }
    }

    private fun setShareWithYou() {
        binding?.run {
            if (accountManager.userOwnsFile(file, user)) {
                sharedWithYouContainer.visibility = View.GONE
            } else {
                sharedWithYouUsername.text = String.format(getString(R.string.shared_with_you_by), file?.ownerDisplayName)
                user?.let {
                    file?.ownerId?.let { userId ->
                        DisplayUtils.setAvatar(
                            it,
                            userId,
                            this@FileDetailSharingFragment,
                            resources.getDimension(
                                R.dimen.file_list_item_avatar_icon_radius
                            ),
                            resources,
                            sharedWithYouAvatar,
                            context
                        )
                    }
                }
                sharedWithYouAvatar.setVisibility(View.VISIBLE)

                val note = file?.getNote()

                if (!TextUtils.isEmpty(note)) {
                    sharedWithYouNote.text = file?.getNote()
                    sharedWithYouNoteContainer.visibility = View.VISIBLE
                } else {
                    sharedWithYouNoteContainer.visibility = View.GONE
                }
            }
        }
    }

    override fun copyInternalLink() {
        val account = accountManager.getCurrentOwnCloudAccount()

        if (account == null) {
            DisplayUtils.showSnackMessage(this, R.string.could_not_retrieve_url)
            return
        }

        file?.let { FileActivity.showShareLinkDialog(fileActivity, file, createInternalLink(account, it)) }
    }

    private fun createInternalLink(account: OwnCloudAccount, file: OCFile): String {
        return account.baseUri.toString() + "/index.php/f/" + file.localId
    }

    override fun createPublicShareLink() {
        if (capabilities != null && (capabilities?.filesSharingPublicPasswordEnforced?.isTrue == true ||
                capabilities?.filesSharingPublicAskForOptionalPassword?.isTrue == true)
        ) {
            // password enforced by server, request to the user before trying to create
            requestPasswordForShareViaLink(
                true,
                capabilities!!.filesSharingPublicAskForOptionalPassword.isTrue
            )
        } else {
            // create without password if not enforced by server or we don't know if enforced;
            fileOperationsHelper?.shareFileViaPublicShare(file, null)
        }
    }

    override fun createSecureFileDrop() {
        fileOperationsHelper?.shareFolderViaSecureFileDrop(file!!)
    }

    private fun showSendLinkTo(publicShare: OCShare) {
        if (file?.isSharedViaLink == true) {
            if (TextUtils.isEmpty(publicShare.shareLink)) {
                fileOperationsHelper?.getFileWithLink(file!!, viewThemeUtils)
            } else {
                FileActivity.showShareLinkDialog(fileActivity, file, publicShare.shareLink)
            }
        }
    }

    override fun copyLink(share: OCShare) {
        if (file?.isSharedViaLink == true) {
            if (TextUtils.isEmpty(share.shareLink)) {
                fileOperationsHelper?.getFileWithLink(file!!, viewThemeUtils)
            } else {
                copyToClipboard(requireActivity(), share.shareLink)
            }
        }
    }

    @VisibleForTesting
    override fun showSharingMenuActionSheet(share: OCShare?) {
        if (fileActivity != null && fileActivity?.isFinishing == false) {
            FileDetailSharingMenuBottomSheetDialog(
                fileActivity,
                this,
                share,
                viewThemeUtils,
                file?.isEncrypted == true
            ).show()
        }
    }

    override fun showPermissionsDialog(share: OCShare?) {
        QuickSharingPermissionsBottomSheetDialog(fileActivity, this, share, viewThemeUtils, file?.isEncrypted == true).show()
    }

    fun onUpdateShareInformation(result: RemoteOperationResult<*>, file: OCFile?) {
        this.file = file

        onUpdateShareInformation(result)
    }

    fun onUpdateShareInformation(result: RemoteOperationResult<*>) {
        if (binding == null) {
            return
        }

        if (result.isSuccess) {
            refreshUiFromDB()
        } else {
            setupView()
        }
    }

    private fun refreshUiFromDB() {
        refreshSharesFromDB()
        setupView()
    }

    private fun unShareWith(share: OCShare) {
        fileOperationsHelper?.unShareShare(file, share.id)
    }

    fun requestPasswordForShareViaLink(createShare: Boolean, askForPassword: Boolean) {
        val dialog = newInstance(
            file,
            createShare,
            askForPassword
        )
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT)
    }

    override fun requestPasswordForShare(share: OCShare?, askForPassword: Boolean) {
        val dialog = newInstance(share, askForPassword)
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT)
    }

    override fun showProfileBottomSheet(user: User, shareWith: String?) {
        if (user.server.version.isNewerOrEqual(NextcloudVersion.nextcloud_23)) {
            RetrieveHoverCardAsyncTask(
                user,
                shareWith,
                fileActivity,
                clientFactory,
                viewThemeUtils
            ).execute()
        }
    }

    fun refreshCapabilitiesFromDB() {
        capabilities = fileDataStorageManager?.getCapability(user?.accountName)
    }

    @SuppressFBWarnings("PSC")
    fun refreshSharesFromDB() {
        if (binding == null) {
            return
        }

        val newFile = file?.fileId?.let { fileDataStorageManager?.getFileById(it) }
        if (newFile != null) {
            file = newFile
        }

        if (internalShareeListAdapter == null) {
            DisplayUtils.showSnackMessage(this, R.string.could_not_retrieve_shares)
            return
        }

        internalShareeListAdapter!!.removeAll()

        // to show share with users/groups info
        val shares = fileDataStorageManager?.getSharesWithForAFile(
            file?.remotePath,
            user?.accountName
        ) ?: listOf<OCShare>()

        val internalShares = ArrayList<OCShare>()
        val externalShares = ArrayList<OCShare>()

        for (share in shares) {
            if (share.shareType != null) {
                when (share.shareType) {
                    ShareType.PUBLIC_LINK, ShareType.FEDERATED_GROUP, ShareType.FEDERATED, ShareType.EMAIL -> externalShares.add(
                        share
                    )

                    else -> internalShares.add(share)
                }
            }
        }

        internalShareeListAdapter?.addShares(internalShares)
        internalShareeListAdapter?.shares?.size?.let { binding?.sharesListInternalShowAll?.setVisibleIf(it > 3) }

        addExternalAndPublicShares(externalShares)
        externalShareeListAdapter?.shares?.size?.let { binding?.sharesListExternalShowAll?.setVisibleIf(it > 3) }
    }

    private fun addExternalAndPublicShares(externalShares: MutableList<OCShare>) {
        val publicShares =
            fileDataStorageManager?.getSharesByPathAndType(file?.remotePath, ShareType.PUBLIC_LINK, "")
        externalShareeListAdapter?.removeAll()
        publicShares?.let {
            externalShares.mergeDistinctByToken(it)
            externalShareeListAdapter?.addShares(it)
        }
    }

    private fun checkContactPermission() {
        if (checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS)) {
            pickContactEmail()
        } else {
            requestContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun pickContactEmail() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI)

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            onContactSelectionResultLauncher.launch(intent)
        } else {
            DisplayUtils.showSnackMessage(this, R.string.file_detail_sharing_fragment_no_contact_app_message)
        }
    }

    private fun handleContactResult(contactUri: Uri) {
        // Define the projection to get all email addresses.
        val projection = arrayOf<String?>(ContactsContract.CommonDataKinds.Email.ADDRESS)

        val cursor = fileActivity?.contentResolver?.query(contactUri, projection, null, null, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // The contact has only one email address, use it.
                val columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                if (columnIndex != -1) {
                    // Use the email address as needed.
                    // email variable contains the selected contact's email address.
                    val email = cursor.getString(columnIndex)
                    binding!!.searchView.post(Runnable {
                        if (binding == null) {
                            return@Runnable
                        }
                        binding?.searchView?.setQuery(email, false)
                        binding?.searchView?.requestFocus()
                    })
                } else {
                    DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
                    Log_OC.e(FileDetailSharingFragment::class.java.getSimpleName(), "Failed to pick email address.")
                }
            } else {
                DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
                Log_OC.e(
                    FileDetailSharingFragment::class.java.getSimpleName(),
                    "Failed to pick email address as no Email found."
                )
            }
            cursor.close()
        } else {
            DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
            Log_OC.e(
                FileDetailSharingFragment::class.java.getSimpleName(),
                "Failed to pick email address as Cursor is null."
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_FILE, file)
        outState.putParcelable(ARG_USER, user)
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        if (binding == null) {
            return
        }
        binding?.sharedWithYouAvatar?.setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        return false
    }

    private fun isReshareForbidden(share: OCShare): Boolean {
        return ShareType.FEDERATED == share.shareType ||
            capabilities != null && capabilities!!.filesSharingResharing.isFalse
    }

    @VisibleForTesting
    fun search(query: String?) {
        val searchView = requireView().findViewById<SearchView>(R.id.searchView)
        searchView.setQuery(query, true)
    }

    override fun advancedPermissions(share: OCShare) {
        modifyExistingShare(share, FileDetailsSharingProcessFragment.SCREEN_TYPE_PERMISSION)
    }

    override fun sendNewEmail(share: OCShare) {
        modifyExistingShare(share, FileDetailsSharingProcessFragment.SCREEN_TYPE_NOTE)
    }

    override fun unShare(share: OCShare) {
        if (binding == null) {
            return
        }

        unShareWith(share)

        val entity = fileDataStorageManager!!.getFileEntity(file)

        if (binding?.sharesListInternal?.adapter is ShareeListAdapter) {
            val adapter = binding?.sharesListInternal?.adapter as ShareeListAdapter
            adapter.remove(share)
            if (entity != null && adapter.isAdapterEmpty()) {
                entity.sharedWithSharee = 0
                fileDataStorageManager?.updateFileEntity(entity)
            }
        } else if (binding?.sharesListExternal?.adapter is ShareeListAdapter) {
            val adapter = binding?.sharesListExternal?.adapter as ShareeListAdapter
            adapter.remove(share)
            if (entity != null && adapter.isAdapterEmpty()) {
                entity.sharedViaLink = 0
                fileDataStorageManager?.updateFileEntity(entity)
            }
        } else {
            DisplayUtils.showSnackMessage(this, R.string.failed_update_ui)
        }
    }

    override fun sendLink(share: OCShare) {
        if (file?.isSharedViaLink == true && !TextUtils.isEmpty(share.shareLink)) {
            FileActivity.showShareLinkDialog(fileActivity, file, share.shareLink)
        } else {
            showSendLinkTo(share)
        }
    }

    override fun addAnotherLink(share: OCShare?) {
        createPublicShareLink()
    }

    private fun modifyExistingShare(share: OCShare, screenTypePermission: Int) {
        onEditShareListener?.editExistingShare(share, screenTypePermission, !isReshareForbidden(share))
    }

    override fun onQuickPermissionChanged(share: OCShare, permission: Int) {
        fileOperationsHelper?.setPermissionsToShare(share, permission)
    }

    override fun openShareDetailWithCustomPermissions(share: OCShare) {
        modifyExistingShare(share, FileDetailsSharingProcessFragment.SCREEN_TYPE_PERMISSION_WITH_CUSTOM_PERMISSION)
    }

    private val requestContactPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickContactEmail()
        } else {
            DisplayUtils.showSnackMessage(this, R.string.contact_no_permission)
        }
    }

    private val onContactSelectionResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent == null) {
                DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
                return@registerForActivityResult
            }

            val contactUri = intent.data
            if (contactUri == null) {
                DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
                return@registerForActivityResult
            }

            handleContactResult(contactUri)
        }
    }

    interface OnEditShareListener {
        fun editExistingShare(share: OCShare?, screenTypePermission: Int, isReshareShown: Boolean)

        fun onShareProcessClosed()
    }

    companion object {
        private const val TAG = "FileDetailSharingFragment"
        private const val ARG_FILE = "FILE"
        private const val ARG_USER = "USER"

        @JvmStatic
        fun newInstance(file: OCFile?, user: User?): FileDetailSharingFragment = FileDetailSharingFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putParcelable(ARG_USER, user)
            }
        }
    }
}
