/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2023 TSI-mc
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
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.annotation.IdRes
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentResultListener
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.lib.resources.clientintegration.Endpoint
import com.nextcloud.android.lib.resources.files.ToggleFileLockRemoteOperation
import com.nextcloud.client.account.User
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.documentscan.AppScanOptionalFeature
import com.nextcloud.client.documentscan.DocumentScanActivity
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.nextcloud.client.network.ConnectivityService.GenericCallback
import com.nextcloud.client.utils.Throttler
import com.nextcloud.ui.fileactions.FileAction.Companion.getFileListActionsToHide
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.setResultListener
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.ShortcutUtil
import com.nextcloud.utils.e2ee.E2EVersionHelper.isV1
import com.nextcloud.utils.e2ee.E2EVersionHelper.isV2Plus
import com.nextcloud.utils.extensions.getDepth
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.nextcloud.utils.extensions.slideHideBottomBehavior
import com.nextcloud.utils.extensions.typedActivity
import com.nextcloud.utils.fileNameValidator.FileNameValidator
import com.nextcloud.utils.fileNameValidator.FileNameValidator.checkFileName
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
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.ToggleFavoriteRemoteOperation
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.status.Type
import com.owncloud.android.ui.CompletionCallback
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.adapter.CommonOCFileListAdapterInterface
import com.owncloud.android.ui.adapter.OCFileListAdapter
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.ChooseTemplateDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment.Companion.newInstance
import com.owncloud.android.ui.events.ChangeMenuEvent
import com.owncloud.android.ui.events.CommentsEvent
import com.owncloud.android.ui.events.EncryptionEvent
import com.owncloud.android.ui.events.FavoriteEvent
import com.owncloud.android.ui.events.FileLockEvent
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.fragment.helper.ParentFolderFinder
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.ui.interfaces.OCFileListFragmentInterface
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewMediaActivity
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.PermissionUtil.checkSelfPermission
import com.owncloud.android.utils.PermissionUtil.requestCameraPermission
import com.owncloud.android.utils.overlay.OverlayManager
import com.owncloud.android.utils.theme.ThemeUtils
import org.apache.commons.httpclient.HttpStatus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.Objects
import java.util.Optional
import java.util.function.Supplier
import javax.inject.Inject

/**
 * A Fragment that lists all files and folders in a given path.
 * TODO refactor to get rid of direct dependency on FileDisplayActivity
 */
open class OCFileListFragment : ExtendedListFragment(), OCFileListFragmentInterface, OCFileListBottomSheetActions,
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
    lateinit var shortcutUtil: ShortcutUtil

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject
    lateinit var appScanOptionalFeature: AppScanOptionalFeature

    @Inject
    lateinit var overlayManager: OverlayManager

    @Inject
    lateinit var deviceInfo: DeviceInfo

    @JvmField
    protected var containerActivity: FileFragment.ContainerActivity? = null

    /**
     * Use this to query the [OCFile] that is currently being displayed by this fragment
     * 
     * @return The currently viewed OCFile
     */
    var currentFile: OCFile? = null
        protected set

    var adapter: OCFileListAdapter? = null
        private set

    protected var onlyFoldersClickable: Boolean = false
    protected var fileSelectable: Boolean = false

    protected var hideFab: Boolean = true
    protected var activeActionMode: ActionMode? = null
    protected var isActionModeNew: Boolean = false

    var multiChoiceModeListener: MultiChoiceModeListener? = null

    var currentSearchType: SearchType? = null
        protected set
    var isSearchFragment: Boolean = false
    var searchEvent: SearchEvent? = null
        protected set

    private var searchTask: OCFileListSearchTask? = null
    protected var mLimitToMimeType: String? = null
    private var floatingActionButton: FloatingActionButton? = null
    val parentFolderFinder: ParentFolderFinder = ParentFolderFinder()

    protected enum class MenuItemAddRemove {
        DO_NOTHING,
        REMOVE_SORT,
        REMOVE_GRID_AND_SORT,
        ADD_GRID_AND_SORT_WITH_SEARCH
    }

    @JvmField
    protected var menuItemAddRemoveValue: MenuItemAddRemove = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH

    private val mOriginalMenuItems: MutableList<MenuItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        multiChoiceModeListener = MultiChoiceModeListener()

        val state = savedInstanceState ?: arguments

        setSearchArgs(state)
        this.currentFile = state.getParcelableArgument(KEY_FILE, OCFile::class.java)
        this.isSearchFragment = currentSearchType != null && isSearchEventSet(searchEvent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listenSetupEncryptionDialogResult()
    }

    override fun onResume() {
        // Don't handle search events if we're coming back from back stack
        // The fragment has already been properly restored in onCreate/onActivityCreated
        if (this.currentFile != null) {
            super.onResume()
            return
        }

        val activity = activity ?: return

        val intent = activity.intent
        if (intent.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java) != null) {
            searchEvent = intent.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)
        }

        if (isSearchEventSet(searchEvent)) {
            handleSearchEvent(searchEvent)
        }

        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter?.cleanup()
    }

    /**
     * {@inheritDoc}
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log_OC.i(TAG, "onAttach")

        try {
            containerActivity = context as FileFragment.ContainerActivity
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                context.toString() + " must implement " +
                    FileFragment.ContainerActivity::class.java.getSimpleName(), e
            )
        }
        try {
            setOnRefreshListener(context as OnEnforceableRefreshListener)
        } catch (e: ClassCastException) {
            throw IllegalArgumentException(
                context.toString() + " must implement " +
                    OnEnforceableRefreshListener::class.java.getSimpleName(), e
            )
        }
    }

    fun setSearchArgs(state: Bundle?) {
        var argSearchType: SearchType? = SearchType.NO_SEARCH
        var argSearchEvent: SearchEvent? = null

        if (state != null) {
            argSearchType = state.getParcelableArgument(KEY_CURRENT_SEARCH_TYPE, SearchType::class.java)
            argSearchEvent = state.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)
        }

        currentSearchType = Objects.requireNonNullElse(argSearchType, SearchType.NO_SEARCH)

        if (argSearchEvent != null) {
            searchEvent = argSearchEvent
        }

        if (searchEvent != null && currentSearchType != SearchType.NO_SEARCH) {
            this.isSearchFragment = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log_OC.i(TAG, "onCreateView() start")
        val v = super.onCreateView(inflater, container, savedInstanceState)

        val state = savedInstanceState ?: arguments
        setSearchArgs(state)

        val allowContextualActions = (state != null && state.getBoolean(ARG_ALLOW_CONTEXTUAL_ACTIONS, false))
        if (allowContextualActions) {
            setChoiceModeAsMultipleModal(state)
        }

        floatingActionButton = activity?.findViewById(R.id.fab_main)

        // is not available in FolderPickerActivity
        floatingActionButton?.let { viewThemeUtils.material.themeFAB(it) }

        Log_OC.i(TAG, "onCreateView() end")
        return v
    }

    override fun onDetach() {
        setOnRefreshListener(null)
        containerActivity = null
        searchTask?.cancel()
        super.onDetach()
    }

    override fun onPause() {
        super.onPause()
        adapter?.cancelAllPendingTasks()
        activity?.intent?.removeExtra(SEARCH_EVENT)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log_OC.i(TAG, "onActivityCreated() start")
        prepareOCFileList(savedInstanceState)
        listDirectory(MainApp.isOnlyOnDevice())
    }

    fun prepareOCFileList(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            this.currentFile = savedInstanceState.getParcelableArgument(KEY_FILE, OCFile::class.java)
        }

        val args = arguments
        onlyFoldersClickable = args != null && args.getBoolean(ARG_ONLY_FOLDERS_CLICKABLE, false)
        fileSelectable = args != null && args.getBoolean(ARG_FILE_SELECTABLE, false)
        mLimitToMimeType = if (args != null) args.getString(ARG_MIMETYPE, "") else ""

        setAdapter(args)

        hideFab = args != null && args.getBoolean(ARG_HIDE_FAB, false)

        if (hideFab) {
            setFabVisible(false)
        } else {
            if (this.currentFile != null) {
                setFabVisible(currentFile?.canCreateFileAndFolder() == true)
            } else {
                setFabVisible(true)
            }

            registerFabListener()
        }

        if (!this.isSearchFragment) {
            // do not touch search event if previously searched
            searchEvent = if (arguments == null) {
                null
            } else {
                arguments.getParcelableArgument(SEARCH_EVENT, SearchEvent::class.java)
            }
        }
        prepareCurrentSearch(searchEvent)
        setEmptyView(searchEvent)

        mSortButton?.setOnClickListener {
            DisplayUtils.openSortingOrderDialogFragment(
                parentFragmentManager,
                preferences.getSortOrderByFolder(this.currentFile)
            )
        }

        mSwitchGridViewButton?.setOnClickListener {
            if (isGridEnabled) {
                setListAsPreferred()
            } else {
                setGridAsPreferred()
            }
            setLayoutSwitchButton()
        }

        val fda = typedActivity<FileDisplayActivity>()
        fda?.updateActionBarTitleAndHomeButton(fda.getCurrentDir())
    }

    protected open fun setAdapter(args: Bundle?) {
        val hideItemOptions = args != null && args.getBoolean(ARG_HIDE_ITEM_OPTIONS, false)

        this.adapter = OCFileListAdapter(
            activity,
            accountManager.user,
            preferences,
            syncedFolderProvider,
            containerActivity,
            this,
            hideItemOptions,
            isGridViewPreferred(this.currentFile),
            viewThemeUtils,
            overlayManager
        )

        setRecyclerViewAdapter(this.adapter)
        recyclerView?.let { fastScrollUtils.applyFastScroll(it) }
    }

    protected fun prepareCurrentSearch(event: SearchEvent?) {
        if (isSearchEventSet(event)) {
            setCurrentSearchType(event)
            prepareActionBarItems(event)
        }
    }

    /**
     * register listener on FAB.
     */
    fun registerFabListener() {
        if (activity !is FileActivity) {
            Log_OC.w(TAG, "activity is null cannot register fab listener")
            return
        }

        val fileActivity = typedActivity<FileActivity>()

        if (floatingActionButton == null) {
            Log_OC.w(TAG, "mFabMain is null cannot register fab listener")
            return
        }

        // is not available in FolderPickerActivity
        floatingActionButton?.let {
            viewThemeUtils.material.themeFAB(it)
        }

        floatingActionButton?.setOnClickListener {
            val currentDir = this.currentFile
            if (currentDir == null) {
                Log_OC.w(TAG, "currentDir is null cannot open bottom sheet dialog")
                return@setOnClickListener
            }

            val dialog = OCFileListBottomSheetDialog(
                fileActivity!!,
                this,
                deviceInfo,
                accountManager.user,
                currentDir,
                themeUtils,
                viewThemeUtils,
                editorUtils,
                appScanOptionalFeature
            )

            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED)
            dialog.getBehavior().skipCollapsed = true
            dialog.show()
        }
    }

    override fun createFolder(encrypted: Boolean) {
        val activity = getActivity()
        if (activity == null) {
            Log_OC.e(TAG, "activity is null, cannot create a folder")
            return
        }

        if (encrypted) {
            val user = accountManager.user
            val publicKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PUBLIC_KEY)
            val privateKey = arbitraryDataProvider.getValue(user, EncryptionUtils.PRIVATE_KEY)

            if (publicKey.isEmpty() || privateKey.isEmpty()) {
                Log_OC.w(TAG, "cannot create encrypted folder directly, needs to setup encryption first")

                activity.runOnUiThread {
                    val dialog = newInstance(user, currentFile?.remotePath)
                    dialog.show(
                        getParentFragmentManager(),
                        SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG
                    )
                }
                return
            }
        }

        newInstance(this.currentFile, encrypted)
            .show(activity.supportFragmentManager, DIALOG_CREATE_FOLDER)
    }

    override fun uploadFromApp() {
        var action = Intent(Intent.ACTION_GET_CONTENT)
        action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)
        action.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        activity?.startActivityForResult(
            Intent.createChooser(action, getString(R.string.upload_chooser_title)),
            FileDisplayActivity.REQUEST_CODE__SELECT_CONTENT_FROM_APPS
        )
    }

    override fun directCameraUpload() {
        val fileDisplayActivity = activity as FileDisplayActivity?

        if (fileDisplayActivity == null) {
            DisplayUtils.showSnackMessage(view, getString(R.string.error_starting_direct_camera_upload))
            return
        }

        if (!checkSelfPermission(fileDisplayActivity, Manifest.permission.CAMERA)) {
            requestCameraPermission(fileDisplayActivity, PermissionUtil.PERMISSIONS_CAMERA)
            return
        }

        showDirectCameraUploadAlertDialog(fileDisplayActivity)
    }

    private fun showDirectCameraUploadAlertDialog(fileDisplayActivity: FileDisplayActivity) {
        val builder = MaterialAlertDialogBuilder(fileDisplayActivity)
            .setTitle(R.string.upload_direct_camera_promt)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(
                R.string.upload_direct_camera_video
            ) { _, _ ->
                fileDisplayActivity.fileOperationsHelper.uploadFromCamera(
                    fileDisplayActivity,
                    FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_VIDEO_CAMERA,
                    true
                )
            }
            .setNegativeButton(
                R.string.upload_direct_camera_photo
            ) { _, _ ->
                fileDisplayActivity.fileOperationsHelper.uploadFromCamera(
                    fileDisplayActivity,
                    FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA,
                    false
                )
            }

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(fileDisplayActivity, builder)

        builder.create()
        builder.show()
    }

    override fun scanDocUpload() {
        val fileDisplayActivity = activity as FileDisplayActivity?

        val currentFile = this.currentFile
        if (fileDisplayActivity != null && currentFile != null && currentFile.isFolder) {
            val intent = Intent(requireContext(), DocumentScanActivity::class.java).apply {
                putExtra(DocumentScanActivity.EXTRA_FOLDER, currentFile.remotePath)
            }
            startActivity(intent)
        } else {
            Log.w(
                TAG, "scanDocUpload: Failed to start doc scanning, fileDisplayActivity=" + fileDisplayActivity +
                    ", currentFile=" + currentFile
            )
            DisplayUtils.showSnackMessage(this, R.string.error_starting_doc_scan)
        }
    }

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
        if (activity !is FileActivity) {
            Log_OC.w(TAG, "Activity is null, cant upload files")
            return
        }

        val fileActivity = typedActivity<FileActivity>()

        val user: Optional<User> = fileActivity!!.getUser()
        if (user.isEmpty) {
            Log_OC.w(TAG, "User not exist, cant upload files")
            return
        }

        val file = this.currentFile
        if (file == null) {
            Log_OC.w(TAG, "File is null cannot determine isWithinEncryptedFolder, cant upload files")
            return
        }

        val isWithinEncryptedFolder = file.isEncrypted
        UploadFilesActivity.startUploadActivityForResult(
            fileActivity,
            user.get(),
            FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM,
            isWithinEncryptedFolder
        )
    }

    override fun createRichWorkspace() {
        typedActivity<FileActivity>()?.let { activity ->
            currentFile?.remotePath?.let { remotePath ->
                activity.filesRepository.createRichWorkspace(remotePath, { url: String? ->
                    containerActivity?.getFileOperationsHelper()
                        ?.openRichWorkspaceWithTextEditor(this.currentFile, url, requireContext())
                }, {
                    DisplayUtils.showSnackMessage(activity, R.string.failed_to_start_editor)
                })
            }
        }
    }

    override fun onShareIconClick(file: OCFile?) {
        containerActivity?.showDetails(file, 1)
    }

    override fun showShareDetailView(file: OCFile?) {
        containerActivity?.showDetails(file, 1)
    }

    override fun showActivityDetailView(file: OCFile?) {
        containerActivity?.showDetails(file, 0)
    }

    override fun onOverflowIconClicked(file: OCFile, view: View) {
        val checkedFiles: MutableSet<OCFile> = HashSet()
        checkedFiles.add(file)
        openActionsMenu(1, checkedFiles, true)
    }

    fun openActionsMenu(filesCount: Int, checkedFiles: MutableSet<OCFile>, isOverflow: Boolean) {
        throttler.run("overflowClick") {
            val actionsToHide = getFileListActionsToHide(checkedFiles)
            val endpoints = this.capabilities.getClientIntegrationEndpoints(
                Type.CONTEXT_MENU,
                checkedFiles.iterator().next().mimeType
            )

            val childFragmentManager = getChildFragmentManager()
            val actionBottomSheet: FileActionsBottomSheet =
                FileActionsBottomSheet.newInstance(filesCount, checkedFiles, isOverflow, actionsToHide, endpoints)
                    .setResultListener(
                        childFragmentManager,
                        this,
                        FileActionsBottomSheet.ResultListener { id: Int -> onFileActionChosen(id, checkedFiles) })
            if (this.isDialogFragmentReady()) {
                actionBottomSheet.show(childFragmentManager, "actions")
            }
        }
    }

    override fun newDocument() {
        newInstance(
            this.currentFile,
            ChooseRichDocumentsTemplateDialogFragment.Type.DOCUMENT
        )
            .show(requireActivity().supportFragmentManager, DIALOG_CREATE_DOCUMENT)
    }

    override fun newSpreadsheet() {
        newInstance(
            this.currentFile,
            ChooseRichDocumentsTemplateDialogFragment.Type.SPREADSHEET
        )
            .show(requireActivity().supportFragmentManager, DIALOG_CREATE_DOCUMENT)
    }

    override fun newPresentation() {
        newInstance(
            this.currentFile,
            ChooseRichDocumentsTemplateDialogFragment.Type.PRESENTATION
        )
            .show(requireActivity().supportFragmentManager, DIALOG_CREATE_DOCUMENT)
    }

    override fun onHeaderClicked() {
        val file = this.currentFile ?: return

        if (TextUtils.isEmpty(file.richWorkspace)) {
            return
        }

        val adapter = this.adapter
        if (adapter == null || adapter.isMultiSelect()) {
            return
        }

        getTypedActivity(FileDisplayActivity::class.java)?.startRichWorkspacePreview(file)
    }

    override fun showTemplate(creator: Creator?, headline: String?) {
        newInstance(this.currentFile, creator, headline).show(
            requireActivity().supportFragmentManager,
            DIALOG_CREATE_DOCUMENT
        )
    }

    /**
     * Handler for multiple selection mode.
     * 
     * 
     * Manages input from the user when one or more files or folders are selected in the list.
     * 
     * 
     * Also listens to changes in navigation drawer to hide and recover multiple selection when it's opened and closed.
     */
    inner class MultiChoiceModeListener : AbsListView.MultiChoiceModeListener, DrawerLayout.DrawerListener {
        /**
         * True when action mode is finished because the drawer was opened
         */
        private var mActionModeClosedByDrawer = false

        /**
         * Selected items in list when action mode is closed by drawer
         */
        private val mSelectionWhenActionModeClosedByDrawer = HashSet<OCFile>()
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
        override fun onDrawerOpened(drawerView: View) = Unit

        /**
         * When the navigation drawer is closed, action mode is recovered in the same state as was when the drawer was
         * (started to be) opened.
         * 
         * @param drawerView Navigation drawer just closed.
         */
        override fun onDrawerClosed(drawerView: View) {
            if (!mActionModeClosedByDrawer || mSelectionWhenActionModeClosedByDrawer.isEmpty()) {
                return
            }

            activity?.startActionMode(multiChoiceModeListener)
            adapter?.setCheckedItem(mSelectionWhenActionModeClosedByDrawer)
            activeActionMode?.invalidate()
            mSelectionWhenActionModeClosedByDrawer.clear()
        }

        /**
         * If the action mode is active when the navigation drawer starts to move, the action mode is closed and the
         * selection stored to be recovered when the drawer is closed.
         * 
         * @param newState One of STATE_IDLE, STATE_DRAGGING or STATE_SETTLING.
         */
        override fun onDrawerStateChanged(newState: Int) {
            if (DrawerLayout.STATE_DRAGGING != newState || activeActionMode == null) {
                return
            }

            if (recyclerView != null && recyclerView?.adapter is OCFileListAdapter) {
                mSelectionWhenActionModeClosedByDrawer.addAll(fileListAdapter.getCheckedItems())
            }

            activeActionMode?.finish()
            mActionModeClosedByDrawer = true
        }

        /**
         * Update action mode bar when an item is selected / unselected in the list
         */
        override fun onItemCheckedStateChanged(mode: ActionMode?, position: Int, id: Long, checked: Boolean) = Unit

        /**
         * Load menu and customize UI when action mode is started.
         */
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
            activeActionMode = mode
            // Determine if actionMode is "new" or not (already affected by item-selection)
            isActionModeNew = true

            // fake menu to be able to use bottom sheet instead
            val inflater = requireActivity().getMenuInflater()
            inflater.inflate(R.menu.custom_menu_placeholder, menu)

            val item = menu.findItem(R.id.custom_menu_placeholder_item)
            if (item.icon != null) {
                item.icon = viewThemeUtils.platform.colorDrawable(
                    item.icon!!,
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
            }

            activeActionMode?.invalidate()

            //set actionMode color
            val statusBarColor = ContextCompat.getColor(requireContext(), R.color.action_mode_background)
            viewThemeUtils.platform.colorStatusBar(requireActivity(), statusBarColor)

            // hide FAB in multi selection mode
            setFabVisible(false)

            commonAdapter?.setMultiSelect(true)
            return true
        }

        /**
         * Updates available action in menu depending on current selection.
         */
        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            val checkedFiles = commonAdapter?.getCheckedItems()
            val checkedCount = checkedFiles?.size

            if (activeActionMode != null) {
                val title = resources.getQuantityString(R.plurals.items_selected_count, checkedCount, checkedCount)
                activeActionMode?.title = title
            }

            // Determine if we need to finish the action mode because there are no items selected
            if (checkedCount == 0 && !isActionModeNew) {
                exitSelectionMode()
            }

            isMultipleFileSelectedForCopyOrMove = (checkedCount > 0)

            return true
        }

        /**
         * Starts the corresponding action when a menu item is tapped by the user.
         */
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            val checkedFiles = commonAdapter?.getCheckedItems()
            if (item.itemId == R.id.custom_menu_placeholder_item) {
                openActionsMenu(commonAdapter?.getFilesCount(), checkedFiles, false)
            }
            return true
        }

        /**
         * Restores UI.
         */
        override fun onDestroyActionMode(mode: ActionMode?) {
            activeActionMode = null

            // show FAB on multi selection mode exit
            if (!hideFab && !isSearchFragment) {
                val file: OCFile? = currentFile
                if (file != null) {
                    setFabVisible(file.canCreateFileAndFolder())
                }
            }

            val activity = getActivity()
            if (activity != null) {
                viewThemeUtils.platform.resetStatusBar(activity)
            }

            val adapter: CommonOCFileListAdapterInterface? = commonAdapter
            adapter?.setMultiSelect(false)
            adapter?.clearCheckedItems()

            isMultipleFileSelectedForCopyOrMove = false
        }

        fun storeStateIn(outState: Bundle) {
            outState.putBoolean(KEY_ACTION_MODE_CLOSED_BY_DRAWER, mActionModeClosedByDrawer)
        }

        fun loadStateFrom(savedInstanceState: Bundle) {
            mActionModeClosedByDrawer = savedInstanceState.getBoolean(
                KEY_ACTION_MODE_CLOSED_BY_DRAWER,
                mActionModeClosedByDrawer
            )
        }

        private val KEY_ACTION_MODE_CLOSED_BY_DRAWER = "KILLED_ACTION_MODE"
    }

    /**
     * Init listener that will handle interactions in multiple selection mode.
     */
    protected fun setChoiceModeAsMultipleModal(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            multiChoiceModeListener?.loadStateFrom(savedInstanceState)
        }
        (activity as FileActivity).addDrawerListener(multiChoiceModeListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_FILE, this.currentFile)
        if (this.isSearchFragment) {
            outState.putParcelable(KEY_CURRENT_SEARCH_TYPE, currentSearchType)
            if (isSearchEventSet(searchEvent)) {
                outState.putParcelable(SEARCH_EVENT, searchEvent)
            }
        }
        multiChoiceModeListener?.storeStateIn(outState)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        if (mOriginalMenuItems.isEmpty()) {
            mOriginalMenuItems.add(menu.findItem(R.id.action_search))
        }

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
        if (mSwitchGridViewButton == null || mSortButton == null) {
            return
        }

        when (menuItemAddRemoveValue) {
            MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH -> {
                mSwitchGridViewButton?.visibility = View.VISIBLE
                mSortButton?.visibility = View.VISIBLE
            }

            MenuItemAddRemove.REMOVE_SORT -> mSortButton?.visibility = View.GONE
            MenuItemAddRemove.REMOVE_GRID_AND_SORT -> {
                mSortButton?.visibility = View.GONE
                mSwitchGridViewButton?.visibility = View.GONE
            }

            MenuItemAddRemove.DO_NOTHING -> Log_OC.v(TAG, "Kept the options menu default structure")
        }
    }

    /**
     * Call this, when the user presses the up button.
     * 
     * 
     * Tries to move up the current folder one level. If the parent folder was removed from the database, it continues
     * browsing up until finding an existing folders.
     * 
     * 
     * return Count of folder levels browsed up.
     */
    fun onBrowseUp(): Int {
        if (this.currentFile == null || currentFile?.isRootDirectory == true) {
            return 0
        }

        val result =
            parentFolderFinder.getParent(this.currentFile, containerActivity!!.getStorageManager())
        val target = result.second

        if (target == null) {
            Log_OC.e(TAG, "onBrowseUp: could not resolve parent, staying put")
            return 0
        }

        this.currentFile = target
        setFileDepth(this.currentFile)

        if (currentFile?.isRootDirectory == true && currentSearchType != SearchType.NO_SEARCH) {
            this.isSearchFragment = true
        }

        updateFileList()
        return result.first
    }

    private fun updateFileList() {
        listDirectory(this.currentFile, MainApp.isOnlyOnDevice())
        onRefresh(false)
        restoreIndexAndTopPosition()
    }

    /**
     * Will toggle a file selection status from the action mode
     * 
     * @param file The concerned OCFile by the selection/deselection
     */
    private fun toggleItemToCheckedList(file: OCFile) {
        if (commonAdapter?.isCheckedFile(file) == true) {
            commonAdapter?.removeCheckedFile(file)
        } else {
            commonAdapter?.addCheckedFile(file)
        }
        updateActionModeFile(file)
    }

    /**
     * Will update (invalidate) the action mode adapter/mode to refresh an item selection change
     * 
     * @param file The concerned OCFile to refresh in adapter
     */
    private fun updateActionModeFile(file: OCFile) {
        isActionModeNew = false
        if (activeActionMode != null) {
            activeActionMode?.invalidate()
            commonAdapter?.notifyItemChanged(file)
        }
    }

    override fun onLongItemClicked(file: OCFile): Boolean {
        val actionBarActivity = activity
        if (actionBarActivity != null) {
            // Create only once instance of action mode
            if (activeActionMode != null) {
                toggleItemToCheckedList(file)
            } else {
                actionBarActivity.startActionMode(multiChoiceModeListener)
                this.commonAdapter?.addCheckedFile(file)
            }
            updateActionModeFile(file)
        }

        return true
    }

    private fun folderOnItemClick(file: OCFile, position: Int) {
        if (requireActivity() is FolderPickerActivity) {
            val filenameErrorMessage = checkFileName(
                file.fileName,
                this.capabilities, requireContext(), null
            )
            if (filenameErrorMessage != null) {
                DisplayUtils.showSnackMessage(activity, filenameErrorMessage)
                return
            }
        }

        if (file.isEncrypted) {
            val user = (containerActivity as FileActivity).user
                .orElseThrow(Supplier { RuntimeException() })

            // check if e2e app is enabled
            val ocCapability = containerActivity?.getStorageManager()
                ?.getCapability(user.accountName)

            if (ocCapability?.endToEndEncryption?.isFalse == true ||
                ocCapability?.endToEndEncryption?.isUnknown == true
            ) {
                recyclerView?.let {
                    Snackbar.make(
                        it, R.string.end_to_end_encryption_not_enabled,
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                return
            }
            // check if keys are stored
            if (FileOperationsHelper.isEndToEndEncryptionSetup(requireContext(), user)) {
                // update state and view of this fragment
                this.isSearchFragment = false
                hideFab = false

                if (containerActivity is FolderPickerActivity &&
                    (containerActivity as FolderPickerActivity)
                        .isDoNotEnterEncryptedFolder
                ) {
                    if (recyclerView != null) {
                        Snackbar.make(
                            recyclerView!!,
                            R.string.copy_move_to_encrypted_folder_not_supported,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } else {
                    browseToFolder(file, position)
                }
            } else {
                Log_OC.d(TAG, "no public key for " + user.accountName)
                val fileActivity = getTypedActivity(FileActivity::class.java)

                val fragmentManager = getParentFragmentManager()
                if (fragmentManager.findFragmentByTag(SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG) == null && requireActivity() is FileActivity) {
                    fileActivity?.connectivityService?.isNetworkAndServerAvailable { result: Boolean? ->
                        if (result == true) {
                            val dialog = newInstance(user, file.remotePath)
                            dialog.show(
                                fragmentManager,
                                SetupEncryptionDialogFragment.SETUP_ENCRYPTION_DIALOG_TAG
                            )
                        } else {
                            DisplayUtils.showSnackMessage(
                                fileActivity,
                                R.string.internet_connection_required_for_encrypted_folder_setup
                            )
                        }
                    }
                }
            }
        } else {
            // update state and view of this fragment
            this.isSearchFragment = false
            setEmptyListMessage(EmptyListState.LOADING)
            browseToFolder(file, position)
        }
    }

    private fun checkFileBeforeOpen(file: OCFile): Int? {
        return if (file.isAPKorAAB) {
            R.string.gplay_restriction
        } else if (file.isOfflineOperation) {
            R.string.offline_operations_file_does_not_exists_yet
        } else {
            null
        }
    }

    private fun fileOnItemClick(file: OCFile) {
        val errorMessageId = checkFileBeforeOpen(file)
        if (recyclerView != null && errorMessageId != null) {
            Snackbar.make(recyclerView!!, errorMessageId, Snackbar.LENGTH_LONG).show()
            return
        }

        if (PreviewImageFragment.canBePreviewed(file) && containerActivity is FileDisplayActivity) {
            containerActivity.previewImageWithSearchContext(file, this.isSearchFragment, currentSearchType)
        } else if (file.isDown && containerActivity is FileDisplayActivity) {
            containerActivity.previewFile(file, CompletionCallback { visible: Boolean -> this.setFabVisible(visible) })
        } else {
            handlePendingDownloadFile(file)
        }
    }

    private fun handlePendingDownloadFile(file: OCFile) {
        if (!isAccountManagerInitialized()) {
            Log_OC.e(TAG, "AccountManager not yet initialized")
            return
        }

        val account = accountManager.getUser()
        val capability = containerActivity!!.getStorageManager().getCapability(account.accountName)

        if (PreviewMediaActivity.Companion.canBePreviewed(file) && !file.isEncrypted() && containerActivity is FileDisplayActivity) {
            setFabVisible(false)
            containerActivity.startMediaPreview(file, 0, true, true, true, true)
        } else if (editorUtils!!.isEditorAvailable(
                accountManager.getUser(),
                file.getMimeType()
            ) && !file.isEncrypted()
        ) {
            containerActivity!!.getFileOperationsHelper().openFileWithTextEditor(file, getContext())
        } else if (capability.richDocumentsMimeTypeList != null &&
            capability.richDocumentsMimeTypeList!!.contains(file.getMimeType()) &&
            capability.richDocumentsDirectEditing.isTrue && !file.isEncrypted()
        ) {
            containerActivity!!.getFileOperationsHelper().openFileAsRichDocument(file, getContext())
        } else if (containerActivity is FileDisplayActivity) {
            containerActivity.startDownloadForPreview(file, this.currentFile)

            // Checks if the file is small enough to be previewed immediately without showing progress.
            // If the file is smaller than or equal to 1MB, it can be displayed directly.
            if (file.isFileEligibleForImmediatePreview()) {
                containerActivity.setFileIDForImmediatePreview(file.getFileId())
            }
        }
    }

    @OptIn(markerClass = UnstableApi::class)
    override fun onItemClicked(file: OCFile?) {
        if (this.commonAdapter != null && this.commonAdapter!!.isMultiSelect()) {
            toggleItemToCheckedList(file!!)
        } else {
            if (file == null) {
                Log_OC.d(TAG, "Null object in ListAdapter!")
                return
            }

            if (this.commonAdapter != null && file.isFolder()) {
                val position = this.commonAdapter!!.getItemPosition(file)
                folderOnItemClick(file, position)
            } else if (fileSelectable) {
                val intent = Intent()
                intent.putExtra(FolderPickerActivity.EXTRA_FILES, file)
                requireActivity().setResult(Activity.RESULT_OK, intent)
                requireActivity().finish()
            } else if (!onlyFoldersClickable) {
                fileOnItemClick(file)
            }
        }
    }

    private fun setFileDepth(file: OCFile?) {
        Companion.fileDepth = file.getDepth()
    }

    fun resetFileDepth() {
        Companion.fileDepth = OCFileDepth.Root
    }

    val fileDepth: OCFileDepth?
        get() = Companion.fileDepth

    private fun browseToFolder(file: OCFile?, position: Int) {
        setFileDepth(file)

        if (currentSearchType == SearchType.FAVORITE_SEARCH) {
            resetMenuItems()
        }

        listDirectory(file, MainApp.isOnlyOnDevice())
        // then, notify parent activity to let it update its state and view
        containerActivity!!.onBrowsedDownTo(file)
        // save index and top position
        saveIndexAndTopPosition(position)
    }

    private fun listenSetupEncryptionDialogResult() {
        getParentFragmentManager().setFragmentResultListener(
            SetupEncryptionDialogFragment.RESULT_REQUEST_KEY,
            this,
            FragmentResultListener { requestKey: String?, bundle: Bundle? ->
                val result = bundle!!.getBoolean(SetupEncryptionDialogFragment.SUCCESS, false)
                if (!result) {
                    Log_OC.d(TAG, "setup encryption dialog is dismissed")
                    return@setFragmentResultListener
                }

                val fileRemotePath = bundle.getString(SetupEncryptionDialogFragment.ARG_FILE_PATH, null)
                if (fileRemotePath == null) {
                    Log_OC.e(TAG, "file path is null")
                    return@setFragmentResultListener
                }

                val file = containerActivity!!.getStorageManager().getFileByDecryptedRemotePath(fileRemotePath)
                if (file == null) {
                    Log_OC.e(TAG, "file is null, cannot toggle encryption")
                    return@setFragmentResultListener
                }

                if (file.isRootDirectory()) {
                    Log_OC.d(
                        TAG, "result of setup encryption triggered in root directory, this call is for " +
                            "creating encrypted folder"
                    )
                    createFolder(true)
                    return@setFragmentResultListener
                }

                containerActivity!!.getFileOperationsHelper().toggleEncryption(file, true)
                adapter!!.updateFileEncryptionById(file.getRemoteId(), true)
                this.isSearchFragment = false
                setFileDepth(file)
                listDirectory(file, MainApp.isOnlyOnDevice())
                containerActivity!!.onBrowsedDownTo(file)

                val position = adapter!!.getItemPosition(file)
                saveIndexAndTopPosition(position)
            })
    }

    /**
     * Start the appropriate action(s) on the currently selected files given menu selected by the user.
     * 
     * @param checkedFiles List of files selected by the user on which the action should be performed
     * @return 'true' if the menu selection started any action, 'false' otherwise.
     */
    open fun onFileActionChosen(@IdRes itemId: Int, checkedFiles: MutableSet<OCFile>): Boolean {
        if (checkedFiles.isEmpty()) {
            return false
        }

        if (checkedFiles.size == SINGLE_SELECTION) {
            /** action only possible on a single file */
            val singleFile = checkedFiles.iterator().next()

            if (itemId == R.id.action_send_share_file) {
                containerActivity!!.showDetails(singleFile, 1)
                return true
            } else if (itemId == R.id.action_open_file_with) {
                containerActivity!!.getFileOperationsHelper().openFile(singleFile)
                return true
            } else if (itemId == R.id.action_stream_media) {
                containerActivity!!.getFileOperationsHelper().streamMediaFile(singleFile)
                return true
            } else if (itemId == R.id.action_edit) {
                // should not be necessary, as menu item is filtered, but better play safe
                if (editorUtils!!.isEditorAvailable(
                        accountManager.getUser(),
                        singleFile.getMimeType()
                    )
                ) {
                    containerActivity!!.getFileOperationsHelper().openFileWithTextEditor(singleFile, getContext())
                } else if (EditImageActivity.Companion.canBePreviewed(singleFile)) {
                    (containerActivity as FileDisplayActivity).startImageEditor(singleFile)
                } else {
                    containerActivity!!.getFileOperationsHelper().openFileAsRichDocument(singleFile, getContext())
                }

                return true
            } else if (itemId == R.id.action_rename_file) {
                val dialog = newInstance(
                    singleFile,
                    this.currentFile
                )
                dialog.show(getFragmentManager()!!, FileDetailFragment.FTAG_RENAME_FILE)
                return true
            } else if (itemId == R.id.action_see_details) {
                if (activeActionMode != null) {
                    activeActionMode!!.finish()
                }

                containerActivity!!.showDetails(singleFile)
                containerActivity!!.showSortListGroup(false)
                return true
            } else if (itemId == R.id.action_set_as_wallpaper) {
                containerActivity!!.getFileOperationsHelper().setPictureAs(singleFile, getView())
                return true
            } else if (itemId == R.id.action_encrypted) {
                containerActivity!!.getFileOperationsHelper().toggleEncryption(singleFile, true)
                return true
            } else if (itemId == R.id.action_unset_encrypted) {
                containerActivity!!.getFileOperationsHelper().toggleEncryption(singleFile, false)
                return true
            } else if (itemId == R.id.action_lock_file) {
                containerActivity!!.getFileOperationsHelper().toggleFileLock(singleFile, true)
            } else if (itemId == R.id.action_unlock_file) {
                containerActivity!!.getFileOperationsHelper().toggleFileLock(singleFile, false)
            } else if (itemId == R.id.action_pin_to_homescreen) {
                shortcutUtil!!.addShortcutToHomescreen(
                    singleFile,
                    viewThemeUtils,
                    accountManager.getUser(),
                    syncedFolderProvider!!
                )
                return true
            } else if (itemId == R.id.action_retry) {
                backgroundJobManager!!.startOfflineOperations()
                return true
            }
        }

        /** actions possible on a batch of files */
        if (itemId == R.id.action_remove_file) {
            val dialog =
                RemoveFilesDialogFragment.newInstance(ArrayList<OCFile?>(checkedFiles), activeActionMode)
            dialog.show(getFragmentManager()!!, ConfirmationDialogFragment.FTAG_CONFIRMATION)
            return true
        } else if (itemId == R.id.action_download_file || itemId == R.id.action_sync_file) {
            syncAndCheckFiles(checkedFiles)
            exitSelectionMode()
            return true
        } else if (itemId == R.id.action_export_file) {
            containerActivity!!.getFileOperationsHelper().exportFiles(
                checkedFiles,
                getContext(),
                getView(),
                backgroundJobManager
            )
            exitSelectionMode()
            return true
        } else if (itemId == R.id.action_cancel_sync) {
            (containerActivity as FileDisplayActivity).cancelTransference(checkedFiles)
            return true
        } else if (itemId == R.id.action_favorite) {
            containerActivity!!.getFileOperationsHelper().toggleFavoriteFiles(checkedFiles, true)
            exitSelectionMode()
            return true
        } else if (itemId == R.id.action_unset_favorite) {
            containerActivity!!.getFileOperationsHelper().toggleFavoriteFiles(checkedFiles, false)
            exitSelectionMode()
            return true
        } else if (itemId == R.id.action_move_or_copy) {
            val invalidFilename = checkInvalidFilenames(checkedFiles)

            if (invalidFilename != null) {
                DisplayUtils.showSnackMessage(
                    requireActivity(),
                    getString(R.string.file_name_validator_rename_before_move_or_copy, invalidFilename)
                )
                return false
            }

            if (!FileNameValidator.checkParentRemotePaths(
                    ArrayList<OCFile?>(checkedFiles),
                    this.capabilities, requireContext()
                )
            ) {
                browseToRoot()
                DisplayUtils.showSnackMessage(requireActivity(), R.string.file_name_validator_current_path_is_invalid)
                return false
            }

            pickFolderForMoveOrCopy(checkedFiles)
            return true
        } else if (itemId == R.id.action_select_all_action_menu) {
            selectAllFiles(true)
            return true
        } else if (itemId == R.id.action_deselect_all_action_menu) {
            selectAllFiles(false)
            return true
        } else if (itemId == R.id.action_send_file) {
            containerActivity!!.getFileOperationsHelper().sendFiles(checkedFiles)
            return true
        } else if (itemId == R.id.action_lock_file) {
            // TODO call lock API
        }

        return false
    }

    private fun browseToRoot() {
        val root = containerActivity!!.getStorageManager().getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
        browseToFolder(root, 0)
    }

    private val capabilities: OCCapability
        get() {
            val currentUser = accountManager.getUser()
            return containerActivity!!.getStorageManager().getCapability(currentUser.accountName)
        }

    private fun checkInvalidFilenames(checkedFiles: MutableSet<OCFile>): String? {
        for (file in checkedFiles) {
            val errorMessage = checkFileName(
                file.getFileName(),
                this.capabilities, requireContext(), null
            )
            if (errorMessage != null) {
                return errorMessage
            }
        }

        return null
    }

    private fun pickFolderForMoveOrCopy(checkedFiles: MutableSet<OCFile>) {
        val requestCode = FileDisplayActivity.REQUEST_CODE__MOVE_OR_COPY_FILES
        val extraAction = FolderPickerActivity.MOVE_OR_COPY

        val action = Intent(getActivity(), FolderPickerActivity::class.java)
        val paths = ArrayList<String?>(checkedFiles.size)
        for (file in checkedFiles) {
            paths.add(file.getRemotePath())
        }
        action.putStringArrayListExtra(FolderPickerActivity.EXTRA_FILE_PATHS, paths)
        action.putExtra(FolderPickerActivity.EXTRA_FOLDER, this.currentFile)
        action.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) // No animation since we stay in the same folder
        action.putExtra(FolderPickerActivity.EXTRA_ACTION, extraAction)
        getActivity()!!.startActivityForResult(action, requestCode)
    }

    /**
     * Calls [OCFileListFragment.listDirectory] with a null parameter
     */
    fun listDirectory(onlyOnDevice: Boolean) {
        listDirectory(null, onlyOnDevice)
    }

    fun refreshDirectory() {
        this.isSearchFragment = false

        if (this.currentFile != null) {
            setFabVisible(currentFile!!.canCreateFileAndFolder())
        }

        val currentFile = this.currentFile
        if (currentFile != null) {
            listDirectory(currentFile, MainApp.isOnlyOnDevice())
        }
    }

    fun listDirectory(directory: OCFile?, onlyOnDevice: Boolean) {
        listDirectory(directory, null, onlyOnDevice)
    }

    private fun getDirectoryForListDirectory(directory: OCFile?, storageManager: FileDataStorageManager): OCFile? {
        var directory = directory
        if (directory == null) {
            if (this.currentFile != null) {
                directory = this.currentFile
            } else {
                directory = storageManager.getFileByPath(OCFile.ROOT_PATH)
            }
        }

        // If that's not a directory -> List its parent
        if (!directory!!.isFolder()) {
            Log_OC.w(TAG, "You see, that is not a directory -> " + directory)
            directory = storageManager.getFileById(directory.getParentId())
        }

        return directory
    }

    /**
     * Lists the given directory on the view. When the input parameter is null, it will either refresh the last known
     * directory. list the root if there never was a directory.
     * 
     * @param directory File to be listed
     */
    fun listDirectory(directory: OCFile?, file: OCFile?, onlyOnDevice: Boolean) {
        var directory = directory
        if (!this.isSearchFragment) {
            val storageManager = containerActivity!!.getStorageManager()
            if (storageManager == null) {
                Log_OC.d(TAG, "fileDataStorageManager is null")
                return
            }

            directory = getDirectoryForListDirectory(directory, storageManager)
            if (directory == null) {
                Log_OC.e(TAG, "directory is null, no files, wait for sync")
                return
            }

            if (mLimitToMimeType == null) {
                Log_OC.w(TAG, "mLimitToMimeType is null")
                return
            }

            if (this.adapter == null) {
                Log_OC.e(TAG, "❗" + "oc file list adapter is null, cannot list directory" + "❗")
                return
            }

            adapter!!.swapDirectory(
                accountManager.getUser(),
                directory,
                storageManager,
                onlyOnDevice,
                mLimitToMimeType!!
            )

            val previousDirectory = this.currentFile
            this.currentFile = directory

            updateLayout()

            if (file != null) {
                adapter!!.setHighlightedItem(file)
                val position = adapter!!.getItemPosition(file)
                if (position != -1 && recyclerView != null) {
                    recyclerView!!.scrollToPosition(position)
                }
            } else if (recyclerView != null && (previousDirectory == null || previousDirectory != directory)) {
                recyclerView!!.scrollToPosition(0)
            }
        } else if (isSearchEventSet(searchEvent)) {
            handleSearchEvent(searchEvent!!)
            if (mRefreshListLayout != null) {
                mRefreshListLayout!!.setRefreshing(false)
            }
        }
    }

    val adapterFiles: MutableList<OCFile?>
        get() = adapter!!.getFiles()

    fun updateOCFile(file: OCFile) {
        val mFiles = adapter!!.getFiles()
        val index = mFiles.indexOf(file)
        if (index == -1) {
            Log_OC.d(TAG, "File cannot be found in adapter's files")
            return
        }

        mFiles.set(index, file)
        adapter!!.notifyItemChanged(file)
    }

    private fun updateLayout() {
        setLayoutViewMode()
        updateSortButton()
        setLayoutSwitchButton()

        setFabVisible(!hideFab)
        slideHideBottomBehaviourForBottomNavigationView(!hideFab)
        setFabEnabled(this.currentFile != null && (currentFile!!.canCreateFileAndFolder() || currentFile!!.isOfflineOperation()))

        invalidateActionMode()
    }

    private fun updateSortButton() {
        if (mSortButton != null) {
            val sortOrder: FileSortOrder
            if (currentSearchType == SearchType.FAVORITE_SEARCH) {
                sortOrder =
                    preferences.getSortOrderByType(FileSortOrder.Type.favoritesListView, FileSortOrder.SORT_A_TO_Z)
            } else {
                sortOrder = preferences.getSortOrderByFolder(this.currentFile)
            }

            mSortButton!!.setText(DisplayUtils.getSortOrderStringId(sortOrder))
        }
    }

    private fun invalidateActionMode() {
        if (activeActionMode != null) {
            activeActionMode!!.invalidate()
        }
    }

    fun sortFiles(sortOrder: FileSortOrder) {
        if (mSortButton != null) {
            mSortButton!!.setText(DisplayUtils.getSortOrderStringId(sortOrder))
        }
        adapter!!.setSortOrder(this.currentFile, sortOrder)
    }

    /**
     * Determines whether a folder should be displayed in grid or list view.
     * 
     * 
     * The preference is checked for the given folder. If the folder itself does not have a preference set,
     * it will fall back to its parent folder recursively until a preference is found (root folder is always set).
     * Additionally, if a search event is active and is of type `SHARED_FILTER`, grid view is disabled.
     * 
     * @param folder The folder to check, or `null` to refer to the root folder.
     * @return `true` if the folder should be displayed in grid mode, `false` if list mode is preferred.
     */
    private fun isGridViewPreferred(folder: OCFile?): Boolean {
        if (searchEvent != null) {
            return (searchEvent!!.toSearchType() != SearchType.SHARED_FILTER) &&
                FOLDER_LAYOUT_GRID == preferences.getFolderLayout(folder)
        } else {
            return FOLDER_LAYOUT_GRID == preferences.getFolderLayout(folder)
        }
    }

    private fun setLayoutViewMode() {
        val isGrid = isGridViewPreferred(this.currentFile)

        if (isGrid) {
            switchToGridView()
        } else {
            switchToListView()
        }

        setLayoutSwitchButton(isGrid)
    }

    fun setListAsPreferred() {
        preferences.setFolderLayout(this.currentFile, FOLDER_LAYOUT_LIST)
        switchToListView()
    }

    public override fun switchToListView() {
        if (isGridEnabled) {
            switchLayoutManager(false)
        }
    }

    fun setGridAsPreferred() {
        preferences.setFolderLayout(this.currentFile, FOLDER_LAYOUT_GRID)
        switchToGridView()
    }

    public override fun switchToGridView() {
        if (!isGridEnabled) {
            switchLayoutManager(true)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun switchLayoutManager(grid: Boolean) {
        val recyclerView = recyclerView
        val adapter = this.adapter
        val context = getContext()

        if (context == null || adapter == null || recyclerView == null) {
            Log_OC.e(TAG, "cannot switch layout, arguments are null")
            return
        }

        var position = 0

        if (recyclerView.getLayoutManager() is LinearLayoutManager) {
            position = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        }

        val layoutManager: RecyclerView.LayoutManager?
        if (grid) {
            layoutManager = GridLayoutManager(context, columnsCount)
            val gridLayoutManager = layoutManager
            gridLayoutManager.setSpanSizeLookup(object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    if (position == this.adapter.getItemCount() - 1 ||
                        position == 0 && this.adapter.shouldShowHeader()
                    ) {
                        return gridLayoutManager.getSpanCount()
                    } else {
                        return 1
                    }
                }
            })
        } else {
            layoutManager = LinearLayoutManager(context)
        }

        recyclerView.setLayoutManager(layoutManager)
        recyclerView.scrollToPosition(position)
        adapter.setGridView(grid)
        recyclerView.setAdapter(adapter)
        adapter.notifyDataSetChanged()
    }

    open val commonAdapter: CommonOCFileListAdapterInterface?
        get() = this.adapter

    fun setCurrentSearchType(event: SearchEvent) {
        val searchType = event.toSearchType()
        if (searchType != null) {
            currentSearchType = searchType
        }
    }

    fun setCurrentSearchType(searchType: SearchType?) {
        currentSearchType = searchType
    }

    protected fun prepareActionBarItems(event: SearchEvent?) {
        if (event != null) {
            when (event.searchType) {
                SearchRemoteOperation.SearchType.FAVORITE_SEARCH, SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH -> menuItemAddRemoveValue =
                    MenuItemAddRemove.REMOVE_SORT

                else -> {}
            }
        }

        if (SearchType.FILE_SEARCH != currentSearchType && getActivity() != null) {
            getActivity()!!.invalidateOptionsMenu()
        }
    }

    protected fun setEmptyView(event: SearchEvent?) {
        if (event != null) {
            when (event.searchType) {
                SearchRemoteOperation.SearchType.FILE_SEARCH -> setEmptyListMessage(SearchType.FILE_SEARCH)
                SearchRemoteOperation.SearchType.FAVORITE_SEARCH -> setEmptyListMessage(SearchType.FAVORITE_SEARCH)
                SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH -> setEmptyListMessage(SearchType.RECENT_FILES_SEARCH)
                SearchRemoteOperation.SearchType.SHARED_FILTER -> setEmptyListMessage(SearchType.SHARED_FILTER)
                else -> setEmptyListMessage(SearchType.NO_SEARCH)
            }
        } else {
            setEmptyListMessage(SearchType.NO_SEARCH)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onMessageEvent(changeMenuEvent: ChangeMenuEvent?) {
        Log_OC.d(TAG, "event bus --- change menu event triggered")

        val arguments = getArguments()
        if (arguments != null) {
            arguments.clear()
        }
        resetSearchAttributes()
        resetMenuItems()

        if (getActivity() is FileDisplayActivity) {
            fda.invalidateOptionsMenu()
            fda.getIntent().removeExtra(SEARCH_EVENT)
            fda.setupHomeSearchToolbarWithSortAndListButtons()
            fda.updateActionBarTitleAndHomeButton(null)
        }

        if (this.currentFile != null) {
            setFabVisible(currentFile!!.canCreateFileAndFolder())
        }

        slideHideBottomBehaviourForBottomNavigationView(true)
    }

    private fun resetMenuItems() {
        menuItemAddRemoveValue = MenuItemAddRemove.ADD_GRID_AND_SORT_WITH_SEARCH
        updateSortAndGridMenuItems()
    }

    fun resetSearchAttributes() {
        this.isSearchFragment = false
        searchEvent = null
        currentSearchType = SearchType.NO_SEARCH
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: CommentsEvent) {
        adapter!!.refreshCommentsCount(event.remoteId)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: FavoriteEvent) {
        try {
            val user = accountManager.getUser()
            val client = clientFactory!!.create(user)

            val toggleFavoriteOperation = ToggleFavoriteRemoteOperation(
                event.shouldFavorite, event.remotePath
            )
            val remoteOperationResult = toggleFavoriteOperation.execute(client)

            if (remoteOperationResult.isSuccess()) {
                val removeFromList = currentSearchType == SearchType.FAVORITE_SEARCH && !event.shouldFavorite
                setEmptyListMessage(SearchType.FAVORITE_SEARCH)
                if (this is GalleryFragment) {
                    galleryFragment.markAsFavorite(event.remotePath, event.shouldFavorite)
                } else {
                    adapter!!.setFavoriteAttributeForItemID(event.remotePath, event.shouldFavorite, removeFromList)
                }
            }
        } catch (e: CreationException) {
            Log_OC.e(TAG, "Error processing event", e)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            searchEvent = savedInstanceState.getParcelableArgument<SearchEvent?>(SEARCH_EVENT, SearchEvent::class.java)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: SearchEvent) {
        handleSearchEvent(event)
    }

    protected fun handleSearchEvent(event: SearchEvent) {
        if (SearchRemoteOperation.SearchType.PHOTO_SEARCH == event.searchType) {
            return
        }

        // avoid calling api multiple times if task is already executing
        if (searchTask != null && !searchTask!!.isFinished()) {
            if (searchEvent != null) {
                Log_OC.d(
                    TAG,
                    "OCFileListSearchTask already running skipping new api call for search event: " + searchEvent!!.searchType
                )
            }

            return
        }

        val activity = getActivity()
        if (activity != null) {
            activity.runOnUiThread(Runnable {
                this.adapter!!.removeAllFiles()
                setEmptyListMessage(EmptyListState.LOADING)
            })
        }

        prepareCurrentSearch(event)
        this.isSearchFragment = true
        setFabVisible(false)

        Handler(Looper.getMainLooper()).post(Runnable {
            updateSortButton()
            setLayoutViewMode()
        })

        val currentUser = accountManager.getUser()
        val remoteOperation: RemoteOperation<*>
        if (currentSearchType == SearchType.RECENT_FILES_SEARCH) {
            remoteOperation = this.recentFilesSearchRemoteOperation
        } else {
            remoteOperation = getSearchRemoteOperation(currentUser, event)
        }

        var storageManager = containerActivity!!.getStorageManager()
        if (storageManager == null) {
            storageManager = FileDataStorageManager(currentUser, requireContext().getContentResolver())
        }

        searchTask = OCFileListSearchTask(
            this,
            remoteOperation,
            currentUser, event,
            SharedListFragment.TASK_TIMEOUT.toLong(),
            preferences,
            storageManager
        )
        searchTask!!.execute()
    }

    protected open fun getSearchRemoteOperation(currentUser: User, event: SearchEvent): RemoteOperation<*> {
        val searchOnlyFolders = (getArguments() != null && getArguments()!!.getBoolean(ARG_SEARCH_ONLY_FOLDER, false))

        val ocCapability = containerActivity!!.getStorageManager()
            .getCapability(currentUser.accountName)

        return SearchRemoteOperation(
            event.searchQuery,
            event.searchType,
            searchOnlyFolders,
            ocCapability
        )
    }

    private val recentFilesSearchRemoteOperation: RemoteOperation<*>
        get() {
            val accountName = accountManager.getUser().accountName
            val capability = containerActivity!!.getStorageManager().getCapability(accountName)
            val searchQuery = ""

            val remoteOperation = SearchRemoteOperation(
                searchQuery,
                SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH,
                false,
                capability
            )

            val nowSeconds = System.currentTimeMillis() / 1000L
            val last14DaysTimestamp = nowSeconds - 14L * 24 * 60 * 60

            remoteOperation.setStartDate(last14DaysTimestamp)
            remoteOperation.setEndDate(nowSeconds)
            remoteOperation.setLimit(100)

            return remoteOperation
        }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: EncryptionEvent) {
        Thread(Runnable {
            run {
                val user = accountManager.getUser()
                // check if keys are stored
                val publicKey = arbitraryDataProvider!!.getValue(user, EncryptionUtils.PUBLIC_KEY)
                val privateKey = arbitraryDataProvider!!.getValue(user, EncryptionUtils.PRIVATE_KEY)

                val storageManager = containerActivity!!.getStorageManager()
                val file = storageManager.getFileByRemoteId(event.remoteId)
                if (publicKey.isEmpty() || privateKey.isEmpty()) {
                    Log_OC.d(TAG, "no public key for " + user.accountName)


                    requireActivity().runOnUiThread(Runnable {
                        val dialog = newInstance(user, file!!.getRemotePath())
                        dialog.show(
                            getParentFragmentManager(),
                            SetupEncryptionDialogFragment.Companion.SETUP_ENCRYPTION_DIALOG_TAG
                        )
                    })
                } else {
                    // TODO E2E: if encryption fails, to not set it as encrypted!
                    encryptFolder(
                        file!!,
                        event.localId,
                        event.remoteId,
                        event.remotePath,
                        event.shouldBeEncrypted,
                        publicKey,
                        privateKey,
                        storageManager
                    )
                }
            }
        }).start()
    }

    private fun encryptFolder(
        folder: OCFile,
        localId: Long,
        remoteId: String?,
        remotePath: String?,
        shouldBeEncrypted: Boolean,
        publicKeyString: String?,
        privateKeyString: String?,
        storageManager: FileDataStorageManager
    ) {
        try {
            Log_OC.d(TAG, "encrypt folder " + folder.getRemoteId())
            val user = accountManager.getUser()
            val client = clientFactory!!.create(user)
            val remoteOperationResult = ToggleEncryptionRemoteOperation(
                localId,
                remotePath,
                shouldBeEncrypted
            )
                .execute(client)

            if (remoteOperationResult.isSuccess()) {
                // lock folder
                val token = EncryptionUtils.lockFolder(folder, client)

                val ocCapability = containerActivity!!.getStorageManager().getCapability(user.accountName)
                if (isV2Plus(ocCapability)) {
                    // Update metadata
                    val metadataPair = EncryptionUtils.retrieveMetadata(
                        folder,
                        client,
                        privateKeyString,
                        publicKeyString,
                        storageManager,
                        user,
                        requireContext(),
                        arbitraryDataProvider
                    )

                    val metadataExists: Boolean = metadataPair.first!!
                    val metadata = metadataPair.second

                    EncryptionUtilsV2().serializeAndUploadMetadata(
                        folder,
                        metadata,
                        token,
                        client,
                        metadataExists,
                        requireContext(),
                        user,
                        storageManager
                    )

                    // unlock folder
                    EncryptionUtils.unlockFolder(folder, client, token)
                } else if (isV1(ocCapability)) {
                    // unlock folder
                    EncryptionUtils.unlockFolderV1(folder, client, token)
                } else require(ocCapability.endToEndEncryptionApiVersion != E2EVersion.UNKNOWN) { "Unknown E2E version" }

                requireActivity().runOnUiThread(Runnable {
                    val isFileExists = (adapter!!.getFileByRemoteId(remoteId) != null)
                    if (!isFileExists) {
                        val newFile = storageManager.getFileByRemoteId(remoteId)
                        adapter!!.insertFile(newFile)
                    }
                    adapter!!.updateFileEncryptionById(remoteId, shouldBeEncrypted)
                })
            } else if (remoteOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN && recyclerView != null) {
                requireActivity().runOnUiThread(Runnable {
                    Snackbar.make(
                        recyclerView!!,
                        R.string.end_to_end_encryption_folder_not_empty,
                        Snackbar.LENGTH_LONG
                    ).show()
                })
            } else {
                requireActivity().runOnUiThread(Runnable {
                    run {
                        if (recyclerView != null) {
                            Snackbar.make(
                                recyclerView!!,
                                R.string.common_error_unknown,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                })
            }
        } catch (e: Throwable) {
            Log_OC.e(TAG, "Error creating encrypted folder", e)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: FileLockEvent) {
        val user = accountManager.getUser()

        try {
            val client = clientFactory!!.createNextcloudClient(user)
            val operation = ToggleFileLockRemoteOperation(event.shouldLock, event.filePath)
            val result = operation.execute(client)

            if (result.isSuccess()) {
                // TODO only refresh the modified file?
                Handler(Looper.getMainLooper()).post(Runnable { this.onRefresh() })
            } else if (recyclerView != null) {
                Snackbar.make(
                    recyclerView!!,
                    R.string.error_file_lock,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } catch (e: CreationException) {
            Log_OC.e(TAG, "Cannot create client", e)

            if (recyclerView != null) {
                Snackbar.make(
                    recyclerView!!,
                    R.string.error_file_lock,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    public override fun onRefresh() {
        if (this.isSearchFragment && isSearchEventSet(searchEvent)) {
            handleSearchEvent(searchEvent!!)

            if (mRefreshListLayout != null) {
                mRefreshListLayout!!.setRefreshing(false)
            }
        } else {
            this.isSearchFragment = false
            super.onRefresh()
        }
    }

    /**
     * De-/select all elements in the current list view.
     * 
     * @param select `true` to select all, `false` to deselect all
     */
    @SuppressLint("NotifyDataSetChanged")
    fun selectAllFiles(select: Boolean) {
        if (recyclerView == null) {
            return
        }

        val adapter = recyclerView!!.getAdapter()
        if (adapter is CommonOCFileListAdapterInterface) {
            adapter.selectAll(select)
            adapter.notifyDataSetChanged()
            activeActionMode!!.invalidate()
        }
    }

    /**
     * Exits the multi file selection mode.
     */
    fun exitSelectionMode() {
        if (activeActionMode != null) {
            activeActionMode!!.finish()
        }
    }

    private fun isSearchEventSet(event: SearchEvent?): Boolean {
        if (event == null) {
            return false
        }
        val searchType = event.searchType
        return !TextUtils.isEmpty(event.searchQuery) || searchType == SearchRemoteOperation.SearchType.SHARED_FILTER || searchType == SearchRemoteOperation.SearchType.FAVORITE_SEARCH || searchType == SearchRemoteOperation.SearchType.RECENTLY_MODIFIED_SEARCH
    }

    private fun syncAndCheckFiles(files: MutableCollection<OCFile>) {
        var isAnyFileFolder = false
        for (file in files) {
            if (file.isFolder()) {
                isAnyFileFolder = true
                break
            }
        }

        if (containerActivity is FileActivity && !files.isEmpty()) {
            containerActivity.showSyncLoadingDialog(isAnyFileFolder)
        }

        val iterator = files.iterator()
        while (iterator.hasNext()) {
            val file = iterator.next()

            val availableSpaceOnDevice = FileOperationsHelper.getAvailableSpaceOnDevice()

            if (FileStorageUtils.checkIfEnoughSpace(file)) {
                val isLastItem = !iterator.hasNext()
                containerActivity!!.getFileOperationsHelper().syncFile(file, isLastItem)
            } else {
                showSpaceErrorDialog(file, availableSpaceOnDevice)
            }
        }
    }

    private fun showSpaceErrorDialog(file: OCFile, availableSpaceOnDevice: Long) {
        val dialog =
            newInstance(file, availableSpaceOnDevice)
        dialog.setTargetFragment(this, NOT_ENOUGH_SPACE_FRAG_REQUEST_CODE)

        if (getFragmentManager() != null) {
            dialog.show(getFragmentManager()!!, ConfirmationDialogFragment.FTAG_CONFIRMATION)
        }
    }

    override fun isLoading(): Boolean {
        return false
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     * 
     * 
     * When 'false' is set, FAB visibility is set to View.GONE programmatically.
     * 
     * @param visible Desired visibility for the FAB.
     */
    fun setFabVisible(visible: Boolean) {
        if (floatingActionButton == null) {
            // is not available in FolderPickerActivity
            return
        }

        val activity = getActivity()
        if (activity == null) {
            return
        }

        activity.runOnUiThread(Runnable {
            if (visible) {
                floatingActionButton!!.show()
                viewThemeUtils.material.themeFAB(floatingActionButton!!)
            } else {
                floatingActionButton!!.hide()
            }
            floatingActionButton.slideHideBottomBehavior<FloatingActionButton?>(visible)
        })
    }

    fun slideHideBottomBehaviourForBottomNavigationView(visible: Boolean) {
        if (getActivity() is DrawerActivity) {
            drawerActivity.getBottomNavigationView().slideHideBottomBehavior<BottomNavigationView?>(visible)
        }
    }

    /**
     * Sets the 'visibility' state of the FAB contained in the fragment.
     * 
     * 
     * When 'false' is set, FAB is greyed out
     * 
     * @param enabled Desired visibility for the FAB.
     */
    fun setFabEnabled(enabled: Boolean) {
        if (floatingActionButton == null) {
            // is not available in FolderPickerActivity
            return
        }

        if (getActivity() != null) {
            getActivity()!!.runOnUiThread(Runnable {
                if (enabled) {
                    floatingActionButton!!.setEnabled(true)
                    viewThemeUtils.material.themeFAB(floatingActionButton!!)
                } else {
                    floatingActionButton!!.setEnabled(false)
                    viewThemeUtils.material.themeFAB(floatingActionButton!!)
                }
            })
        }
    }

    val menuItemId: Int
        /**
         * Returns the navigation drawer menu item corresponding to this fragment.
         * 
         * 
         * 
         * OCFileListFragment is the parent for GalleryFragment, SharedListFragment,
         * and GroupfolderListFragment. It also internally handles listing favorites,
         * shared files, or recently modified items via search events. This method
         * checks the current fragment type and search state to give correct drawer menu ID.
         * 
         * 
         * @return the menu item ID to highlight in the navigation drawer
         */
        get() {
            if (javaClass == GalleryFragment::class.java) {
                return R.id.nav_gallery
            } else if (javaClass == SharedListFragment::class.java || this.isSearchEventShared || currentSearchType == SearchType.SHARED_FILTER) {
                return R.id.nav_shared
            } else if (javaClass == GroupfolderListFragment::class.java || currentSearchType == SearchType.GROUPFOLDER) {
                return R.id.nav_groupfolders
            } else if (this.isSearchEventFavorite || currentSearchType == SearchType.FAVORITE_SEARCH) {
                return R.id.nav_favorites
            } else if (currentSearchType == SearchType.RECENT_FILES_SEARCH) {
                return R.id.nav_recent_files
            } else {
                return R.id.nav_all_files
            }
        }

    val isEmpty: Boolean
        get() = this.adapter == null || adapter!!.isEmpty()

    val isSearchEventFavorite: Boolean
        get() = isSearchEvent(SearchRemoteOperation.SearchType.FAVORITE_SEARCH)

    val isSearchEventShared: Boolean
        get() = isSearchEvent(SearchRemoteOperation.SearchType.SHARED_FILTER)

    private fun isSearchEvent(givenEvent: SearchRemoteOperation.SearchType?): Boolean {
        if (searchEvent == null) {
            return false
        }
        return searchEvent!!.searchType == givenEvent
    }

    fun shouldNavigateBackToAllFiles(): Boolean {
        return this is GalleryFragment ||
            this.isSearchEventFavorite ||
            this.isSearchEventShared
    }

    companion object {
        protected val TAG: String = OCFileListFragment::class.java.getSimpleName()

        private val MY_PACKAGE: String? =
            if (OCFileListFragment::class.java.getPackage() != null) OCFileListFragment::class.java.getPackage()
                .getName() else "com.owncloud.android.ui.fragment"

        val ARG_ONLY_FOLDERS_CLICKABLE: String = MY_PACKAGE + ".ONLY_FOLDERS_CLICKABLE"
        val ARG_FILE_SELECTABLE: String = MY_PACKAGE + ".FILE_SELECTABLE"
        val ARG_ALLOW_CONTEXTUAL_ACTIONS: String = MY_PACKAGE + ".ALLOW_CONTEXTUAL"
        val ARG_HIDE_FAB: String = MY_PACKAGE + ".HIDE_FAB"
        val ARG_HIDE_ITEM_OPTIONS: String = MY_PACKAGE + ".HIDE_ITEM_OPTIONS"
        val ARG_SEARCH_ONLY_FOLDER: String = MY_PACKAGE + ".SEARCH_ONLY_FOLDER"
        val ARG_MIMETYPE: String = MY_PACKAGE + ".MIMETYPE"

        const val DOWNLOAD_SEND: String = "DOWNLOAD_SEND"

        const val FOLDER_LAYOUT_LIST: String = "LIST"
        const val FOLDER_LAYOUT_GRID: String = "GRID"

        const val SEARCH_EVENT: String = "SEARCH_EVENT"
        private val KEY_FILE: String = MY_PACKAGE + ".extra.FILE"
        const val KEY_CURRENT_SEARCH_TYPE: String = "CURRENT_SEARCH_TYPE"

        private const val DIALOG_CREATE_FOLDER = "DIALOG_CREATE_FOLDER"
        private const val DIALOG_CREATE_DOCUMENT = "DIALOG_CREATE_DOCUMENT"
        private const val DIALOG_BOTTOM_SHEET = "DIALOG_BOTTOM_SHEET"

        private const val SINGLE_SELECTION = 1
        private const val NOT_ENOUGH_SPACE_FRAG_REQUEST_CODE = 2

        @JvmField
        var isMultipleFileSelectedForCopyOrMove: Boolean = false
        private val scanIntentExternalApp = Intent("org.fairscan.app.action.SCAN_TO_PDF")

        @JvmField
        private var fileDepth: OCFileDepth? = OCFileDepth.Root
    }
}
