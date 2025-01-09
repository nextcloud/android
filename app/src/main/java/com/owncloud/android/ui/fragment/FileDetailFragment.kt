/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 TSI-mc
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2011 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.annotation.IdRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadHelper.Companion.buildRemoteName
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.utils.MenuUtils.hideAll
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.logFileSize
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.mdm.MDMConfig.shareViaLink
import com.nextcloud.utils.mdm.MDMConfig.shareViaUser
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsFragmentBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncResizedImageDrawable
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ResizedImageGenerationTask
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ToggleFavoriteRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.adapter.FileDetailTabAdapter
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.ui.events.FavoriteEvent
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment.Companion.newInstance
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Creates an empty details fragment.
 *
 * It's necessary to keep a public constructor without parameters; the system uses it when tries
 * to reinstate a fragment automatically.
 */
/**
 * This Fragment is used to display the details about a file.
 */
@Suppress("TooManyFunctions")
class FileDetailFragment : FileFragment(), View.OnClickListener, Injectable {
    private var view: View? = null
    private var user: User? = null
    private var parentFolder: OCFile? = null
    private var previewLoaded = false

    private var binding: FileDetailsFragmentBinding? = null
    private var progressListener: ProgressListener? = null
    private var toolbarActivity: ToolbarActivity? = null
    private var activeTab = 0

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var storageManager: FileDataStorageManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    val fileDetailSharingFragment: FileDetailSharingFragment?
        get() {
            if (binding == null) {
                return null
            }

            if (binding?.pager?.adapter is FileDetailTabAdapter) {
                val adapter = binding?.pager?.adapter as FileDetailTabAdapter
                return adapter.fileDetailSharingFragment
            }

            return null
        }

    val fileDetailActivitiesFragment: FileDetailActivitiesFragment?
        get() {
            if (binding?.pager?.adapter is FileDetailTabAdapter) {
                val adapter = binding?.pager?.adapter as FileDetailTabAdapter
                return adapter.fileDetailActivitiesFragment
            }

            return null
        }

    fun goBackToOCFileListFragment() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    @Suppress("MagicNumber")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val arguments = arguments

        requireNotNull(arguments) { "Arguments may not be null" }

        initArgs(savedInstanceState, arguments)
        binding = FileDetailsFragmentBinding.inflate(inflater, container, false)
        view = binding?.root

        setupEmptyList()
        setupTags()
        addMenuProvider()

        return view
    }

    private fun initArgs(savedInstanceState: Bundle?, arguments: Bundle) {
        file = arguments.getParcelableArgument(ARG_FILE, OCFile::class.java)
        parentFolder = arguments.getParcelableArgument(ARG_PARENT_FOLDER, OCFile::class.java)
        user = arguments.getParcelableArgument(
            ARG_USER,
            User::class.java
        )
        activeTab = arguments.getInt(ARG_ACTIVE_TAB, 0)

        if (savedInstanceState != null) {
            file = savedInstanceState.getParcelableArgument(ARG_FILE, OCFile::class.java)
            user = savedInstanceState.getParcelableArgument(
                ARG_USER,
                User::class.java
            )
        }
    }

    private fun setupEmptyList() {
        if (file == null || user == null) {
            showEmptyContent()
        } else {
            binding?.emptyList?.emptyListView?.visibility = View.GONE
        }
    }

    private fun setupTags() {
        val context = context ?: return

        if (file.tags.isEmpty()) {
            binding?.tagsGroup?.visibility = View.GONE
        } else {
            for (tag in file.tags) {
                val chip = Chip(context)
                chip.text = tag
                chip.chipBackgroundColor = ColorStateList.valueOf(
                    resources.getColor(
                        R.color.bg_default,
                        context.theme
                    )
                )
                chip.shapeAppearanceModel = chip.shapeAppearanceModel.toBuilder().setAllCornerSizes((100.0f))
                    .build()
                chip.setEnsureMinTouchTargetSize(false)
                chip.isClickable = false
                viewThemeUtils.material.themeChipSuggestion(chip)
                binding?.tagsGroup?.addView(chip)
            }
        }
    }

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(
            object: MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                }

                override fun onPrepareMenu(menu: Menu) {
                    hideAll(menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (file == null || user == null) {
            return
        }

        setupOnClickListeners()
    }

    private fun setupOnClickListeners() {
        binding?.run {
            viewThemeUtils.platform.themeHorizontalProgressBar(progressBar)
            progressListener = ProgressListener(progressBar)
            cancelBtn.setOnClickListener(this@FileDetailFragment)
            favorite.setOnClickListener(this@FileDetailFragment)
            overflowMenu.setOnClickListener(this@FileDetailFragment)
            lastModificationTimestamp.setOnClickListener(this@FileDetailFragment)
            folderSyncButton.setOnClickListener(this@FileDetailFragment)
            updateFileDetails(false, false)
        }
    }

    private fun onOverflowIconClicked() {
        val additionalFilter = mutableListOf(
            R.id.action_lock_file,
            R.id.action_unlock_file,
            R.id.action_edit,
            R.id.action_favorite,
            R.id.action_unset_favorite,
            R.id.action_see_details,
            R.id.action_move_or_copy,
            R.id.action_stream_media,
            R.id.action_send_share_file,
            R.id.action_pin_to_homescreen
        )

        if (file.isFolder) {
            additionalFilter.add(R.id.action_send_file)
            additionalFilter.add(R.id.action_sync_file)
        }

        if (file.isAPKorAAB) {
            additionalFilter.add(R.id.action_download_file)
            additionalFilter.add(R.id.action_export_file)
        }

        newInstance(file, true, additionalFilter)
            .setResultListener(
                childFragmentManager,
                this
            ) { itemId: Int -> this.optionsItemSelected(itemId) }
            .show(childFragmentManager, "actions")
    }

    private fun setupTabLayout() {
        binding?.tabLayout?.run {
            removeAllTabs()

            newTab().setText(R.string.drawer_item_activities).setIcon(R.drawable.ic_activity).let {
                addTab(it)
            }

            if (showSharingTab()) {
                newTab().setText(R.string.share_dialog_title).setIcon(R.drawable.shared_via_users).let {
                    addTab(it)
                }
            }

            if (MimeTypeUtil.isImage(file)) {
                newTab().setText(R.string.filedetails_details).setIcon(R.drawable.image_32dp).let {
                    addTab(it)
                }
            }

            addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    binding?.pager?.currentItem = tab.position
                    if (tab.position == 0) {
                        fileDetailActivitiesFragment?.markCommentsAsRead()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            })

            post {
                getTabAt(activeTab)?.select()
            }

            viewThemeUtils.material.themeTabLayout(this)
        }
    }

    private fun setupViewPager() {
        val fileDetailTabAdapter = FileDetailTabAdapter(
            requireActivity(),
            file,
            user,
            showSharingTab()
        )

        binding?.pager?.run {
            adapter = fileDetailTabAdapter
            registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    if (activeTab == 0) {
                        fileDetailActivitiesFragment?.markCommentsAsRead()
                    }
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                }
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        file.logFileSize(TAG)

        outState.run {
            putParcelable(ARG_FILE, file)
            putParcelable(ARG_USER, user)
        }
    }

    override fun onStart() {
        super.onStart()
        listenForTransferProgress()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()

        if (previewLoaded) {
            toolbarActivity?.setPreviewImageVisibility(true)
        }
    }

    override fun onStop() {
        leaveTransferProgress()
        toolbarActivity?.hidePreviewImage()
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is ToolbarActivity) {
            toolbarActivity = context
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun getView(): View? {
        return if (super.getView() == null) view else super.getView()
    }

    private fun optionsItemSelected(@IdRes itemId: Int) {
        when (itemId) {
            R.id.action_send_file -> {
                containerActivity.fileOperationsHelper.sendShareFile(file, true)
            }
            R.id.action_open_file_with -> {
                containerActivity.fileOperationsHelper.openFile(file)
            }
            R.id.action_remove_file -> {
                RemoveFilesDialogFragment.newInstance(file).show(parentFragmentManager, FTAG_CONFIRMATION)
            }
            R.id.action_rename_file -> {
                RenameFileDialogFragment.newInstance(file, parentFolder).show(parentFragmentManager, FTAG_RENAME_FILE)
            }
            R.id.action_cancel_sync -> {
                (containerActivity as FileDisplayActivity).cancelTransference(file)
            }
            R.id.action_download_file, R.id.action_sync_file -> {
                containerActivity.fileOperationsHelper.syncFile(file)
            }
            R.id.action_export_file -> {
                val list = ArrayList<OCFile>()
                list.add(file)
                containerActivity.fileOperationsHelper.exportFiles(
                    list,
                    context,
                    getView(),
                    backgroundJobManager
                )
            }
            R.id.action_set_as_wallpaper -> {
                containerActivity.fileOperationsHelper.setPictureAs(file, getView())
            }
            R.id.action_retry -> {
                backgroundJobManager.startOfflineOperations()
            }
            R.id.action_encrypted -> Unit
            R.id.action_unset_encrypted -> Unit
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.cancelBtn -> {
                (containerActivity as FileDisplayActivity).cancelTransference(file)
            }
            R.id.favorite -> {
                containerActivity.fileOperationsHelper.toggleFavoriteFile(file, !file.isFavorite)
                setFavoriteIconStatus(!file.isFavorite)
            }
            R.id.overflow_menu -> {
                onOverflowIconClicked()
            }
            R.id.last_modification_timestamp -> {
                val showDetailedTimestamp = preferences.isShowDetailedTimestampEnabled
                preferences.isShowDetailedTimestampEnabled = showDetailedTimestamp
                setFileModificationTimestamp(file, showDetailedTimestamp)
            }
            R.id.folder_sync_button -> {
                if (binding?.folderSyncButton?.isChecked == true) {
                    file.internalFolderSyncTimestamp = 0L
                } else {
                    file.internalFolderSyncTimestamp = -1L
                }

                storageManager.saveFile(file)
            }
            else -> {
                Log_OC.e(TAG, "Incorrect view clicked!")
            }
        }
    }

    val isEmpty: Boolean
        /**
         * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be
         * replaced.
         *
         * @return True when the fragment was created with the empty layout.
         */
        get() = file == null || user == null

    /**
     * Use this method to signal this Activity that it shall update its view.
     *
     * @param file : An [OCFile]
     */
    fun updateFileDetails(file: OCFile?, user: User?) {
        setFile(file)
        this.user = user
        updateFileDetails(transferring = false, refresh = false)
    }

    /**
     * Updates the view with all relevant details about that file.
     *
     *
     * TODO Remove parameter when the transferring state of files is kept in database.
     *
     * @param transferring Flag signaling if the file should be considered as downloading or uploading, although
     * [FileDownloadHelper.isDownloading]  and
     * [FileUploadHelper.isUploading] return false.
     * @param refresh      If 'true', try to refresh the whole file from the database
     */
    fun updateFileDetails(transferring: Boolean, refresh: Boolean) {
        if (readyToShow()) {
            val storageManager = containerActivity.storageManager ?: return

            if (refresh) {
                file = storageManager.getFileByPath(file.remotePath)
            }
            val file = file

            // set file details
            if (MimeTypeUtil.isImage(file)) {
                binding?.filename?.text = file.fileName
            } else {
                binding?.filename?.visibility = View.GONE
            }
            binding?.size?.text = DisplayUtils.bytesToHumanReadable(file.fileLength)

            val showDetailedTimestamp = preferences.isShowDetailedTimestampEnabled
            setFileModificationTimestamp(file, showDetailedTimestamp)

            setFilePreview(file)
            setFavoriteIconStatus(file.isFavorite)

            // configure UI for depending upon local state of the file
            if (transferring ||
                (FileDownloadHelper.instance().isDownloading(user, file)) ||
                (FileUploadHelper.instance().isUploading(user, file))
            ) {
                setButtonsForTransferring()
            } else if (file.isDown) {
                setButtonsForDown()
            } else {
                // TODO load default preview image; when the local file is removed, the preview
                // remains there
                setButtonsForRemote()
            }

            val fabMain = requireActivity().findViewById<FloatingActionButton>(R.id.fab_main)
            fabMain?.hide()

            binding?.syncBlock.setVisibleIf(file.isFolder)

            if (file.isInternalFolderSync) {
                binding?.folderSyncButton?.isChecked = file.isInternalFolderSync
            } else {
                if (storageManager.isPartOfInternalTwoWaySync(file)) {
                    binding?.folderSyncButton?.isChecked = true
                    binding?.folderSyncButton?.isEnabled = false
                }
            }
        }

        setupTabLayout()
        setupViewPager()
        getView()?.invalidate()
    }

    private fun setFileModificationTimestamp(file: OCFile, showDetailedTimestamp: Boolean) {
        binding?.lastModificationTimestamp?.run {
            text = if (showDetailedTimestamp) {
                DisplayUtils.unixTimeToHumanReadable(file.modificationTimestamp)
            } else {
                DisplayUtils.getRelativeTimestamp(
                    context,
                    file.modificationTimestamp
                )
            }
        }
    }

    private fun setFavoriteIconStatus(isFavorite: Boolean) {
        binding?.run {
            if (isFavorite) {
                favorite.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_star, null))
                favorite.contentDescription = getString(R.string.unset_favorite)
            } else {
                favorite.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_star_outline,
                        null
                    )
                )
                favorite.contentDescription = getString(R.string.favorite)
            }
        }
    }

    /**
     * Checks if the fragment is ready to show details of a OCFile
     *
     * @return 'True' when the fragment is ready to show details of a file
     */
    private fun readyToShow(): Boolean {
        return file != null && user != null
    }

    /**
     * Updates the file preview if possible
     *
     * @param file a [OCFile] to be previewed
     */
    private fun setFilePreview(file: OCFile) {
        var resizedImage: Bitmap?
        if (toolbarActivity == null || !MimeTypeUtil.isImage(file)) {
            previewLoaded = false
            return
        }

        val tagId = ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + getFile().remoteId
        resizedImage = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId)

        if (resizedImage != null && !file.isUpdateThumbnailNeeded) {
            toolbarActivity?.setPreviewImageBitmap(resizedImage)
            previewLoaded = true
        } else {
            // show thumbnail while loading resized image
            var thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                ThumbnailsCacheManager.PREFIX_THUMBNAIL + getFile().remoteId
            )

            if (thumbnail != null) {
                toolbarActivity?.setPreviewImageBitmap(thumbnail)
            } else {
                thumbnail = ThumbnailsCacheManager.mDefaultImg
            }

            // generate new resized image
            if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(
                    getFile(),
                    toolbarActivity?.previewImageView
                ) &&
                containerActivity.storageManager != null
            ) {
                val task =
                    ResizedImageGenerationTask(
                        this,
                        toolbarActivity?.previewImageView,
                        toolbarActivity?.previewImageContainer,
                        containerActivity.storageManager,
                        connectivityService,
                        containerActivity.storageManager.user,
                        resources.getColor(
                            R.color.background_color_inverse,
                            requireContext().theme
                        )
                    )

                if (resizedImage == null) {
                    resizedImage = thumbnail
                }

                val asyncDrawable =
                    AsyncResizedImageDrawable(
                        requireContext().resources,
                        resizedImage,
                        task
                    )

                toolbarActivity?.setPreviewImageDrawable(asyncDrawable)
                previewLoaded = true
                task.execute(getFile())
            }
        }
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private fun setButtonsForTransferring() {
        if (isEmpty) {
            return
        }

        binding?.run {
            progressBlock.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE

            if (FileDownloadHelper.instance().isDownloading(user, file)) {
                progressText.setText(R.string.downloader_download_in_progress_ticker)
            } else {
                if (FileUploadHelper.instance().isUploading(user, file)) {
                    progressText.setText(R.string.uploader_upload_in_progress_ticker)
                }
            }
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private fun setButtonsForDown() {
        if (!isEmpty) {
            binding?.progressBlock?.visibility = View.GONE
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private fun setButtonsForRemote() {
        if (!isEmpty) {
            binding?.progressBlock?.visibility = View.GONE
        }
    }

    @Suppress("ReturnCount")
    fun listenForTransferProgress() {
        if (progressListener == null) {
            Log_OC.d(TAG, "progressListener == null")
            return
        }

        containerActivity.fileDownloadProgressListener?.addDataTransferProgressListener(progressListener, file)

        if (containerActivity.fileUploaderHelper == null) {
            return
        }

        if (user == null || file == null) {
            return
        }

        val targetKey = buildRemoteName(user!!.accountName, file.remotePath)
        containerActivity.fileUploaderHelper.addUploadTransferProgressListener(progressListener!!, targetKey)
    }

    @Suppress("ReturnCount")
    private fun leaveTransferProgress() {
        if (progressListener == null) {
            return
        }

        containerActivity.fileDownloadProgressListener?.removeDataTransferProgressListener(progressListener, file)

        if (containerActivity.fileUploaderHelper == null) {
            return
        }

        if (user == null || file == null) {
            return
        }

        val targetKey = buildRemoteName(user!!.accountName, file.remotePath)
        containerActivity.fileUploaderHelper.removeUploadTransferProgressListener(progressListener!!, targetKey)
    }

    private fun showEmptyContent() {
        binding?.run {
            emptyList.emptyListView.visibility = View.VISIBLE
            detailContainer.visibility = View.GONE
            emptyList.emptyListViewHeadline.setText(R.string.file_details_no_content)
            emptyList.emptyListIcon.setImageResource(R.drawable.ic_list_empty_error)
            emptyList.emptyListIcon.visibility = View.VISIBLE
        }
    }

    fun initiateSharingProcess(shareeName: String, shareType: ShareType, secureShare: Boolean) {
        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .add(
                R.id.sharing_frame_container,
                newInstance(
                    file,
                    shareeName,
                    shareType,
                    secureShare
                ),
                FileDetailsSharingProcessFragment.TAG
            )
            .commit()

        showHideFragmentView(true)
    }

    fun showHideFragmentView(isFragmentReplaced: Boolean) {
        binding?.run {
            tabLayout.setVisibleIf(!isFragmentReplaced)
            pager.setVisibleIf(!isFragmentReplaced)
            sharingFrameContainer.setVisibleIf(isFragmentReplaced)
        }

        val mFabMain = requireActivity().findViewById<FloatingActionButton>(R.id.fab_main)
        if (isFragmentReplaced) {
            mFabMain.hide()
        } else {
            mFabMain.show()
        }
    }

    fun editExistingShare(
        share: OCShare,
        screenTypePermission: Int,
        isReshareShown: Boolean,
        isExpiryDateShown: Boolean
    ) {
        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .add(
                R.id.sharing_frame_container,
                newInstance(
                    share,
                    screenTypePermission,
                    isReshareShown,
                    isExpiryDateShown
                ),
                FileDetailsSharingProcessFragment.TAG
            )
            .commit()
        showHideFragmentView(true)
    }

    @Suppress("DEPRECATION")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: FavoriteEvent) {
        try {
            val user = accountManager.user
            val client = clientFactory.create(user) ?: return

            val toggleFavoriteOperation = ToggleFavoriteRemoteOperation(
                event.shouldFavorite,
                event.remotePath
            )

            val remoteOperationResult = toggleFavoriteOperation.execute(client)

            if (remoteOperationResult.isSuccess) {
                file.isFavorite = event.shouldFavorite
                val file = storageManager.getFileByEncryptedRemotePath(event.remotePath)
                file?.isFavorite = event.shouldFavorite
                storageManager.saveFile(file)
            }
        } catch (e: CreationException) {
            Log_OC.e(TAG, "Error processing event", e)
        }
    }

    private fun showSharingTab(): Boolean {
        if (!shareViaLink(requireContext()) && !shareViaUser(requireContext())) {
            return false
        }

        return if (file.isEncrypted) {
            if (parentFolder == null) {
                parentFolder = storageManager.getFileById(file.parentId)
            }

            EncryptionUtils.supportsSecureFiledrop(file, user) && parentFolder?.isEncrypted != true
        } else {
            true
        }
    }

    private inner class ProgressListener(progressBar: ProgressBar) : OnDatatransferProgressListener {
        private var lastPercent = 0
        private val progressBarReference =
            WeakReference(progressBar)

        @Suppress("MagicNumber")
        override fun onTransferProgress(
            progressRate: Long,
            totalTransferredSoFar: Long,
            totalToTransfer: Long,
            filename: String
        ) {
            val percent = (100.0 * (totalTransferredSoFar.toDouble()) / (totalToTransfer.toDouble())).toInt()
            if (percent != lastPercent) {
                val pb = progressBarReference.get()
                pb?.progress = percent
                pb?.postInvalidate()
            }
            lastPercent = percent
        }
    }

    companion object {
        private val TAG: String = FileDetailFragment::class.java.simpleName
        private const val FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT"
        const val FTAG_RENAME_FILE: String = "RENAME_FILE_FRAGMENT"

        private const val ARG_FILE = "FILE"
        private const val ARG_PARENT_FOLDER = "PARENT_FOLDER"
        private const val ARG_USER = "USER"
        private const val ARG_ACTIVE_TAB = "TAB"

        /**
         *
         * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout
         * (to use when a file wasn't tapped before).
         *
         * @param fileToDetail An [OCFile] to show in the fragment
         * @param user         Currently active user
         * @return New fragment with arguments set
         */
        @JvmStatic
        fun newInstance(fileToDetail: OCFile?, parentFolder: OCFile?, user: User?): FileDetailFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, fileToDetail)
                putParcelable(ARG_PARENT_FOLDER, parentFolder)
                putParcelable(ARG_USER, user)
            }

            return FileDetailFragment().apply {
                arguments = args
            }
        }

        /**
         *
         * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
         *
         * @param fileToDetail An [OCFile] to show in the fragment
         * @param user         Currently active user
         * @param activeTab    to be active tab
         * @return New fragment with arguments set
         */
        @JvmStatic
        fun newInstance(fileToDetail: OCFile?, user: User?, activeTab: Int): FileDetailFragment {
            val args = Bundle().apply {
                putParcelable(ARG_FILE, fileToDetail)
                putParcelable(ARG_USER, user)
                putInt(ARG_ACTIVE_TAB, activeTab)
            }

            return FileDetailFragment().apply {
                arguments = args
            }
        }
    }
}
