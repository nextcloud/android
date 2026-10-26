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
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.annotation.IdRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.systembars.SystemBarBackgroundCallbacks
import com.nextcloud.client.utils.Throttler
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
import com.owncloud.android.ui.activity.AlbumsPickerActivity
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull
import com.owncloud.android.lib.resources.files.SearchRemoteOperation.SearchType as RemoteSearchType

/**
 * A Fragment that lists all files and folders in a given path.
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
@Suppress("TooManyFunctions", "LargeClass")
open class OCFileListFragment :
    ExtendedListFragment(),
    OCFileListFragmentInterface,
    OCFileListBottomSheetActions,
    Injectable {

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var throttler: Throttler

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

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

    /**
     * Use this to query the [OCFile] that is currently being displayed by this fragment
     */
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
    private var activeActionMode: ActionMode? = null
    private var isActionModeNew = false
    private var searchTask: OCFileListSearchTask? = null
    private var fabMain: FloatingActionButton? = null
    private val multiChoiceModeListener = MultiChoiceModeListener()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val activeSearchEvent: SearchEvent?
        get() = searchEvent?.takeIf { it.isSet() }

    private val capabilities: OCCapability
        get() = requireContainerActivity().storageManager.getCapability(accountManager.user.accountName)

    protected enum class MenuItemAddRemove {
        DO_NOTHING,
        REMOVE_SORT,
        REMOVE_GRID_AND_SORT,
        ADD_GRID_AND_SORT_WITH_SEARCH
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log_OC.i(TAG, "onAttach")
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
        setSearchArgs(state)
        currentFile = state.getParcelableArgument(KEY_FILE, OCFile::class.java)
        isSearchFragment = activeSearchEvent != null
        folderEncryption = FolderEncryption(this)
        encryptedClickHandler = EncryptedFolderClickHandler(this)
        e2eeDialogPresenter = E2EEDialogPresenter(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log_OC.i(TAG, "onCreateView() start")
        val view = super.onCreateView(inflater, container, savedInstanceState)

        val state = savedInstanceState ?: arguments
        setSearchArgs(state)

        if (state?.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, false) == true) {
            setChoiceModeAsMultipleModal(state)
        }

        fabMain = requireActivity().findViewById<FloatingActionButton?>(R.id.fab_main)?.also {
            viewThemeUtils.material.themeFAB(it)
        }

        Log_OC.i(TAG, "onCreateView() end")
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
        Log_OC.i(TAG, "onActivityCreated() start")
        prepareOCFileList(savedInstanceState)
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
        // Don't handle search events if we're coming back from back stack
        // The fragment has already been properly restored in onCreate/onActivityCreated
        if (currentFile != null) {
            super.onResume()
            return
        }

        val activity = activity ?: return
        activity.intent.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)?.let { searchEvent = it }
        activeSearchEvent?.let { handleSearchEvent(it) }

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
        multiChoiceModeListener.storeStateIn(outState)
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

    fun setSearchArgs(state: Bundle?) {
        currentSearchType = state?.getParcelableArgument(KEY_CURRENT_SEARCH_TYPE, SearchType::class.java)
            ?: SearchType.NO_SEARCH
        state?.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)?.let { searchEvent = it }

        if (searchEvent != null && currentSearchType != SearchType.NO_SEARCH) {
            isSearchFragment = true
        }
    }

    fun prepareOCFileList(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            currentFile = savedInstanceState.getParcelableArgument(KEY_FILE, OCFile::class.java)
        }

        val args = arguments
        onlyFoldersClickable = args?.getBoolean(ARG_ONLY_FOLDERS_CLICKABLE, false) == true
        fileSelectable = args?.getBoolean(ARG_FILE_SELECTABLE, false) == true
        limitToMimeType = args?.getString(ARG_MIMETYPE, "") ?: ""

        setAdapter(args)

        hideFab = args?.getBoolean(ARG_HIDE_FAB, false) == true
        if (hideFab) {
            setFabVisible(false)
        } else {
            setFabVisible(currentFile?.let { canCreateInside(it) } ?: true)
            registerFabListener()
        }

        // do not touch search event if previously searched
        if (!isSearchFragment) {
            searchEvent = arguments.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)
        }
        prepareCurrentSearch(searchEvent)
        setEmptyView(searchEvent)

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

    protected fun setChoiceModeAsMultipleModal(savedInstanceState: Bundle?) {
        savedInstanceState?.let { multiChoiceModeListener.loadStateFrom(it) }
        (requireActivity() as? FileActivity)?.addDrawerListener(multiChoiceModeListener)
    }

    fun registerFabListener() {
        val fileActivity = activity as? FileActivity
        if (fileActivity == null) {
            Log_OC.w(TAG, "activity is null cannot register fab listener")
            return
        }

        val fab = fabMain
        if (fab == null) {
            Log_OC.w(TAG, "mFabMain is null cannot register fab listener")
            return
        }

        viewThemeUtils.material.themeFAB(fab)
        fab.setOnClickListener { showCreateBottomSheet(fileActivity) }
    }

    private fun showCreateBottomSheet(fileActivity: FileActivity) {
        val currentDir = currentFile
        if (currentDir == null) {
            Log_OC.w(TAG, "currentDir is null cannot open bottom sheet dialog")
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

    fun setFabVisible(visible: Boolean) {
        // is not available in FolderPickerActivity
        val fab = fabMain ?: return

        val fileDisplayActivity = activity as? FileDisplayActivity
        if (fileDisplayActivity != null &&
            (fileDisplayActivity.isAlbumsFragment || fileDisplayActivity.isAlbumItemsFragment)
        ) {
            fab.hide()
            return
        }

        val activity = activity ?: return
        activity.runOnUiThread {
            if (visible) {
                fab.show()
                viewThemeUtils.material.themeFAB(fab)
            } else {
                fab.hide()
            }
            fab.slideHideBottomBehavior(visible)
        }
    }

    fun setFabEnabled(enabled: Boolean) {
        val fab = fabMain ?: return
        val activity = activity ?: return
        activity.runOnUiThread {
            fab.isEnabled = enabled
            viewThemeUtils.material.themeFAB(fab)
        }
    }

    fun slideHideBottomBehaviourForBottomNavigationView(visible: Boolean) {
        val drawerActivity = activity as? DrawerActivity ?: return
        drawerActivity.bottomNavigationView.slideHideBottomBehavior(visible)
    }

    private fun isReadOnlyFolder(folder: OCFile?): Boolean {
        folder ?: return false
        val storageManager = containerActivity?.storageManager ?: return false
        return storageManager.isReadOnly(folder)
    }

    private fun canCreateInside(folder: OCFile): Boolean = folder.canCreateFileAndFolder() && !isReadOnlyFolder(folder)

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
            Log_OC.e(TAG, "activity is null, cannot create a folder")
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
            SnackbarUtil.show(view, getString(R.string.error_starting_direct_camera_upload))
            return
        }

        if (!PermissionUtil.checkSelfPermission(fileDisplayActivity, Manifest.permission.CAMERA)) {
            PermissionUtil.requestCameraPermission(fileDisplayActivity, PermissionUtil.PERMISSIONS_CAMERA)
            return
        }

        showDirectCameraUploadAlertDialog(fileDisplayActivity)
    }

    private fun showDirectCameraUploadAlertDialog(fileDisplayActivity: FileDisplayActivity) {
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
            Log.w(
                TAG,
                "scanDocUpload: Failed to start doc scanning, fileDisplayActivity=$fileDisplayActivity, " +
                    "currentFile=$currentFile"
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
            Log_OC.w(TAG, "Activity is null, cant upload files")
            return
        }

        val user = fileActivity.user.getOrNull()
        if (user == null) {
            Log_OC.w(TAG, "User not exist, cant upload files")
            return
        }

        val file = currentFile
        if (file == null) {
            Log_OC.w(TAG, "File is null cannot determine isWithinEncryptedFolder, cant upload files")
            return
        }

        UploadFilesActivity.startUploadActivityForResult(
            fileActivity,
            user,
            FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM,
            file.isEncrypted
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
            onError = { SnackbarUtil.show(view, R.string.failed_to_start_editor) }
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
        val file = currentFile ?: return
        if (file.richWorkspace.isNullOrEmpty()) return

        val adapter = adapter
        if (adapter == null || adapter.isMultiSelect()) return

        val fileDisplayActivity = containerActivity as? FileDisplayActivity ?: return
        fileDisplayActivity.startRichWorkspacePreview(file)
    }

    override fun onLongItemClicked(file: OCFile): Boolean {
        val activity = activity ?: return true

        if (activeActionMode != null) {
            toggleItemToCheckedList(file)
        } else {
            activity.startActionMode(multiChoiceModeListener)
            commonAdapter?.addCheckedFile(file)
        }
        updateActionModeFile(file)

        return true
    }

    override fun onItemClicked(file: OCFile) {
        onItemClicked(file, null)
    }

    @OptIn(markerClass = [UnstableApi::class])
    override fun onItemClicked(file: OCFile?, sourceView: View?) {
        if (file == null) {
            Log_OC.d(TAG, "Null object in ListAdapter!")
            return
        }

        val commonAdapter = commonAdapter
        when {
            commonAdapter?.isMultiSelect() == true -> toggleItemToCheckedList(file)
            commonAdapter != null && file.isFolder -> folderOnItemClick(file, commonAdapter.getItemPosition(file))
            fileSelectable -> finishWithSelectedFile(file)
            !onlyFoldersClickable -> fileOnItemClick(file, sourceView)
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

    private fun folderOnItemClick(file: OCFile, position: Int) {
        val folderPickerActivity = requireActivity() as? FolderPickerActivity
        if (folderPickerActivity != null) {
            val filenameErrorMessage = FileNameValidator.checkFileName(file.fileName, capabilities, requireContext())
            if (filenameErrorMessage != null) {
                SnackbarUtil.show(folderPickerActivity, filenameErrorMessage)
                return
            }
        }

        if (file.isEncrypted) {
            encryptedClickHandler.onEncryptedFolderClick(file, position)
            return
        }

        isSearchFragment = false
        setEmptyListMessage(EmptyListState.LOADING)
        browseToFolder(file, position)
    }

    private fun fileOnItemClick(file: OCFile, sourceView: View?) {
        val errorMessageId = getOpenRestrictionMessage(file)
        val recyclerView = recyclerView
        if (recyclerView != null && errorMessageId != null) {
            Snackbar.make(recyclerView, errorMessageId, Snackbar.LENGTH_LONG).show()
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
                        override fun onComplete(value: Boolean) = setFabVisible(value)
                    }
                )

            else -> handlePendingDownloadFile(file)
        }
    }

    @StringRes
    private fun getOpenRestrictionMessage(file: OCFile): Int? = when {
        file.isAPKorAAB -> R.string.gplay_restriction
        file.isOfflineOperation -> R.string.offline_operations_file_does_not_exists_yet
        else -> null
    }

    private fun handlePendingDownloadFile(file: OCFile) {
        if (!isAccountManagerInitialized()) {
            Log_OC.e(TAG, "AccountManager not yet initialized")
            return
        }

        val context = context
        if (context == null) {
            Log_OC.e(TAG, "context not available")
            return
        }

        val webViewAvailable = context.isWebViewAvailable()
        val fileDisplayActivity = containerActivity as? FileDisplayActivity

        when {
            !file.isEncrypted && fileDisplayActivity != null && fileDisplayActivity.canPreviewInAudioPlayer(file) -> {
                setFabVisible(false)
                fileDisplayActivity.startAudioPreview(file, true, true)
            }

            webViewAvailable && hasTextEditor(file) && !file.isEncrypted ->
                TextEditorWebView.startTextEditor(file, context)

            supportsDirectEditing(file, webViewAvailable) ->
                containerActivity?.fileOperationsHelper?.openFileAsRichDocument(file, context)

            fileDisplayActivity != null -> {
                fileDisplayActivity.startDownloadForPreview(file, currentFile)

                // Checks if the file is small enough to be previewed immediately without showing progress.
                // If the file is smaller than or equal to 1MB, it can be displayed directly.
                if (file.isFileEligibleForImmediatePreview) {
                    fileDisplayActivity.setFileIDForImmediatePreview(file.fileId)
                }
            }
        }
    }

    private fun hasTextEditor(file: OCFile): Boolean = editorUtils.getEditor(accountManager.user, file.mimeType) != null

    private fun supportsDirectEditing(file: OCFile, webViewAvailable: Boolean): Boolean {
        if (!webViewAvailable) return false

        val user = accountManager.user
        if (user.isAnonymous) return false

        val capability = containerActivity?.storageManager?.getCapability(user.accountName) ?: return false
        val mimeTypes = capability.richDocumentsMimeTypeList ?: return false

        return file.mimeType in mimeTypes &&
            capability.richDocumentsDirectEditing.isTrue &&
            !file.isEncrypted
    }

    private fun toggleItemToCheckedList(file: OCFile) {
        commonAdapter?.run {
            if (isCheckedFile(file)) removeCheckedFile(file) else addCheckedFile(file)
        }
        updateActionModeFile(file)
    }

    private fun updateActionModeFile(file: OCFile) {
        isActionModeNew = false
        val actionMode = activeActionMode ?: return
        actionMode.invalidate()
        commonAdapter?.notifyItemChanged(file)
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

    private fun invalidateActionMode() {
        activeActionMode?.invalidate()
    }

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

    /**
     * Start the appropriate action(s) on the currently selected files given menu selected by the user.
     *
     * @param checkedFiles List of files selected by the user on which the action should be performed
     * @return 'true' if the menu selection started any action, 'false' otherwise.
     */
    open fun onFileActionChosen(@IdRes itemId: Int, checkedFiles: Set<OCFile>): Boolean {
        if (checkedFiles.isEmpty()) return false
        if (onListActionChosen(itemId, checkedFiles)) return true

        val containerActivity = containerActivity ?: return false
        val host = FileActionHost(containerActivity, parentFragmentManager, view, currentFile, activeActionMode)
        val isHandled = fileActionHandler.handle(itemId, checkedFiles, host)
        if (isHandled && itemId in SELECTION_ENDING_ACTIONS) {
            exitSelectionMode()
        }

        return isHandled
    }

    private fun onListActionChosen(@IdRes itemId: Int, checkedFiles: Set<OCFile>): Boolean {
        when {
            itemId == R.id.action_see_details && checkedFiles.size == SINGLE_SELECTION ->
                showDetailsAndHideSortGroup(checkedFiles.first())

            itemId == R.id.action_download_file || itemId == R.id.action_sync_file -> {
                syncAndCheckFiles(checkedFiles)
                exitSelectionMode()
            }

            itemId == R.id.action_move_or_copy -> moveOrCopy(checkedFiles)

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

    private fun moveOrCopy(checkedFiles: Set<OCFile>) {
        val invalidFilename = findInvalidFilename(checkedFiles)
        if (invalidFilename != null) {
            SnackbarUtil.show(
                requireActivity(),
                getString(R.string.file_name_validator_rename_before_move_or_copy, invalidFilename)
            )
            return
        }

        if (!FileNameValidator.checkParentRemotePaths(checkedFiles.toList(), capabilities, requireContext())) {
            browseToRoot()
            SnackbarUtil.show(requireActivity(), R.string.file_name_validator_current_path_is_invalid)
            return
        }

        pickFolderForMoveOrCopy(checkedFiles)
    }

    private fun findInvalidFilename(files: Set<OCFile>): String? = files.firstNotNullOfOrNull {
        FileNameValidator.checkFileName(it.fileName, capabilities, requireContext())
    }

    @Suppress("DEPRECATION")
    private fun pickFolderForMoveOrCopy(checkedFiles: Set<OCFile>) {
        val activity = requireActivity()
        val paths = checkedFiles.mapTo(ArrayList(checkedFiles.size)) { it.remotePath }
        val action = Intent(activity, FolderPickerActivity::class.java).apply {
            putStringArrayListExtra(FolderPickerActivity.EXTRA_FILE_PATHS, paths)
            putExtra(FolderPickerActivity.EXTRA_FOLDER, currentFile)
            // No animation since we stay in the same folder
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(FolderPickerActivity.EXTRA_ACTION, FolderPickerActivity.MOVE_OR_COPY)
        }
        activity.startActivityForResult(action, FileDisplayActivity.REQUEST_CODE__MOVE_OR_COPY_FILES)
    }

    private fun syncAndCheckFiles(files: Collection<OCFile>) {
        if (files.isEmpty()) return

        (containerActivity as? FileActivity)?.showSyncLoadingDialog(files.any { it.isFolder })

        val lastIndex = files.size - 1
        files.forEachIndexed { index, file ->
            if (FileStorageUtils.checkIfEnoughSpace(file)) {
                containerActivity?.fileOperationsHelper?.syncFileOrFolder(file, index == lastIndex, false)
            } else {
                showSpaceErrorDialog(file, FileOperationsHelper.getAvailableSpaceOnDevice())
            }
        }
    }

    private fun showSpaceErrorDialog(file: OCFile, availableSpaceOnDevice: Long) {
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

    /**
     * Call this, when the user presses the up button.
     *
     * Tries to move up the current folder one level. If the parent folder was removed from the database, it continues
     * browsing up until finding an existing folders.
     *
     * @return Count of folder levels browsed up.
     */
    fun onBrowseUp(): Int {
        val file = currentFile
        if (file == null || file.isRootDirectory) return 0

        val (levelsUp, target) = parentFolderFinder.getParent(file, containerActivity?.storageManager)
        if (target == null) {
            Log_OC.e(TAG, "onBrowseUp: could not resolve parent, staying put")
            return 0
        }

        currentFile = target
        setFileDepth(target)

        if (target.isRootDirectory && currentSearchType != SearchType.NO_SEARCH && activeSearchEvent != null) {
            isSearchFragment = true
        }

        updateFileList()
        return levelsUp
    }

    private fun updateFileList() {
        listDirectory(currentFile, MainApp.isOnlyOnDevice())
        onRefresh(false)
        restoreIndexAndTopPosition()
    }

    fun browseToFolder(file: OCFile?, position: Int) {
        setFileDepth(file)

        if (currentSearchType == SearchType.FAVORITE_SEARCH) {
            resetMenuItems()
        }

        listDirectory(file, MainApp.isOnlyOnDevice())
        containerActivity?.onBrowsedDownTo(file)
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

    /**
     * Calls [OCFileListFragment.listDirectory] with a null parameter
     */
    fun listDirectory(onlyOnDevice: Boolean) {
        listDirectory(null, onlyOnDevice)
    }

    fun listDirectory(directory: OCFile?, onlyOnDevice: Boolean) {
        listDirectory(directory, null, onlyOnDevice)
    }

    /**
     * Lists the given directory on the view. When the input parameter is null, it will either refresh the last known
     * directory. list the root if there never was a directory.
     *
     * @param directory File to be listed
     */
    fun listDirectory(directory: OCFile?, file: OCFile?, onlyOnDevice: Boolean) {
        if (isSearchFragment) {
            activeSearchEvent?.let {
                handleSearchEvent(it)
                mRefreshListLayout?.isRefreshing = false
            }
            return
        }

        val storageManager = containerActivity?.storageManager
        if (storageManager == null) {
            Log_OC.d(TAG, "fileDataStorageManager is null")
            return
        }

        val directoryToList = resolveDirectoryToList(directory, storageManager)
        if (directoryToList == null) {
            Log_OC.e(TAG, "directory is null, no files, wait for sync")
            return
        }

        val mimeType = limitToMimeType
        if (mimeType == null) {
            Log_OC.w(TAG, "mLimitToMimeType is null")
            return
        }

        val adapter = adapter
        if (adapter == null) {
            Log_OC.e(TAG, "❗oc file list adapter is null, cannot list directory❗")
            return
        }

        Log_OC.i(TAG, "listing directory: " + directoryToList.decryptedRemotePath)
        adapter.swapDirectory(accountManager.user, directoryToList, storageManager, onlyOnDevice, mimeType)

        val previousDirectory = currentFile
        currentFile = directoryToList
        updateLayout()
        scrollAfterListing(adapter, file, previousDirectory, directoryToList)
    }

    private fun scrollAfterListing(
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

        Log_OC.w(TAG, "You see, that is not a directory -> $candidate")
        return storageManager.getFileById(candidate.parentId)
    }

    fun refreshDirectory() {
        isSearchFragment = false
        val currentFile = currentFile ?: return
        setFabVisible(canCreateInside(currentFile))
        listDirectory(currentFile, MainApp.isOnlyOnDevice())
    }

    val adapterFiles: List<OCFile>
        get() = adapter?.files.orEmpty()

    fun updateOCFile(file: OCFile) {
        adapter?.replaceFile(file)
    }

    val isEmpty: Boolean
        get() = adapter?.isEmpty ?: true

    private fun updateLayout() {
        fileListLayoutManager?.setLayoutViewMode()
        updateSortButton()
        setLayoutSwitchButton()

        val file = currentFile
        val readOnly = isReadOnlyFolder(file)
        setFabVisible(!hideFab && !readOnly)
        slideHideBottomBehaviourForBottomNavigationView(!hideFab)
        setFabEnabled(file != null && !readOnly && (file.canCreateFileAndFolder() || file.isOfflineOperation))

        invalidateActionMode()
    }

    private fun updateSortButton() {
        val sortButton = mSortButton ?: return
        val sortOrder = if (currentSearchType == SearchType.FAVORITE_SEARCH) {
            preferences.getSortOrderByType(FileSortOrder.Type.favoritesListView, FileSortOrder.SORT_A_TO_Z)
        } else {
            preferences.getSortOrderByFolder(currentFile)
        }
        sortButton.setText(SortOrderUi.labelRes(sortOrder))
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareOptionsMenu(menu: Menu) {
        if (menuItemAddRemoveValue == MenuItemAddRemove.REMOVE_GRID_AND_SORT) {
            menu.removeItem(R.id.action_search)
        }

        if (currentSearchType == SearchType.FAVORITE_SEARCH) {
            resetMenuItems()
        } else {
            updateSortAndGridMenuItems()
        }
    }

    private fun updateSortAndGridMenuItems() {
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

            MenuItemAddRemove.DO_NOTHING -> Log_OC.v(TAG, "Kept the options menu default structure")
        }
    }

    private fun resetMenuItems() {
        menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH
        updateSortAndGridMenuItems()
    }

    /**
     * Returns the navigation drawer menu item corresponding to this fragment.
     *
     * OCFileListFragment is the parent for GalleryFragment, SharedListFragment,
     * and GroupfolderListFragment. It also internally handles listing favorites,
     * shared files, or recently modified items via search events. This method
     * checks the current fragment type and search state to give correct drawer menu ID.
     */
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

    protected fun prepareCurrentSearch(event: SearchEvent?) {
        if (event == null || !event.isSet()) return
        setCurrentSearchType(event)
        prepareActionBarItems(event)
    }

    fun setCurrentSearchType(event: SearchEvent) {
        event.toSearchType()?.let { currentSearchType = it }
    }

    protected fun prepareActionBarItems(event: SearchEvent?) {
        val searchType = event?.searchType
        if (searchType == RemoteSearchType.FAVORITE_SEARCH || searchType == RemoteSearchType.RECENTLY_MODIFIED_SEARCH) {
            menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_SORT
        }

        if (currentSearchType != SearchType.FILE_SEARCH) {
            activity?.invalidateOptionsMenu()
        }
    }

    protected fun setEmptyView(event: SearchEvent?) {
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

    protected fun handleSearchEvent(event: SearchEvent) {
        if (event.searchType == RemoteSearchType.PHOTO_SEARCH) return

        if (searchTask?.isFinished() == false) {
            searchEvent?.let {
                Log_OC.d(
                    TAG,
                    "OCFileListSearchTask already running skipping new api call for search event: " +
                        it.searchType
                )
            }
            return
        }

        if (searchTask == null) {
            activity?.runOnUiThread {
                adapter?.removeAllFiles()
                setEmptyListMessage(EmptyListState.LOADING)
            }
        }

        prepareCurrentSearch(event)
        isSearchFragment = true
        setFabVisible(false)

        mainHandler.post {
            updateSortButton()
            fileListLayoutManager?.setLayoutViewMode()
        }

        startSearchTask(event)
    }

    private fun startSearchTask(event: SearchEvent) {
        val currentUser = accountManager.user
        val remoteOperation = if (currentSearchType == SearchType.RECENT_FILES_SEARCH) {
            createRecentFilesSearchRemoteOperation()
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

    private fun createRecentFilesSearchRemoteOperation(): RemoteOperation<*> {
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
            handleSearchEvent(event)
            mRefreshListLayout?.isRefreshing = false
            return
        }

        isSearchFragment = false
        super.onRefresh()
    }

    fun cancelAndRetriggerSearch() {
        searchTask?.cancel()
        searchTask = null
        searchEvent?.let { handleSearchEvent(it) }
    }

    val isSearchEventFavorite: Boolean
        get() = searchEvent?.searchType == RemoteSearchType.FAVORITE_SEARCH

    val isSearchEventShared: Boolean
        get() = searchEvent?.searchType == RemoteSearchType.SHARED_FILTER

    fun shouldNavigateBackToAllFiles(): Boolean =
        this is GalleryFragment || isSearchEventFavorite || isSearchEventShared

    private fun SearchEvent.isSet(): Boolean = searchQuery.isNotEmpty() || searchType in SEARCH_TYPES_WITHOUT_QUERY

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(changeMenuEvent: ChangeMenuEvent) {
        Log_OC.d(TAG, "event bus --- change menu event triggered")

        arguments?.clear()
        resetSearchAttributes()
        resetMenuItems()

        (activity as? FileDisplayActivity)?.run {
            invalidateOptionsMenu()
            intent.removeExtra(SEARCH_EVENT)
            setupHomeSearchToolbarWithSortAndListButtons()
            updateActionBarTitleAndHomeButton(null)
        }

        currentFile?.let { setFabVisible(canCreateInside(it)) }
        slideHideBottomBehaviourForBottomNavigationView(true)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: CommentsEvent) {
        adapter?.markCommentsAsRead(event.remoteId)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: FavoriteEvent) {
        try {
            val client = clientFactory.create(accountManager.user)
            val result = ToggleFavoriteRemoteOperation(event.shouldFavorite, event.remotePath).execute(client)
            if (!result.isSuccess) return

            val removeFromList = currentSearchType == SearchType.FAVORITE_SEARCH && !event.shouldFavorite
            setEmptyListMessage(SearchType.FAVORITE_SEARCH)
            if (this is GalleryFragment) {
                markAsFavorite(event.remotePath, event.shouldFavorite)
            } else {
                adapter?.updateFavoriteState(event.remotePath, event.shouldFavorite, removeFromList)
            }
        } catch (e: ClientFactory.CreationException) {
            Log_OC.e(TAG, "Error processing event", e)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: SearchEvent) {
        handleSearchEvent(event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EncryptionEvent) {
        event.onResult(this)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: FileLockEvent) {
        val user = accountManager.user

        try {
            val client = clientFactory.createNextcloudClient(user)
            val result = ToggleFileLockRemoteOperation(event.shouldLock, event.filePath).execute(client)
            if (result.isSuccess) {
                // TODO only refresh the modified file?
                mainHandler.post { onRefresh() }
                return
            }
        } catch (e: ClientFactory.CreationException) {
            Log_OC.e(TAG, "Cannot create client", e)
        }

        showFileLockError()
    }

    private fun showFileLockError() {
        val recyclerView = recyclerView ?: return
        Snackbar.make(recyclerView, R.string.error_file_lock, Snackbar.LENGTH_LONG).show()
    }

    fun requireContainerActivity(): ContainerActivity =
        checkNotNull(containerActivity) { "Fragment $this is not attached to a ContainerActivity" }

    /**
     * Handler for multiple selection mode.
     *
     * Manages input from the user when one or more files or folders are selected in the list.
     *
     * Also listens to changes in navigation drawer to hide and recover multiple selection when it's opened and closed.
     */
    private inner class MultiChoiceModeListener :
        AbsListView.MultiChoiceModeListener,
        DrawerLayout.DrawerListener {

        private var actionModeClosedByDrawer = false
        private val selectionWhenActionModeClosedByDrawer = HashSet<OCFile>()

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

        override fun onDrawerOpened(drawerView: View) = Unit

        /**
         * When the navigation drawer is closed, action mode is recovered in the same state as was when the drawer was
         * (started to be) opened.
         */
        override fun onDrawerClosed(drawerView: View) {
            if (!actionModeClosedByDrawer || selectionWhenActionModeClosedByDrawer.isEmpty()) return

            activity?.startActionMode(this)
            adapter?.setCheckedItem(selectionWhenActionModeClosedByDrawer)
            activeActionMode?.invalidate()
            selectionWhenActionModeClosedByDrawer.clear()
        }

        /**
         * If the action mode is active when the navigation drawer starts to move, the action mode is closed and the
         * selection stored to be recovered when the drawer is closed.
         */
        override fun onDrawerStateChanged(newState: Int) {
            val actionMode = activeActionMode
            if (newState != DrawerLayout.STATE_DRAGGING || actionMode == null) return

            (recyclerView?.adapter as? OCFileListAdapter)?.let {
                selectionWhenActionModeClosedByDrawer.addAll(it.getCheckedItems())
            }

            actionMode.finish()
            actionModeClosedByDrawer = true
        }

        override fun onItemCheckedStateChanged(mode: ActionMode?, position: Int, id: Long, checked: Boolean) = Unit

        @SuppressFBWarnings("RV")
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
            activeActionMode = mode
            isActionModeNew = true

            // fake menu to be able to use bottom sheet instead
            requireActivity().menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
            val placeholderItem = menu.findItem(R.id.custom_menu_placeholder_item)
            placeholderItem.icon?.let {
                placeholderItem.icon = viewThemeUtils.platform.colorDrawable(
                    it,
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
            }

            mode?.invalidate()
            colorStatusBarForActionMode()
            setFabVisible(false)

            if (this@OCFileListFragment is GalleryFragment && activity is AlbumsPickerActivity) {
                placeholderItem.isVisible = false
                menu.findItem(R.id.add_to_album).isVisible = true
            }

            commonAdapter?.setMultiSelect(true)
            return true
        }

        private fun colorStatusBarForActionMode() {
            val activity = requireActivity()
            val statusBarColor = ContextCompat.getColor(requireContext(), R.color.action_mode_background)
            viewThemeUtils.platform.colorStatusBar(activity, statusBarColor)
            SystemBarBackgroundCallbacks.colorStatusBar(activity, statusBarColor)
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val checkedCount = commonAdapter?.getCheckedItems()?.size ?: 0

            activeActionMode?.title = resources.getQuantityString(
                R.plurals.items_selected_count,
                checkedCount,
                checkedCount
            )

            if (checkedCount == 0 && !isActionModeNew) {
                exitSelectionMode()
            }

            isMultipleFileSelectedForCopyOrMove = checkedCount > 0
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            val commonAdapter = commonAdapter ?: return true
            val checkedFiles = commonAdapter.getCheckedItems()

            when (item.itemId) {
                R.id.custom_menu_placeholder_item ->
                    openActionsMenu(commonAdapter.getFilesCount(), checkedFiles, false)

                R.id.add_to_album ->
                    (this@OCFileListFragment as? GalleryFragment)?.addImagesToAlbum(checkedFiles)
            }

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            activeActionMode = null

            if (!hideFab && !isSearchFragment) {
                currentFile?.let { setFabVisible(it.canCreateFileAndFolder()) }
            }

            activity?.let {
                viewThemeUtils.platform.resetStatusBar(it)
                SystemBarBackgroundCallbacks.apply(it, viewThemeUtils)
            }

            commonAdapter?.run {
                setMultiSelect(false)
                clearCheckedItems()
            }

            isMultipleFileSelectedForCopyOrMove = false
        }

        fun storeStateIn(outState: Bundle) {
            outState.putBoolean(KEY_ACTION_MODE_CLOSED_BY_DRAWER, actionModeClosedByDrawer)
        }

        fun loadStateFrom(savedInstanceState: Bundle) {
            actionModeClosedByDrawer = savedInstanceState.getBoolean(
                KEY_ACTION_MODE_CLOSED_BY_DRAWER,
                actionModeClosedByDrawer
            )
        }
    }

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
        private const val KEY_ACTION_MODE_CLOSED_BY_DRAWER = "KILLED_ACTION_MODE"

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
