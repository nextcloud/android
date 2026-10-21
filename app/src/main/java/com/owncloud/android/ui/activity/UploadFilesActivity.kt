/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2020-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Joris Bodin <joris.bodin@infomaniak.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.core.Clock
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.utils.extensions.hasEnabledParent
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadFilesLayoutBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.uploadfiles.UploadBehaviourSelector
import com.owncloud.android.ui.activity.uploadfiles.UploadFilesDialogs
import com.owncloud.android.ui.activity.uploadfiles.UploadFilesDialogs.Companion.QUERY_TO_MOVE_DIALOG_TAG
import com.owncloud.android.ui.adapter.StoragePathAdapter.StoragePathAdapterListener
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask.CheckAvailableSpaceListener
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment.OnSortingOrderListener
import com.owncloud.android.ui.fragment.localfilelist.LocalFileListFragment
import com.owncloud.android.ui.fragment.localfilelist.LocalFileListListener
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.PermissionUtil.checkStoragePermission
import java.io.File
import javax.inject.Inject

class UploadFilesActivity :
    DrawerActivity(),
    LocalFileListListener,
    View.OnClickListener,
    ConfirmationDialogFragmentListener,
    OnSortingOrderListener,
    CheckAvailableSpaceListener,
    StoragePathAdapterListener,
    Injectable {

    @Inject
    lateinit var clock: Clock

    lateinit var fileListFragment: LocalFileListFragment
        private set

    override var isFolderPickerMode = false
        private set

    override var isWithinEncryptedFolder = false
        private set

    override val initialDirectory: File?
        get() = currentDir

    private lateinit var binding: UploadFilesLayoutBinding
    private lateinit var directories: ArrayAdapter<String>
    private lateinit var uploadBehaviour: UploadBehaviourSelector
    private val dialogs = UploadFilesDialogs(this)

    private var currentDir: File? = null
    private var accountOnCreation: Account? = null
    private var requestCode = 0
    private var isAllSelected = false
    private var optionsMenu: Menu? = null
    private var searchView: SearchView? = null
    private var chosenFilePaths: Array<String>? = null

    private val isRoot: Boolean
        get() = currentDir?.absolutePath == ROOT_DIR

    private val isSearchOpen: Boolean
        get() = searchView?.findViewById<View>(androidx.appcompat.R.id.search_edit_frame)?.visibility == View.VISIBLE

    private val selectAllMenuItem: MenuItem?
        get() = optionsMenu?.findItem(R.id.action_select_all)

    private val isGivenLocalPathHasEnabledParent: Boolean
        get() {
            val chosenPath = currentDir?.path ?: return false
            return SyncedFolderProvider(contentResolver, preferences, clock).syncedFolders.hasEnabledParent(chosenPath)
        }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isSearchOpen) closeSearch() else navigateToParentDirectory()
        }
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            optionsMenu = menu
            menuInflater.inflate(R.menu.activity_upload_files, menu)

            if (!isFolderPickerMode) {
                setSelectAllMenuItem(menu.findItem(R.id.action_select_all), isAllSelected)
            }

            searchView = (menu.findItem(R.id.action_search).actionView as SearchView).also {
                viewThemeUtils.androidx.themeToolbarSearchView(it)
                it.setOnSearchClickListener { mToolbarSpinner.visibility = View.GONE }
            }

            menu.findItem(R.id.action_choose_storage_path)?.let { item ->
                item.isVisible = checkStoragePermission(this@UploadFilesActivity)
                item.icon?.let {
                    viewThemeUtils.platform.tintDrawable(this@UploadFilesActivity, it, ColorRole.ON_SURFACE)
                }
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
            android.R.id.home -> {
                handleHomePressed()
                true
            }

            R.id.action_select_all -> {
                isAllSelected = !menuItem.isChecked
                menuItem.isChecked = isAllSelected
                fileListFragment.selectAllFiles(isAllSelected)
                setSelectAllMenuItem(menuItem, isAllSelected)
                true
            }

            R.id.action_choose_storage_path -> {
                if (checkStoragePermission(this@UploadFilesActivity)) {
                    dialogs.showStoragePathPicker()
                } else {
                    cancelAndFinish()
                }
                true
            }

            else -> false
        }
    }

    @SuppressLint("WrongViewCast") // wrong error on finding local_files_list
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.d(TAG, "onCreate() start")
        super.onCreate(savedInstanceState)
        restoreState(savedInstanceState)
        accountOnCreation = account

        directories = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            addDirectoryHierarchy(currentDir)
        }

        binding = UploadFilesLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fileListFragment = supportFragmentManager.findFragmentByTag(LOCAL_FILES_LIST_TAG) as LocalFileListFragment

        setupButtons()
        uploadBehaviour = UploadBehaviourSelector(this, binding, preferences).apply {
            setup(themeUtils.getDefaultDisplayNameForRootFolder(this@UploadFilesActivity))
        }
        setupToolbar()
        setupActionBar()
        setupToolbarSpinner()
        uploadBehaviour.checkWritableFolder(currentDir)
        addMenuProvider(menuProvider, this)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        Log_OC.d(TAG, "onCreate() end")
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        intent.extras?.let { extras ->
            isFolderPickerMode = extras.getBoolean(KEY_LOCAL_FOLDER_PICKER_MODE, false)
            requestCode = extras.getInt(REQUEST_CODE_KEY)
            isWithinEncryptedFolder = extras.getBoolean(ENCRYPTED_FOLDER_KEY, false)
        }

        if (savedInstanceState != null) {
            val defaultPath = Environment.getExternalStorageDirectory().absolutePath
            currentDir = File(savedInstanceState.getString(KEY_DIRECTORY_PATH, defaultPath))
            isAllSelected = savedInstanceState.getBoolean(KEY_ALL_SELECTED, false)
            isWithinEncryptedFolder = savedInstanceState.getBoolean(ENCRYPTED_FOLDER_KEY, false)
            return
        }

        val lastUploadFrom = preferences.uploadFromLocalLastPath
        currentDir = if (lastUploadFrom.isEmpty()) {
            Environment.getExternalStorageDirectory()
        } else {
            generateSequence(File(lastUploadFrom)) { it.parentFile }.first { it.exists() }
        }
    }

    private fun setupButtons() {
        if (isFolderPickerMode) {
            binding.uploadOptions.visibility = View.GONE
            binding.uploadFilesBtnUpload.setText(R.string.uploader_btn_alternative_text)
        }

        viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(binding.uploadFilesBtnCancel)
        binding.uploadFilesBtnCancel.setOnClickListener(this)

        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(binding.uploadFilesBtnUpload)
        binding.uploadFilesBtnUpload.setOnClickListener(this)
        binding.uploadFilesBtnUpload.isEnabled = isFolderPickerMode
    }

    private fun setupActionBar() {
        binding.uploadFilesToolbar.sortListButtonGroup.visibility = View.VISIBLE
        binding.uploadFilesToolbar.switchGridViewButton.visibility = View.GONE

        supportActionBar?.let { actionBar ->
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(currentDir != null)
            actionBar.setDisplayShowTitleEnabled(false)
            viewThemeUtils.files.themeActionBar(this, actionBar)
        }
    }

    private fun setupToolbarSpinner() {
        mToolbarSpinner.visibility = View.VISIBLE
        mToolbarSpinner.adapter = directories
        mToolbarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                repeat(position) { onBackPressedDispatcher.onBackPressed() }
                if (position != 0) {
                    mToolbarSpinner.setSelection(0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }

    private fun cancelAndFinish() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun handleHomePressed() {
        val isRoot = isRoot
        if (isRoot && !checkStoragePermission(this)) {
            cancelAndFinish()
            return
        }

        if (currentDir?.parentFile == null) return

        if (isRoot) cancelAndFinish() else onBackPressedDispatcher.onBackPressed()
    }

    override fun onSortingOrderChosen(selection: FileSortOrder?) {
        val sortOrder = selection ?: return
        preferences.setSortOrder(FileSortOrder.Type.localFileListView, sortOrder)
        fileListFragment.sortFiles(sortOrder)
    }

    private fun closeSearch() {
        val searchView = searchView ?: return
        searchView.setQuery("", false)
        fileListFragment.onClose()
        searchView.onActionViewCollapsed()
        setDrawerIndicatorEnabled(isDrawerIndicatorAvailable)
    }

    private fun navigateToParentDirectory() {
        if (directories.count <= SINGLE_DIR) {
            finish()
            return
        }

        val isParentUnreadable = currentDir?.parentFile?.canRead() == false
        if (currentDir == null || isParentUnreadable) return

        directories.remove(directories.getItem(0))
        fileListFragment.onNavigateUp()
        currentDir = fileListFragment.currentDirectory
        uploadBehaviour.checkWritableFolder(currentDir)

        if (currentDir?.parentFile == null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
        }

        if (!isFolderPickerMode) {
            setSelectAllMenuItem(selectAllMenuItem, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentDir?.let { outState.putString(KEY_DIRECTORY_PATH, it.absolutePath) }
        outState.putBoolean(KEY_ALL_SELECTED, optionsMenu?.findItem(R.id.action_select_all)?.isChecked == true)
        Log_OC.d(TAG, "onSaveInstanceState() end")
    }

    private fun updateUploadButtonActive() {
        binding.uploadFilesBtnUpload.isEnabled = fileListFragment.checkedFilesCount > 0 || isFolderPickerMode
    }

    private fun setSelectAllMenuItem(selectAll: MenuItem?, checked: Boolean) {
        val item = selectAll ?: return
        item.isChecked = checked
        if (checked) {
            item.setIcon(R.drawable.ic_select_none)
        } else {
            item.icon = viewThemeUtils.platform.tintDrawable(this, R.drawable.ic_select_all, ColorRole.PRIMARY)
        }
        updateUploadButtonActive()
    }

    override fun onCheckAvailableSpaceStart() {
        if (requestCode == FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM) {
            dialogs.showWaitDialog()
        }
    }

    override fun onCheckAvailableSpaceFinish(hasEnoughSpaceAvailable: Boolean, vararg filesToUpload: String) {
        dialogs.dismissWaitDialog()

        if (!hasEnoughSpaceAvailable) {
            dialogs.showQueryToMoveDialog(this)
            return
        }

        val data = Intent()
        if (requestCode == FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA) {
            data.putExtra(EXTRA_CHOSEN_FILES, arrayOf(filesToUpload[0]))
            setResult(RESULT_OK_AND_DELETE, data)
            preferences.uploaderBehaviour = FileUploadWorker.LOCAL_BEHAVIOUR_DELETE
        } else {
            data.putExtra(EXTRA_CHOSEN_FILES, filesToUpload)
            data.putExtra(LOCAL_BASE_PATH, currentDir?.absolutePath)
            uploadBehaviour.resultCode?.let { setResult(it, data) }
            uploadBehaviour.saveSelection()
        }
        finish()
    }

    override fun chosenPath(path: String) {
        val directory = File(path)
        fileListFragment.listDirectory(directory)
        onDirectoryClick(directory)

        currentDir = directory
        directories.clear()
        directories.addDirectoryHierarchy(currentDir)
    }

    override fun onDirectoryClick(directory: File?) {
        if (directory == null) return

        if (!isFolderPickerMode) {
            setSelectAllMenuItem(selectAllMenuItem, false)
        }

        require(directory.isDirectory) { "Only directories may be pushed!" }
        directories.insert(directory.name, 0)
        currentDir = directory
        uploadBehaviour.checkWritableFolder(currentDir)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onFileClick(file: File?) {
        updateUploadButtonActive()
        val isAllChecked = fileListFragment.checkedFilesCount == fileListFragment.filesCount
        setSelectAllMenuItem(selectAllMenuItem, isAllChecked)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.upload_files_btn_cancel -> cancelAndFinish()
            R.id.upload_files_btn_upload -> if (checkStoragePermission(this)) upload()
        }
    }

    @Suppress("SpreadOperator", "DEPRECATION")
    private fun upload() {
        currentDir?.let { preferences.uploadFromLocalLastPath = it.absolutePath }

        if (isFolderPickerMode) {
            finishWithChosenFolder()
            return
        }

        fileListFragment.collectCheckedFilePaths { chosenFiles ->
            if (chosenFiles.size > FileUploadHelper.MAX_FILE_COUNT) {
                FileUploadHelper.instance().showFileUploadLimitMessage(this)
                return@collectCheckedFilePaths
            }

            chosenFilePaths = chosenFiles
            CheckAvailableSpaceTask(this, *chosenFiles).execute(uploadBehaviour.isMoveSelected)
        }
    }

    private fun finishWithChosenFolder() {
        val data = Intent()
        currentDir?.let { data.putExtra(EXTRA_CHOSEN_FILES, it.absolutePath) }
        setResult(RESULT_OK, data)

        if (isGivenLocalPathHasEnabledParent) {
            dialogs.showSubFolderWarningDialog { finish() }
        } else {
            finish()
        }
    }

    override fun onConfirmation(callerTag: String?) {
        Log_OC.d(TAG, "Positive button in dialog was clicked; dialog tag is $callerTag")

        val chosenFilePaths = chosenFilePaths
        if (callerTag != QUERY_TO_MOVE_DIALOG_TAG || chosenFilePaths == null) return

        val data = Intent().apply {
            putExtra(EXTRA_CHOSEN_FILES, chosenFilePaths)
            putExtra(LOCAL_BASE_PATH, currentDir?.absolutePath)
        }
        setResult(RESULT_OK_AND_MOVE, data)
        finish()
    }

    override fun onNeutral(callerTag: String?) {
        Log_OC.d(TAG, "Phantom neutral button in dialog was clicked; dialog tag is $callerTag")
    }

    override fun onCancel(callerTag: String?) {
        Log_OC.d(TAG, "Negative button in dialog was clicked; dialog tag is $callerTag")
    }

    override fun onStart() {
        super.onStart()
        val accountOnCreation = accountOnCreation
        if (accountOnCreation == null || accountOnCreation != account) {
            cancelAndFinish()
        }
    }

    override fun onStop() {
        dialogs.dismissStoragePathPicker()
        super.onStop()
    }

    fun setupStoragePermissionWarningBanner() {
        fileListFragment.setupStoragePermissionWarningBanner()
    }

    private fun ArrayAdapter<String>.addDirectoryHierarchy(directory: File?) {
        generateSequence(directory) { it.parentFile }
            .takeWhile { it.parentFile != null }
            .forEach { add(it.name) }
        add(File.separator)
    }

    companion object {
        private const val PREFIX = "com.owncloud.android.ui.activity.UploadFilesActivity."
        private const val KEY_ALL_SELECTED = PREFIX + "KEY_ALL_SELECTED"
        const val KEY_LOCAL_FOLDER_PICKER_MODE = PREFIX + "LOCAL_FOLDER_PICKER_MODE"
        const val LOCAL_BASE_PATH = PREFIX + "LOCAL_BASE_PATH"
        const val EXTRA_CHOSEN_FILES = PREFIX + "EXTRA_CHOSEN_FILES"
        const val KEY_DIRECTORY_PATH = PREFIX + "KEY_DIRECTORY_PATH"

        const val RESULT_OK_AND_DELETE = 3
        const val RESULT_OK_AND_DO_NOTHING = 2
        const val RESULT_OK_AND_MOVE = RESULT_FIRST_USER
        const val REQUEST_CODE_KEY = "requestCode"

        private const val SINGLE_DIR = 1
        private const val ENCRYPTED_FOLDER_KEY = "encrypted_folder"
        private const val LOCAL_FILES_LIST_TAG = "local_files_list"
        private const val ROOT_DIR = "/storage/emulated/0"
        private const val TAG = "UploadFilesActivity"

        @JvmStatic
        fun startUploadActivityForResult(
            activity: Activity,
            user: User?,
            requestCode: Int,
            isWithinEncryptedFolder: Boolean
        ) {
            val intent = Intent(activity, UploadFilesActivity::class.java).apply {
                putExtra(FileActivity.EXTRA_USER, user)
                putExtra(REQUEST_CODE_KEY, requestCode)
                putExtra(ENCRYPTED_FOLDER_KEY, isWithinEncryptedFolder)
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }
}
