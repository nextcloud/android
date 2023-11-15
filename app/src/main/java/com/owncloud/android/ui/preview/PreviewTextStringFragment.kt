/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.preview

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.android.lib.richWorkspace.RichWorkspaceDirectEditingRemoteOperation
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.DisplayUtils

class PreviewTextStringFragment : PreviewTextFragment() {

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val args = arguments
        if (args?.containsKey(FileDisplayActivity.EXTRA_SEARCH_QUERY) == true) {
            searchQuery = args.getString(FileDisplayActivity.EXTRA_SEARCH_QUERY)!!
        }

        searchOpen = requireArguments().getBoolean(FileDisplayActivity.EXTRA_SEARCH, false)
        handler = Handler(Looper.getMainLooper())
    }

    /**
     * {@inheritDoc}
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_FILE, file)
        super.onSaveInstanceState(outState)
    }

    @Suppress("TooGenericExceptionThrown")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
            ?: throw RuntimeException("View may not be null")
        val fabMain = requireActivity().findViewById<FloatingActionButton>(R.id.fab_main)
        fabMain.visibility = View.VISIBLE
        fabMain.isEnabled = true
        fabMain.setOnClickListener { edit() }
        fabMain.setImageResource(R.drawable.ic_edit)
        viewThemeUtils?.material?.themeFAB(fabMain)
        return view
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val menuItem = menu.findItem(R.id.action_search)
        menuItem.isVisible = true
        searchView = MenuItemCompat.getActionView(menuItem) as SearchView
        searchView?.setOnQueryTextListener(this)
        searchView?.maxWidth = Int.MAX_VALUE
        viewThemeUtils?.androidx?.themeToolbarSearchView(searchView!!)
        if (searchOpen) {
            searchView?.isIconified = false
            searchView?.setQuery(searchQuery, true)
            searchView?.clearFocus()
        }
    }

    override fun loadAndShowTextPreview() {
        originalText = file.richWorkspace
        setText(binding!!.textPreview, originalText, file, requireActivity(), true, false, viewThemeUtils)
        binding?.textPreview?.visibility = View.VISIBLE
        binding?.emptyListProgress?.visibility = View.GONE
    }

    private fun edit() {
        Thread {
            val result = RichWorkspaceDirectEditingRemoteOperation(file.remotePath)
                .execute(accountManager?.user, context)
            if (result.isSuccess) {
                val url = result.singleData as String
                containerActivity.fileOperationsHelper.openRichWorkspaceWithTextEditor(
                    file,
                    url,
                    context
                )
            } else {
                DisplayUtils.showSnackMessage(view, "Error")
            }
        }.start()
    } // TODO on close clean search query

    companion object {
        private const val EXTRA_FILE = "FILE"
    }
}
