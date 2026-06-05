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
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
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
import com.owncloud.android.ui.fragment.util.FileDetailSharingFragmentHelper
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.ClipboardUtil.copyToClipboard
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.PermissionUtil.checkSelfPermission
import com.owncloud.android.utils.theme.ViewThemeUtils
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // region lifecycle methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initArguments(savedInstanceState)
        fileActivity = (activity as FileActivity?)

        requireNotNull(file) { "File may not be null" }
        requireNotNull(user) { "Account may not be null" }
        requireNotNull(fileActivity) { "FileActivity may not be null" }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileActivity ?: return
        fileDataStorageManager = fileActivity?.storageManager
        fileOperationsHelper = fileActivity?.fileOperationsHelper

        startAnimation()

        val userId = getUserId()

        setupInternalShares(userId)
        setupExternalShares(userId)

        binding?.pickContactEmailBtn?.setOnClickListener { checkContactPermission() }

        fetchSharees()
        setupView()
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
    // endregion

    // region private methods
    private fun initArguments(savedInstanceState: Bundle?) {
        val args = (savedInstanceState ?: arguments) ?: return
        file = args.getParcelableArgument(ARG_FILE, OCFile::class.java)
        user = args.getParcelableArgument(ARG_USER, User::class.java)
    }

    private fun getUserId(): String {
        val accountManager = AccountManager.get(requireContext())
        return accountManager.getUserData(
            user?.toPlatformAccount(),
            AccountUtils.Constants.KEY_USER_ID
        )
    }

    private fun setupInternalShares(userId: String) {
        internalShareeListAdapter = createShareListAdapter(userId, SharesType.INTERNAL)
        binding?.sharesListInternal?.run {
            adapter = internalShareeListAdapter
            layoutManager = createShareListLayoutManager()
        }
    }

    private fun setupExternalShares(userId: String) {
        externalShareeListAdapter = createShareListAdapter(userId, SharesType.EXTERNAL)
        binding?.sharesListExternal?.run {
            adapter = internalShareeListAdapter
            layoutManager = createShareListLayoutManager()
        }
    }

    private fun createShareListAdapter(userId: String, type: SharesType): ShareeListAdapter {
        return ShareeListAdapter(
            fileActivity!!,
            ArrayList(),
            this,
            userId,
            user,
            viewThemeUtils,
            (file?.isEncrypted == true),
            type
        ).apply {
            setHasStableIds(true)
        }
    }

    private fun createShareListLayoutManager(): LinearLayoutManager {
        return LinearLayoutManager(requireContext())
    }

    private fun startAnimation() {
        val blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink)
        binding?.shimmerLayout?.getRoot()?.startAnimation(blinkAnimation)
    }

    private fun fetchSharees() {
        val activity = fileActivity ?: return
        val clientRepository = activity.clientRepository ?: return
        val storageManager = fileDataStorageManager ?: return
        val remotePath = file?.remotePath ?: return

        val shareRepository = RemoteShareRepository(clientRepository, storageManager)
        lifecycleScope.launch {
            val result =  shareRepository.fetchSharees(remotePath)
            if (binding == null) {
                return@launch
            }

            if (result) {
                refreshCapabilitiesFromDB()
                refreshSharesFromDB()
                stopLoadingAnimationAndShowShareContainer()
                return@launch
            }

            stopLoadingAnimationAndShowShareContainer()
            DisplayUtils.showSnackMessage(this@FileDetailSharingFragment, R.string.error_fetching_sharees)
        }
    }

    private fun stopLoadingAnimationAndShowShareContainer() {
        binding?.run {
            shimmerLayout.root.run {
                clearAnimation()
                visibility = View.GONE
            }
            shareContainer.visibility = View.VISIBLE
        }
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
            FileDetailSharingFragmentHelper.setupSearchView(
                fileActivity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager?,
                searchView,
                fileActivity?.componentName
            )

            themeView(this)
            setupShareList(this)

            if (file?.canReshare() == true && !FileDetailSharingFragmentHelper.isPublicShareDisabled(capabilities)) {
                val parentFile = file?.parentId?.let { fileDataStorageManager?.getFileById(it) }
                setupShareView(this, parentFile)
            } else {
                setupDisabledShareView(this)
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

    private fun themeView(binding: FileDetailsSharingFragmentBinding) {
        binding.run {
            viewThemeUtils.material.run {
                themeSearchCardView(searchCardWrapper)
                colorMaterialButtonPrimaryOutlined(sendCopyBtn)
                colorMaterialButtonPrimaryBorderless(sharesListInternalShowAll)
                colorMaterialTextButton(sharesListInternalShowAll)
                colorMaterialButtonPrimaryOutlined(createLink)
                colorMaterialButtonPrimaryBorderless(sharesListExternalShowAll)
            }

            viewThemeUtils.platform.run {
                colorImageView(searchViewIcon, ColorRole.ON_SURFACE_VARIANT)
                colorImageView(pickContactEmailBtn, ColorRole.ON_SURFACE_VARIANT)
            }

            viewThemeUtils.files.run {
                themeContentSearchView(searchView)
            }
        }
    }

    private fun setupShareList(binding: FileDetailsSharingFragmentBinding) {
        binding.run {
            sharesListInternalShowAll.setOnClickListener {
                expandOrCollapseAdapter(internalShareeListAdapter, sharesListInternalShowAll)
            }

            sharesListExternalShowAll.setOnClickListener {
                expandOrCollapseAdapter(externalShareeListAdapter, sharesListExternalShowAll)
            }
            viewThemeUtils.material.colorMaterialTextButton(sharesListExternalShowAll)
        }
    }

    private fun expandOrCollapseAdapter(adapter: ShareeListAdapter?, button: MaterialButton) {
        adapter ?: return
        adapter.toggleShowAll()
        val actionTextId = adapter.getExpandOrCollapseActionTextId()
        button.setText(actionTextId)
    }

    private fun setupViewForEncryptedShare(binding: FileDetailsSharingFragmentBinding) {
        binding.run {
            internalShareHeadline.text = resources.getString(R.string.internal_share_headline_end_to_end_encrypted)
            internalShareDescription.visibility = View.VISIBLE
            externalSharesHeadline.text = resources.getString(R.string.create_end_to_end_encrypted_share_title)

            lifecycleScope.launch {
                val result = fetchE2EECounter()
                if (!result) {
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (file?.e2eCounter == -1L) {
                        disableE2EEShareForV1(binding)
                        return@withContext
                    }

                    createLink.setText(R.string.add_new_secure_file_drop)
                    searchView.setQueryHint(resources.getString(R.string.secure_share_search))

                    if (file?.isSharedViaLink == true) {
                        setupSearchViewForSharedLink(searchView)
                    }
                }
            }
        }
    }

    private fun disableE2EEShareForV1(binding: FileDetailsSharingFragmentBinding) {
        binding.searchContainer.visibility = View.GONE
        binding.createLink.visibility = View.GONE
    }

    private fun setupSearchViewForSharedLink(searchView: SearchView) {
        searchView.setQueryHint(resources.getString(R.string.share_not_allowed_when_file_drop))
        searchView.inputType = InputType.TYPE_NULL
        toggleSearchViewEnable(searchView, false)
    }

    private fun setupShareView(binding: FileDetailsSharingFragmentBinding, parentFile: OCFile?) {
        binding.run {
            if (file?.isEncrypted == true || (parentFile != null && parentFile.isEncrypted)) {
                setupViewForEncryptedShare(this)
            } else {
                createLink.setText(R.string.create_link)
                searchView.setQueryHint(getResources().getString(R.string.share_search_internal))
            }

            createLink.setOnClickListener { createPublicShareLink() }
        }
    }

    private fun setupDisabledShareView(binding: FileDetailsSharingFragmentBinding) {
        binding.run {
            searchView.setQueryHint(getResources().getString(R.string.resharing_is_not_allowed))
            createLink.visibility = View.GONE
            externalSharesHeadline.visibility = View.GONE
            searchView.inputType = InputType.TYPE_NULL
            pickContactEmailBtn.setVisibility(View.GONE)
            toggleSearchViewEnable(searchView, false)
            createLink.setOnClickListener(null)
        }
    }

    private suspend fun fetchE2EECounter(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val context = requireContext()
            val client = clientFactory.create(user)
            val metadata = RefreshFolderOperation.getDecryptedFolderMetadata(true, file, client, user, context)
            if (metadata is DecryptedFolderMetadataFile) {
                file?.setE2eCounter(metadata.metadata.counter)
                fileDataStorageManager?.saveFile(file)
            }
            true
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error refreshing E2E counter: " + e.message)
            false
        }
    }

    private fun checkShareViaUser() {
        if (shareViaUser(requireContext())) {
            return
        }

        binding?.searchContainer?.visibility = View.GONE
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
                return
            }

            sharedWithYouUsername.text = String.format(getString(R.string.shared_with_you_by), file?.ownerDisplayName)
            setupUserAvatar(sharedWithYouAvatar)
            setupNote(this)
        }
    }

    private fun setupUserAvatar(sharedWithYouAvatar: ImageView) {
        val user = user ?: return
        val userId = file?.ownerId ?: return

        DisplayUtils.setAvatar(
            user,
            userId,
            this@FileDetailSharingFragment,
            resources.getDimension(
                R.dimen.file_list_item_avatar_icon_radius
            ),
            resources,
            sharedWithYouAvatar,
            context
        )

        sharedWithYouAvatar.setVisibility(View.VISIBLE)
    }

    private fun setupNote(binding: FileDetailsSharingFragmentBinding) {
        val note = file?.getNote()

        if (!note.isNullOrEmpty()) {
            binding.sharedWithYouNote.text = file?.getNote()
            binding.sharedWithYouNoteContainer.visibility = View.VISIBLE
            return
        }

        binding.sharedWithYouNoteContainer.visibility = View.GONE
    }

    private fun createInternalLink(account: OwnCloudAccount, file: OCFile): String {
        return account.baseUri.toString() + "/index.php/f/" + file.localId
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

    private fun refreshUiFromDB() {
        refreshSharesFromDB()
        setupView()
    }

    private fun unShareWith(share: OCShare) {
        fileOperationsHelper?.unShareShare(file, share.id)
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
        val canReadContacts = (checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS))
        if (canReadContacts) {
            pickContactEmail()
            return
        }

        requestContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun pickContactEmail() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI)

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            onContactSelectionResultLauncher.launch(intent)
            return
        }

        DisplayUtils.showSnackMessage(this, R.string.file_detail_sharing_fragment_no_contact_app_message)
    }

    private fun handleContactResult(contactUri: Uri) {
        // Define the projection to get all email addresses.
        val projection = arrayOf<String?>(ContactsContract.CommonDataKinds.Email.ADDRESS)

        val cursor = fileActivity?.contentResolver?.query(contactUri, projection, null, null, null)
        if (cursor == null) {
            DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
            Log_OC.e(
                TAG,
                "Failed to pick email address as Cursor is null."
            )
            return
        }

        if (!cursor.moveToFirst()) {
            DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
            Log_OC.e(
                TAG,
                "Failed to pick email address as no Email found."
            )
            return
        }

        // The contact has only one email address, use it.
        val columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        if (columnIndex == -1) {
            DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
            Log_OC.e(TAG, "Failed to pick email address.")
            cursor.close()
            return
        }

        // Use the email address as needed.
        // email variable contains the selected contact's email address.
        val email = cursor.getString(columnIndex)
        binding?.searchView?.post(Runnable {
            if (binding == null) {
                return@Runnable
            }
            binding?.searchView?.setQuery(email, false)
            binding?.searchView?.requestFocus()
        })
        cursor.close()
    }

    private fun isReshareForbidden(share: OCShare): Boolean {
        return ShareType.FEDERATED == share.shareType ||
            capabilities != null && (capabilities?.filesSharingResharing?.isFalse == true)
    }

    private fun modifyExistingShare(share: OCShare, screenTypePermission: Int) {
        onEditShareListener?.editExistingShare(share, screenTypePermission, !isReshareForbidden(share))
    }
    // endregion

    // region overridden methods
    override fun onQuickPermissionChanged(share: OCShare, permission: Int) {
        fileOperationsHelper?.setPermissionsToShare(share, permission)
    }

    override fun openShareDetailWithCustomPermissions(share: OCShare) {
        modifyExistingShare(share, FileDetailsSharingProcessFragment.SCREEN_TYPE_PERMISSION_WITH_CUSTOM_PERMISSION)
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

        val entity = fileDataStorageManager?.getFileEntity(file)

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
        if (file?.isSharedViaLink == true && !share.shareLink.isNullOrEmpty()) {
            FileActivity.showShareLinkDialog(fileActivity, file, share.shareLink)
        } else {
            showSendLinkTo(share)
        }
    }

    override fun addAnotherLink(share: OCShare?) {
        createPublicShareLink()
    }

    override fun copyInternalLink() {
        val account = accountManager.getCurrentOwnCloudAccount()

        if (account == null) {
            DisplayUtils.showSnackMessage(this, R.string.could_not_retrieve_url)
            return
        }

        file?.let { FileActivity.showShareLinkDialog(fileActivity, file, createInternalLink(account, it)) }
    }

    private fun OCCapability?.isPasswordEnforced(): Boolean {
        return this?.filesSharingPublicPasswordEnforced?.isTrue == true && filesSharingPublicAskForOptionalPassword.isTrue
    }

    override fun createPublicShareLink() {
        if (capabilities?.isPasswordEnforced() == true) {
            requestPasswordForShareViaLink(
                true,
                (capabilities?.filesSharingPublicAskForOptionalPassword?.isTrue == true)
            )
            return
        }

        // create without password
        fileOperationsHelper?.shareFileViaPublicShare(file, null)
    }

    override fun createSecureFileDrop() {
        fileOperationsHelper?.shareFolderViaSecureFileDrop(file!!)
    }

    override fun copyLink(share: OCShare) {
        val file = file ?: return
        if (!file.isSharedViaLink) {
            return
        }

        if (share.shareLink.isNullOrEmpty()) {
            fileOperationsHelper?.getFileWithLink(file, viewThemeUtils)
            return
        }

        copyToClipboard(requireActivity(), share.shareLink)
    }

    @VisibleForTesting
    override fun showSharingMenuActionSheet(share: OCShare?) {
        if (fileActivity == null || fileActivity?.isFinishing == true) {
            return
        }

        FileDetailSharingMenuBottomSheetDialog(
            fileActivity,
            this,
            share,
            viewThemeUtils,
            file?.isEncrypted == true
        ).show()
    }

    override fun showPermissionsDialog(share: OCShare?) {
        QuickSharingPermissionsBottomSheetDialog(fileActivity, this, share, viewThemeUtils, file?.isEncrypted == true).show()
    }
    override fun requestPasswordForShare(share: OCShare?, askForPassword: Boolean) {
        val dialog = newInstance(share, askForPassword)
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT)
    }

    override fun showProfileBottomSheet(user: User, shareWith: String?) {
        if (!user.server.version.isNewerOrEqual(NextcloudVersion.nextcloud_23)) {
            return
        }

        RetrieveHoverCardAsyncTask(
            user,
            shareWith,
            fileActivity,
            clientFactory,
            viewThemeUtils
        ).execute()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putParcelable(ARG_FILE, file)
            putParcelable(ARG_USER, user)
        }
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        binding?.sharedWithYouAvatar?.setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean {
        return false
    }
    // endregion

    // region public methods
    fun search(query: String?) {
        binding?.searchView?.setQuery(query, true)
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

    fun requestPasswordForShareViaLink(createShare: Boolean, askForPassword: Boolean) {
        val dialog = newInstance(
            file,
            createShare,
            askForPassword
        )
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT)
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

        internalShareeListAdapter?.removeAll()

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
    // endregion

    // region private values
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
    // endregion

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
