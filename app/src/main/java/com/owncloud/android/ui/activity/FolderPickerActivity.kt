/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.button.MaterialButton
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RefreshFolderOperation
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
import java.io.File
import javax.inject.Inject

@Suppress("Detekt.TooManyFunctions") // legacy code
open class FolderPickerActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    View.OnClickListener,
    OnEnforceableRefreshListener,
    Injectable,
    OnSortingOrderListener {

    private var mSyncBroadcastReceiver: SyncBroadcastReceiver? = null
    private var mSyncInProgress = false
    private var mSearchOnlyFolders = false
    var isDoNotEnterEncryptedFolder = false
        private set
    private var mCancelBtn: MaterialButton? = null
    private var mChooseBtn: MaterialButton? = null
    private var caption: String? = null

    private var mAction: String? = null
    private var mTargetFilePaths: ArrayList<String>? = null

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.d(TAG, "onCreate() start")
        super.onCreate(savedInstanceState)
        if (this is FilePickerActivity) {
            setContentView(R.layout.files_picker)
        } else {
            setContentView(R.layout.files_folder_picker)
        }

        // sets callback listeners for UI elements
        initControls()

        // Action bar setup
        setupToolbar()
        findViewById<View>(R.id.sort_list_button_group).visibility =
            View.VISIBLE
        findViewById<View>(R.id.switch_grid_view_button).visibility =
            View.GONE
        mAction = intent.getStringExtra(EXTRA_ACTION)
        if (mAction != null) {
            when (mAction) {
                MOVE -> {
                    caption = resources.getText(R.string.move_to).toString()
                    mSearchOnlyFolders = true
                    isDoNotEnterEncryptedFolder = true
                }
                COPY -> {
                    caption = resources.getText(R.string.copy_to).toString()
                    mSearchOnlyFolders = true
                    isDoNotEnterEncryptedFolder = true
                }
                CHOOSE_LOCATION -> {
                    caption = resources.getText(R.string.choose_location).toString()
                    mSearchOnlyFolders = true
                    isDoNotEnterEncryptedFolder = true
                    mChooseBtn!!.text = resources.getString(R.string.common_select)
                }
                else -> caption = themeUtils.getDefaultDisplayNameForRootFolder(this)
            }
        } else {
            caption = themeUtils.getDefaultDisplayNameForRootFolder(this)
        }
        mTargetFilePaths = intent.getStringArrayListExtra(EXTRA_FILE_PATHS)
        if (intent.getParcelableExtra<Parcelable?>(EXTRA_CURRENT_FOLDER) != null) {
            file = intent.getParcelableExtra(EXTRA_CURRENT_FOLDER)
        }
        if (savedInstanceState == null) {
            createFragments()
        }
        updateActionBarTitleAndHomeButtonByString(caption)

        // always AFTER setContentView(...) ; to work around bug in its implementation

        // sets message for empty list of folders
        setBackgroundText()
        Log_OC.d(TAG, "onCreate() end")
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        if (account != null) {
            updateFileFromDB()
            var folder = file
            if (folder == null || !folder.isFolder) {
                // fall back to root folder
                file = storageManager.getFileByPath(OCFile.ROOT_PATH)
                folder = file
            }
            val listOfFolders = listOfFilesFragment
            listOfFolders!!.listDirectory(folder, false, false)
            startSyncFolderOperation(folder, false)
            updateUiElements()
        }
    }

    private val activity: Activity
        get() = this

    protected open fun createFragments() {
        val listOfFiles = OCFileListFragment()
        val args = Bundle()
        args.putBoolean(OCFileListFragment.ARG_ONLY_FOLDERS_CLICKABLE, true)
        args.putBoolean(OCFileListFragment.ARG_HIDE_FAB, true)
        args.putBoolean(OCFileListFragment.ARG_HIDE_ITEM_OPTIONS, true)
        args.putBoolean(OCFileListFragment.ARG_SEARCH_ONLY_FOLDER, mSearchOnlyFolders)
        listOfFiles.arguments = args
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, listOfFiles, TAG_LIST_OF_FOLDERS)
        transaction.commit()
    }

    /**
     * Show a text message on screen view for notifying user if content is loading or folder is empty
     */
    private fun setBackgroundText() {
        val listFragment = listOfFilesFragment
        if (listFragment != null) {
            if (!mSyncInProgress) {
                listFragment.setMessageForEmptyList(
                    R.string.file_list_empty_headline,
                    R.string.file_list_empty_moving,
                    R.drawable.ic_list_empty_create_folder,
                    true
                )
            } else {
                listFragment.setEmptyListLoadingMessage()
            }
        } else {
            Log_OC.e(TAG, "OCFileListFragment is null")
        }
    }

    protected val listOfFilesFragment: OCFileListFragment?
        protected get() {
            val listOfFiles = supportFragmentManager.findFragmentByTag(TAG_LIST_OF_FOLDERS)
            if (listOfFiles != null) {
                return listOfFiles as OCFileListFragment?
            }
            Log_OC.e(TAG, "Access to non existing list of files fragment!!")
            return null
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
        // Sync Folder
        startSyncFolderOperation(directory, false)
    }

    override fun onSavedCertificate() {
        startSyncFolderOperation(currentDir, false)
    }

    private fun startSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean) {
        val currentSyncTime = System.currentTimeMillis()
        mSyncInProgress = true

        // perform folder synchronization
        val refreshFolderOperation: RemoteOperation<*> = RefreshFolderOperation(
            folder,
            currentSyncTime,
            false,
            ignoreETag,
            storageManager,
            user.orElseThrow { RuntimeException("User not set") },
            applicationContext
        )
        refreshFolderOperation.execute(account, this, null, null)
        listOfFilesFragment!!.isLoading = true
        setBackgroundText()
    }

    override fun onResume() {
        super.onResume()
        Log_OC.e(TAG, "onResume() start")
        listOfFilesFragment!!.isLoading = mSyncInProgress

        // refresh list of files
        refreshListOfFilesFragment(false)

        updateUiElements()

        // Listen for sync messages
        val syncIntentFilter = IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START)
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_END)
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED)
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED)
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED)
        mSyncBroadcastReceiver = SyncBroadcastReceiver()
        localBroadcastManager.registerReceiver(mSyncBroadcastReceiver!!, syncIntentFilter)
        Log_OC.d(TAG, "onResume() end")
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
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_folder_picker, menu)
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
            var finalFolder: OCFile? = null
            val storageManager = storageManager

            // If the file is null, take the root folder to avoid any error in functions depending on this one
            if (currentFile != null) {
                if (currentFile.isFolder) {
                    finalFolder = currentFile
                } else if (currentFile.remotePath != null) {
                    val parentPath = File(currentFile.remotePath).parent
                    finalFolder = storageManager.getFileByPath(parentPath)
                }
            } else {
                finalFolder = storageManager.getFileByPath(OCFile.ROOT_PATH)
            }
            return finalFolder
        }

    private fun refreshListOfFilesFragment(fromSearch: Boolean) {
        val fileListFragment = listOfFilesFragment
        fileListFragment?.listDirectory(false, fromSearch)
    }

    fun browseToRoot() {
        val listOfFiles = listOfFilesFragment
        if (listOfFiles != null) { // should never be null, indeed
            val root = storageManager.getFileByPath(OCFile.ROOT_PATH)
            listOfFiles.listDirectory(root, false, false)
            file = listOfFiles.currentFile
            updateUiElements()
            startSyncFolderOperation(root, false)
        }
    }

    override fun onBackPressed() {
        val listOfFiles = listOfFilesFragment
        if (listOfFiles != null) { // should never be null, indeed
            val levelsUp = listOfFiles.onBrowseUp()
            if (levelsUp == 0) {
                finish()
                return
            }
            file = listOfFiles.currentFile
            updateUiElements()
        }
    }

    private fun updateUiElements() {
        toggleChooseEnabled()
        updateNavigationElementsInActionBar()
    }

    private fun toggleChooseEnabled() {
        mChooseBtn?.isEnabled = checkFolderSelectable()
    }

    // for copy and move, disable selecting parent folder of target files
    private fun checkFolderSelectable(): Boolean {
        return when {
            mAction != COPY && mAction != MOVE -> true
            mTargetFilePaths.isNullOrEmpty() -> true
            file?.isFolder != true -> true
            // only disable if ALL target files are in selected folder
            else -> mTargetFilePaths!!.any { !isParentFolder(file.remotePath, it) }
        }
    }

    private fun isParentFolder(folderPath: String, filePath: String): Boolean {
        return folderPath == File(filePath).parent
    }

    private fun updateNavigationElementsInActionBar() {
        val currentDir = currentFolder
        val actionBar = supportActionBar
        if (actionBar != null) {
            val atRoot = currentDir == null || currentDir.parentId == 0L
            actionBar.setDisplayHomeAsUpEnabled(!atRoot)
            actionBar.setHomeButtonEnabled(!atRoot)
            val title = if (atRoot) caption ?: "" else currentDir!!.fileName
            viewThemeUtils.files.themeActionBar(this, actionBar, title)
        }
    }

    /**
     * Set per-view controllers
     */
    private fun initControls() {
        mCancelBtn = findViewById(R.id.folder_picker_btn_cancel)
        mChooseBtn = findViewById(R.id.folder_picker_btn_choose)
        if (mChooseBtn != null) {
            viewThemeUtils.material.colorMaterialButtonPrimaryFilled(mChooseBtn!!)
            mChooseBtn!!.setOnClickListener(this)
        }
        if (mCancelBtn != null) {
            if (this is FilePickerActivity) {
                viewThemeUtils.material.colorMaterialButtonPrimaryFilled(mCancelBtn!!)
            } else {
                viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(mCancelBtn!!)
            }
            mCancelBtn!!.setOnClickListener(this)
        }
    }

    override fun onClick(v: View) {
        if (v == mCancelBtn) {
            finish()
        } else if (v == mChooseBtn) {
            val i = intent
            val resultData = Intent()
            resultData.putExtra(EXTRA_FOLDER, listOfFilesFragment!!.currentFile)
            val targetFiles = i.getParcelableArrayListExtra<Parcelable>(EXTRA_FILES)
            if (targetFiles != null) {
                resultData.putParcelableArrayListExtra(EXTRA_FILES, targetFiles)
            }
            mTargetFilePaths.let {
                resultData.putStringArrayListExtra(EXTRA_FILE_PATHS, it)
            }
            setResult(RESULT_OK, resultData)
            finish()
        }
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
    private fun onCreateFolderOperationFinish(
        operation: CreateFolderOperation,
        result: RemoteOperationResult<*>
    ) {
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
        val fileListFragment = listOfFilesFragment
        fileListFragment?.onMessageEvent(
            SearchEvent(
                query!!,
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
                val sameAccount = account != null && accountName == account.name && storageManager != null
                if (sameAccount) {
                    if (FileSyncAdapter.EVENT_FULL_SYNC_START == event) {
                        mSyncInProgress = true
                    } else {
                        var currentFile = if (file == null) null else storageManager.getFileByPath(file.remotePath)
                        val currentDir = if (currentFolder == null) {
                            null
                        } else {
                            storageManager.getFileByPath(
                                currentFolder!!.remotePath
                            )
                        }
                        if (currentDir == null) {
                            // current folder was removed from the server
                            DisplayUtils.showSnackMessage(
                                activity,
                                R.string.sync_current_folder_was_removed,
                                currentFolder!!.fileName
                            )
                            browseToRoot()
                        } else {
                            if (currentFile == null && !file.isFolder) {
                                // currently selected file was removed in the server, and now we know it
                                currentFile = currentDir
                            }
                            if (currentDir.remotePath == syncFolderRemotePath) {
                                val fileListFragment = listOfFilesFragment
                                fileListFragment?.listDirectory(currentDir, false, false)
                            }
                            file = currentFile
                        }
                        mSyncInProgress = FileSyncAdapter.EVENT_FULL_SYNC_END != event &&
                            RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED != event
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
                    DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT))
                    Log_OC.d(TAG, "Setting progress visibility to $mSyncInProgress")
                    listOfFilesFragment!!.isLoading = mSyncInProgress
                    setBackgroundText()
                }
            } catch (e: RuntimeException) {
                Log_OC.e(TAG, "Error on broadcast receiver", e)
                // avoid app crashes after changing the serial id of RemoteOperationResult
                // in owncloud library with broadcast notifications pending to process
                DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT))
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
        val listOfFiles = listOfFilesFragment
        if (listOfFiles != null) {
            val folder = listOfFiles.currentFile
            folder?.let { startSyncFolderOperation(it, ignoreETag) }
        }
    }

    override fun onSortingOrderChosen(selection: FileSortOrder) {
        listOfFilesFragment!!.sortFiles(selection)
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

        @JvmField
        val EXTRA_CURRENT_FOLDER = FolderPickerActivity::class.java.canonicalName?.plus(".EXTRA_CURRENT_FOLDER")

        const val MOVE = "MOVE"
        const val COPY = "COPY"
        const val CHOOSE_LOCATION = "CHOOSE_LOCATION"
        private val TAG = FolderPickerActivity::class.java.simpleName
        protected const val TAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS"
    }
}
