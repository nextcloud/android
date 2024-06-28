/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.MediaFoldersDetectionWork
import com.nextcloud.client.jobs.NotificationWork
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.isDialogFragmentReady
import com.owncloud.android.BuildConfig
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.databinding.SyncedFoldersLayoutBinding
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.MediaFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.MediaProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.SyncedFolderAdapter
import com.owncloud.android.ui.decoration.MediaGridItemDecoration
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment.OnSyncedFolderPreferenceListener
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.SyncedFolderUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Activity displaying all auto-synced folders and/or instant upload media folders.
 */
@Suppress("TooManyFunctions", "LargeClass")
class SyncedFoldersActivity :
    FileActivity(),
    SyncedFolderAdapter.ClickListener,
    OnSyncedFolderPreferenceListener,
    Injectable {

    companion object {
        private const val SYNCED_FOLDER_PREFERENCES_DIALOG_TAG = "SYNCED_FOLDER_PREFERENCES_DIALOG"

        // yes, there is a typo in this value
        private const val KEY_SYNCED_FOLDER_INITIATED_PREFIX = "syncedFolderIntitiated_"
        private val PRIORITIZED_FOLDERS = arrayOf("Camera", "Screenshots")
        private val TAG = SyncedFoldersActivity::class.java.simpleName

        /**
         * Sorts list of [SyncedFolderDisplayItem]s.
         *
         * @param syncFolderItemList list of items to be sorted
         * @return sorted list of items
         */
        @JvmStatic
        @Suppress("ComplexMethod")
        fun sortSyncedFolderItems(syncFolderItemList: List<SyncedFolderDisplayItem?>): List<SyncedFolderDisplayItem?> {
            return syncFolderItemList.sortedWith { f1, f2 ->
                if (f1 == null && f2 == null) {
                    0
                } else if (f1 == null) {
                    -1
                } else if (f2 == null) {
                    1
                } else if (f1.isEnabled && f2.isEnabled) {
                    when {
                        f1.folderName == null -> -1
                        f2.folderName == null -> 1
                        else -> f1.folderName.lowercase(Locale.getDefault()).compareTo(
                            f2.folderName.lowercase(Locale.getDefault())
                        )
                    }
                } else if (f1.folderName == null && f2.folderName == null) {
                    0
                } else if (f1.isEnabled) {
                    -1
                } else if (f2.isEnabled) {
                    1
                } else if (f1.folderName == null) {
                    -1
                } else if (f2.folderName == null) {
                    1
                } else {
                    for (folder in PRIORITIZED_FOLDERS) {
                        if (folder == f1.folderName && folder == f2.folderName) {
                            return@sortedWith 0
                        } else if (folder == f1.folderName) {
                            return@sortedWith -1
                        } else if (folder == f2.folderName) {
                            return@sortedWith 1
                        }
                    }
                    f1.folderName.lowercase(Locale.getDefault()).compareTo(
                        f2.folderName.lowercase(Locale.getDefault())
                    )
                }
            }
        }
    }

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var powerManagementService: PowerManagementService

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var syncedFolderProvider: SyncedFolderProvider

    lateinit var binding: SyncedFoldersLayoutBinding
    lateinit var adapter: SyncedFolderAdapter

    private var dialogFragment: SyncedFolderPreferencesDialogFragment? = null
    private var path: String? = null
    private var type = 0
    private var loadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SyncedFoldersLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (intent != null && intent.extras != null) {
            val accountName = intent.extras!!.getString(NotificationWork.KEY_NOTIFICATION_ACCOUNT)
            val optionalUser = user
            if (optionalUser.isPresent && accountName != null) {
                val user = optionalUser.get()
                if (!accountName.equals(user.accountName, ignoreCase = true)) {
                    accountManager.setCurrentOwnCloudAccount(accountName)
                    setUser(userAccountManager.user)
                }
            }
            path = intent.getStringExtra(MediaFoldersDetectionWork.KEY_MEDIA_FOLDER_PATH)
            type = intent.getIntExtra(MediaFoldersDetectionWork.KEY_MEDIA_FOLDER_TYPE, -1)

            // Cancel notification
            val notificationId = intent.getIntExtra(MediaFoldersDetectionWork.NOTIFICATION_ID, 0)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }

        // setup toolbar
        setupToolbar()
        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_synced_folders))
        setupDrawer()
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        if (mDrawerToggle != null) {
            mDrawerToggle.isDrawerIndicatorEnabled = false
        }

        setupContent()
        if (themeUtils.themingEnabled(this)) {
            setTheme(R.style.FallbackThemingTheme)
        }
        binding.emptyList.emptyListViewAction.setOnClickListener { showHiddenItems() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_synced_folders, menu)
        if (powerManagementService.isPowerSavingExclusionAvailable) {
            val item = menu.findItem(R.id.action_disable_power_save_check)
            item.isVisible = true
            item.isChecked = preferences.isPowerCheckDisabled
            item.setOnMenuItemClickListener { powerCheck -> onDisablePowerSaveCheckClicked(powerCheck) }
        }
        return true
    }

    private fun onDisablePowerSaveCheckClicked(powerCheck: MenuItem): Boolean {
        if (!powerCheck.isChecked) {
            showPowerCheckDialog()
        }
        preferences.isPowerCheckDisabled = !powerCheck.isChecked
        powerCheck.isChecked = !powerCheck.isChecked
        return true
    }

    private fun showPowerCheckDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setView(R.id.root_layout)
            .setPositiveButton(R.string.common_ok) { dialog, _ -> dialog.dismiss() }
            .setTitle(R.string.autoupload_disable_power_save_check)
            .setMessage(getString(R.string.power_save_check_dialog_message))
            .show()

        viewThemeUtils.platform.colorTextButtons(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE))
    }

    /**
     * sets up the UI elements and loads all media/synced folders.
     */
    private fun setupContent() {
        val gridWidth = resources.getInteger(R.integer.media_grid_width)
        val lightVersion = resources.getBoolean(R.bool.syncedFolder_light)
        adapter = SyncedFolderAdapter(
            this,
            clock,
            gridWidth,
            this,
            lightVersion,
            viewThemeUtils
        )
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.nav_synced_folders)
        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(binding.emptyList.emptyListViewAction)
        val lm = GridLayoutManager(this, gridWidth)
        adapter.setLayoutManager(lm)
        val spacing = resources.getDimensionPixelSize(R.dimen.media_grid_spacing)
        binding.list.addItemDecoration(MediaGridItemDecoration(spacing))
        binding.list.layoutManager = lm
        binding.list.adapter = adapter
        load(getItemsDisplayedPerFolder(), false)
    }

    private fun showHiddenItems() {
        if (adapter.sectionCount == 0 && adapter.unfilteredSectionCount > adapter.sectionCount) {
            adapter.toggleHiddenItemsVisibility()
            binding.emptyList.emptyListView.visibility = View.GONE
            binding.list.visibility = View.VISIBLE
        }
    }

    /**
     * loads all media/synced folders, adds them to the recycler view adapter and shows the list.
     *
     * @param perFolderMediaItemLimit the amount of media items to be loaded/shown per media folder
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun load(perFolderMediaItemLimit: Int, force: Boolean) {
        if (adapter.itemCount > 0 && !force) {
            return
        }
        showLoadingContent()
        loadJob = CoroutineScope(Dispatchers.IO).launch {
            loadJob?.cancel()
            val mediaFolders = MediaProvider.getImageFolders(
                contentResolver,
                perFolderMediaItemLimit,
                this@SyncedFoldersActivity,
                false,
                viewThemeUtils
            )
            mediaFolders.addAll(
                MediaProvider.getVideoFolders(
                    contentResolver,
                    perFolderMediaItemLimit,
                    this@SyncedFoldersActivity,
                    false,
                    viewThemeUtils
                )
            )
            val syncedFolderArrayList = syncedFolderProvider.syncedFolders
            val currentAccountSyncedFoldersList: MutableList<SyncedFolder> = ArrayList()
            val user = userAccountManager.user
            for (syncedFolder in syncedFolderArrayList) {
                if (syncedFolder.account == user.accountName) {
                    // delete non-existing & disabled synced folders
                    if (!File(syncedFolder.localPath).exists() && !syncedFolder.isEnabled) {
                        syncedFolderProvider.deleteSyncedFolder(syncedFolder.id)
                    } else {
                        currentAccountSyncedFoldersList.add(syncedFolder)
                    }
                }
            }
            val syncFolderItems = sortSyncedFolderItems(
                mergeFolderData(currentAccountSyncedFoldersList, mediaFolders)
            ).filterNotNull()

            CoroutineScope(Dispatchers.Main).launch {
                adapter.setSyncFolderItems(syncFolderItems)
                adapter.notifyDataSetChanged()
                showList()
                if (!TextUtils.isEmpty(path)) {
                    val section = adapter.getSectionByLocalPathAndType(path, type)
                    if (section >= 0) {
                        adapter.get(section)?.let {
                            onSyncFolderSettingsClick(section, it)
                        }
                    }
                }
                loadJob = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loadJob?.cancel()
    }

    /**
     * merges two lists of [SyncedFolder] and [MediaFolder] items into one of SyncedFolderItems.
     *
     * @param syncedFolders the synced folders
     * @param mediaFolders  the media folders
     * @return the merged list of SyncedFolderItems
     */
    @Suppress("NestedBlockDepth") // legacy code
    private fun mergeFolderData(
        syncedFolders: List<SyncedFolder>,
        mediaFolders: List<MediaFolder>
    ): List<SyncedFolderDisplayItem?> {
        val syncedFoldersMap = createSyncedFoldersMap(syncedFolders)
        val result: MutableList<SyncedFolderDisplayItem?> = ArrayList()
        for (mediaFolder in mediaFolders) {
            if (syncedFoldersMap.containsKey(mediaFolder.absolutePath + "-" + mediaFolder.type)) {
                val syncedFolder = syncedFoldersMap[mediaFolder.absolutePath + "-" + mediaFolder.type]
                syncedFoldersMap.remove(mediaFolder.absolutePath + "-" + mediaFolder.type)
                if (syncedFolder != null && SyncedFolderUtils.isQualifyingMediaFolder(syncedFolder)) {
                    if (MediaFolderType.CUSTOM == syncedFolder.type) {
                        result.add(createSyncedFolderWithoutMediaFolder(syncedFolder))
                    } else {
                        result.add(createSyncedFolder(syncedFolder, mediaFolder))
                    }
                }
            } else {
                if (SyncedFolderUtils.isQualifyingMediaFolder(mediaFolder)) {
                    result.add(createSyncedFolderFromMediaFolder(mediaFolder))
                }
            }
        }
        for (syncedFolder in syncedFoldersMap.values) {
            result.add(createSyncedFolderWithoutMediaFolder(syncedFolder))
        }
        return result
    }

    private fun createSyncedFolderWithoutMediaFolder(syncedFolder: SyncedFolder): SyncedFolderDisplayItem {
        val localFolder = File(syncedFolder.localPath)
        val files = SyncedFolderUtils.getFileList(localFolder)
        val filePaths = getDisplayFilePathList(files)
        return SyncedFolderDisplayItem(
            syncedFolder.id,
            syncedFolder.localPath,
            syncedFolder.remotePath,
            syncedFolder.isWifiOnly,
            syncedFolder.isChargingOnly,
            syncedFolder.isExisting,
            syncedFolder.isSubfolderByDate,
            syncedFolder.account,
            syncedFolder.uploadAction,
            syncedFolder.nameCollisionPolicyInt,
            syncedFolder.isEnabled,
            clock.currentTime,
            filePaths,
            localFolder.name,
            files.size.toLong(),
            syncedFolder.type,
            syncedFolder.isHidden,
            syncedFolder.subfolderRule,
            syncedFolder.isExcludeHidden,
            syncedFolder.lastScanTimestampMs
        )
    }

    /**
     * creates a SyncedFolderDisplayItem merging a [SyncedFolder] and a [MediaFolder] object instance.
     *
     * @param syncedFolder the synced folder object
     * @param mediaFolder  the media folder object
     * @return the created SyncedFolderDisplayItem
     */
    private fun createSyncedFolder(syncedFolder: SyncedFolder, mediaFolder: MediaFolder): SyncedFolderDisplayItem {
        return SyncedFolderDisplayItem(
            syncedFolder.id,
            syncedFolder.localPath,
            syncedFolder.remotePath,
            syncedFolder.isWifiOnly,
            syncedFolder.isChargingOnly,
            syncedFolder.isExisting,
            syncedFolder.isSubfolderByDate,
            syncedFolder.account,
            syncedFolder.uploadAction,
            syncedFolder.nameCollisionPolicyInt,
            syncedFolder.isEnabled,
            clock.currentTime,
            mediaFolder.filePaths,
            mediaFolder.folderName,
            mediaFolder.numberOfFiles,
            mediaFolder.type,
            syncedFolder.isHidden,
            syncedFolder.subfolderRule,
            syncedFolder.isExcludeHidden,
            syncedFolder.lastScanTimestampMs
        )
    }

    /**
     * creates a [SyncedFolderDisplayItem] based on a [MediaFolder] object instance.
     *
     * @param mediaFolder the media folder object
     * @return the created SyncedFolderDisplayItem
     */
    private fun createSyncedFolderFromMediaFolder(mediaFolder: MediaFolder): SyncedFolderDisplayItem {
        return SyncedFolderDisplayItem(
            SyncedFolder.UNPERSISTED_ID,
            mediaFolder.absolutePath,
            getString(R.string.instant_upload_path) + "/" + mediaFolder.folderName,
            true,
            false,
            true,
            false,
            account.name,
            FileUploadWorker.LOCAL_BEHAVIOUR_FORGET,
            NameCollisionPolicy.ASK_USER.serialize(),
            false,
            clock.currentTime,
            mediaFolder.filePaths,
            mediaFolder.folderName,
            mediaFolder.numberOfFiles,
            mediaFolder.type,
            false,
            SubFolderRule.YEAR_MONTH,
            false,
            SyncedFolder.NOT_SCANNED_YET
        )
    }

    private fun getItemsDisplayedPerFolder(): Int {
        return resources.getInteger(R.integer.media_grid_width) * 2
    }

    private fun getDisplayFilePathList(files: List<File>?): List<String>? {
        if (!files.isNullOrEmpty()) {
            return files.take(getItemsDisplayedPerFolder())
                .map { it.absolutePath }
        }
        return null
    }

    /**
     * creates a lookup map for a list of given [SyncedFolder]s with their local path as the key.
     *
     * @param syncFolders list of [SyncedFolder]s
     * @return the lookup map for [SyncedFolder]s
     */
    private fun createSyncedFoldersMap(syncFolders: List<SyncedFolder>?): MutableMap<String, SyncedFolder> {
        val result: MutableMap<String, SyncedFolder> = HashMap()
        if (syncFolders != null) {
            for (syncFolder in syncFolders) {
                result[syncFolder.localPath + "-" + syncFolder.type] = syncFolder
            }
        }
        return result
    }

    /**
     * show recycler view list or the empty message info (in case list is empty).
     */
    private fun showList() {
        binding.list.visibility = View.VISIBLE
        binding.loadingContent.visibility = View.GONE
        checkAndShowEmptyListContent()
    }

    private fun checkAndShowEmptyListContent() {
        if (adapter.sectionCount == 0 && adapter.unfilteredSectionCount > adapter.sectionCount) {
            binding.emptyList.emptyListView.visibility = View.VISIBLE
            val hiddenFoldersCount = adapter.hiddenFolderCount
            showEmptyContent(
                getString(R.string.drawer_synced_folders),
                resources.getQuantityString(
                    R.plurals.synced_folders_show_hidden_folders,
                    hiddenFoldersCount,
                    hiddenFoldersCount
                ),
                resources.getQuantityString(
                    R.plurals.synced_folders_show_hidden_folders,
                    hiddenFoldersCount,
                    hiddenFoldersCount
                )
            )
        } else if (adapter.sectionCount == 0 && adapter.unfilteredSectionCount == 0) {
            binding.emptyList.emptyListView.visibility = View.VISIBLE
            showEmptyContent(
                getString(R.string.drawer_synced_folders),
                getString(R.string.synced_folders_no_results)
            )
        } else {
            binding.emptyList.emptyListView.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var result = true
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_create_custom_folder -> {
                Log_OC.d(TAG, "Show custom folder dialog")
                if (PermissionUtil.checkExternalStoragePermission(this)) {
                    val emptyCustomFolder = SyncedFolderDisplayItem(
                        SyncedFolder.UNPERSISTED_ID,
                        null,
                        null,
                        true,
                        false,
                        true,
                        false,
                        account.name,
                        FileUploadWorker.LOCAL_BEHAVIOUR_FORGET,
                        NameCollisionPolicy.ASK_USER.serialize(),
                        false,
                        clock.currentTime,
                        null,
                        MediaFolderType.CUSTOM,
                        false,
                        SubFolderRule.YEAR_MONTH,
                        false,
                        SyncedFolder.NOT_SCANNED_YET
                    )
                    onSyncFolderSettingsClick(0, emptyCustomFolder)
                } else {
                    PermissionUtil.requestExternalStoragePermission(this, viewThemeUtils, true)
                }
                result = super.onOptionsItemSelected(item)
            }
            else -> result = super.onOptionsItemSelected(item)
        }
        return result
    }

    override fun onSyncStatusToggleClick(section: Int, syncedFolderDisplayItem: SyncedFolderDisplayItem?) {
        if (syncedFolderDisplayItem == null) return

        if (syncedFolderDisplayItem.id > SyncedFolder.UNPERSISTED_ID) {
            syncedFolderProvider.updateSyncedFolderEnabled(
                syncedFolderDisplayItem.id,
                syncedFolderDisplayItem.isEnabled
            )
        } else {
            val storedId = syncedFolderProvider.storeSyncedFolder(syncedFolderDisplayItem)
            if (storedId != -1L) {
                syncedFolderDisplayItem.id = storedId
            }
        }
        if (syncedFolderDisplayItem.isEnabled) {
            backgroundJobManager.startImmediateFilesSyncJob(syncedFolderDisplayItem.id, overridePowerSaving = false)
            showBatteryOptimizationInfo()
        }
    }

    override fun onSyncFolderSettingsClick(section: Int, syncedFolderDisplayItem: SyncedFolderDisplayItem?) {
        val fragmentTransaction = supportFragmentManager.beginTransaction().apply {
            addToBackStack(null)
        }

        dialogFragment = SyncedFolderPreferencesDialogFragment.newInstance(
            syncedFolderDisplayItem,
            section
        )

        dialogFragment?.let {
            if (isDialogFragmentReady(it)) {
                it.show(fragmentTransaction, SYNCED_FOLDER_PREFERENCES_DIALOG_TAG)
            } else {
                Log_OC.d(TAG, "SyncedFolderPreferencesDialogFragment not ready")
            }
        }
    }

    override fun onVisibilityToggleClick(section: Int, syncedFolder: SyncedFolderDisplayItem?) {
        if (syncedFolder == null) return

        syncedFolder.isHidden = !syncedFolder.isHidden
        saveOrUpdateSyncedFolder(syncedFolder)
        adapter.setSyncFolderItem(section, syncedFolder)
        checkAndShowEmptyListContent()
    }

    private fun showEmptyContent(headline: String, message: String, action: String) {
        showEmptyContent(headline, message)
        binding.emptyList.emptyListViewAction.text = action
        binding.emptyList.emptyListViewAction.visibility = View.VISIBLE
        binding.emptyList.emptyListViewText.visibility = View.GONE
    }

    private fun showLoadingContent() {
        binding.loadingContent.visibility = View.VISIBLE
        binding.emptyList.emptyListViewAction.visibility = View.GONE
    }

    private fun showEmptyContent(headline: String, message: String) {
        binding.emptyList.emptyListViewAction.visibility = View.GONE
        binding.emptyList.emptyListView.visibility = View.VISIBLE
        binding.list.visibility = View.GONE
        binding.loadingContent.visibility = View.GONE
        binding.emptyList.emptyListViewHeadline.text = headline
        binding.emptyList.emptyListViewText.text = message
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE
        binding.emptyList.emptyListIcon.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SyncedFolderPreferencesDialogFragment.REQUEST_CODE__SELECT_REMOTE_FOLDER &&
            resultCode == RESULT_OK && dialogFragment != null
        ) {
            val chosenFolder: OCFile? = FolderPickerActivity.EXTRA_FOLDER?.let {
                data?.getParcelableArgument(it, OCFile::class.java)
            }
            dialogFragment?.setRemoteFolderSummary(chosenFolder?.remotePath)
        } else if (
            requestCode == SyncedFolderPreferencesDialogFragment.REQUEST_CODE__SELECT_LOCAL_FOLDER &&
            resultCode == RESULT_OK && dialogFragment != null
        ) {
            val localPath = data!!.getStringExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES)
            dialogFragment!!.setLocalFolderSummary(localPath)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveSyncedFolderPreference(syncedFolder: SyncedFolderParcelable?) {
        if (syncedFolder == null) {
            return
        }

        // custom folders newly created aren't in the list already,
        // so triggering a refresh
        if (MediaFolderType.CUSTOM == syncedFolder.type && syncedFolder.id == SyncedFolder.UNPERSISTED_ID) {
            val newCustomFolder = SyncedFolderDisplayItem(
                SyncedFolder.UNPERSISTED_ID,
                syncedFolder.localPath,
                syncedFolder.remotePath,
                syncedFolder.isWifiOnly,
                syncedFolder.isChargingOnly,
                syncedFolder.isExisting,
                syncedFolder.isSubfolderByDate,
                syncedFolder.account,
                syncedFolder.uploadAction,
                syncedFolder.nameCollisionPolicy.serialize(),
                syncedFolder.isEnabled,
                clock.currentTime,
                File(syncedFolder.localPath).name,
                syncedFolder.type,
                syncedFolder.isHidden,
                syncedFolder.subFolderRule,
                syncedFolder.isExcludeHidden,
                SyncedFolder.NOT_SCANNED_YET
            )
            saveOrUpdateSyncedFolder(newCustomFolder)
            adapter.addSyncFolderItem(newCustomFolder)
        } else {
            val item = adapter.get(syncedFolder.section) ?: return
            updateSyncedFolderItem(
                item,
                syncedFolder.id,
                syncedFolder.localPath,
                syncedFolder.remotePath,
                syncedFolder.isWifiOnly,
                syncedFolder.isChargingOnly,
                syncedFolder.isExisting,
                syncedFolder.isSubfolderByDate,
                syncedFolder.uploadAction,
                syncedFolder.nameCollisionPolicy.serialize(),
                syncedFolder.isEnabled,
                syncedFolder.subFolderRule,
                syncedFolder.isExcludeHidden
            )
            saveOrUpdateSyncedFolder(item)

            adapter.notifyItemChanged(adapter.getSectionHeaderIndex(syncedFolder.section))
        }
        dialogFragment = null
        if (syncedFolder.isEnabled) {
            showBatteryOptimizationInfo()
        }
    }

    private fun saveOrUpdateSyncedFolder(item: SyncedFolderDisplayItem) {
        if (item.id == SyncedFolder.UNPERSISTED_ID) {
            // newly set up folder sync config
            storeSyncedFolder(item)
        } else {
            // existing synced folder setup to be updated
            syncedFolderProvider.updateSyncFolder(item)
            if (item.isEnabled) {
                backgroundJobManager.startImmediateFilesSyncJob(item.id, overridePowerSaving = false)
            } else {
                val syncedFolderInitiatedKey = KEY_SYNCED_FOLDER_INITIATED_PREFIX + item.id
                val arbitraryDataProvider =
                    ArbitraryDataProviderImpl(MainApp.getAppContext())
                arbitraryDataProvider.deleteKeyForAccount("global", syncedFolderInitiatedKey)
            }
        }
    }

    private fun storeSyncedFolder(item: SyncedFolderDisplayItem) {
        val arbitraryDataProvider =
            ArbitraryDataProviderImpl(MainApp.getAppContext())
        val storedId = syncedFolderProvider.storeSyncedFolder(item)
        if (storedId != -1L) {
            item.id = storedId
            if (item.isEnabled) {
                backgroundJobManager.startImmediateFilesSyncJob(item.id, overridePowerSaving = false)
            } else {
                val syncedFolderInitiatedKey = KEY_SYNCED_FOLDER_INITIATED_PREFIX + item.id
                arbitraryDataProvider.deleteKeyForAccount("global", syncedFolderInitiatedKey)
            }
        }
    }

    override fun onCancelSyncedFolderPreference() {
        dialogFragment = null
    }

    override fun onDeleteSyncedFolderPreference(syncedFolder: SyncedFolderParcelable?) {
        if (syncedFolder == null) {
            return
        }

        syncedFolderProvider.deleteSyncedFolder(syncedFolder.id)
        adapter.removeItem(syncedFolder.section)
    }

    /**
     * update given synced folder with the given values.
     *
     * @param item            the synced folder to be updated
     * @param localPath       the local path
     * @param remotePath      the remote path
     * @param wifiOnly        upload on wifi only
     * @param chargingOnly    upload on charging only
     * @param existing        also upload existing
     * @param subfolderByDate created sub folders
     * @param uploadAction    upload action
     * @param nameCollisionPolicy what to do on name collision
     * @param enabled         is sync enabled
     * @param excludeHidden   exclude hidden file or folder, for {@link MediaFolderType#CUSTOM} only
     */
    @Suppress("LongParameterList")
    private fun updateSyncedFolderItem(
        item: SyncedFolderDisplayItem,
        id: Long,
        localPath: String,
        remotePath: String,
        wifiOnly: Boolean,
        chargingOnly: Boolean,
        existing: Boolean,
        subfolderByDate: Boolean,
        uploadAction: Int,
        nameCollisionPolicy: Int,
        enabled: Boolean,
        subFolderRule: SubFolderRule,
        excludeHidden: Boolean
    ) {
        item.id = id
        item.localPath = localPath
        item.remotePath = remotePath
        item.isWifiOnly = wifiOnly
        item.isChargingOnly = chargingOnly
        item.isExisting = existing
        item.isSubfolderByDate = subfolderByDate
        item.uploadAction = uploadAction
        item.setNameCollisionPolicy(nameCollisionPolicy)
        item.setEnabled(enabled, clock.currentTime)
        item.setSubFolderRule(subFolderRule)
        item.setExcludeHidden(excludeHidden)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PermissionUtil.PERMISSIONS_EXTERNAL_STORAGE -> {
                // If request is cancelled, result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    load(getItemsDisplayedPerFolder(), true)
                } else {
                    // permission denied --> request again
                    PermissionUtil.requestExternalStoragePermission(this, viewThemeUtils, true)
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showBatteryOptimizationInfo() {
        if (powerManagementService.isPowerSavingExclusionAvailable || checkIfBatteryOptimizationEnabled()) {
            val alertDialogBuilder = AlertDialog.Builder(this, R.style.Theme_ownCloud_Dialog)
                .setTitle(getString(R.string.battery_optimization_title))
                .setMessage(getString(R.string.battery_optimization_message))
                .setPositiveButton(getString(R.string.battery_optimization_disable)) { _, _ ->
                    // show instant upload
                    @SuppressLint("BatteryLife")
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                    )
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                }
                .setNeutralButton(getString(R.string.battery_optimization_close)) { dialog, _ -> dialog.dismiss() }
                .setIcon(R.drawable.ic_battery_alert)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val alertDialog = alertDialogBuilder.show()
                viewThemeUtils.platform.colorTextButtons(
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                )
            }
        }
    }

    /**
     * Check if battery optimization is enabled. If unknown, fallback to true.
     *
     * @return true if battery optimization is enabled
     */
    private fun checkIfBatteryOptimizationEnabled(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager?
        return when {
            powerManager != null -> !powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
            else -> true
        }
    }
}
