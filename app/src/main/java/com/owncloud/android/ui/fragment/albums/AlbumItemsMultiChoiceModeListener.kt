/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.albums

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.adapter.GalleryAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils

/**
 * Handles the album items selection mode and keeps it alive across navigation drawer interactions: the drawer closes
 * the action mode while it is dragged and the selection is restored once it is closed again.
 */
internal class AlbumItemsMultiChoiceModeListener(
    private val activity: FragmentActivity,
    private val adapter: GalleryAdapter?,
    private val viewThemeUtils: ViewThemeUtils,
    private val openActionsMenu: (Int, Set<OCFile>) -> Unit,
    private val onSelectionModeChanged: (isActive: Boolean) -> Unit
) : AbsListView.MultiChoiceModeListener,
    DrawerLayout.DrawerListener {

    private var activeActionMode: ActionMode? = null
    private var isSelectionUntouched = false
    private var closedByDrawer = false
    private val selectionClosedByDrawer = mutableSetOf<OCFile>()

    val isActionModeActive: Boolean
        get() = activeActionMode != null

    //region Drawer
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

    override fun onDrawerOpened(drawerView: View) = Unit

    override fun onDrawerClosed(drawerView: View) {
        if (!closedByDrawer || selectionClosedByDrawer.isEmpty()) {
            return
        }

        activity.startActionMode(this)
        adapter?.setCheckedItem(selectionClosedByDrawer)
        activeActionMode?.invalidate()
        selectionClosedByDrawer.clear()
    }

    override fun onDrawerStateChanged(newState: Int) {
        if (newState != DrawerLayout.STATE_DRAGGING || activeActionMode == null) {
            return
        }

        adapter?.let { selectionClosedByDrawer.addAll(it.getCheckedItems()) }
        activeActionMode?.finish()
        closedByDrawer = true
    }
    //endregion

    //region Action mode
    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) = Unit

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        activeActionMode = mode
        isSelectionUntouched = true

        inflatePlaceholderMenu(menu)
        mode.invalidate()

        viewThemeUtils.platform.colorStatusBar(
            activity,
            ContextCompat.getColor(activity, R.color.action_mode_background)
        )

        adapter?.setMultiSelect(true)
        onSelectionModeChanged(true)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val checkedCount = adapter?.getCheckedItems()?.size ?: 0
        mode.title = activity.resources.getQuantityString(R.plurals.items_selected_count, checkedCount, checkedCount)

        if (checkedCount == 0 && !isSelectionUntouched) {
            exitSelectionMode()
        }

        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val adapter = adapter ?: return false

        if (item.itemId == R.id.custom_menu_placeholder_item) {
            openActionsMenu(adapter.getFilesCount(), adapter.getCheckedItems())
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        activeActionMode = null

        viewThemeUtils.platform.resetStatusBar(activity)

        adapter?.setMultiSelect(false)
        adapter?.clearCheckedItems()
        onSelectionModeChanged(false)
    }

    fun exitSelectionMode() {
        activeActionMode?.finish()
    }

    fun updateActionModeFile(file: OCFile) {
        isSelectionUntouched = false
        activeActionMode?.let {
            it.invalidate()
            adapter?.notifyItemChanged(file)
        }
    }

    fun invalidateActionMode() {
        activeActionMode?.invalidate()
    }
    //endregion

    /**
     * The action mode only carries a placeholder so that the bottom sheet can be shown instead of a real menu.
     */
    private fun inflatePlaceholderMenu(menu: Menu) {
        activity.menuInflater.inflate(R.menu.custom_menu_placeholder, menu)

        val item = menu.findItem(R.id.custom_menu_placeholder_item)
        val icon = item.icon ?: return

        item.icon = viewThemeUtils.platform.colorDrawable(icon, ContextCompat.getColor(activity, R.color.white))
    }
}
