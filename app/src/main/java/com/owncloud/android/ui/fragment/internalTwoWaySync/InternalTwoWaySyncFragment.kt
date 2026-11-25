/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.internalTwoWaySync

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadWorker
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.hourPlural
import com.nextcloud.utils.extensions.minPlural
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentInternalTwoWaySyncBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.adapter.InternalTwoWaySyncAdapter
import com.owncloud.android.ui.fragment.internalTwoWaySync.ui.InternalTwoWaySyncMenuProvider
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class InternalTwoWaySyncFragment :
    Fragment(),
    InternalTwoWaySyncAdapter.InternalTwoWaySyncAdapterOnUpdate {

    companion object {
        private const val TAG = "InternalTwoWaySyncFragment"
    }

    @Inject lateinit var backgroundJobManager: BackgroundJobManager

    @Inject lateinit var preferences: AppPreferences

    @Inject lateinit var fileDataStorageManager: FileDataStorageManager

    @Inject lateinit var viewThemeUtils: ViewThemeUtils

    private var binding: FragmentInternalTwoWaySyncBinding? = null
    private var internalTwoWaySyncAdapter: InternalTwoWaySyncAdapter? = null
    private var disableForAllFoldersMenuButton: MenuItem? = null

    private val user: User?
        get() = getTypedActivity(NavigatorActivity::class.java)?.user?.orElse(null)

    // region Lifecycle
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentInternalTwoWaySyncBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = user ?: return

        internalTwoWaySyncAdapter =
            InternalTwoWaySyncAdapter(fileDataStorageManager, user, requireContext(), this, viewThemeUtils)

        setupTwoWaySyncAdapter()
        setupEmptyList()
        setupTwoWaySyncToggle()
        setupTwoWaySyncInterval()
        checkLayoutVisibilities(preferences.isTwoWaySyncEnabled)
        addMenuProvider()
    }

    override fun onDestroyView() {
        disableForAllFoldersMenuButton = null
        internalTwoWaySyncAdapter = null
        super.onDestroyView()
        binding = null
    }
    // endregion

    // region Setup
    @SuppressLint("NotifyDataSetChanged")
    private fun setupTwoWaySyncAdapter() {
        if (!preferences.isTwoWaySyncEnabled) return
        val binding = binding ?: return

        binding.list.run {
            setEmptyView(binding.emptyList.emptyListView)
            adapter = internalTwoWaySyncAdapter
            layoutManager = LinearLayoutManager(requireContext())
            adapter?.notifyDataSetChanged()
        }
    }

    private fun setupEmptyList() {
        val binding = binding ?: return

        binding.emptyList.run {
            emptyListViewHeadline.run {
                visibility = View.VISIBLE
                setText(R.string.two_way_sync_activity_empty_list_title)
            }

            emptyListViewText.run {
                visibility = View.VISIBLE
                setText(R.string.two_way_sync_activity_empty_list_desc)
            }

            emptyListIcon.run {
                visibility = View.VISIBLE
                setImageDrawable(
                    viewThemeUtils.platform.tintDrawable(
                        context,
                        R.drawable.ic_sync,
                        ColorRole.PRIMARY
                    )
                )
            }
        }
    }

    @Suppress("MagicNumber")
    private fun setupTwoWaySyncInterval() {
        val binding = binding ?: return
        val context = requireContext()

        val durations = listOf(
            15.minutes to context.minPlural(15),
            30.minutes to context.minPlural(30),
            45.minutes to context.minPlural(45),
            1.hours to context.hourPlural(1),
            2.hours to context.hourPlural(2),
            4.hours to context.hourPlural(4),
            6.hours to context.hourPlural(6),
            8.hours to context.hourPlural(8),
            12.hours to context.hourPlural(12),
            24.hours to context.hourPlural(24)
        )
        val selectedDuration = durations.find { it.first.inWholeMinutes == preferences.twoWaySyncInterval }

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            durations.map { it.second }
        )

        binding.twoWaySyncInterval.run {
            setAdapter(adapter)
            setText(selectedDuration?.second ?: context.minPlural(15), false)
            setOnItemClickListener { _, _, position, _ ->
                handleDurationSelected(durations[position].first.inWholeMinutes)
            }
        }
        binding.twoWaySyncIntervalLayout.run {
            viewThemeUtils.material.colorTextInputLayout(this)
            setEndIconTintList(ColorStateList.valueOf(viewThemeUtils.platform.getScheme(context).primary))
        }
    }

    private fun setupTwoWaySyncToggle() {
        val binding = binding ?: return

        binding.twoWaySyncToggle.isChecked = preferences.isTwoWaySyncEnabled
        binding.twoWaySyncToggle.setOnCheckedChangeListener { _, isChecked ->
            preferences.setTwoWaySyncStatus(isChecked)
            setupTwoWaySyncAdapter()
            checkLayoutVisibilities(isChecked)
            checkDisableForAllFoldersMenuButtonVisibility()

            if (isChecked) {
                backgroundJobManager.scheduleInternal2WaySync(preferences.twoWaySyncInterval)
            } else {
                backgroundJobManager.cancelTwoWaySyncJob()
            }
        }
        viewThemeUtils.material.colorMaterialSwitch(binding.twoWaySyncToggle)
    }

    private fun addMenuProvider() {
        val menuProvider = InternalTwoWaySyncMenuProvider(
            onDisableForAllFoldersItemCreated = { menuItem ->
                disableForAllFoldersMenuButton = menuItem
                checkDisableForAllFoldersMenuButtonVisibility()
            },
            onDisableForAllFolders = { disableTwoWaySyncAndWorkers() },
            onRunNow = { backgroundJobManager.runNowInternal2WaySync() }
        )
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    // endregion

    // region Actions
    private fun handleDurationSelected(duration: Long) {
        preferences.twoWaySyncInterval = duration
        backgroundJobManager.scheduleInternal2WaySync(duration)
    }

    private fun disableTwoWaySyncAndWorkers() {
        val currentUser = user ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                backgroundJobManager.cancelTwoWaySyncJob()

                val folders = fileDataStorageManager.getInternalTwoWaySyncFolders(currentUser)
                folders.forEach { folder ->
                    FileDownloadWorker.cancelOperation(currentUser.accountName, folder.fileId)
                    backgroundJobManager.cancelFilesDownloadJob(currentUser.accountName, folder.fileId)

                    folder.internalFolderSyncTimestamp = -1L
                    fileDataStorageManager.saveFile(folder)
                }

                withContext(Dispatchers.Main) {
                    internalTwoWaySyncAdapter?.update()
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "Error caught at disableTwoWaySyncAndWorkers: $e")
            }
        }
    }
    // endregion

    // region Visibility
    private fun checkLayoutVisibilities(condition: Boolean) {
        val binding = binding ?: return

        binding.listFrameLayout.setVisibleIf(condition)
        binding.twoWaySyncIntervalLayout.setVisibleIf(condition)
    }

    private fun checkDisableForAllFoldersMenuButtonVisibility() {
        val currentUser = user ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val folderSize = withContext(Dispatchers.IO) {
                fileDataStorageManager.getInternalTwoWaySyncFolders(currentUser).size
            }

            checkDisableForAllFoldersMenuButtonVisibility(preferences.isTwoWaySyncEnabled, folderSize)
        }
    }

    private fun checkDisableForAllFoldersMenuButtonVisibility(isTwoWaySyncEnabled: Boolean, folderSize: Int) {
        val showDisableButton = isTwoWaySyncEnabled && folderSize > 0

        disableForAllFoldersMenuButton?.let {
            it.setVisible(showDisableButton)
            it.setEnabled(showDisableButton)
        }
    }

    override fun onUpdate(folderSize: Int) {
        checkDisableForAllFoldersMenuButtonVisibility(preferences.isTwoWaySyncEnabled, folderSize)
    }
    // endregion
}
