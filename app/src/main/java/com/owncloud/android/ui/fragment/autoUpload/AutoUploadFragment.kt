/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.ui.component.UploadWarningCard
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentAutoUploadBinding
import com.owncloud.android.databinding.StoragePermissionWarningBannerBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.ui.adapter.SyncedFolderAdapter
import com.owncloud.android.ui.adapter.storagePermissionBanner.setup
import com.owncloud.android.ui.decoration.MediaGridItemDecoration
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment.OnSyncedFolderPreferenceListener
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable
import com.owncloud.android.ui.fragment.autoUpload.data.AutoUploadDisplayItemFactory
import com.owncloud.android.ui.fragment.autoUpload.data.AutoUploadItemsLoader
import com.owncloud.android.ui.fragment.autoUpload.data.AutoUploadPersistence
import com.owncloud.android.ui.fragment.autoUpload.handler.AutoUploadClickHandler
import com.owncloud.android.ui.fragment.autoUpload.handler.AutoUploadPickers
import com.owncloud.android.ui.fragment.autoUpload.handler.AutoUploadIntentHandler
import com.owncloud.android.ui.fragment.autoUpload.ui.AutoUploadDialogs
import com.owncloud.android.ui.fragment.autoUpload.ui.AutoUploadEmptyContent
import com.owncloud.android.ui.fragment.autoUpload.ui.AutoUploadMenuProvider
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ThemeUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
class AutoUploadFragment :
    Fragment(),
    OnSyncedFolderPreferenceListener {

    @Inject lateinit var powerManagementService: PowerManagementService

    @Inject lateinit var clock: Clock

    @Inject lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject lateinit var userAccountManager: UserAccountManager

    @Inject lateinit var backgroundJobManager: BackgroundJobManager

    @Inject lateinit var connectivityService: ConnectivityService

    @Inject lateinit var viewThemeUtils: ViewThemeUtils

    @Inject lateinit var themeUtils: ThemeUtils

    lateinit var adapter: SyncedFolderAdapter

    private var binding: FragmentAutoUploadBinding? = null
    private var uploadWarningCard: UploadWarningCard? = null
    private var emptyContent: AutoUploadEmptyContent? = null
    private lateinit var itemsLoader: AutoUploadItemsLoader
    private lateinit var dialogs: AutoUploadDialogs
    private lateinit var clickHandler: AutoUploadClickHandler

    private val intentHandler = AutoUploadIntentHandler()

    private val folderPickers = AutoUploadPickers(this) {
        childFragmentManager.findFragmentByTag(AutoUploadClickHandler.SYNCED_FOLDER_PREFERENCES_DIALOG_TAG)
            as? SyncedFolderPreferencesDialogFragment
    }

    private val navigatorActivity: NavigatorActivity?
        get() = getTypedActivity(NavigatorActivity::class.java)

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentHandler.readMediaFolder(activity?.intent)
        val activity = navigatorActivity ?: return
        if (savedInstanceState == null) {
            intentHandler.handleNotification(activity, userAccountManager)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentAutoUploadBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = navigatorActivity ?: return
        val binding = binding ?: return

        setupHelpers(activity)
        PermissionUtil.requestStoragePermissionIfNeeded(activity)
        setupList(activity, binding)
        if (themeUtils.themingEnabled(activity)) {
            activity.theme.applyStyle(R.style.FallbackThemingTheme, true)
        }
        addMenuProvider(activity)
    }

    override fun onResume() {
        super.onResume()
        val activity = navigatorActivity ?: return
        val binding = binding ?: return

        setupStoragePermissionWarningBanner(activity, binding)
        PermissionUtil.dismissStoragePermissionDialogFragment(activity)
        if (PermissionUtil.checkStoragePermission(activity)) {
            load(activity)
        }
        uploadWarningCard?.bind(binding.autoUploadBatterySaverWarningCard)
    }

    override fun onDestroyView() {
        uploadWarningCard?.unregister(requireContext())
        uploadWarningCard = null
        emptyContent = null
        super.onDestroyView()
        binding = null
    }
    // endregion

    // region OnSyncedFolderPreferenceListener
    override fun onSaveSyncedFolderPreference(syncedFolder: SyncedFolderParcelable?) {
        clickHandler.onSaveAutoUploadFolderPreference(syncedFolder)
    }

    override fun onCancelSyncedFolderPreference() = Unit

    override fun onDeleteSyncedFolderPreference(syncedFolder: SyncedFolderParcelable?) {
        clickHandler.onDeleteAutoUploadFolderPreference(syncedFolder)
    }

    override fun onRemoteFolderPickerRequested() {
        folderPickers.pickRemoteFolder()
    }

    override fun onLocalFolderPickerRequested() {
        folderPickers.pickLocalFolder()
    }
    // endregion

    @VisibleForTesting
    fun buildPowerCheckDialog(): AlertDialog = dialogs.buildPowerCheckDialog()

    @VisibleForTesting
    fun showPowerCheckDialog() {
        buildPowerCheckDialog().show()
    }

    // region Setup
    private fun setupHelpers(activity: NavigatorActivity) {
        val itemFactory = AutoUploadDisplayItemFactory(
            clock,
            getItemsDisplayedPerFolder(),
            getString(R.string.instant_upload_path)
        )
        val persistence = AutoUploadPersistence(syncedFolderProvider, backgroundJobManager)
        itemsLoader = AutoUploadItemsLoader(syncedFolderProvider, itemFactory)
        dialogs = AutoUploadDialogs(this, viewThemeUtils, syncedFolderProvider, backgroundJobManager)
        clickHandler = AutoUploadClickHandler(
            this,
            itemFactory,
            persistence,
            dialogs,
            backgroundJobManager,
            getAdapter = { adapter },
            onItemVisibilityChanged = { emptyContent?.checkAndShowEmptyListContent() }
        )
        uploadWarningCard = UploadWarningCard(
            activity,
            powerManagementService,
            syncedFolderProvider,
            backgroundJobManager,
            lifecycleScope,
            viewThemeUtils
        )
    }

    private fun setupList(activity: NavigatorActivity, binding: FragmentAutoUploadBinding) {
        val gridWidth = resources.getInteger(R.integer.media_grid_width)
        adapter = SyncedFolderAdapter(
            lifecycleScope,
            activity,
            clock,
            gridWidth,
            clickHandler,
            resources.getBoolean(R.bool.syncedFolder_light),
            viewThemeUtils,
            powerManagementService,
            connectivityService
        )
        uploadWarningCard?.register(activity, binding.autoUploadBatterySaverWarningCard)
        emptyContent = AutoUploadEmptyContent(binding, adapter, viewThemeUtils)

        val layoutManager = GridLayoutManager(activity, gridWidth)
        adapter.setLayoutManager(layoutManager)
        binding.list.run {
            addItemDecoration(MediaGridItemDecoration(resources.getDimensionPixelSize(R.dimen.media_grid_spacing)))
            this.layoutManager = layoutManager
            adapter = this@AutoUploadFragment.adapter
        }
    }

    private fun addMenuProvider(activity: NavigatorActivity) {
        val menuProvider = AutoUploadMenuProvider(
            onCreateCustomFolder = { onCreateCustomFolderClick() },
            onDeleteAllUploaded = { onDeleteAllUploadedClick() }
        )
        activity.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupStoragePermissionWarningBanner(activity: NavigatorActivity, binding: FragmentAutoUploadBinding) {
        val banner = binding.storagePermissionWarningBanner.root
        StoragePermissionWarningBannerBinding.bind(banner).setup(
            activity,
            R.string.storage_permission_banner_auto_upload_text
        )
        banner.setVisibleIf(!PermissionUtil.checkStoragePermission(activity))
    }
    // endregion

    // region Menu Actions
    private fun onCreateCustomFolderClick() {
        val activity = navigatorActivity ?: return
        clickHandler.onCreateCustomFolderClick(activity, activity.account.name)
    }

    private fun onDeleteAllUploadedClick() {
        val user = navigatorActivity?.user?.orElse(null) ?: return
        clickHandler.onDeleteAllUploadedClick(user)
    }
    // endregion

    // region Loading
    private fun getItemsDisplayedPerFolder(): Int = resources.getInteger(R.integer.media_grid_width) * 2

    @SuppressLint("NotifyDataSetChanged")
    private fun load(activity: NavigatorActivity) {
        if (adapter.itemCount > 0) {
            return
        }

        val perFolderMediaItemLimit = getItemsDisplayedPerFolder()
        val newItemAccountName = activity.account.name
        emptyContent?.showLoadingContent()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val currentAccountName = userAccountManager.user.accountName
            val items = itemsLoader.load(activity, perFolderMediaItemLimit, currentAccountName, newItemAccountName)

            withContext(Dispatchers.Main) {
                adapter.setSyncFolderItems(items)
                adapter.notifyDataSetChanged()
                emptyContent?.showList()
                openMediaFolderSettingsFromIntent()
            }
        }
    }

    private fun openMediaFolderSettingsFromIntent() {
        val section = intentHandler.findMediaFolderSection(adapter) ?: return
        adapter.get(section)?.let { clickHandler.onSyncFolderSettingsClick(section, it) }
    }
    // endregion
}
