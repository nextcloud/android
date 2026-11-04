/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.app.Activity
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.nextcloud.client.systembars.SystemBarBackgroundCallbacks
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.AlbumsPickerActivity
import com.owncloud.android.ui.adapter.filelist.OCFileListAdapter
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

internal class OCFileListMultiChoiceModeListener(private val fragment: OCFileListFragment) :
    AbsListView.MultiChoiceModeListener,
    DrawerLayout.DrawerListener {

    var activeActionMode: ActionMode? = null
        private set

    private var isSelectionUntouched = false
    private var closedByDrawer = false
    private val selectionClosedByDrawer = HashSet<OCFile>()

    fun onSelectionChanged(file: OCFile) {
        isSelectionUntouched = false
        val actionMode = activeActionMode ?: return
        actionMode.invalidate()
        fragment.commonAdapter?.notifyItemChanged(file)
    }

    fun saveState(outState: Bundle) {
        outState.putBoolean(KEY_CLOSED_BY_DRAWER, closedByDrawer)
    }

    fun restoreState(savedState: Bundle) {
        closedByDrawer = savedState.getBoolean(KEY_CLOSED_BY_DRAWER, closedByDrawer)
    }

    //region Drawer
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

    override fun onDrawerOpened(drawerView: View) = Unit

    override fun onDrawerClosed(drawerView: View) {
        if (!closedByDrawer || selectionClosedByDrawer.isEmpty()) return

        Log_OC.d(TAG, "Drawer closed, restoring selection of ${selectionClosedByDrawer.size} file(s)")
        fragment.activity?.startActionMode(this)
        fragment.adapter?.setCheckedItem(selectionClosedByDrawer)
        activeActionMode?.invalidate()
        selectionClosedByDrawer.clear()
    }

    override fun onDrawerStateChanged(newState: Int) {
        val actionMode = activeActionMode
        if (newState != DrawerLayout.STATE_DRAGGING || actionMode == null) return

        (fragment.recyclerView?.adapter as? OCFileListAdapter)?.let {
            selectionClosedByDrawer.addAll(it.getCheckedItems())
        }

        actionMode.finish()
        closedByDrawer = true
    }
    //endregion

    //region Action mode
    override fun onItemCheckedStateChanged(mode: ActionMode?, position: Int, id: Long, checked: Boolean) = Unit

    @SuppressFBWarnings("RV")
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu): Boolean {
        Log_OC.d(TAG, "Selection mode started")
        activeActionMode = mode
        isSelectionUntouched = true

        val activity = fragment.requireActivity()
        activity.menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
        val placeholderItem = menu.findItem(R.id.custom_menu_placeholder_item)
        tintPlaceholderIcon(placeholderItem)

        mode?.invalidate()
        colorStatusBar(activity)
        fragment.setFabState(FabState.HIDDEN)

        if (fragment is GalleryFragment && activity is AlbumsPickerActivity) {
            placeholderItem.isVisible = false
            menu.findItem(R.id.add_to_album).isVisible = true
        }

        fragment.commonAdapter?.setMultiSelect(true)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        val checkedCount = fragment.commonAdapter?.getCheckedItems()?.size ?: 0

        activeActionMode?.title = fragment.resources.getQuantityString(
            R.plurals.items_selected_count,
            checkedCount,
            checkedCount
        )

        if (checkedCount == 0 && !isSelectionUntouched) {
            fragment.exitSelectionMode()
        }

        OCFileListFragment.isMultipleFileSelectedForCopyOrMove = checkedCount > 0
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
        val commonAdapter = fragment.commonAdapter ?: return true
        val checkedFiles = commonAdapter.getCheckedItems()

        when (item.itemId) {
            R.id.custom_menu_placeholder_item ->
                fragment.openActionsMenu(commonAdapter.getFilesCount(), checkedFiles, false)

            R.id.add_to_album -> (fragment as? GalleryFragment)?.addImagesToAlbum(checkedFiles)
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        Log_OC.d(TAG, "Selection mode finished")
        activeActionMode = null

        fragment.updateFabState()

        fragment.activity?.let {
            fragment.viewThemeUtils.platform.resetStatusBar(it)
            SystemBarBackgroundCallbacks.apply(it, fragment.viewThemeUtils)
        }

        fragment.commonAdapter?.run {
            setMultiSelect(false)
            clearCheckedItems()
        }

        OCFileListFragment.isMultipleFileSelectedForCopyOrMove = false
    }
    //endregion

    private fun tintPlaceholderIcon(placeholderItem: MenuItem) {
        val icon = placeholderItem.icon ?: return
        val white = ContextCompat.getColor(fragment.requireContext(), R.color.white)
        placeholderItem.icon = fragment.viewThemeUtils.platform.colorDrawable(icon, white)
    }

    private fun colorStatusBar(activity: Activity) {
        val statusBarColor = ContextCompat.getColor(fragment.requireContext(), R.color.action_mode_background)
        fragment.viewThemeUtils.platform.colorStatusBar(activity, statusBarColor)
        SystemBarBackgroundCallbacks.colorStatusBar(activity, statusBarColor)
    }

    companion object {
        private const val TAG = "OCFileListMultiChoiceModeListener"
        private const val KEY_CLOSED_BY_DRAWER = "KILLED_ACTION_MODE"
    }
}
