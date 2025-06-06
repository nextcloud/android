/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.activity

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.FilesFolderPickerBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.albums.CreateNewAlbumRemoteOperation
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.ui.activity.FolderPickerActivity.Companion.TAG_LIST_OF_FOLDERS
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.albums.AlbumsFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ErrorMessageAdapter

class AlbumsPickerActivity : FileActivity(), FileFragment.ContainerActivity, OnEnforceableRefreshListener, Injectable {

    private var captionText: String? = null

    private var action: String? = null

    private lateinit var folderPickerBinding: FilesFolderPickerBinding

    private fun initBinding() {
        folderPickerBinding = FilesFolderPickerBinding.inflate(layoutInflater)
        setContentView(folderPickerBinding.root)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log_OC.d(TAG, "onCreate() start")

        super.onCreate(savedInstanceState)

        initBinding()
        setupToolbar()
        setupActionBar()
        setupAction()

        if (savedInstanceState == null) {
            createFragments()
        }

        updateActionBarTitleAndHomeButtonByString(captionText)
    }

    private fun setupActionBar() {
        findViewById<View>(R.id.sort_list_button_group).visibility =
            View.GONE
        findViewById<View>(R.id.switch_grid_view_button).visibility =
            View.GONE
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupAction() {
        action = intent.getStringExtra(EXTRA_ACTION)
        setupUIForChooseButton()
    }

    private fun setupUIForChooseButton() {
        if (action == CHOOSE_ALBUM) {
            captionText = resources.getText(R.string.album_picker_toolbar_title).toString()
        } else if (action == CHOOSE_MEDIA_FILES) {
            captionText = resources.getText(R.string.media_picker_toolbar_title).toString()
        }

        folderPickerBinding.folderPickerBtnCopy.visibility = View.GONE
        folderPickerBinding.folderPickerBtnMove.visibility = View.GONE
        folderPickerBinding.folderPickerBtnChoose.visibility = View.GONE
        folderPickerBinding.folderPickerBtnCancel.visibility = View.GONE
        folderPickerBinding.chooseButtonSpacer.visibility = View.GONE
        folderPickerBinding.moveOrCopyButtonSpacer.visibility = View.GONE
    }

    private fun createFragments() {
        if (action == CHOOSE_ALBUM) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(
                R.id.fragment_container,
                AlbumsFragment.newInstance(isSelectionMode = true),
                AlbumsFragment.TAG
            )
            transaction.commit()
        } else if (action == CHOOSE_MEDIA_FILES) {
            createGalleryFragment()
        }
    }

    private fun createGalleryFragment() {
        val photoFragment = GalleryFragment()
        val bundle = Bundle()
        bundle.putParcelable(
            OCFileListFragment.SEARCH_EVENT,
            SearchEvent("image/%", SearchRemoteOperation.SearchType.PHOTO_SEARCH)
        )
        bundle.putBoolean(EXTRA_FROM_ALBUM, true)
        photoFragment.arguments = bundle

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, photoFragment, TAG_LIST_OF_FOLDERS)
        transaction.commit()
    }

    private val listOfFilesFragment: AlbumsFragment?
        get() {
            val listOfFiles = supportFragmentManager.findFragmentByTag(AlbumsFragment.TAG)

            return if (listOfFiles != null) {
                return listOfFiles as AlbumsFragment?
            } else {
                Log_OC.e(TAG, "Access to non existing list of albums fragment!!")
                null
            }
        }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        super.onRemoteOperationFinish(operation, result)
        if (operation is CreateNewAlbumRemoteOperation) {
            onCreateAlbumOperationFinish(operation, result)
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to create a new folder.
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    @Suppress("MaxLineLength")
    private fun onCreateAlbumOperationFinish(
        operation: CreateNewAlbumRemoteOperation,
        result: RemoteOperationResult<*>
    ) {
        if (result.isSuccess) {
            val fileListFragment = listOfFilesFragment
            fileListFragment?.refreshAlbums()
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

    override fun showDetails(file: OCFile?) {
        // not used at the moment
    }

    override fun showDetails(file: OCFile?, activeTab: Int) {
        // not used at the moment
    }

    override fun onBrowsedDownTo(folder: OCFile?) {
        // not used at the moment
    }

    override fun onTransferStateChanged(file: OCFile?, downloading: Boolean, uploading: Boolean) {
        // not used at the moment
    }

    companion object {
        private val EXTRA_ACTION = AlbumsPickerActivity::class.java.canonicalName?.plus(".EXTRA_ACTION")
        private val CHOOSE_ALBUM = AlbumsPickerActivity::class.java.canonicalName?.plus(".CHOOSE_ALBUM")
        private val CHOOSE_MEDIA_FILES = AlbumsPickerActivity::class.java.canonicalName?.plus(".CHOOSE_MEDIA_FILES")
        val EXTRA_FROM_ALBUM = AlbumsPickerActivity::class.java.canonicalName?.plus(".EXTRA_FROM_ALBUM")
        val EXTRA_MEDIA_FILES_PATH = AlbumsPickerActivity::class.java.canonicalName?.plus(".EXTRA_MEDIA_FILES_PATH")

        private val TAG = AlbumsPickerActivity::class.java.simpleName

        fun intentForPickingAlbum(context: FragmentActivity): Intent {
            return Intent(context, AlbumsPickerActivity::class.java).apply {
                putExtra(EXTRA_ACTION, CHOOSE_ALBUM)
            }
        }

        fun intentForPickingMediaFiles(context: FragmentActivity): Intent {
            return Intent(context, AlbumsPickerActivity::class.java).apply {
                putExtra(EXTRA_ACTION, CHOOSE_MEDIA_FILES)
            }
        }
    }

    override fun onRefresh(enforced: Boolean) {
        // do nothing
    }

    override fun onRefresh() {
        // do nothing
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> super.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
