/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2023-2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2018-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020 Joris Bodin <joris.bodin@infomaniak.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2011-2012 Bartosz Przybylski
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.lib.resources.files.ToggleFileLockRemoteOperation
import com.nextcloud.client.account.User
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.documentscan.AppScanOptionalFeature
import com.nextcloud.client.documentscan.DocumentScanActivity
import com.nextcloud.client.utils.Throttler
import com.nextcloud.repository.ClientRepository
import com.nextcloud.ui.fileactions.FileAction
import com.nextcloud.ui.fileactions.FileActionHandler
import com.nextcloud.ui.fileactions.FileActionHost
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.ui.sort.SortOrderUi
import com.nextcloud.utils.AppConstants
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.avatar.AvatarGenerator
import com.nextcloud.utils.e2ee.E2EEActionResolver
import com.nextcloud.utils.e2ee.E2EEDialogPresenter
import com.nextcloud.utils.extensions.getDepth
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.nextcloud.utils.extensions.isWebViewAvailable
import com.nextcloud.utils.extensions.listenEncryptionDialogResult
import com.nextcloud.utils.extensions.logFileSize
import com.nextcloud.utils.extensions.resolveMimeType
import com.nextcloud.utils.extensions.slideHideBottomBehavior
import com.nextcloud.utils.fileNameValidator.FileNameValidator
import com.nextcloud.utils.thumbnail.ThumbnailGenerator
import com.nextcloud.utils.view.FastScrollUtils
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCFileDepth
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.Creator
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.ToggleFavoriteRemoteOperation
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.status.Type
import com.owncloud.android.ui.CompletionCallback
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener
import com.owncloud.android.ui.activity.TextEditorWebView
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.adapter.filelist.CommonOCFileListAdapterInterface
import com.owncloud.android.ui.adapter.filelist.OCFileListAdapter
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment
import com.owncloud.android.ui.dialog.ChooseTemplateDialogFragment
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment
import com.owncloud.android.ui.events.ChangeMenuEvent
import com.owncloud.android.ui.events.CommentsEvent
import com.owncloud.android.ui.events.EncryptionEvent
import com.owncloud.android.ui.events.FavoriteEvent
import com.owncloud.android.ui.events.FileLockEvent
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.fragment.FileFragment.ContainerActivity
import com.owncloud.android.ui.fragment.helper.ParentFolderFinder
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull
import com.owncloud.android.lib.resources.files.SearchRemoteOperation.SearchType as RemoteSearchType

@Suppress("TooManyFunctions", "LargeClass")
open class OCFileListFragment :
    ExtendedListFragment(),
    OCFileListFragmentInterface,
    OCFileListBottomSheetActions,
    Injectable {

    //region Dependencies and state
    @Inject
    lateinit var clientRepository: ClientRepository

    @Inject
    lateinit var throttler: Throttler

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject
    lateinit var fastScrollUtils: FastScrollUtils

    @Inject
    lateinit var editorUtils: EditorUtils

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var fileActionHandler: FileActionHandler

    @Inject
    lateinit var appScanOptionalFeature: AppScanOptionalFeature

    @Inject
    lateinit var thumbnailGenerator: ThumbnailGenerator

    @Inject
    lateinit var avatarGenerator: AvatarGenerator

    @Inject
    lateinit var e2eeActionResolver: E2EEActionResolver

    @Inject
    lateinit var deviceInfo: DeviceInfo

    lateinit var e2eeDialogPresenter: E2EEDialogPresenter
        private set

    lateinit var encryptedClickHandler: EncryptedFolderClickHandler
        private set

    lateinit var folderEncryption: FolderEncryption
        private set

    var containerActivity: ContainerActivity? = null
        private set

    var currentFile: OCFile? = null
        protected set

    var adapter: OCFileListAdapter? = null
        protected set

    open val commonAdapter: CommonOCFileListAdapterInterface?
        get() = adapter

    var currentSearchType: SearchType = SearchType.NO_SEARCH
    var isSearchFragment = false
    var searchEvent: SearchEvent? = null
        protected set

    var hideFab = true
    val parentFolderFinder = ParentFolderFinder()

    var fileListLayoutManager: FileListLayoutManager? = null
        private set

    protected var menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH

    private var onlyFoldersClickable = false
    private var fileSelectable = false
    private var limitToMimeType: String? = null
    private var searchTask: OCFileListSearchTask? = null
    private var fabMain: FloatingActionButton? = null
    private val multiChoiceModeListener by lazy { OCFileListMultiChoiceModeListener(this) }
    private val mainHandler = Handler(Looper.getMainLooper())

    private val activeActionMode: ActionMode?
        get() = multiChoiceModeListener.activeActionMode

    private val activeSearchEvent: SearchEvent?
        get() = searchEvent?.takeIf { it.hasQueryOrFilter() }

    private val capabilities: OCCapability
        get() = requireContainerActivity().storageManager.getCapability(accountManager.user.accountName)

    protected enum class MenuItemAddRemove {
        DO_NOTHING,
        REMOVE_SORT,
        REMOVE_GRID_AND_SORT,
        ADD_GRID_AND_SORT_WITH_SEARCH
    }

    fun requireContainerActivity(): ContainerActivity =
        checkNotNull(containerActivity) { "Fragment $this is not attached to a ContainerActivity" }
    //endregion

    //region Lifecycle
    override fun onAttach(context: Context) {
        super.onAttach(context)
        containerActivity = requireNotNull(context as? ContainerActivity) {
            "$context must implement ${ContainerActivity::class.java.simpleName}"
        }
        val refreshListener = requireNotNull(context as? OnEnforceableRefreshListener) {
            "$context must implement ${OnEnforceableRefreshListener::class.java.simpleName}"
        }
        setOnRefreshListener(refreshListener)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        fileListLayoutManager = FileListLayoutManager(this, preferences)

        val state = savedInstanceState ?: arguments
        loadSearchState(state)
        currentFile = state.getParcelableArgument(KEY_FILE, OCFile::class.java)
        isSearchFragment = activeSearchEvent != null
        folderEncryption = FolderEncryption(this)
        encryptedClickHandler = EncryptedFolderClickHandler(this)
        e2eeDialogPresenter = E2EEDialogPresenter(this)

        Log_OC.d(
            TAG,
            "Created ${javaClass.simpleName}: folder=${currentFile?.remotePath}, " +
                "searchType=$currentSearchType, isSearchFragment=$isSearchFragment"
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val state = savedInstanceState ?: arguments
        loadSearchState(state)

        if (state?.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, false) == true) {
            setupSelectionMode(state)
        }

        fabMain = requireActivity().findViewById<FloatingActionButton?>(R.id.fab_main)?.also {
            viewThemeUtils.material.themeFAB(it)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenEncryptionDialogResult()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupFileList(savedInstanceState)
        listDirectory(MainApp.isOnlyOnDevice())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            searchEvent = it.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        if (currentFile != null) {
            super.onResume()
            return
        }

        val activity = activity ?: return
        activity.intent.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)?.let { searchEvent = it }
        activeSearchEvent?.let { startSearch(it) }

        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        adapter?.cancelAllPendingTasks()
        activity?.intent?.removeExtra(SEARCH_EVENT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentFile.logFileSize(TAG)
        outState.putParcelable(KEY_FILE, currentFile)
        if (isSearchFragment) {
            outState.putParcelable(KEY_CURRENT_SEARCH_TYPE, currentSearchType)
            activeSearchEvent?.let { outState.putParcelable(SEARCH_EVENT, it) }
        }
        multiChoiceModeListener.saveState(outState)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter?.cleanup()
    }

    override fun onDetach() {
        setOnRefreshListener(null)
        containerActivity = null
        searchTask?.cancel()
        super.onDetach()
    }
    //endregion

    //region Setup
    fun loadSearchState(state: Bundle?) {
        currentSearchType = state?.getParcelableArgument(KEY_CURRENT_SEARCH_TYPE, SearchType::class.java)
            ?: SearchType.NO_SEARCH
        state?.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)?.let { searchEvent = it }

        if (searchEvent != null && currentSearchType != SearchType.NO_SEARCH) {
            isSearchFragment = true
        }
    }

    private fun setupFileList(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            currentFile = savedInstanceState.getParcelableArgument(KEY_FILE, OCFile::class.java)
        }

        val args = arguments
        onlyFoldersClickable = args?.getBoolean(ARG_ONLY_FOLDERS_CLICKABLE, false) == true
        fileSelectable = args?.getBoolean(ARG_FILE_SELECTABLE, false) == true
        limitToMimeType = args?.getString(ARG_MIMETYPE, "") ?: ""

        setAdapter(args)

        hideFab = args?.getBoolean(ARG_HIDE_FAB, false) == true
        updateFabState()
        if (!hideFab) {
            registerFabListener()
        }

        if (!isSearchFragment) {
            searchEvent = arguments.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)
        }
        applySearchEvent(searchEvent)
        showEmptyStateForSearch(searchEvent)

        mSortButton?.setOnClickListener {
            SortOrderUi.showDialog(parentFragmentManager, preferences.getSortOrderByFolder(currentFile))
        }
        mSwitchGridViewButton?.setOnClickListener { toggleGridAndListLayout() }

        (activity as? FileDisplayActivity)?.let { it.updateActionBarTitleAndHomeButton(it.currentDir) }
    }

    private fun toggleGridAndListLayout() {
        fileListLayoutManager?.let {
            if (isGridEnabled) it.setListAsPreferred() else it.setGridAsPreferred()
        }
        setLayoutSwitchButton()
    }

    protected open fun setAdapter(args: Bundle?) {
        val hideItemOptions = args?.getBoolean(ARG_HIDE_ITEM_OPTIONS, false) == true
        val isGridViewPreferred = fileListLayoutManager?.isGridViewPreferred() == true

        adapter = OCFileListAdapter(
            requireActivity(),
            accountManager.user,
            preferences,
            syncedFolderProvider,
            requireContainerActivity(),
            this,
            hideItemOptions,
            isGridViewPreferred,
            viewThemeUtils,
            thumbnailGenerator,
            avatarGenerator
        )

        setRecyclerViewAdapter(adapter)
        recyclerView?.let { fastScrollUtils.applyFastScroll(it) }
    }

    private fun setupSelectionMode(savedState: Bundle?) {
        savedState?.let { multiChoiceModeListener.restoreState(it) }
        (requireActivity() as? FileActivity)?.addDrawerListener(multiChoiceModeListener)
    }
    //endregion

    //region FAB and bottom navigation
    fun registerFabListener() {
        val fileActivity = activity as? FileActivity
        if (fileActivity == null) {
            Log_OC.w(TAG, "Cannot register FAB listener: activity is not a FileActivity")
            return
        }

        val fab = fabMain
        if (fab == null) {
            Log_OC.w(TAG, "Cannot register FAB listener: FAB is not part of this screen")
            return
        }

        viewThemeUtils.material.themeFAB(fab)
        fab.setOnClickListener { showCreateBottomSheet(fileActivity) }
    }

    private fun showCreateBottomSheet(fileActivity: FileActivity) {
        val currentDir = currentFile
        if (currentDir == null) {
            Log_OC.w(TAG, "Cannot open create bottom sheet: current folder is unknown")
            return
        }

        OCFileListBottomSheetDialog(
            fileActivity,
            this,
            deviceInfo,
            accountManager.user,
            currentDir,
            themeUtils,
            viewThemeUtils,
            editorUtils,
            appScanOptionalFeature
        ).run {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            show()
        }
    }

    fun updateFabState() {
        setFabState(resolveFabState(currentFile))
    }

    fun setFabState(state: FabState) {
        val fab = fabMain ?: return
        val activity = activity ?: return
        activity.runOnUiThread {
            val visible = state != FabState.HIDDEN
            if (visible) fab.show() else fab.hide()
            fab.isEnabled = state == FabState.ENABLED
            viewThemeUtils.material.themeFAB(fab)
            fab.slideHideBottomBehavior(visible)
        }
    }

    private fun resolveFabState(folder: OCFile?): FabState = when {
        hideFab || isSearchFragment || isAlbumScreen() || isReadOnlyFolder(folder) -> FabState.HIDDEN
        folder != null && (folder.canCreateFileAndFolder() || folder.isOfflineOperation) -> FabState.ENABLED
        else -> FabState.DISABLED
    }

    private fun isAlbumScreen(): Boolean {
        val fileDisplayActivity = activity as? FileDisplayActivity ?: return false
        return fileDisplayActivity.isAlbumsFragment || fileDisplayActivity.isAlbumItemsFragment
    }

    private fun setBottomNavigationVisible(visible: Boolean) {
        val drawerActivity = activity as? DrawerActivity ?: return
        drawerActivity.bottomNavigationView.slideHideBottomBehavior(visible)
    }

    private fun isReadOnlyFolder(folder: OCFile?): Boolean {
        folder ?: return false
        val storageManager = containerActivity?.storageManager ?: return false
        return storageManager.isReadOnly(folder)
    }
    //endregion

    //region Create and upload actions
    override fun createFolder(encrypted: Boolean) {
        if (encrypted) {
            encryptedClickHandler.onNewEncryptedFolder()
            return
        }

        showCreateFolderDialog(false)
    }

    fun showCreateFolderDialog(encrypted: Boolean) {
        val activity = activity
        if (activity == null) {
            Log_OC.e(TAG, "Cannot show create folder dialog: fragment is not attached")
            return
        }

        CreateFolderDialogFragment.newInstance(currentFile, encrypted)
            .show(activity.supportFragmentManager, DIALOG_CREATE_FOLDER)
    }

    @Suppress("DEPRECATION")
    override fun uploadFromApp() {
        val action = Intent(Intent.ACTION_GET_CONTENT)
            .setType(ALL_MIME_TYPES)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        requireActivity().startActivityForResult(
            Intent.createChooser(action, getString(R.string.upload_chooser_title)),
            FileDisplayActivity.REQUEST_CODE__SELECT_CONTENT_FROM_APPS
        )
    }

    override fun directCameraUpload() {
        val fileDisplayActivity = activity as? FileDisplayActivity
        if (fileDisplayActivity == null) {
            Log_OC.w(TAG, "Cannot start camera upload: activity is not a FileDisplayActivity")
            SnackbarUtil.show(view, getString(R.string.error_starting_direct_camera_upload))
            return
        }

        if (!PermissionUtil.checkSelfPermission(fileDisplayActivity, Manifest.permission.CAMERA)) {
            PermissionUtil.requestCameraPermission(fileDisplayActivity, PermissionUtil.PERMISSIONS_CAMERA)
            return
        }

        showCameraUploadTypeDialog(fileDisplayActivity)
    }

    private fun showCameraUploadTypeDialog(fileDisplayActivity: FileDisplayActivity) {
        val builder = MaterialAlertDialogBuilder(fileDisplayActivity)
            .setTitle(R.string.upload_direct_camera_promt)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(R.string.upload_direct_camera_video) { _, _ ->
                fileDisplayActivity.fileOperationsHelper.uploadFromCamera(
                    fileDisplayActivity,
                    FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA,
                    true
                )
            }
            .setNegativeButton(R.string.upload_direct_camera_photo) { _, _ ->
                fileDisplayActivity.fileOperationsHelper.uploadFromCamera(
                    fileDisplayActivity,
                    FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA,
                    false
                )
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(fileDisplayActivity, builder)
        builder.show()
    }

    override fun scanDocUpload() {
        val fileDisplayActivity = activity as? FileDisplayActivity
        val currentFile = currentFile
        if (fileDisplayActivity == null || currentFile == null || !currentFile.isFolder) {
            Log_OC.w(
                TAG,
                "Cannot start document scan: activity=${activity?.javaClass?.simpleName}, " +
                    "folder=${currentFile?.remotePath}, isFolder=${currentFile?.isFolder}"
            )
            SnackbarUtil.show(this, R.string.error_starting_doc_scan)
            return
        }

        val intent = Intent(requireContext(), DocumentScanActivity::class.java)
            .putExtra(DocumentScanActivity.EXTRA_FOLDER, currentFile.remotePath)
        startActivity(intent)
    }

    @Suppress("DEPRECATION")
    override fun scanDocUploadFromApp() {
        requireActivity().startActivityForResult(
            scanIntentExternalApp,
            FileDisplayActivity.REQUEST_CODE__SELECT_CONTENT_FROM_APPS_AUTO_RENAME
        )
    }

    override val isScanDocUploadFromAppAvailable: Boolean
        get() {
            val context = activity ?: return false
            return scanIntentExternalApp.resolveActivity(context.packageManager) != null
        }

    override fun uploadFiles() {
        val fileActivity = activity as? FileActivity
        if (fileActivity == null) {
            Log_OC.w(TAG, "Cannot upload files: activity is not a FileActivity")
            return
        }

        val user = fileActivity.user.getOrNull()
        if (user == null) {
            Log_OC.w(TAG, "Cannot upload files: no user is logged in")
            return
        }

        val folder = currentFile
        if (folder == null) {
            Log_OC.w(TAG, "Cannot upload files: current folder is unknown")
            return
        }

        UploadFilesActivity.startUploadActivityForResult(
            fileActivity,
            user,
            FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM,
            folder.isEncrypted
        )
    }

    override fun createRichWorkspace() {
        val fileActivity = activity as? FileActivity ?: return
        val remotePath = currentFile?.remotePath ?: return

        fileActivity.filesRepository.createRichWorkspace(
            remotePath,
            onCompleted = { url ->
                containerActivity?.fileOperationsHelper
                    ?.openRichWorkspaceWithTextEditor(currentFile, url, requireContext())
            },
            onError = {
                Log_OC.e(TAG, "Cannot create rich workspace for $remotePath")
                SnackbarUtil.show(view, R.string.failed_to_start_editor)
            }
        )
    }

    override fun newDocument() =
        showRichDocumentsTemplateDialog(ChooseRichDocumentsTemplateDialogFragment.Type.DOCUMENT)

    override fun newSpreadsheet() =
        showRichDocumentsTemplateDialog(ChooseRichDocumentsTemplateDialogFragment.Type.SPREADSHEET)

    override fun newPresentation() =
        showRichDocumentsTemplateDialog(ChooseRichDocumentsTemplateDialogFragment.Type.PRESENTATION)

    private fun showRichDocumentsTemplateDialog(type: ChooseRichDocumentsTemplateDialogFragment.Type) {
        ChooseRichDocumentsTemplateDialogFragment.newInstance(currentFile, type)
            .show(requireActivity().supportFragmentManager, DIALOG_CREATE_DOCUMENT)
    }

    override fun showTemplate(creator: Creator?, headline: String?) {
        ChooseTemplateDialogFragment.newInstance(currentFile, creator, headline)
            .show(requireActivity().supportFragmentManager, DIALOG_CREATE_DOCUMENT)
    }
    //endregion

    //region List item interaction
    override fun onShareIconClick(file: OCFile) {
        containerActivity?.showDetails(file, SHARING_TAB)
    }

    override fun showShareDetailView(file: OCFile) {
        containerActivity?.showDetails(file, SHARING_TAB)
    }

    override fun showActivityDetailView(file: OCFile) {
        containerActivity?.showDetails(file, ACTIVITY_TAB)
    }

    override fun onOverflowIconClicked(file: OCFile, view: View?) {
        openActionsMenu(SINGLE_SELECTION, hashSetOf(file), true)
    }

    override fun onHeaderClicked() {
        val folder = currentFile ?: return
        if (folder.richWorkspace.isNullOrEmpty()) return

        val adapter = adapter
        if (adapter == null || adapter.isMultiSelect()) return

        val fileDisplayActivity = containerActivity as? FileDisplayActivity ?: return
        fileDisplayActivity.startRichWorkspacePreview(folder)
    }

    override fun onLongItemClicked(file: OCFile): Boolean {
        val activity = activity ?: return true

        if (activeActionMode != null) {
            toggleFileSelection(file)
            return true
        }

        activity.startActionMode(multiChoiceModeListener)
        commonAdapter?.addCheckedFile(file)
        multiChoiceModeListener.onSelectionChanged(file)
        return true
    }

    override fun onItemClicked(file: OCFile) {
        onItemClicked(file, null)
    }

    @OptIn(markerClass = [UnstableApi::class])
    override fun onItemClicked(file: OCFile?, sourceView: View?) {
        if (file == null) {
            Log_OC.w(TAG, "Ignoring click on a null list item")
            return
        }

        val commonAdapter = commonAdapter
        when {
            commonAdapter?.isMultiSelect() == true -> toggleFileSelection(file)
            commonAdapter != null && file.isFolder -> onFolderClicked(file, commonAdapter.getItemPosition(file))
            fileSelectable -> finishWithSelectedFile(file)
            !onlyFoldersClickable -> onFileClicked(file, sourceView)
        }
    }

    override fun isLoading(): Boolean = false

    private fun finishWithSelectedFile(file: OCFile) {
        val intent = Intent().putExtra(FolderPickerActivity.EXTRA_FILES, file)
        requireActivity().run {
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun onFolderClicked(folder: OCFile, position: Int) {
        val folderPickerActivity = requireActivity() as? FolderPickerActivity
        if (folderPickerActivity != null) {
            val filenameErrorMessage = FileNameValidator.checkFileName(folder.fileName, capabilities, requireContext())
            if (filenameErrorMessage != null) {
                SnackbarUtil.show(folderPickerActivity, filenameErrorMessage)
                return
            }
        }

        if (folder.isEncrypted) {
            encryptedClickHandler.onEncryptedFolderClick(folder, position)
            return
        }

        isSearchFragment = false
        setEmptyListMessage(EmptyListState.LOADING)
        browseToFolder(folder, position)
    }

    private fun onFileClicked(file: OCFile, sourceView: View?) {
        val restrictionMessage = getOpenRestrictionMessage(file)
        val recyclerView = recyclerView
        if (recyclerView != null && restrictionMessage != null) {
            Snackbar.make(recyclerView, restrictionMessage, Snackbar.LENGTH_LONG).show()
            return
        }

        val fileDisplayActivity = containerActivity as? FileDisplayActivity
        when {
            fileDisplayActivity != null && fileDisplayActivity.canPreviewInMediaPager(file) ->
                fileDisplayActivity.previewImageWithSearchContext(file, isSearchFragment, currentSearchType, sourceView)

            file.isDown && fileDisplayActivity != null ->
                fileDisplayActivity.previewFile(
                    file,
                    object : CompletionCallback {
                        override fun onComplete(value: Boolean) {
                            if (value) updateFabState() else setFabState(FabState.HIDDEN)
                        }
                    }
                )

            else -> openFile(file)
        }
    }

    @StringRes
    private fun getOpenRestrictionMessage(file: OCFile): Int? = when {
        file.isAPKorAAB -> R.string.gplay_restriction
        file.isOfflineOperation -> R.string.offline_operations_file_does_not_exists_yet
        else -> null
    }

    private fun openFile(file: OCFile) {
        if (!isAccountManagerInitialized()) {
            Log_OC.e(TAG, "Cannot open ${file.remotePath}: account manager is not initialized")
            return
        }

        val context = context
        if (context == null) {
            Log_OC.e(TAG, "Cannot open ${file.remotePath}: fragment is not attached")
            return
        }

        val webViewAvailable = context.isWebViewAvailable()
        val fileDisplayActivity = containerActivity as? FileDisplayActivity

        when {
            !file.isEncrypted && fileDisplayActivity != null && fileDisplayActivity.canPreviewInAudioPlayer(file) -> {
                setFabState(FabState.HIDDEN)
                fileDisplayActivity.startAudioPreview(file, true, true)
            }

            webViewAvailable && hasTextEditor(file) && !file.isEncrypted ->
                TextEditorWebView.startTextEditor(file, context)

            canOpenAsRichDocument(file, webViewAvailable) ->
                containerActivity?.fileOperationsHelper?.openFileAsRichDocument(file, context)

            fileDisplayActivity != null -> {
                fileDisplayActivity.startDownloadForPreview(file, currentFile)
                if (file.isFileEligibleForImmediatePreview) {
                    fileDisplayActivity.setFileIDForImmediatePreview(file.fileId)
                }
            }
        }
    }

    private fun hasTextEditor(file: OCFile): Boolean = editorUtils.getEditor(accountManager.user, file.mimeType) != null

    private fun canOpenAsRichDocument(file: OCFile, webViewAvailable: Boolean): Boolean {
        if (!webViewAvailable) return false

        val user = accountManager.user
        if (user.isAnonymous) return false

        val capability = containerActivity?.storageManager?.getCapability(user.accountName) ?: return false
        val mimeTypes = capability.richDocumentsMimeTypeList ?: return false

        return file.mimeType in mimeTypes &&
            capability.richDocumentsDirectEditing.isTrue &&
            !file.isEncrypted
    }
    //endregion

    //region Selection
    private fun toggleFileSelection(file: OCFile) {
        commonAdapter?.run {
            if (isCheckedFile(file)) removeCheckedFile(file) else addCheckedFile(file)
        }
        multiChoiceModeListener.onSelectionChanged(file)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAllFiles(select: Boolean) {
        val adapter = recyclerView?.adapter
        if (adapter !is CommonOCFileListAdapterInterface) return

        adapter.selectAll(select)
        adapter.notifyDataSetChanged()
        activeActionMode?.invalidate()
    }

    fun exitSelectionMode() {
        activeActionMode?.finish()
    }
    //endregion

    //region File actions
    fun openActionsMenu(filesCount: Int, checkedFiles: Set<OCFile>, isOverflow: Boolean) {
        throttler.run(OVERFLOW_CLICK_THROTTLE_KEY) {
            val actionsToHide = FileAction.getFileListActionsToHide(checkedFiles)
            val mimeType = checkedFiles.first().resolveMimeType()
            val endpoints = capabilities.getClientIntegrationEndpoints(Type.CONTEXT_MENU, mimeType)
            val bottomSheet = FileActionsBottomSheet.newInstance(
                filesCount,
                checkedFiles,
                isOverflow,
                actionsToHide,
                endpoints = endpoints
            )
            showActionsBottomSheet(bottomSheet, checkedFiles)
        }
    }

    fun openCurrentFolderActionsMenu() {
        val currentFolder = currentFile
        if (currentFolder == null || currentFolder.isRootDirectory) return

        val checkedFiles = hashSetOf(currentFolder)
        throttler.run(OVERFLOW_CLICK_THROTTLE_KEY) {
            val actionsToHide = FileAction.getCurrentFolderActionsToHide(currentFolder)
            val bottomSheet = FileActionsBottomSheet.newInstance(currentFolder, true, actionsToHide)
            showActionsBottomSheet(bottomSheet, checkedFiles)
        }
    }

    private fun showActionsBottomSheet(bottomSheet: FileActionsBottomSheet, checkedFiles: Set<OCFile>) {
        bottomSheet.setResultListener(childFragmentManager, this) { id -> onFileActionChosen(id, checkedFiles) }
        if (isDialogFragmentReady()) {
            bottomSheet.show(childFragmentManager, ACTIONS_BOTTOM_SHEET_TAG)
        }
    }

    open fun onFileActionChosen(@IdRes itemId: Int, checkedFiles: Set<OCFile>): Boolean {
        if (checkedFiles.isEmpty()) return false
        if (handleListAction(itemId, checkedFiles)) return true

        val containerActivity = containerActivity ?: return false
        val host = FileActionHost(containerActivity, parentFragmentManager, view, currentFile, activeActionMode)
        val isHandled = fileActionHandler.handle(itemId, checkedFiles, host)
        if (isHandled && itemId in SELECTION_ENDING_ACTIONS) {
            exitSelectionMode()
        }

        return isHandled
    }

    private fun handleListAction(@IdRes itemId: Int, checkedFiles: Set<OCFile>): Boolean {
        when {
            itemId == R.id.action_send_share_file && checkedFiles.size == SINGLE_SELECTION ->
                containerActivity?.showDetails(checkedFiles.first(), SHARING_TAB)

            itemId == R.id.action_see_details && checkedFiles.size == SINGLE_SELECTION ->
                showDetailsAndHideSortGroup(checkedFiles.first())

            itemId == R.id.action_download_file || itemId == R.id.action_sync_file -> {
                syncFilesIfSpaceAvailable(checkedFiles)
                exitSelectionMode()
            }

            itemId == R.id.action_move_or_copy -> startMoveOrCopy(checkedFiles)

            itemId == R.id.action_select_all_action_menu -> selectAllFiles(true)

            itemId == R.id.action_deselect_all_action_menu -> selectAllFiles(false)

            itemId == R.id.action_send_file -> containerActivity?.fileOperationsHelper?.sendFiles(checkedFiles)

            else -> return false
        }

        return true
    }

    private fun showDetailsAndHideSortGroup(file: OCFile) {
        activeActionMode?.finish()
        containerActivity?.run {
            showDetails(file)
            showSortListGroup(false)
        }
    }

    private fun startMoveOrCopy(checkedFiles: Set<OCFile>) {
        val invalidFilename = findInvalidFilename(checkedFiles)
        if (invalidFilename != null) {
            SnackbarUtil.show(
                requireActivity(),
                getString(R.string.file_name_validator_rename_before_move_or_copy, invalidFilename)
            )
            return
        }

        if (!FileNameValidator.checkParentRemotePaths(checkedFiles.toList(), capabilities, requireContext())) {
            Log_OC.w(TAG, "Cannot move or copy: current path contains an invalid folder name, browsing to root")
            browseToRoot()
            SnackbarUtil.show(requireActivity(), R.string.file_name_validator_current_path_is_invalid)
            return
        }

        openFolderPickerForMoveOrCopy(checkedFiles)
    }

    private fun findInvalidFilename(files: Set<OCFile>): String? = files.firstNotNullOfOrNull {
        FileNameValidator.checkFileName(it.fileName, capabilities, requireContext())
    }

    @Suppress("DEPRECATION")
    private fun openFolderPickerForMoveOrCopy(checkedFiles: Set<OCFile>) {
        val activity = requireActivity()
        val paths = checkedFiles.mapTo(ArrayList(checkedFiles.size)) { it.remotePath }
        val action = Intent(activity, FolderPickerActivity::class.java).apply {
            putStringArrayListExtra(FolderPickerActivity.EXTRA_FILE_PATHS, paths)
            putExtra(FolderPickerActivity.EXTRA_FOLDER, currentFile)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.MOVE_OR_COPY)
        }
        activity.startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_OR_COPY_FILES)
    }

    private fun syncFilesIfSpaceAvailable(files: Collection<OCFile>) {
        if (files.isEmpty()) return

        (containerActivity as? FileActivity)?.showSyncLoadingDialog(files.any { it.isFolder })

        val lastIndex = files.size - 1
        files.forEachIndexed { index, file ->
            if (FileStorageUtils.checkIfEnoughSpace(file)) {
                containerActivity?.fileOperationsHelper?.syncFileOrFolder(file, index == lastIndex, false)
            } else {
                Log_OC.w(TAG, "Not enough space to sync ${file.remotePath}")
                showNotEnoughSpaceDialog(file, FileOperationsHelper.getAvailableSpaceOnDevice())
            }
        }
    }

    private fun showNotEnoughSpaceDialog(file: OCFile, availableSpaceOnDevice: Long) {
        parentFragmentManager.setFragmentResultListener(
            SyncFileNotEnoughSpaceDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            val resultFile = result.getParcelableArgument(
                SyncFileNotEnoughSpaceDialogFragment.RESULT_FILE,
                OCFile::class.java
            )
            val action = result.getString(SyncFileNotEnoughSpaceDialogFragment.RESULT_ACTION)
            if (action == SyncFileNotEnoughSpaceDialogFragment.ACTION_CHOOSE && resultFile != null) {
                onItemClicked(resultFile)
            }
        }

        SyncFileNotEnoughSpaceDialogFragment.newInstance(file, availableSpaceOnDevice)
            .show(parentFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
    }
    //endregion

    //region Navigation
    fun onBrowseUp(): Int {
        val folder = currentFile
        if (folder == null || folder.isRootDirectory) return 0

        val (levelsUp, parent) = parentFolderFinder.getParent(folder, containerActivity?.storageManager)
        if (parent == null) {
            Log_OC.e(TAG, "Cannot browse up from ${folder.remotePath}: no existing parent folder found")
            return 0
        }

        Log_OC.d(TAG, "Browsed up $levelsUp level(s) from ${folder.remotePath} to ${parent.remotePath}")
        currentFile = parent
        setFileDepth(parent)

        if (parent.isRootDirectory && currentSearchType != SearchType.NO_SEARCH && activeSearchEvent != null) {
            isSearchFragment = true
        }

        reloadAndRestorePosition()
        return levelsUp
    }

    private fun reloadAndRestorePosition() {
        listDirectory(currentFile, MainApp.isOnlyOnDevice())
        onRefresh(false)
        restoreIndexAndTopPosition()
    }

    fun browseToFolder(folder: OCFile?, position: Int) {
        setFileDepth(folder)

        if (currentSearchType == SearchType.FAVORITE_SEARCH) {
            resetSortAndGridButtons()
        }

        listDirectory(folder, MainApp.isOnlyOnDevice())
        containerActivity?.onBrowsedDownTo(folder)
        saveIndexAndTopPosition(position)
    }

    private fun browseToRoot() {
        val root = requireContainerActivity().storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
        browseToFolder(root, FIRST_POSITION)
    }

    fun setFileDepth(file: OCFile?) {
        currentFileDepth = file.getDepth()
    }

    fun resetFileDepth() {
        currentFileDepth = OCFileDepth.Root
    }

    val fileDepth: OCFileDepth?
        get() = currentFileDepth
    //endregion

    //region Directory listing
    fun listDirectory(onlyOnDevice: Boolean) {
        listDirectory(null, onlyOnDevice)
    }

    fun listDirectory(directory: OCFile?, onlyOnDevice: Boolean) {
        listDirectory(directory, null, onlyOnDevice)
    }

    fun listDirectory(directory: OCFile?, highlightedFile: OCFile?, onlyOnDevice: Boolean) {
        if (isSearchFragment) {
            activeSearchEvent?.let {
                startSearch(it)
                mRefreshListLayout?.isRefreshing = false
            }
            return
        }

        val storageManager = containerActivity?.storageManager
        if (storageManager == null) {
            Log_OC.w(TAG, "Cannot list directory: storage manager is not available")
            return
        }

        val directoryToList = resolveDirectoryToList(directory, storageManager)
        if (directoryToList == null) {
            Log_OC.w(TAG, "Cannot list directory: folder is not in the database yet, waiting for sync")
            return
        }

        val mimeType = limitToMimeType
        if (mimeType == null) {
            Log_OC.w(TAG, "Cannot list directory: list is not set up yet")
            return
        }

        val adapter = adapter
        if (adapter == null) {
            Log_OC.w(TAG, "Cannot list directory: adapter is not set up yet")
            return
        }

        Log_OC.i(TAG, "Listing directory ${directoryToList.decryptedRemotePath}, onlyOnDevice=$onlyOnDevice")
        adapter.swapDirectory(accountManager.user, directoryToList, storageManager, onlyOnDevice, mimeType)

        val previousDirectory = currentFile
        currentFile = directoryToList
        updateUiForCurrentFolder()
        scrollToHighlightedFileOrTop(adapter, highlightedFile, previousDirectory, directoryToList)
    }

    private fun scrollToHighlightedFileOrTop(
        adapter: OCFileListAdapter,
        highlightedFile: OCFile?,
        previousDirectory: OCFile?,
        listedDirectory: OCFile
    ) {
        val recyclerView = recyclerView

        if (highlightedFile != null) {
            adapter.setHighlightedItem(highlightedFile)
            val position = adapter.getItemPosition(highlightedFile)
            if (position != RecyclerView.NO_POSITION && recyclerView != null) {
                recyclerView.scrollToPosition(position)
            }
            return
        }

        if (recyclerView != null && previousDirectory != listedDirectory) {
            recyclerView.scrollToPosition(FIRST_POSITION)
        }
    }

    private fun resolveDirectoryToList(directory: OCFile?, storageManager: FileDataStorageManager): OCFile? {
        val candidate = directory ?: currentFile ?: storageManager.getFileByPath(OCFile.ROOT_PATH) ?: return null
        if (candidate.isFolder) return candidate

        Log_OC.d(TAG, "${candidate.remotePath} is not a folder, listing its parent instead")
        return storageManager.getFileById(candidate.parentId)
    }

    fun refreshDirectory() {
        isSearchFragment = false
        val folder = currentFile ?: return
        updateFabState()
        listDirectory(folder, MainApp.isOnlyOnDevice())
    }

    val adapterFiles: List<OCFile>
        get() = adapter?.files.orEmpty()

    fun updateOCFile(file: OCFile) {
        adapter?.replaceFile(file)
    }

    val isEmpty: Boolean
        get() = adapter?.isEmpty ?: true

    private fun updateUiForCurrentFolder() {
        fileListLayoutManager?.setLayoutViewMode()
        updateSortButtonLabel()
        setLayoutSwitchButton()
        updateFabState()
        setBottomNavigationVisible(!hideFab)

        activeActionMode?.invalidate()
    }

    private fun updateSortButtonLabel() {
        val sortButton = mSortButton ?: return
        val sortOrder = if (currentSearchType == SearchType.FAVORITE_SEARCH) {
            preferences.getSortOrderByType(FileSortOrder.Type.favoritesListView, FileSortOrder.SORT_A_TO_Z)
        } else {
            preferences.getSortOrderByFolder(currentFile)
        }
        sortButton.setText(SortOrderUi.labelRes(sortOrder))
    }
    //endregion

    //region Menu
    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        if (menuItemAddRemoveValue == MenuItemAddRemove.REMOVE_GRID_AND_SORT) {
            menu.removeItem(R.id.action_search)
        }

        if (currentSearchType == SearchType.FAVORITE_SEARCH) {
            resetSortAndGridButtons()
        } else {
            updateSortAndGridButtons()
        }
    }

    private fun updateSortAndGridButtons() {
        val switchGridViewButton = mSwitchGridViewButton ?: return
        val sortButton = mSortButton ?: return

        when (menuItemAddRemoveValue) {
            MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH -> {
                switchGridViewButton.visibility = View.VISIBLE
                sortButton.visibility = View.VISIBLE
            }

            MenuItemAddRemove.REMOVE_SORT -> sortButton.visibility = View.GONE

            MenuItemAddRemove.REMOVE_GRID_AND_SORT -> {
                sortButton.visibility = View.GONE
                switchGridViewButton.visibility = View.GONE
            }

            MenuItemAddRemove.DO_NOTHING -> Unit
        }
    }

    private fun resetSortAndGridButtons() {
        menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH
        updateSortAndGridButtons()
    }

    val menuItemId: Int
        get() = when {
            javaClass == GalleryFragment::class.java -> R.id.nav_gallery

            javaClass == SharedListFragment::class.java ||
                isSearchEventShared ||
                currentSearchType == SearchType.SHARED_FILTER -> R.id.nav_shared

            javaClass == GroupfolderListFragment::class.java ||
                currentSearchType == SearchType.GROUPFOLDER -> R.id.nav_groupfolders

            isSearchEventFavorite || currentSearchType == SearchType.FAVORITE_SEARCH -> R.id.nav_favorites

            currentSearchType == SearchType.RECENT_FILES_SEARCH -> R.id.nav_recent_files

            else -> R.id.nav_all_files
        }
    //endregion

    //region Search
    protected fun applySearchEvent(event: SearchEvent?) {
        if (event == null || !event.hasQueryOrFilter()) return
        updateSearchType(event)
        updateMenuForSearch(event)
    }

    fun updateSearchType(event: SearchEvent) {
        event.toSearchType()?.let { currentSearchType = it }
    }

    private fun updateMenuForSearch(event: SearchEvent) {
        if (event.searchType == RemoteSearchType.FAVORITE_SEARCH ||
            event.searchType == RemoteSearchType.RECENTLY_MODIFIED_SEARCH
        ) {
            menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SORT
        }

        if (currentSearchType != SearchType.FILE_SEARCH) {
            activity?.invalidateOptionsMenu()
        }
    }

    private fun showEmptyStateForSearch(event: SearchEvent?) {
        val emptyListType = when (event?.searchType) {
            RemoteSearchType.FILE_SEARCH -> SearchType.FILE_SEARCH
            RemoteSearchType.FAVORITE_SEARCH -> SearchType.FAVORITE_SEARCH
            RemoteSearchType.RECENTLY_MODIFIED_SEARCH -> SearchType.RECENT_FILES_SEARCH
            RemoteSearchType.SHARED_FILTER -> SearchType.SHARED_FILTER
            else -> SearchType.NO_SEARCH
        }
        setEmptyListMessage(emptyListType)
    }

    fun resetSearchAttributes() {
        isSearchFragment = false
        searchEvent = null
        currentSearchType = SearchType.NO_SEARCH
    }

    private fun startSearch(event: SearchEvent) {
        if (event.searchType == RemoteSearchType.PHOTO_SEARCH) return

        if (searchTask?.isFinished() == false) {
            Log_OC.d(TAG, "Skipping ${event.searchType} search, previous search is still running")
            return
        }

        Log_OC.i(TAG, "Starting ${event.searchType} search")

        if (searchTask == null) {
            activity?.runOnUiThread {
                adapter?.removeAllFiles()
                setEmptyListMessage(EmptyListState.LOADING)
            }
        }

        applySearchEvent(event)
        isSearchFragment = true
        setFabState(FabState.HIDDEN)

        mainHandler.post {
            updateSortButtonLabel()
            fileListLayoutManager?.setLayoutViewMode()
        }

        launchSearchTask(event)
    }

    private fun launchSearchTask(event: SearchEvent) {
        val currentUser = accountManager.user
        val remoteOperation = if (currentSearchType == SearchType.RECENT_FILES_SEARCH) {
            createRecentFilesSearchOperation()
        } else {
            getSearchRemoteOperation(currentUser, event)
        }

        val storageManager = requireContainerActivity().storageManager
            ?: FileDataStorageManager(currentUser, requireContext().contentResolver)

        @Suppress("UNCHECKED_CAST")
        searchTask = OCFileListSearchTask(
            this,
            remoteOperation as RemoteOperation<List<Any>>,
            currentUser,
            event,
            SharedListFragment.TASK_TIMEOUT.toLong(),
            preferences,
            storageManager
        ).also { it.execute() }
    }

    protected open fun getSearchRemoteOperation(currentUser: User, event: SearchEvent): RemoteOperation<*> {
        val searchOnlyFolders = arguments?.getBoolean(ARG_SEARCH_ONLY_FOLDER, false) == true
        val capability = requireContainerActivity().storageManager.getCapability(currentUser.accountName)
        return SearchRemoteOperation(event.searchQuery, event.searchType, searchOnlyFolders, capability)
    }

    private fun createRecentFilesSearchOperation(): RemoteOperation<*> {
        val capability = capabilities
        val nowSeconds = System.currentTimeMillis() / AppConstants.MILLIS_PER_SECOND

        return SearchRemoteOperation("", RemoteSearchType.RECENTLY_MODIFIED_SEARCH, false, capability).apply {
            startDate = nowSeconds - TimeUnit.DAYS.toSeconds(RECENT_FILES_MAX_AGE_DAYS)
            endDate = nowSeconds
            limit = RECENT_FILES_SEARCH_LIMIT
        }
    }

    override fun onRefresh() {
        val event = activeSearchEvent
        if (isSearchFragment && event != null) {
            startSearch(event)
            mRefreshListLayout?.isRefreshing = false
            return
        }

        isSearchFragment = false
        super.onRefresh()
    }

    fun restartSearch() {
        searchTask?.cancel()
        searchTask = null
        searchEvent?.let { startSearch(it) }
    }

    val isSearchEventFavorite: Boolean
        get() = searchEvent?.searchType == RemoteSearchType.FAVORITE_SEARCH

    val isSearchEventShared: Boolean
        get() = searchEvent?.searchType == RemoteSearchType.SHARED_FILTER

    fun shouldNavigateBackToAllFiles(): Boolean =
        this is GalleryFragment || isSearchEventFavorite || isSearchEventShared

    private fun SearchEvent.hasQueryOrFilter(): Boolean =
        searchQuery.isNotEmpty() || searchType in SEARCH_TYPES_WITHOUT_QUERY
    //endregion

    //region Event bus
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChangeMenuEvent(event: ChangeMenuEvent) {
        Log_OC.d(TAG, "Leaving search, resetting search and menu state")

        arguments?.clear()
        resetSearchAttributes()
        resetSortAndGridButtons()

        (activity as? FileDisplayActivity)?.run {
            invalidateOptionsMenu()
            intent.removeExtra(SEARCH_EVENT)
            setupHomeSearchToolbarWithSortAndListButtons()
            updateActionBarTitleAndHomeButton(null)
        }

        updateFabState()
        setBottomNavigationVisible(true)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onCommentsEvent(event: CommentsEvent) {
        adapter?.markCommentsAsRead(event.remoteId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFavoriteEvent(event: FavoriteEvent) {
        lifecycleScope.launch {
            val client = clientRepository.getOwncloudClient()
            if (client == null) {
                Log_OC.e(TAG, "Cannot toggle favorite for ${event.remotePath}: client is not available")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                ToggleFavoriteRemoteOperation(event.shouldFavorite, event.remotePath).execute(client)
            }
            if (!result.isSuccess) {
                Log_OC.e(TAG, "Cannot toggle favorite for ${event.remotePath}: ${result.code}")
                return@launch
            }

            showFavoriteChange(event)
        }
    }

    private fun showFavoriteChange(event: FavoriteEvent) {
        val removeFromList = currentSearchType == SearchType.FAVORITE_SEARCH && !event.shouldFavorite
        setEmptyListMessage(SearchType.FAVORITE_SEARCH)
        if (this is GalleryFragment) {
            markAsFavorite(event.remotePath, event.shouldFavorite)
        } else {
            adapter?.updateFavoriteState(event.remotePath, event.shouldFavorite, removeFromList)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSearchEvent(event: SearchEvent) {
        startSearch(event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEncryptionEvent(event: EncryptionEvent) {
        event.onResult(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFileLockEvent(event: FileLockEvent) {
        lifecycleScope.launch {
            val client = clientRepository.getNextcloudClient()
            if (client == null) {
                Log_OC.e(TAG, "Cannot toggle lock for ${event.filePath}: client is not available")
                showFileLockError()
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                ToggleFileLockRemoteOperation(event.shouldLock, event.filePath).execute(client)
            }
            if (!result.isSuccess) {
                Log_OC.e(TAG, "Cannot toggle lock for ${event.filePath}: ${result.code}")
                showFileLockError()
                return@launch
            }

            onRefresh()
        }
    }

    private fun showFileLockError() {
        val recyclerView = recyclerView ?: return
        Snackbar.make(recyclerView, R.string.error_file_lock, Snackbar.LENGTH_LONG).show()
    }
    //endregion

    companion object {
        val TAG: String = OCFileListFragment::class.java.simpleName

        private val MY_PACKAGE = OCFileListFragment::class.java.`package`?.name ?: "com.owncloud.android.ui.fragment"

        val ARG_ONLY_FOLDERS_CLICKABLE = "$MY_PACKAGE.ONLY_FOLDERS_CLICKABLE"
        val ARG_FILE_SELECTABLE = "$MY_PACKAGE.FILE_SELECTABLE"
        val ARG_ALLOW_CONTEXTUAL_ACTIONS = "$MY_PACKAGE.ALLOW_CONTEXTUAL"
        val ARG_HIDE_FAB = "$MY_PACKAGE.HIDE_FAB"
        val ARG_HIDE_ITEM_OPTIONS = "$MY_PACKAGE.HIDE_ITEM_OPTIONS"
        val ARG_SEARCH_ONLY_FOLDER = "$MY_PACKAGE.SEARCH_ONLY_FOLDER"
        val ARG_MIMETYPE = "$MY_PACKAGE.MIMETYPE"

        const val DOWNLOAD_SEND = "DOWNLOAD_SEND"

        const val FOLDER_LAYOUT_LIST = "LIST"
        const val FOLDER_LAYOUT_GRID = "GRID"

        const val SEARCH_EVENT = "SEARCH_EVENT"
        const val KEY_CURRENT_SEARCH_TYPE = "CURRENT_SEARCH_TYPE"
        private val KEY_FILE = "$MY_PACKAGE.extra.FILE"

        private const val DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER"
        private const val DIALOG_CREATE_DOCUMENT = "DIALOG_CREATE_DOCUMENT"
        private const val ACTIONS_BOTTOM_SHEET_TAG = "actions"
        private const val OVERFLOW_CLICK_THROTTLE_KEY = "overflowClick"

        private const val ALL_MIME_TYPES = "*/*"
        private const val SCAN_TO_PDF_ACTION = "org.fairscan.app.action.SCAN_TO_PDF"

        private const val ACTIVITY_TAB = 0
        private const val SHARING_TAB = 1
        private const val FIRST_POSITION = 0
        private const val SINGLE_SELECTION = 1

        private const val RECENT_FILES_MAX_AGE_DAYS = 14L
        private const val RECENT_FILES_SEARCH_LIMIT = 100

        private val SELECTION_ENDING_ACTIONS = setOf(
            R.id.action_export_file,
            R.id.action_favorite,
            R.id.action_unset_favorite,
            R.id.action_add_to_album
        )

        private val SEARCH_TYPES_WITHOUT_QUERY = setOf(
            RemoteSearchType.SHARED_FILTER,
            RemoteSearchType.FAVORITE_SEARCH,
            RemoteSearchType.RECENTLY_MODIFIED_SEARCH
        )

        private val scanIntentExternalApp = Intent(SCAN_TO_PDF_ACTION)

        private var currentFileDepth: OCFileDepth? = OCFileDepth.Root

        var isMultipleFileSelectedForCopyOrMove = false
    }
}
