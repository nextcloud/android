/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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

import android.accounts.Account
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.DialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadFilesLayoutBinding
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.StoragePathAdapter.StoragePathAdapterListener
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask.CheckAvailableSpaceListener
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog.Companion.newInstance
import com.owncloud.android.ui.dialog.LocalStoragePathPickerDialogFragment
import com.owncloud.android.ui.dialog.LocalStoragePathPickerDialogFragment.Companion.newInstance
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment.OnSortingOrderListener
import com.owncloud.android.ui.fragment.ExtendedListFragment
import com.owncloud.android.ui.fragment.LocalFileListFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.PermissionUtil.checkExternalStoragePermission
import com.owncloud.android.utils.PermissionUtil.requestExternalStoragePermission
import java.io.File
import javax.inject.Inject

/**
 * Displays local files and let the user choose what of them wants to upload to the current ownCloud account.
 */
@Suppress("TooManyFunctions")
class UploadFilesActivity :
    DrawerActivity(),
    LocalFileListFragment.ContainerActivity,
    View.OnClickListener,
    ConfirmationDialogFragmentListener,
    OnSortingOrderListener,
    CheckAvailableSpaceListener,
    StoragePathAdapterListener,
    Injectable {

    @JvmField
    @Inject
    var preferences: AppPreferences? = null

    private var mAccountOnCreation: Account? = null
    private var mDirectories: ArrayAdapter<String>? = null
    private var mLocalFolderPickerMode = false
    private var mSelectAll = false
    private var mCurrentDialog: DialogFragment? = null
    private var mCurrentDir: File? = null
    private var requestCode = 0

    @get:VisibleForTesting
    var fileListFragment: LocalFileListFragment? = null
        private set

    private var dialog: LocalStoragePathPickerDialogFragment? = null
    private var mOptionsMenu: Menu? = null
    private var mSearchView: SearchView? = null
    private lateinit var binding: UploadFilesLayoutBinding
    private var isWithinEncryptedFolder = false

    @SuppressLint("WrongViewCast") // wrong error on finding local_files_list
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.d(TAG, "onCreate() start")

        super.onCreate(savedInstanceState)

        getArguments()
        setupCurrentDirectory(savedInstanceState)
        mAccountOnCreation = account
        setupDirectoryDropdown()

        binding = UploadFilesLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (mLocalFolderPickerMode) {
            binding.uploadOptions.visibility = View.GONE
            binding.uploadFilesBtnUpload.setText(R.string.uploader_btn_alternative_text)
        }

        fileListFragment = supportFragmentManager.findFragmentByTag("local_files_list") as LocalFileListFragment?

        setupInputControllers()
        setupBehaviourAdapter()
        initToolbar()
        setupActionBar()
        showToolbarSpinner()
        setupToolbarSpinner()
        waitDialog()
        checkWritableFolder(mCurrentDir)

        Log_OC.d(TAG, "onCreate() end")
    }

    private fun getArguments() {
        intent.extras?.let {
            mLocalFolderPickerMode = it.getBoolean(KEY_LOCAL_FOLDER_PICKER_MODE, false)
            requestCode = it[REQUEST_CODE_KEY] as Int
            isWithinEncryptedFolder = it.getBoolean(ENCRYPTED_FOLDER_KEY, false)
        }
    }

    private fun setupCurrentDirectory(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mCurrentDir = File(
                savedInstanceState.getString(
                    KEY_DIRECTORY_PATH,
                    Environment.getExternalStorageDirectory().absolutePath
                )
            )
            mSelectAll = savedInstanceState.getBoolean(KEY_ALL_SELECTED, false)
            isWithinEncryptedFolder = savedInstanceState.getBoolean(ENCRYPTED_FOLDER_KEY, false)
        } else {
            val lastUploadFrom = preferences.uploadFromLocalLastPath
            if (lastUploadFrom.isNotEmpty()) {
                mCurrentDir = File(lastUploadFrom)
                while (mCurrentDir?.exists() == false) {
                    mCurrentDir = mCurrentDir?.parentFile
                }
            } else {
                mCurrentDir = Environment.getExternalStorageDirectory()
            }
        }
    }

    private fun setupDirectoryDropdown() {
        mDirectories = ArrayAdapter(this, android.R.layout.simple_spinner_item)
        mDirectories?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fillDirectoryDropdown()
    }

    private fun setupBehaviourAdapter() {
        val localBehaviour = preferences.uploaderBehaviour

        val behaviours: MutableList<String> = ArrayList()
        behaviours.add(
            getString(
                R.string.uploader_upload_files_behaviour_move_to_nextcloud_folder,
                themeUtils.getDefaultDisplayNameForRootFolder(this)
            )
        )
        behaviours.add(getString(R.string.uploader_upload_files_behaviour_only_upload))
        behaviours.add(getString(R.string.uploader_upload_files_behaviour_upload_and_delete_from_source))
        val behaviourAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            behaviours
        )
        behaviourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.uploadFilesSpinnerBehaviour.adapter = behaviourAdapter
        binding.uploadFilesSpinnerBehaviour.setSelection(localBehaviour)
    }

    private fun initToolbar() {
        setupToolbar()
        binding.uploadFilesToolbar.sortListButtonGroup.visibility = View.VISIBLE
        binding.uploadFilesToolbar.switchGridViewButton.visibility = View.GONE
    }

    private fun setupActionBar() {
        val actionBar = supportActionBar
        if (actionBar != null) {
            // mandatory since Android ICS, according to the official documentation
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(mCurrentDir != null)
            actionBar.setDisplayShowTitleEnabled(false)
            viewThemeUtils.files.themeActionBar(this, actionBar)
        }
    }

    private fun setupInputControllers() {
        viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(binding.uploadFilesBtnCancel)
        binding.uploadFilesBtnCancel.setOnClickListener(this)
        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(binding.uploadFilesBtnUpload)
        binding.uploadFilesBtnUpload.setOnClickListener(this)
        binding.uploadFilesBtnUpload.isEnabled = mLocalFolderPickerMode
    }

    private fun setupToolbarSpinner() {
        mToolbarSpinner.adapter = mDirectories
        mToolbarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                var i = position
                while (i-- != 0) {
                    onBackPressed()
                }
                // the next operation triggers a new call to this method, but it's necessary to
                // ensure that the name exposed in the action bar is the current directory when the
                // user selected it in the navigation list
                if (position != 0) {
                    mToolbarSpinner.setSelection(0)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // no action
            }
        }
    }

    private fun waitDialog() {
        mCurrentDialog?.let {
            it.dismiss()
            mCurrentDialog = null
        }
    }

    private fun requestPermissions() {
        requestExternalStoragePermission(this, viewThemeUtils, true)
    }

    fun showToolbarSpinner() {
        mToolbarSpinner.visibility = View.VISIBLE
    }

    private fun fillDirectoryDropdown() {
        var currentDir = mCurrentDir
        while (currentDir != null && currentDir.parentFile != null) {
            mDirectories?.add(currentDir.name)
            currentDir = currentDir.parentFile
        }
        mDirectories?.add(File.separator)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mOptionsMenu = menu
        menuInflater.inflate(R.menu.activity_upload_files, menu)

        if (!mLocalFolderPickerMode) {
            val selectAll = menu.findItem(R.id.action_select_all)
            setSelectAllMenuItem(selectAll, mSelectAll)
        }

        setupSearchView(menu)

        val drawable = menu.findItem(R.id.action_choose_storage_path).icon
        drawable?.let {
            viewThemeUtils.platform.tintDrawable(this, it, ColorRole.ON_SURFACE)
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun setupSearchView(menu: Menu) {
        val item = menu.findItem(R.id.action_search)
        mSearchView = MenuItemCompat.getActionView(item) as SearchView
        mSearchView?.let {
            viewThemeUtils.androidx.themeToolbarSearchView(it)
        }
        mSearchView?.setOnSearchClickListener { mToolbarSpinner.visibility = View.GONE }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        val itemId = item.itemId
        if (itemId == R.id.home) {
            if (mCurrentDir != null && mCurrentDir?.parentFile != null) {
                onBackPressed()
            }
        } else if (itemId == R.id.action_select_all) {
            mSelectAll = !item.isChecked
            item.isChecked = mSelectAll
            fileListFragment?.selectAllFiles(mSelectAll)
            setSelectAllMenuItem(item, mSelectAll)
        } else if (itemId == R.id.action_choose_storage_path) {
            checkLocalStoragePathPickerPermission()
        } else {
            retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    private fun checkLocalStoragePathPickerPermission() {
        if (!checkExternalStoragePermission(this)) {
            requestPermissions()
        } else {
            showLocalStoragePathPickerDialog()
        }
    }

    private fun showLocalStoragePathPickerDialog() {
        val fm = supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)
        dialog = newInstance()
        dialog?.show(ft, LocalStoragePathPickerDialogFragment.LOCAL_STORAGE_PATH_PICKER_FRAGMENT)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PermissionUtil.PERMISSIONS_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showLocalStoragePathPickerDialog()
            } else {
                DisplayUtils.showSnackMessage(this, R.string.permission_storage_access)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onSortingOrderChosen(selection: FileSortOrder?) {
        preferences.setSortOrder(FileSortOrder.Type.localFileListView, selection)
        fileListFragment?.sortFiles(selection)
    }

    private val isSearchOpen: Boolean
        get() = if (mSearchView == null) {
            false
        } else {
            val mSearchEditFrame = mSearchView?.findViewById<View>(androidx.appcompat.R.id.search_edit_frame)
            mSearchEditFrame != null && mSearchEditFrame.visibility == View.VISIBLE
        }

    override fun onBackPressed() {
        if (isSearchOpen && mSearchView != null) {
            mSearchView?.setQuery("", false)
            fileListFragment?.onClose()
            mSearchView?.onActionViewCollapsed()
            setDrawerIndicatorEnabled(isDrawerIndicatorAvailable)
        } else {
            if ((mDirectories?.count ?: 0) <= SINGLE_DIR) {
                finish()
                return
            }
            val parentFolder = mCurrentDir?.parentFile
            if (parentFolder?.canRead() == false) {
                checkLocalStoragePathPickerPermission()
                return
            }
            popDirname()
            fileListFragment?.onNavigateUp()
            mCurrentDir = fileListFragment?.currentDirectory
            checkWritableFolder(mCurrentDir)
            if (mCurrentDir?.parentFile == null) {
                val actionBar = supportActionBar
                actionBar?.setDisplayHomeAsUpEnabled(false)
            }

            // invalidate checked state when navigating directories
            if (!mLocalFolderPickerMode) {
                setSelectAllMenuItem(mOptionsMenu?.findItem(R.id.action_select_all), false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        Log_OC.d(TAG, "onSaveInstanceState() start")
        super.onSaveInstanceState(outState)
        outState.putString(KEY_DIRECTORY_PATH, mCurrentDir?.absolutePath)
        if (mOptionsMenu != null && mOptionsMenu?.findItem(R.id.action_select_all) != null) {
            outState.putBoolean(
                KEY_ALL_SELECTED,
                mOptionsMenu?.findItem(R.id.action_select_all)?.isChecked ?: false
            )
        } else {
            outState.putBoolean(KEY_ALL_SELECTED, false)
        }
        Log_OC.d(TAG, "onSaveInstanceState() end")
    }

    /**
     * Pushes a directory to the drop down list
     *
     * @param directory to push
     * @throws IllegalArgumentException If the [File.isDirectory] returns false.
     */
    private fun pushDirname(directory: File) {
        require(directory.isDirectory) { "Only directories may be pushed!" }
        mDirectories?.insert(directory.name, 0)
        mCurrentDir = directory
        checkWritableFolder(mCurrentDir)
    }

    /**
     * Pops a directory name from the drop down list
     *
     * @return True, unless the stack is empty
     */
    private fun popDirname(): Boolean {
        mDirectories?.remove(mDirectories?.getItem(0))
        return mDirectories?.isEmpty != false
    }

    private fun updateUploadButtonActive() {
        val anySelected = (fileListFragment?.checkedFilesCount ?: 0) > 0
        binding.uploadFilesBtnUpload.isEnabled = anySelected || mLocalFolderPickerMode
    }

    private fun setSelectAllMenuItem(selectAll: MenuItem?, checked: Boolean) {
        if (selectAll != null) {
            selectAll.isChecked = checked
            if (checked) {
                selectAll.setIcon(R.drawable.ic_select_none)
            } else {
                selectAll.icon = viewThemeUtils.platform.tintPrimaryDrawable(
                    this,
                    R.drawable.ic_select_all
                )
            }
            updateUploadButtonActive()
        }
    }

    override fun onCheckAvailableSpaceStart() {
        if (requestCode == FileDisplayActivity.REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM) {
            mCurrentDialog = newInstance(R.string.wait_a_moment, false)
            mCurrentDialog?.show(supportFragmentManager, WAIT_DIALOG_TAG)
        }
    }

    /**
     * Updates the activity UI after the check of space is done. If there is not space enough. shows a new dialog to
     * query the user if wants to move the files instead of copy them.
     *
     * @param hasEnoughSpaceAvailable 'True' when there is space enough to copy all the selected files.
     */
    override fun onCheckAvailableSpaceFinish(hasEnoughSpaceAvailable: Boolean, vararg filesToUpload: String) {
        waitDialog()

        if (hasEnoughSpaceAvailable) {
            // return the list of files (success)
            val data = Intent()
            if (requestCode == FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA) {
                data.putExtra(
                    EXTRA_CHOSEN_FILES,
                    arrayOf(
                        filesToUpload[0]
                    )
                )
                setResult(RESULT_OK_AND_DELETE, data)
                preferences.uploaderBehaviour = FileUploader.LOCAL_BEHAVIOUR_DELETE
            } else {
                data.putExtra(EXTRA_CHOSEN_FILES, fileListFragment?.checkedFilePaths)
                data.putExtra(LOCAL_BASE_PATH, mCurrentDir?.absolutePath)
                when (binding.uploadFilesSpinnerBehaviour.selectedItemPosition) {
                    0 -> setResult(RESULT_OK_AND_MOVE, data)
                    1 -> setResult(RESULT_OK_AND_DO_NOTHING, data)
                    2 -> setResult(RESULT_OK_AND_DELETE, data)
                    else -> {}
                }

                // store behaviour
                preferences.uploaderBehaviour = binding.uploadFilesSpinnerBehaviour.selectedItemPosition
            }
            finish()
        } else {
            // show a dialog to query the user if wants to move the selected files
            // to the ownCloud folder instead of copying
            val args = arrayOf<String?>(getString(R.string.app_name))
            val dialog = newInstance(
                R.string.upload_query_move_foreign_files,
                args,
                0,
                R.string.common_yes,
                R.string.common_no,
                -1
            )
            dialog.setOnConfirmationListener(this)
            dialog.show(supportFragmentManager, QUERY_TO_MOVE_DIALOG_TAG)
        }
    }

    override fun chosenPath(path: String) {
        if (listOfFilesFragment is LocalFileListFragment) {
            val file = File(path)
            (listOfFilesFragment as LocalFileListFragment?)?.listDirectory(file)
            onDirectoryClick(file)
            mCurrentDir = File(path)
            mDirectories?.clear()
            fillDirectoryDropdown()
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onDirectoryClick(directory: File) {
        if (!mLocalFolderPickerMode) {
            // invalidate checked state when navigating directories
            val selectAll = mOptionsMenu?.findItem(R.id.action_select_all)
            setSelectAllMenuItem(selectAll, false)
        }
        pushDirname(directory)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun checkWritableFolder(folder: File?) {
        folder?.let {
            val canWriteIntoFolder = it.canWrite()
            binding.uploadFilesSpinnerBehaviour.isEnabled = canWriteIntoFolder
            val textView = findViewById<TextView>(R.id.upload_files_upload_files_behaviour_text)
            if (canWriteIntoFolder) {
                textView.text = getString(R.string.uploader_upload_files_behaviour)
                val localBehaviour = preferences.uploaderBehaviour
                binding.uploadFilesSpinnerBehaviour.setSelection(localBehaviour)
            } else {
                binding.uploadFilesSpinnerBehaviour.setSelection(1)
                textView.text =
                    StringBuilder().append(getString(R.string.uploader_upload_files_behaviour))
                        .append(' ')
                        .append(getString(R.string.uploader_upload_files_behaviour_not_writable))
                        .toString()
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun onFileClick(file: File) {
        updateUploadButtonActive()
        val selectAll = fileListFragment?.checkedFilesCount == fileListFragment?.filesCount
        setSelectAllMenuItem(mOptionsMenu?.findItem(R.id.action_select_all), selectAll)
    }

    /**
     * {@inheritDoc}
     */
    override fun getInitialDirectory(): File {
        return mCurrentDir!!
    }

    /**
     * {@inheritDoc}
     */
    override fun isFolderPickerMode(): Boolean {
        return mLocalFolderPickerMode
    }

    override fun isWithinEncryptedFolder(): Boolean {
        return isWithinEncryptedFolder
    }

    /**
     * Performs corresponding action when user presses 'Cancel' or 'Upload' button
     *
     *
     * TODO Make here the real request to the Upload service ; will require to receive the account and target folder
     * where the upload must be done in the received intent.
     */
    @Suppress("NestedBlockDepth")
    override fun onClick(v: View) {
        if (v.id == R.id.upload_files_btn_cancel) {
            setResult(RESULT_CANCELED)
            finish()
        } else if (v.id == R.id.upload_files_btn_upload) {
            if (checkExternalStoragePermission(this)) {
                if (mCurrentDir != null) {
                    preferences.uploadFromLocalLastPath = mCurrentDir?.absolutePath
                }
                if (mLocalFolderPickerMode) {
                    val data = Intent()
                    if (mCurrentDir != null) {
                        data.putExtra(EXTRA_CHOSEN_FILES, mCurrentDir?.absolutePath)
                    }
                    setResult(RESULT_OK, data)
                    finish()
                } else {
                    @Suppress("SpreadOperator")
                    CheckAvailableSpaceTask(this, *fileListFragment?.checkedFilePaths)
                        .execute(binding.uploadFilesSpinnerBehaviour.selectedItemPosition == 0)
                }
            } else {
                requestPermissions()
            }
        }
    }

    override fun onConfirmation(callerTag: String?) {
        Log_OC.d(TAG, "Positive button in dialog was clicked; dialog tag is $callerTag")

        if (QUERY_TO_MOVE_DIALOG_TAG == callerTag) {
            // return the list of selected files to the caller activity (success),
            // signaling that they should be moved to the ownCloud folder, instead of copied
            val data = Intent()
            data.putExtra(EXTRA_CHOSEN_FILES, fileListFragment?.checkedFilePaths)
            data.putExtra(LOCAL_BASE_PATH, mCurrentDir?.absolutePath)
            setResult(RESULT_OK_AND_MOVE, data)
            finish()
        }
    }

    override fun onNeutral(callerTag: String?) {
        Log_OC.d(TAG, "Phantom neutral button in dialog was clicked; dialog tag is $callerTag")
    }

    override fun onCancel(callerTag: String?) {
        // / nothing to do; don't finish, let the user change the selection
        Log_OC.d(TAG, "Negative button in dialog was clicked; dialog tag is $callerTag")
    }

    override fun onStart() {
        super.onStart()
        val account = account
        if (mAccountOnCreation != null && mAccountOnCreation == account) {
            requestPermissions()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private val listOfFilesFragment: ExtendedListFragment?
        get() {
            if (fileListFragment == null) {
                Log_OC.e(TAG, "Access to unexisting list of files fragment")
            }
            return fileListFragment
        }

    override fun onStop() {
        dialog?.dismissAllowingStateLoss()
        super.onStop()
    }

    companion object {
        private val KEY_ALL_SELECTED = UploadFilesActivity::class.java.canonicalName?.plus(".KEY_ALL_SELECTED")
        val KEY_LOCAL_FOLDER_PICKER_MODE =
            UploadFilesActivity::class.java.canonicalName?.plus(".LOCAL_FOLDER_PICKER_MODE")

        @JvmField
        val LOCAL_BASE_PATH = UploadFilesActivity::class.java.canonicalName?.plus(".LOCAL_BASE_PATH")

        @JvmField
        val EXTRA_CHOSEN_FILES = UploadFilesActivity::class.java.canonicalName?.plus(".EXTRA_CHOSEN_FILES")
        val KEY_DIRECTORY_PATH = UploadFilesActivity::class.java.canonicalName?.plus(".KEY_DIRECTORY_PATH")

        private const val SINGLE_DIR = 1
        const val RESULT_OK_AND_DELETE = 3
        const val RESULT_OK_AND_DO_NOTHING = 2
        const val RESULT_OK_AND_MOVE = RESULT_FIRST_USER
        const val REQUEST_CODE_KEY = "requestCode"
        private const val ENCRYPTED_FOLDER_KEY = "encrypted_folder"
        private const val QUERY_TO_MOVE_DIALOG_TAG = "QUERY_TO_MOVE"
        private const val TAG = "UploadFilesActivity"
        private const val WAIT_DIALOG_TAG = "WAIT"

        /**
         * Helper to launch the UploadFilesActivity for which you would like a result when it finished. Your
         * onActivityResult() method will be called with the given requestCode.
         *
         * @param activity    the activity which should call the upload activity for a result
         * @param user        the user for which the upload activity is called
         * @param requestCode If >= 0, this code will be returned in onActivityResult()
         */
        @JvmStatic
        fun startUploadActivityForResult(
            activity: Activity,
            user: User?,
            requestCode: Int,
            isWithinEncryptedFolder: Boolean
        ) {
            val action = Intent(activity, UploadFilesActivity::class.java)
            action.putExtra(FileActivity.EXTRA_USER, user)
            action.putExtra(REQUEST_CODE_KEY, requestCode)
            action.putExtra(ENCRYPTED_FOLDER_KEY, isWithinEncryptedFolder)
            activity.startActivityForResult(action, requestCode)
        }
    }
}
