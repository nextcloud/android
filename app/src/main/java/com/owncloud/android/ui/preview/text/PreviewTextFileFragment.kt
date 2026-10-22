/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview.text

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.ui.fileactions.FileAction
import com.nextcloud.ui.fileactions.FileActionHandler
import com.nextcloud.ui.fileactions.FileActionHost
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.universalchardet.ReaderFactory
import java.io.File
import java.io.IOException
import java.io.Reader
import java.util.Scanner
import javax.inject.Inject

class PreviewTextFileFragment : PreviewTextFragment() {

    @Inject
    lateinit var fileActionHandler: FileActionHandler

    private var textLoadJob: Job? = null
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = requireArguments()
        readSearchArguments(args)
        file = if (savedInstanceState == null) requireFileAndUser(args) else restoreFileAndUser(savedInstanceState)
        handler = Handler(Looper.getMainLooper())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addMenuProvider()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_FILE, file)
        outState.putParcelable(EXTRA_USER, user)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        Log_OC.e(TAG, "onStop")
        textLoadJob?.cancel()
    }

    private fun readSearchArguments(args: Bundle) {
        args.getString(EXTRA_SEARCH_QUERY)?.let { searchQuery = it }
        searchOpen = args.getBoolean(EXTRA_OPEN_SEARCH, false)
    }

    private fun requireFileAndUser(args: Bundle): OCFile {
        val previewFile = checkNotNull(file ?: args.getParcelableArgument(EXTRA_FILE, OCFile::class.java)) {
            "Instanced with a NULL OCFile"
        }
        user = checkNotNull(user ?: args.getParcelableArgument(EXTRA_USER, User::class.java)) {
            "Instanced with a NULL ownCloud Account"
        }
        return previewFile
    }

    private fun restoreFileAndUser(savedInstanceState: Bundle): OCFile? {
        user = savedInstanceState.getParcelableArgument(EXTRA_USER, User::class.java)
        return savedInstanceState.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
    }

    override fun loadAndShowTextPreview() {
        val storagePath = file?.storagePath ?: return
        textLoadJob = viewLifecycleOwner.lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) { readText(storagePath) }
            showText(text)
        }
    }

    private fun readText(storagePath: String): String {
        val text = StringBuilder()
        try {
            ReaderFactory.createBufferedReader(File(storagePath)).use { text.appendLines(it) }
        } catch (e: IOException) {
            Log_OC.e(TAG, e.message, e)
            finish()
        }
        return text.toString()
    }

    private fun StringBuilder.appendLines(reader: Reader) {
        val scanner = Scanner(reader)
        while (scanner.hasNextLine()) {
            append(scanner.nextLine())
            if (scanner.hasNextLine()) {
                append(LINE_SEPARATOR)
            }
        }
        scanner.ioException()?.let { throw it }
    }

    private fun showText(text: String) {
        originalText = text
        setText(binding.textPreview, text, file, requireActivity(), false, false, viewThemeUtils)

        searchView?.let {
            it.setOnQueryTextListener(this)
            if (searchOpen) {
                it.setQuery(searchQuery, true)
            }
        }

        binding.textPreview.isVisible = true
        binding.emptyListProgress.isGone = true
    }

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.custom_menu_placeholder, menu)
                    setupSearchView(menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (menuItem.itemId != R.id.custom_menu_placeholder_item) {
                        return false
                    }

                    refreshFileAndShowActions()
                    return true
                }
            },
            viewLifecycleOwner
        )
    }

    private fun setupSearchView(menu: Menu) {
        val searchMenuItem = menu.findItem(R.id.action_search) ?: return
        searchMenuItem.isVisible = true
        searchView = searchMenuItem.actionView as? SearchView

        val searchView = searchView ?: return
        searchView.maxWidth = Int.MAX_VALUE
        viewThemeUtils.androidx.themeToolbarSearchView(searchView)

        if (searchOpen) {
            searchView.isIconified = false
            searchView.setQuery(searchQuery, false)
            searchView.clearFocus()
        }
    }

    private fun refreshFileAndShowActions() {
        val currentFile = file ?: return
        val storageManager = containerActivity?.storageManager ?: return

        file = storageManager.getFileById(currentFile.fileId)
        if (file != null) {
            showFileActions(currentFile)
        }
    }

    private fun showFileActions(actionsFile: OCFile) {
        val additionalFilter = FileAction.getFilePreviewActions(file)
        FileActionsBottomSheet.newInstance(actionsFile, false, additionalFilter)
            .setResultListener(childFragmentManager, this) { itemId -> onFileActionChosen(itemId) }
            .show(childFragmentManager, TAG_FILE_ACTIONS)
    }

    private fun onFileActionChosen(@IdRes itemId: Int) {
        val previewFile = file ?: return
        val containerActivity = containerActivity ?: return
        val host = FileActionHost(containerActivity, parentFragmentManager, view, null, onFileOpened = ::finish)
        fileActionHandler.handle(itemId, listOf(previewFile), host)
    }

    fun updateFile(file: OCFile?) {
        this.file = file
    }

    companion object {
        private const val EXTRA_FILE = "FILE"
        private const val EXTRA_USER = "USER"
        private const val EXTRA_OPEN_SEARCH = "SEARCH"
        private const val EXTRA_SEARCH_QUERY = "SEARCH_QUERY"
        private const val TAG_FILE_ACTIONS = "actions"
        private const val LINE_SEPARATOR = "\n"

        private val TAG: String = PreviewTextFileFragment::class.java.simpleName

        private val UNSUPPORTED_MIME_TYPES = setOf(
            "text/richtext",
            "text/rtf",
            "text/calendar",
            "text/vnd.abc",
            "text/vnd.fmi.flexstor",
            "text/vnd.rn-realtext",
            "text/vnd.wap.wml",
            "text/vnd.wap.wmlscript",
            "text/html"
        )

        fun create(user: User, file: OCFile?, openSearch: Boolean, searchQuery: String?): PreviewTextFileFragment =
            PreviewTextFileFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_FILE, file)
                    putParcelable(EXTRA_USER, user)
                    putBoolean(EXTRA_OPEN_SEARCH, openSearch)
                    putString(EXTRA_SEARCH_QUERY, searchQuery)
                }
            }

        fun canBePreviewed(file: OCFile?): Boolean = file != null &&
            file.isDown &&
            MimeTypeUtil.isText(file) &&
            file.mimeType !in UNSUPPORTED_MIME_TYPES &&
            MimeTypeUtil.getMimeTypeFromPath(file.remotePath) !in UNSUPPORTED_MIME_TYPES
    }
}
