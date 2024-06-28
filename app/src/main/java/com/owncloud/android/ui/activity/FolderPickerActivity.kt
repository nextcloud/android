/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.accounts.AuthenticatorException
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.FilesFolderPickerBinding
import com.owncloud.android.databinding.FilesPickerBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.services.OperationsService
import com.owncloud.android.syncadapter.FileSyncAdapter
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment.OnSortingOrderListener
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.DataHolderUtil
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.PathUtils
import java.io.File
import javax.inject.Inject

@Suppress("Detekt.TooManyFunctions")
open class FolderPickerActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    OnEnforceableRefreshListener,
    Injectable,
    OnSortingOrderListener {

    private var mSyncBroadcastReceiver: SyncBroadcastReceiver? = null
    private var mSyncInProgress = false
    private var mSearchOnlyFolders = false
    var isDoNotEnterEncryptedFolder = false
        private set

    private var captionText: String? = null

    private var action: String? = null
    private var targetFilePaths: ArrayList<String>? = null

    private lateinit var filesPickerBinding: FilesPickerBinding
    private lateinit var folderPickerBinding: FilesFolderPickerBinding

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

    private fun initBinding() {
        if (this is FilePickerActivity) {
            filesPickerBinding = FilesPickerBinding.inflate(layoutInflater)
            setContentView(filesPickerBinding.root)
        } else {
            folderPickerBinding = FilesFolderPickerBinding.inflate(layoutInflater)
            setContentView(folderPickerBinding.root)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.d(TAG, "onCreate() start")

        super.onCreate(savedInstanceState)

        initBinding()
        initControls()
        setupToolbar()
        setupActionBar()
        setupAction()
        initTargetFilesPath()

        if (savedInstanceState == null) {
            createFragments()
        }

        updateActionBarTitleAndHomeButtonByString(captionText)
        setBackgroundText()
        handleOnBackPressed()
    }

    private fun setupActionBar() {
        findViewById<View>(R.id.sort_list_button_group).visibility =
            View.VISIBLE
        findViewById<View>(R.id.switch_grid_view_button).visibility =
            View.GONE
    }

    private fun setupAction() {
        action = intent.getStringExtra(EXTRA_ACTION)

        if (action != null && action == CHOOSE_LOCATION) {
            setupUIForChooseButton()
        } else {
            captionText = themeUtils.getDefaultDisplayNameForRootFolder(this)
        }
    }

    private fun initTargetFilesPath() {
        targetFilePaths = intent.getStringArrayListExtra(EXTRA_FILE_PATHS)
    }

    private fun setupUIForChooseButton() {
        captionText = resources.getText(R.string.folder_picker_choose_caption_text).toString()
        mSearchOnlyFolders = true
        isDoNotEnterEncryptedFolder = true

        if (this is FilePickerActivity) {
            return
        } else {
            folderPickerBinding.folderPickerBtnCopy.visibility = View.GONE
            folderPickerBinding.folderPickerBtnMove.visibility = View.GONE
            folderPickerBinding.folderPickerBtnChoose.visibility = View.VISIBLE
            folderPickerBinding.chooseButtonSpacer.visibility = View.VISIBLE
            folderPickerBinding.moveOrCopyButtonSpacer.visibility = View.GONE
        }
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    listOfFilesFragment?.let {
                        val levelsUp = it.onBrowseUp()

                        if (levelsUp == 0) {
                            finish()
                            return
                        }

                        file = it.currentFile
                        updateUiElements()
                    }
                }
            }
        )
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)

        if (action == null) {
            return
        }

        updateFileFromDB()
        var folder = file
        if (folder == null || !folder.isFolder) {
            file = storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
            folder = file
        }

        listOfFilesFragment?.listDirectory(folder, false, false)
        startSyncFolderOperation(folder, false)
        updateUiElements()
    }

    private val activity: Activity
        get() = this

    protected open fun createFragments() {
        val listOfFiles = OCFileListFragment()

        val bundle = Bundle().apply {
            putBoolean(OCFileListFragment.ARG_ONLY_FOLDERS_CLICKABLE, true)
            putBoolean(OCFileListFragment.ARG_HIDE_FAB, true)
            putBoolean(OCFileListFragment.ARG_HIDE_ITEM_OPTIONS, true)
            putBoolean(OCFileListFragment.ARG_SEARCH_ONLY_FOLDER, mSearchOnlyFolders)
        }

        listOfFiles.arguments = bundle

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, listOfFiles, TAG_LIST_OF_FOLDERS)
        transaction.commit()
    }

    /**
     * Show a text message on screen view for notifying user if content is loading or folder is empty
     */
    private fun setBackgroundText() {
        val listFragment = listOfFilesFragment

        if (listFragment == null) {
            Log_OC.e(TAG, "OCFileListFragment is null")
        }

        listFragment?.let {
            if (!mSyncInProgress) {
                it.setMessageForEmptyList(
                    R.string.folder_list_empty_headline,
                    R.string.file_list_empty_moving,
                    R.drawable.ic_list_empty_create_folder,
                    true
                )
            } else {
                it.setEmptyListLoadingMessage()
            }
        }
    }

    protected val listOfFilesFragment: OCFileListFragment?
        get() {
            val listOfFiles = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FOLDERS)

            return if (listOfFiles != null) {
                return listOfFiles as OCFileListFragment?
            } else {
                Log_OC.e(TAG, "Access to non existing list of files fragment!!")
                null
            }
        }

    /**
     * {@inheritDoc}
     *
     *
     * Updates action bar and second fragment, if in dual pane mode.
     */
    override fun onBrowsedDownTo(directory: OCFile) {
        file = directory
        updateUiElements()
        startSyncFolderOperation(directory, false)
    }

    override fun onSavedCertificate() {
        startSyncFolderOperation(currentDir, false)
    }

    private fun startSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean) {
        val currentSyncTime = System.currentTimeMillis()
        mSyncInProgress = true

        RefreshFolderOperation(
            folder,
            currentSyncTime,
            false,
            ignoreETag,
            storageManager,
            user.orElseThrow { RuntimeException("User not set") },
            applicationContext
        ).also {
            it.execute(account, this, null, null)
        }

        listOfFilesFragment?.isLoading = true
        setBackgroundText()
    }

    override fun onResume() {
        super.onResume()
        Log_OC.e(TAG, "onResume() start")

        listOfFilesFragment?.isLoading = mSyncInProgress
        refreshListOfFilesFragment(false)
        file = listOfFilesFragment?.currentFile
        updateUiElements()

        val intentFilter = getSyncIntentFilter()
        mSyncBroadcastReceiver = SyncBroadcastReceiver()
        mSyncBroadcastReceiver?.let {
            localBroadcastManager.registerReceiver(it, intentFilter)
        }

        Log_OC.d(TAG, "onResume() end")
    }

    private fun getSyncIntentFilter(): IntentFilter {
        return IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START).apply {
            addAction(FileSyncAdapter.EVENT_FULL_SYNC_END)
            addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED)
            addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED)
            addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED)
        }
    }

    override fun onPause() {
        Log_OC.e(TAG, "onPause() start")

        if (mSyncBroadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver(mSyncBroadcastReceiver!!)
            mSyncBroadcastReceiver = null
        }

        Log_OC.d(TAG, "onPause() end")
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_folder_picker, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        val itemId = item.itemId

        if (itemId == R.id.action_create_dir) {
            val dialog = CreateFolderDialogFragment.newInstance(currentFolder)
            dialog.show(supportFragmentManager, CreateFolderDialogFragment.CREATE_FOLDER_FRAGMENT)
        } else if (itemId == android.R.id.home) {
            val currentDir = currentFolder
            if (currentDir != null && currentDir.parentId != 0L) {
                onBackPressed()
            }
        } else {
            retval = super.onOptionsItemSelected(item)
        }

        return retval
    }

    // If the file is null, take the root folder to avoid any error in functions depending on this one
    val currentFolder: OCFile?
        get() {
            val currentFile = file
            val storageManager = storageManager

            return if (currentFile != null) {
                if (currentFile.isFolder) {
                    currentFile
                } else if (currentFile.remotePath != null) {
                    val parentPath = File(currentFile.remotePath).parent
                    storageManager.getFileByEncryptedRemotePath(parentPath)
                } else {
                    null
                }
            } else {
                storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
            }
        }

    private fun refreshListOfFilesFragment(fromSearch: Boolean) {
        listOfFilesFragment?.listDirectory(false, fromSearch)
    }

    fun browseToRoot() {
        listOfFilesFragment?.let {
            val root = storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
            it.listDirectory(root, false, false)
            file = it.currentFile
            updateUiElements()
            startSyncFolderOperation(root, false)
        }
    }

    private fun updateUiElements() {
        toggleChooseEnabled()
        updateNavigationElementsInActionBar()
    }

    private fun toggleChooseEnabled() {
        if (this is FilePickerActivity) {
            return
        } else {
            folderPickerBinding.folderPickerBtnCopy.isEnabled = checkFolderSelectable()
            folderPickerBinding.folderPickerBtnMove.isEnabled = checkFolderSelectable()
        }
    }

    // for copy and move, disable selecting parent folder of target files
    private fun checkFolderSelectable(): Boolean {
        return when {
            action != MOVE_OR_COPY -> true
            targetFilePaths.isNullOrEmpty() -> true
            file?.isFolder != true -> true

            // all of the target files are already in the selected directory
            targetFilePaths?.all { PathUtils.isDirectParent(file.remotePath, it) } == true -> false

            // some of the target files are parents of the selected folder
            targetFilePaths?.any { PathUtils.isAncestor(it, file.remotePath) } == true -> false
            else -> true
        }
    }

    private fun updateNavigationElementsInActionBar() {
        val currentDir = currentFolder
        supportActionBar?.let { actionBar ->
            val atRoot = (currentDir == null || currentDir.parentId == 0L)
            actionBar.setDisplayHomeAsUpEnabled(!atRoot)
            actionBar.setHomeButtonEnabled(!atRoot)
            val title = if (atRoot) captionText ?: "" else currentDir?.fileName
            title?.let {
                viewThemeUtils.files.themeActionBar(this, actionBar, title)
            }
        }
    }

    private fun initControls() {
        if (this is FilePickerActivity) {
            viewThemeUtils.material.colorMaterialButtonPrimaryFilled(filesPickerBinding.folderPickerBtnCancel)
            filesPickerBinding.folderPickerBtnCancel.setOnClickListener { finish() }
        } else {
            viewThemeUtils.material.colorMaterialButtonText(folderPickerBinding.folderPickerBtnCancel)
            folderPickerBinding.folderPickerBtnCancel.setOnClickListener { finish() }

            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(folderPickerBinding.folderPickerBtnChoose)
            folderPickerBinding.folderPickerBtnChoose.setOnClickListener { processOperation(null) }

            viewThemeUtils.material.colorMaterialButtonPrimaryFilled(folderPickerBinding.folderPickerBtnCopy)
            folderPickerBinding.folderPickerBtnCopy.setOnClickListener {
                processOperation(
                    OperationsService.ACTION_COPY_FILE
                )
            }

            viewThemeUtils.material.colorMaterialButtonPrimaryTonal(folderPickerBinding.folderPickerBtnMove)
            folderPickerBinding.folderPickerBtnMove.setOnClickListener {
                processOperation(
                    OperationsService.ACTION_MOVE_FILE
                )
            }
        }
    }

    private fun processOperation(action: String?) {
        val i = intent
        val resultData = Intent()
        resultData.putExtra(EXTRA_FOLDER, listOfFilesFragment?.currentFile)

        i.getParcelableArrayListExtra<Parcelable>(EXTRA_FILES)?.let { targetFiles ->
            resultData.putParcelableArrayListExtra(EXTRA_FILES, targetFiles)
        }

        targetFilePaths?.let { filePaths ->
            action?.let { action ->
                fileOperationsHelper.moveOrCopyFiles(action, filePaths, file)
            }

            resultData.putStringArrayListExtra(EXTRA_FILE_PATHS, filePaths)
        }

        setResult(RESULT_OK, resultData)
        finish()
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)
        if (operation is CreateFolderOperation) {
            onCreateFolderOperationFinish(operation, result)
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to create a new folder.
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private fun onCreateFolderOperationFinish(operation: CreateFolderOperation, result: RemoteOperationResult<*>) {
        if (result.isSuccess) {
            val fileListFragment = listOfFilesFragment
            fileListFragment?.onItemClicked(storageManager.getFileByPath(operation.remotePath))
        } else {
            try {
                DisplayUtils.showSnackMessage(
                    this,
                    ErrorMessageAdapter.getErrorCauseMessage(result, operation, resources)
                )
            } catch (e: Resources.NotFoundException) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e)
            }
        }
    }

    fun search(query: String?) {
        if (query == null) {
            return
        }

        listOfFilesFragment?.onMessageEvent(
            SearchEvent(
                query,
                SearchRemoteOperation.SearchType.FILE_SEARCH
            )
        )
    }

    private inner class SyncBroadcastReceiver : BroadcastReceiver() {
        /**
         * [BroadcastReceiver] to enable syncing feedback in UI
         */
        @Suppress(
            "Detekt.ComplexMethod",
            "Detekt.NestedBlockDepth",
            "Detekt.TooGenericExceptionCaught",
            "Detekt.LongMethod"
        ) // legacy code
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val event = intent.action
                Log_OC.d(TAG, "Received broadcast $event")
                val accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME)
                val syncFolderRemotePath = intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH)

                val syncResult = DataHolderUtil.getInstance()
                    .retrieve(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT)) as RemoteOperationResult<*>
                val sameAccount = (account != null && accountName == account.name && storageManager != null)

                if (!sameAccount) {
                    return
                }

                if (FileSyncAdapter.EVENT_FULL_SYNC_START == event) {
                    mSyncInProgress = true
                } else {
                    var (currentFile, currentDir) = getCurrentFileAndDirectory()

                    if (currentDir == null) {
                        browseRootForRemovedFolder()
                    } else {
                        if (currentFile == null && !file.isFolder) {
                            // currently selected file was removed in the server, and now we know it
                            currentFile = currentDir
                        }
                        if (currentDir.remotePath == syncFolderRemotePath) {
                            listOfFilesFragment?.listDirectory(currentDir, false, false)
                        }
                        file = currentFile
                    }

                    mSyncInProgress = (
                        FileSyncAdapter.EVENT_FULL_SYNC_END != event &&
                            RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED != event
                        )

                    checkCredentials(syncResult, context, event)
                }

                DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT))
                Log_OC.d(TAG, "Setting progress visibility to $mSyncInProgress")
                listOfFilesFragment?.isLoading = mSyncInProgress
                setBackgroundText()
            } catch (e: RuntimeException) {
                Log_OC.e(TAG, "Error on broadcast receiver", e)
                // avoid app crashes after changing the serial id of RemoteOperationResult
                // in owncloud library with broadcast notifications pending to process
                DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT))
            }
        }

        private fun getCurrentFileAndDirectory(): Pair<OCFile?, OCFile?> {
            val currentFile =
                if (file == null) null else storageManager.getFileByEncryptedRemotePath(file.remotePath)

            val currentDir = if (currentFolder == null) {
                null
            } else {
                storageManager.getFileByEncryptedRemotePath(
                    currentFolder?.remotePath
                )
            }

            return Pair(currentFile, currentDir)
        }

        private fun browseRootForRemovedFolder() {
            DisplayUtils.showSnackMessage(
                activity,
                R.string.sync_current_folder_was_removed,
                currentFolder?.fileName
            )
            browseToRoot()
        }

        private fun checkCredentials(syncResult: RemoteOperationResult<*>, context: Context, event: String?) {
            if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED == event && !syncResult.isSuccess
            ) {
                if (ResultCode.UNAUTHORIZED == syncResult.code || (
                        syncResult.isException &&
                            syncResult.exception is AuthenticatorException
                        )
                ) {
                    requestCredentialsUpdate(context)
                } else if (ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED == syncResult.code) {
                    showUntrustedCertDialog(syncResult)
                }
            }
        }
    }

    override fun showDetails(file: OCFile) {
        // not used at the moment
    }

    override fun showDetails(file: OCFile, activeTab: Int) {
        // not used at the moment
    }

    /**
     * {@inheritDoc}
     */
    override fun onTransferStateChanged(file: OCFile, downloading: Boolean, uploading: Boolean) {
        // not used at the moment
    }

    override fun onRefresh() {
        refreshList(true)
    }

    override fun onRefresh(enforced: Boolean) {
        refreshList(enforced)
    }

    private fun refreshList(ignoreETag: Boolean) {
        listOfFilesFragment?.currentFile?.let {
            startSyncFolderOperation(it, ignoreETag)
        }
    }

    override fun onSortingOrderChosen(selection: FileSortOrder?) {
        listOfFilesFragment?.sortFiles(selection)
    }

    companion object {
        @JvmField
        val EXTRA_FOLDER = FolderPickerActivity::class.java.canonicalName?.plus(".EXTRA_FOLDER")

        @JvmField
        @Deprecated(
            """This leads to crashes when too many files are passed. Use EXTRA_FILE_PATHS instead, or
      better yet, store the target files wherever you need to use them instead of passing them through this activity."""
        )
        val EXTRA_FILES = FolderPickerActivity::class.java.canonicalName?.plus(".EXTRA_FILES")

        @JvmField
        val EXTRA_FILE_PATHS = FolderPickerActivity::class.java.canonicalName?.plus(".EXTRA_FILE_PATHS")

        @JvmField
        val EXTRA_ACTION = FolderPickerActivity::class.java.canonicalName?.plus(".EXTRA_ACTION")

        const val MOVE_OR_COPY = "MOVE_OR_COPY"
        const val CHOOSE_LOCATION = "CHOOSE_LOCATION"
        private val TAG = FolderPickerActivity::class.java.simpleName

        const val TAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS"
    }
}
