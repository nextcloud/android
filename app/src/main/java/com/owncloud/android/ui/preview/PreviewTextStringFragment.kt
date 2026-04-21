/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nextcloud.android.lib.richWorkspace.RichWorkspaceDirectEditingRemoteOperation
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooGenericExceptionThrown")
class PreviewTextStringFragment : PreviewTextFragment() {
    private var isEditorWebviewLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            searchQuery = getString(FileActivity.EXTRA_SEARCH_QUERY, "")
            searchOpen = getBoolean(FileActivity.EXTRA_SEARCH, false)
        }
        handler = Handler()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_FILE, file)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
            ?: throw RuntimeException("View can not be null")

        requireActivity().findViewById<FloatingActionButton>(R.id.fab_main).apply {
            visibility = View.VISIBLE
            isEnabled = true
            setOnClickListener { edit() }
            setImageResource(R.drawable.ic_edit)
            viewThemeUtils.material.themeFAB(this)
        }

        addMenuProvider()
        return view
    }

    override fun onStart() {
        super.onStart()
        if (isEditorWebviewLaunched && containerActivity is FileDisplayActivity) {
            (containerActivity as FileDisplayActivity).run {
                supportFragmentManager.popBackStack()
                onRefresh()
            }
        }
    }

    private fun addMenuProvider() {
        (requireActivity() as MenuHost).addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.findItem(R.id.action_search).apply {
                        isVisible = true
                        searchView = (actionView as? SearchView)?.also { sv ->
                            sv.setOnQueryTextListener(this@PreviewTextStringFragment)
                            sv.maxWidth = Int.MAX_VALUE
                            viewThemeUtils.androidx.themeToolbarSearchView(sv)
                            if (searchOpen) {
                                sv.isIconified = false
                                sv.setQuery(searchQuery, true)
                                sv.clearFocus()
                            }
                        }
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem) = false
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun loadAndShowTextPreview() {
        originalText = file.richWorkspace
        setText(binding.textPreview, originalText, file, requireActivity(), true, false, viewThemeUtils)
        binding.textPreview.visibility = View.VISIBLE
        binding.emptyListProgress.visibility = View.GONE
    }

    private fun edit() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = RichWorkspaceDirectEditingRemoteOperation(file.remotePath)
                .execute(accountManager.user, context)

            if (result.isSuccess) {
                containerActivity?.getFileOperationsHelper()?.openRichWorkspaceWithTextEditor(
                    file,
                    result.singleData as? String,
                    context
                )
                isEditorWebviewLaunched = true
            } else {
                withContext(Dispatchers.Main) {
                    activity?.let {
                        DisplayUtils.showSnackMessage(
                            it,
                            R.string.preview_text_string_fragment_open_rich_text_editor_error_message
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_FILE = "FILE"
    }
}
