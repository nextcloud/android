/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.client.utils.Throttler
import com.nextcloud.ui.albumItemActions.AlbumItemActionsBottomSheet
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.nextcloud.utils.extensions.isLandscape
import com.nextcloud.utils.extensions.setVisibleIf
import com.nextcloud.utils.extensions.toAlbumItem
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.ListFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.PhotoAlbumEntry
import com.owncloud.android.lib.resources.albums.ReadAlbumsRemoteOperation
import com.owncloud.android.lib.resources.albums.RemoveAlbumFileRemoteOperation
import com.owncloud.android.lib.resources.albums.ToggleAlbumFavoriteRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.status.Type
import com.owncloud.android.operations.albums.ReadAlbumItemsOperation
import com.owncloud.android.ui.activity.AlbumsPickerActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.GalleryAdapter
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.ui.dialog.CreateAlbumDialogFragment
import com.owncloud.android.ui.events.FavoriteEvent
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.albums.bottomsheet.AlbumSharingBottomSheet
import com.owncloud.android.ui.fragment.albums.bottomsheet.AlbumSharingBottomSheetActions
import com.owncloud.android.ui.fragment.albums.model.AlbumItemsEmptyState
import com.owncloud.android.ui.fragment.albums.util.AlbumCollageLayout
import com.owncloud.android.ui.fragment.helper.ColumnCount
import com.owncloud.android.ui.helpers.UriUploader
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewMediaActivity.Companion.canBePreviewed
import com.owncloud.android.utils.ClipboardUtil
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Optional
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@Suppress("TooManyFunctions")
class AlbumItemsFragment :
    Fragment(),
    OCFileListFragmentInterface,
    AlbumSharingBottomSheetActions,
    Injectable {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var throttler: Throttler

    @Inject
    lateinit var thumbnailGenerator: ThumbnailGenerator

    private lateinit var binding: ListFragmentBinding
    private lateinit var albumName: String

    private var adapter: GalleryAdapter? = null
    private var addMediaFab: FloatingActionButton? = null
    private var client: OwnCloudClient? = null
    private var optionalUser: Optional<User>? = null
    private var containerActivity: FileFragment.ContainerActivity? = null
    private var selectionMode: AlbumItemsMultiChoiceModeListener? = null
    private var albumSharingBottomSheet: AlbumSharingBottomSheet? = null
    private var photoAlbumEntry: PhotoAlbumEntry? = null

    private var columnSize = 0
    private var isNewAlbum = false

    private var albumRemoteFiles = listOf<RemoteFile>()
    private val albumItems = mutableListOf<OCFile>()

    private val refreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val selectMediaFromAppsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { requestUploadOfContentFromApps(it) }
            }
        }

    //region Lifecycle
    override fun onAttach(context: Context) {
        super.onAttach(context)
        containerActivity = context as? FileFragment.ContainerActivity
            ?: throw IllegalArgumentException(
                "$context must implement ${FileFragment.ContainerActivity::class.java.simpleName}"
            )

        arguments?.let {
            albumName = it.getString(ARG_ALBUM_NAME) ?: ""
            isNewAlbum = it.getBoolean(ARG_IS_NEW_ALBUM)
        }
    }

    override fun onDetach() {
        containerActivity = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        columnSize = ColumnCount.Wide.get(resources.isLandscape())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        optionalUser = Optional.of(accountManager.user)

        expandAppBar()
        createMenu()
        setupSwipeRefresh()
        setupList()
        createAddMediaButton()
        observeRefreshRequests()

        if (isNewAlbum) {
            openGalleryToAddMedia()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        getTypedActivity(FileDisplayActivity::class.java)?.run {
            setupToolbar()
            supportActionBar?.let { viewThemeUtils.files.themeActionBar(requireContext(), it, albumName) }
            showSortListGroup(false)
            setMainFabVisible(false)
            clearToolbarSubtitle()
        }
    }

    override fun onPause() {
        super.onPause()
        adapter?.cancelAllPendingTasks()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroyView() {
        lastMediaItemPosition = 0
        addMediaFab = null
        super.onDestroyView()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        columnSize = ColumnCount.Wide.get(newConfig.isLandscape())
        adapter?.changeColumn(columnSize)
        adapter?.notifyDataSetChanged()
    }
    //endregion

    //region View setup
    private fun expandAppBar() {
        getTypedActivity(FileDisplayActivity::class.java)
            ?.findViewById<AppBarLayout>(R.id.appbar)
            ?.setExpanded(true, false)
    }

    private fun setupList() {
        binding.listRoot.setEmptyView(binding.emptyList.emptyListView)
        binding.listRoot.layoutManager = GridLayoutManager(requireContext(), SINGLE_SPAN)
    }

    private fun setupSwipeRefresh() {
        viewThemeUtils.androidx.themeSwipeRefreshLayout(binding.swipeContainingList)
        binding.swipeContainingList.setOnRefreshListener {
            binding.swipeContainingList.isRefreshing = true
            refreshData()
        }
    }

    private fun createAddMediaButton() {
        addMediaFab = FloatingActionButton(requireContext()).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.ic_plus)
            contentDescription = getString(R.string.add_media)
            viewThemeUtils.material.themeFAB(this)
            setOnClickListener { openAddMediaMenu() }
        }

        val layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            marginEnd = resources.getDimensionPixelSize(R.dimen.standard_margin)
            bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_navigation_view_margin)
        }

        binding.listFragmentLayout.addView(addMediaFab, layoutParams)
    }

    private fun initializeClient() {
        if (client != null) {
            return
        }

        val user = optionalUser?.takeIf { it.isPresent }?.get() ?: return

        client = try {
            clientFactory.create(user)
        } catch (e: CreationException) {
            Log_OC.e(TAG, "Error initializing client", e)
            null
        }
    }

    private fun initializeAdapter() {
        initializeClient()

        if (adapter == null) {
            adapter = GalleryAdapter(
                requireContext(),
                accountManager.user,
                this,
                containerActivity!!,
                viewThemeUtils,
                columnSize,
                ThumbnailsCacheManager.getThumbnailDimension(),
                thumbnailGenerator
            ).apply { setHasStableIds(true) }

            setUpSelectionMode()
        }

        binding.listRoot.adapter = adapter

        lastMediaItemPosition?.let { binding.listRoot.layoutManager?.scrollToPosition(it) }
    }

    private fun setUpSelectionMode() {
        if (selectionMode != null) {
            return
        }

        selectionMode = AlbumItemsMultiChoiceModeListener(
            activity = requireActivity(),
            adapter = adapter,
            viewThemeUtils = viewThemeUtils,
            openActionsMenu = { filesCount, checkedFiles -> openActionsMenu(filesCount, checkedFiles) },
            onSelectionModeChanged = { isActive -> addMediaFab.setVisibleIf(!isActive) }
        )

        (requireActivity() as FileDisplayActivity).addDrawerListener(selectionMode)
    }
    //endregion

    //region Album items loading
    @OptIn(FlowPreview::class)
    private fun observeRefreshRequests() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                refreshFlow.onStart { emit(Unit) }
                    .onEach { binding.swipeContainingList.isRefreshing = true }
                    .debounce(DEBOUNCE_DELAY.milliseconds)
                    .collect { fetchAndSetData() }
            }
        }
    }

    fun refreshData() {
        refreshFlow.tryEmit(Unit)
    }

    private fun fetchAndSetData() {
        binding.swipeContainingList.isRefreshing = true
        selectionMode?.exitSelectionMode()
        initializeAdapter()
        showLoadingMessageWhenReachable()

        lifecycleScope.launch(Dispatchers.IO) {
            val client = client ?: run {
                withContext(Dispatchers.Main) { onAlbumItemsFailed(null) }
                return@launch
            }

            val result = ReadAlbumItemsOperation(albumName, containerActivity?.storageManager).execute(client)

            if (result?.isSuccess != true || result.resultData == null) {
                withContext(Dispatchers.Main) { onAlbumItemsFailed(result) }
                return@launch
            }

            storeAlbumItems(result.resultData)

            withContext(Dispatchers.Main) { onAlbumItemsLoaded() }
        }
    }

    private fun storeAlbumItems(remoteFiles: List<RemoteFile>) {
        val storageManager = containerActivity?.storageManager
        storageManager?.deleteVirtuals(VirtualFolderType.ALBUM)

        albumRemoteFiles = remoteFiles
        albumItems.clear()
        albumItems.addAll(
            remoteFiles.map { storageManager?.getFileByLocalId(it.localId) ?: it.toAlbumItem() }
        )

        val virtuals = albumItems.filter { it.fileId > 0 }.map { it.toAlbumVirtualEntry() }
        storageManager?.saveVirtuals(virtuals)
    }

    private fun OCFile.toAlbumVirtualEntry(): ContentValues = ContentValues().apply {
        put(ProviderTableMeta.VIRTUAL_TYPE, VirtualFolderType.ALBUM.toString())
        put(ProviderTableMeta.VIRTUAL_OCFILE_ID, fileId)
    }

    private fun onAlbumItemsLoaded() {
        if (albumItems.isEmpty()) {
            setMessageForEmptyList(AlbumItemsEmptyState.NO_ITEMS)
        }

        populateList(albumItems)
        refreshAlbumMetaData()
        hideRefreshLayoutLoader()
    }

    private fun onAlbumItemsFailed(result: RemoteOperationResult<*>?) {
        Log_OC.e(TAG, "reading album items failed: ${result?.logMessage}")
        setMessageForEmptyList(AlbumItemsEmptyState.fromFailure(result))
        refreshAlbumMetaData()
        hideRefreshLayoutLoader()
    }

    @VisibleForTesting
    fun populateList(albums: List<OCFile>) {
        selectionMode?.exitSelectionMode()
        getTypedActivity(FileDisplayActivity::class.java)?.setMainFabVisible(false)
        initializeAdapter()
        adapter?.showAlbumItems(albums)
    }

    /**
     * Also called by FileDisplayActivity once a share link operation finished, so the sharing sheet picks up the
     * new share id.
     */
    fun refreshAlbumMetaData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val albumsRemoteOperation = ReadAlbumsRemoteOperation(albumName)
            val result = client?.let { albumsRemoteOperation.execute(it) }

            withContext(Dispatchers.Main) {
                photoAlbumEntry = result
                    ?.takeIf { it.isSuccess }
                    ?.resultData
                    ?.firstOrNull()

                sendRefreshedShareIdToAlbumsSharingSheet()
            }
        }
    }

    private fun hideRefreshLayoutLoader() {
        binding.swipeContainingList.isRefreshing = false
    }
    //endregion

    //region Empty state
    private fun showLoadingMessageWhenReachable() {
        val connectivityService = getTypedActivity(FileActivity::class.java)?.connectivityService ?: return

        connectivityService.isNetworkAndServerAvailable { available ->
            if (!available) {
                return@isNetworkAndServerAvailable
            }

            with(binding.emptyList) {
                emptyListViewHeadline.setText(R.string.file_list_loading)
                emptyListViewText.text = ""
                emptyListIcon.visibility = View.GONE
            }
        }
    }

    private fun setMessageForEmptyList(state: AlbumItemsEmptyState) = with(binding.emptyList) {
        emptyListViewHeadline.setText(state.headline)
        emptyListViewText.setText(state.message)
        emptyListIcon.setImageResource(state.icon)

        emptyListIcon.visibility = View.VISIBLE
        emptyListViewText.visibility = View.VISIBLE
    }
    //endregion

    //region Album menu
    private fun createMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.clear()
                    menuInflater.inflate(R.menu.fragment_album_items, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean = onAlbumActionChosen(menuItem.itemId)
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun onAlbumActionChosen(@IdRes itemId: Int): Boolean {
        when (itemId) {
            R.id.action_upload_from_camera_roll -> addFromCameraRoll()
            R.id.action_select_images_from_account -> openGalleryToAddMedia()
            R.id.action_rename_album -> showRenameAlbumDialog()
            R.id.action_share_album -> openAlbumSharingBottomSheet()
            R.id.action_delete_album -> confirmAlbumRemoval()
            else -> return false
        }

        return true
    }

    private fun openAddMediaMenu() {
        throttler.run(THROTTLE_ADD_MEDIA) {
            val fragmentManager = requireActivity().supportFragmentManager

            AlbumItemActionsBottomSheet.newInstance()
                .setResultListener(fragmentManager, this) { id -> onAlbumActionChosen(id) }
                .show(fragmentManager, TAG_ALBUM_ACTIONS)
        }
    }

    private fun showRenameAlbumDialog() {
        CreateAlbumDialogFragment.newInstance(albumName)
            .show(requireActivity().supportFragmentManager, CreateAlbumDialogFragment.TAG)
    }

    fun onAlbumRenamed(newAlbumName: String) {
        albumName = newAlbumName
        getTypedActivity(FileDisplayActivity::class.java)?.updateActionBarTitleAndHomeButtonByString(albumName)
    }

    fun onAlbumDeleted() {
        requireActivity().supportFragmentManager.popBackStack()
    }
    //endregion

    //region Item interaction
    override fun getColumnsCount(): Int = columnSize

    override fun isLoading(): Boolean = false

    override fun onHeaderClicked() = Unit

    override fun onShareIconClick(file: OCFile?) = Unit

    override fun showShareDetailView(file: OCFile?) = Unit

    override fun showActivityDetailView(file: OCFile?) = Unit

    override fun onOverflowIconClicked(file: OCFile?, view: View?) = Unit

    override fun onItemClicked(file: OCFile) {
        if (adapter?.isMultiSelect() == true) {
            toggleItemToCheckedList(file)
            return
        }

        val activity = containerActivity as? FileDisplayActivity ?: return

        when {
            PreviewImageFragment.canBePreviewed(file) ->
                activity.startImagePreview(file, !file.isDown, VirtualFolderType.ALBUM)

            file.isDown && canBePreviewed(file) ->
                activity.startMediaPreview(file, 0, true, true, false, true)

            file.isDown -> containerActivity?.fileOperationsHelper?.openFile(file)

            canBePreviewed(file) && !file.isEncrypted ->
                activity.startMediaPreview(file, 0, true, true, true, true)

            else -> Log_OC.d(TAG, "Couldn't handle item click")
        }
    }

    override fun onLongItemClicked(file: OCFile): Boolean {
        if (selectionMode?.isActionModeActive == true) {
            toggleItemToCheckedList(file)
            return true
        }

        requireActivity().startActionMode(selectionMode)
        adapter?.addCheckedFile(file)
        selectionMode?.updateActionModeFile(file)
        return true
    }

    private fun toggleItemToCheckedList(file: OCFile) {
        adapter?.run {
            if (isCheckedFile(file)) {
                removeCheckedFile(file)
            } else {
                addCheckedFile(file)
            }
        }

        selectionMode?.updateActionModeFile(file)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun selectAllFiles(select: Boolean) {
        adapter?.let {
            it.selectAll(select)
            it.notifyDataSetChanged()
            selectionMode?.invalidateActionMode()
        }
    }
    //endregion

    //region File actions
    private fun openActionsMenu(filesCount: Int, checkedFiles: Set<OCFile>) {
        throttler.run(THROTTLE_OVERFLOW_CLICK) {
            val actionsSheet = FileActionsBottomSheet.newInstance(
                filesCount,
                checkedFiles,
                true,
                hiddenFileActions(checkedFiles),
                false,
                contextMenuEndpoints(checkedFiles)
            ).setResultListener(childFragmentManager, this) { id -> onFileActionChosen(id, checkedFiles) }

            if (isDialogFragmentReady()) {
                actionsSheet.show(childFragmentManager, TAG_FILE_ACTIONS)
            }
        }
    }

    private fun hiddenFileActions(checkedFiles: Set<OCFile>): List<Int> {
        val hidden = UNSUPPORTED_ALBUM_FILE_ACTIONS.toMutableList()

        if (checkedFiles.any { it.isOfflineOperation }) {
            hidden.add(R.id.action_favorite)
        }

        return hidden
    }

    private fun contextMenuEndpoints(checkedFiles: Set<OCFile>) = containerActivity
        ?.storageManager
        ?.getCapability(optionalUser?.get())
        ?.getClientIntegrationEndpoints(Type.CONTEXT_MENU, checkedFiles.first().mimeType)
        .orEmpty()

    private fun onFileActionChosen(@IdRes itemId: Int, checkedFiles: Set<OCFile>): Boolean {
        if (checkedFiles.isEmpty()) {
            return false
        }

        val fileOperationsHelper = containerActivity?.fileOperationsHelper

        when (itemId) {
            R.id.action_remove_file -> confirmFilesRemoval(checkedFiles)
            R.id.action_favorite -> fileOperationsHelper?.toggleFavoriteFiles(checkedFiles, true)
            R.id.action_unset_favorite -> fileOperationsHelper?.toggleFavoriteFiles(checkedFiles, false)
            R.id.action_open_file_with -> fileOperationsHelper?.openFile(checkedFiles.first())
            R.id.action_stream_media -> fileOperationsHelper?.streamMediaFile(checkedFiles.first())
            R.id.action_select_all_action_menu -> selectAllFiles(true)
            R.id.action_deselect_all_action_menu -> selectAllFiles(false)
        }

        return true
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: FavoriteEvent) {
        try {
            val client = clientFactory.create(accountManager.user)
            val toggleFavoriteOperation = ToggleAlbumFavoriteRemoteOperation(event.shouldFavorite, event.remotePath)

            if (!toggleFavoriteOperation.execute(client).isSuccess) {
                return
            }

            Handler(Looper.getMainLooper()).post { selectionMode?.exitSelectionMode() }
            adapter?.markAsFavorite(event.remotePath, event.shouldFavorite)
        } catch (e: CreationException) {
            Log_OC.e(TAG, "Error processing event", e)
        }
    }
    //endregion

    //region Removal
    private fun confirmAlbumRemoval() = showRemovalConfirmation(
        R.string.confirmation_remove_folder_alert,
        albumName
    ) { containerActivity?.fileOperationsHelper?.removeAlbum(albumName) }

    private fun confirmFilesRemoval(files: Collection<OCFile>) {
        val isSingleFile = files.size == SINGLE_SELECTION
        val messageId = if (isSingleFile) {
            R.string.confirmation_remove_file_from_album_message
        } else {
            R.string.confirmation_remove_files_from_album_message
        }

        showRemovalConfirmation(messageId,files.first().fileName.takeIf { isSingleFile }) {
            removeFilesFromAlbum(files)
        }
    }

    private fun showRemovalConfirmation(
        @StringRes messageId: Int,
        name: String?,
        onConfirmed: () -> Unit
    ) {
        val dialog = ConfirmationDialogFragment.newInstance(
            messageResId = messageId,
            messageArguments = arrayOf(name),
            titleResId = NO_RESOURCE,
            positiveButtonTextId = R.string.remove,
            negativeButtonTextId = R.string.common_cancel,
            neutralButtonTextId = NO_RESOURCE
        )

        dialog.setCancelable(false)
        dialog.setOnConfirmationListener(
            object : ConfirmationDialogFragmentListener {
                override fun onConfirmation(callerTag: String?) = onConfirmed()
                override fun onNeutral(callerTag: String?) = Unit
                override fun onCancel(callerTag: String?) = Unit
            }
        )

        dialog.show(requireActivity().supportFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
    }

    private fun removeFilesFromAlbum(files: Collection<OCFile>) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { showLoadingDialog() }

            val failedFiles = try {
                removeFromAlbum(clientFactory.create(accountManager.user), files)
            } catch (e: CreationException) {
                Log_OC.e(TAG, "Error removing album files", e)
                emptyList()
            }

            Log_OC.d(TAG, "Files that could not be removed: ${failedFiles.size}")

            withContext(Dispatchers.Main) {
                if (failedFiles.isNotEmpty() && files.size > SINGLE_SELECTION) {
                    DisplayUtils.showSnackMessage(requireActivity(), getString(R.string.album_delete_failed_message))
                }

                dismissLoadingDialog()
                refreshData()
            }
        }
    }

    private suspend fun removeFromAlbum(client: OwnCloudClient, files: Collection<OCFile>): List<OCFile> {
        val failedFiles = mutableListOf<OCFile>()

        files.forEach { file ->
            val operation = RemoveAlbumFileRemoteOperation(albumRemotePathForRemoval(file))
            val result = operation.execute(client)

            if (result.isSuccess) {
                return@forEach
            }

            failedFiles.add(file)

            if (files.size == SINGLE_SELECTION) {
                val message = ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources)
                withContext(Dispatchers.Main) { DisplayUtils.showSnackMessage(requireActivity(), message) }
            }
        }

        return failedFiles
    }

    /**
     * Once the items have been fetched they carry their real remote path instead of the album one, which the removal
     * endpoint needs.
     */
    private fun albumRemotePathForRemoval(file: OCFile): String {
        if (file.remotePath.startsWith("$ALBUMS_REMOTE_PATH$albumName")) {
            return file.remotePath
        }

        return albumRemoteFiles
            .find { it.etag == file.etag || it.etag == file.etagOnServer }
            ?.remotePath
            ?: file.remotePath
    }

    private fun showLoadingDialog() {
        getTypedActivity(FileDisplayActivity::class.java)
            ?.showLoadingDialog(getString(R.string.wait_a_moment))
    }

    private fun dismissLoadingDialog() {
        getTypedActivity(FileDisplayActivity::class.java)?.dismissLoadingDialog()
    }
    //endregion

    //region Adding media
    private fun openGalleryToAddMedia() {
        requireActivity().startActivity(AlbumsPickerActivity.intentForPickingMediaFiles(requireActivity(), albumName))
    }

    private fun addFromCameraRoll() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = ANY_MIME_TYPE
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        selectMediaFromAppsLauncher.launch(
            Intent.createChooser(intent, getString(R.string.upload_chooser_title))
        )
    }

    private fun requestUploadOfContentFromApps(contentIntent: Intent) {
        val mediaUris = contentIntent.mediaUris()

        if (mediaUris.isEmpty()) {
            DisplayUtils.showSnackMessage(requireActivity(), R.string.album_unsupported_file)
            return
        }

        uploadToAlbum(mediaUris)
    }

    private fun uploadToAlbum(mediaUris: List<Uri>) {
        val activity = getTypedActivity(FileDisplayActivity::class.java) ?: return
        val user = activity.user.takeIf { it.isPresent }?.get() ?: return

        UriUploader(
            activity = activity,
            urisToUpload = ArrayList<Parcelable?>(mediaUris),
            uploadPath = albumUploadPath(),
            user = user,
            behaviour = FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            showWaitingDialog = false,
            copyTmpTaskListener = null,
            fileDisplayNameTransformer = null,
            albumName = albumName
        ).uploadUris()
    }

    private fun Intent.mediaUris(): List<Uri> {
        val uris = clipData
            ?.let { clip -> (0 until clip.itemCount).map { clip.getItemAt(it).uri } }
            ?: listOfNotNull(data)

        val contentResolver = requireActivity().contentResolver
        return uris.filter { MimeTypeUtil.isImageOrVideo(contentResolver.getType(it)) }
    }

    private fun albumUploadPath(): String =
        "${getString(R.string.instant_upload_path)}/${getString(R.string.drawer_item_album)}/"
    //endregion

    //region Album sharing
    private fun openAlbumSharingBottomSheet() {
        throttler.run(THROTTLE_SHARING_SHEET) {
            val album = photoAlbumEntry ?: return@run
            val fragmentManager = requireActivity().supportFragmentManager

            albumSharingBottomSheet = AlbumSharingBottomSheet.newInstance(
                album,
                albumItems.take(AlbumCollageLayout.MAX_IMAGES),
                this
            )

            albumSharingBottomSheet?.show(fragmentManager, TAG_ALBUM_SHARING)
        }
    }

    private fun sendRefreshedShareIdToAlbumsSharingSheet() {
        if (!isAdded || isDetached) {
            return
        }

        albumSharingBottomSheet
            ?.takeIf { it.isAdded && it.isVisible }
            ?.updateShareId(photoAlbumEntry?.collaborators?.firstOrNull()?.id)
    }

    override fun createShare() {
        containerActivity?.fileOperationsHelper?.albumPublicShareLink(albumName, true)
    }

    override fun removeShare() {
        containerActivity?.fileOperationsHelper?.albumPublicShareLink(albumName, false)
    }

    override fun copyShareLink() {
        ClipboardUtil.copyToClipboard(requireActivity(), shareLink())
    }

    override fun shareAlbumLink() {
        IntentUtil.showShareLinkDialog(requireActivity(), shareLink())
    }

    private fun shareLink(): String? = photoAlbumEntry?.collaborators?.firstOrNull()?.shareLink
    //endregion

    companion object {
        val TAG: String = AlbumItemsFragment::class.java.simpleName

        var lastMediaItemPosition: Int? = null

        private const val ARG_ALBUM_NAME = "album_name"
        private const val ARG_IS_NEW_ALBUM = "is_new_album"

        private const val TAG_ALBUM_ACTIONS = "album_actions"
        private const val TAG_ALBUM_SHARING = "album_sharing_sheet"
        private const val TAG_FILE_ACTIONS = "actions"

        private const val THROTTLE_ADD_MEDIA = "addMediaClick"
        private const val THROTTLE_SHARING_SHEET = "albumSharingSheet"
        private const val THROTTLE_OVERFLOW_CLICK = "overflowClick"

        private const val ALBUMS_REMOTE_PATH = "/albums/"
        private const val ANY_MIME_TYPE = "*/*"

        private const val SINGLE_SELECTION = 1
        private const val SINGLE_SPAN = 1
        private const val NO_RESOURCE = -1
        private const val DEBOUNCE_DELAY = 500L

        /**
         * Actions that never apply to an album item, either because the album endpoint does not support them or
         * because they would act on the file outside of the album.
         */
        private val UNSUPPORTED_ALBUM_FILE_ACTIONS = listOf(
            R.id.action_move_or_copy,
            R.id.action_sync_file,
            R.id.action_encrypted,
            R.id.action_unset_encrypted,
            R.id.action_edit,
            R.id.action_download_file,
            R.id.action_export_file,
            R.id.action_set_as_wallpaper,
            R.id.action_send_file,
            R.id.action_send_share_file,
            R.id.action_see_details,
            R.id.action_rename_file,
            R.id.action_pin_to_homescreen,
            R.id.action_add_to_album,
            R.id.action_lock_file,
            R.id.action_unlock_file
        )

        fun newInstance(albumName: String, isNewAlbum: Boolean = false): AlbumItemsFragment =
            AlbumItemsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ALBUM_NAME, albumName)
                    putBoolean(ARG_IS_NEW_ALBUM, isNewAlbum)
                }
            }
    }
}
