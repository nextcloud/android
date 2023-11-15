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

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import com.nextcloud.client.account.User
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.Companion.newInstance
import com.nextcloud.ui.fileactions.FileActionsBottomSheet.ResultListener
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.mozilla.universalchardet.ReaderFactory
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.Reader
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.Scanner

class PreviewTextFileFragment : PreviewTextFragment() {
    /**
     * Creates an empty fragment for previews.
     *
     *
     * MUST BE KEPT: the system uses it when tries to re-instantiate a fragment automatically (for instance, when the
     * device is turned a aside).
     *
     *
     * DO NOT CALL IT: an [OCFile] and [User] must be provided for a successful construction
     */

    private var textLoadAsyncTask: TextLoadAsyncTask? = null
    private var user: User? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        var file = file
        val args = arguments
        if (file == null) {
            file = args?.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
        }
        if (user == null) {
            user = args?.getParcelableArgument(EXTRA_USER, User::class.java)
        }
        if (args?.containsKey(EXTRA_SEARCH_QUERY) == true) {
            searchQuery = args.getString(EXTRA_SEARCH_QUERY)!!
        }
        searchOpen = args?.getBoolean(EXTRA_OPEN_SEARCH, false) ?: false

        if (savedInstanceState == null) {
            checkNotNull(file) { "Instanced with a NULL OCFile" }
            checkNotNull(user) { "Instanced with a NULL ownCloud Account" }
        } else {
            file = savedInstanceState.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
            user = savedInstanceState.getParcelableArgument(EXTRA_USER, User::class.java)
        }

        handler = Handler(Looper.getMainLooper())
        setFile(file)
    }

    /**
     * {@inheritDoc}
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(EXTRA_FILE, file)
        outState.putParcelable(EXTRA_USER, user)
        super.onSaveInstanceState(outState)
    }

    override fun loadAndShowTextPreview() {
        textLoadAsyncTask = TextLoadAsyncTask(
            WeakReference(binding?.textPreview),
            WeakReference(binding?.emptyListProgress)
        )
        textLoadAsyncTask?.execute(file.storagePath)
    }

    /**
     * Reads the file to preview and shows its contents. Too critical to be anonymous.
     */

    @SuppressLint("StaticFieldLeak")
    private inner class TextLoadAsyncTask(
        private val textViewReference: WeakReference<TextView>,
        private val progressViewReference: WeakReference<FrameLayout>
    ) : AsyncTask<Any?, Void?, StringWriter>() {
        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            // not used at the moment
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Any?): StringWriter {
            require(params.size == paramLength) {
                (
                    "The parameter to " + TextLoadAsyncTask::class.java.name +
                        " must be (1) the file location"
                    )
            }
            val location = params[0] as String
            var scanner: Scanner? = null
            val source = StringWriter()
            val bufferedWriter = BufferedWriter(source)
            var reader: Reader? = null
            try {
                val file = File(location)
                reader = ReaderFactory.createReaderFromFile(file)
                scanner = Scanner(reader)
                while (scanner.hasNextLine()) {
                    bufferedWriter.append(scanner.nextLine())
                    if (scanner.hasNextLine()) {
                        bufferedWriter.append("\n")
                    }
                }
                bufferedWriter.close()
                val exc = scanner.ioException()
                if (exc != null) {
                    throw exc
                }
            } catch (e: IOException) {
                Log_OC.e(TAG, e.message, e)
                finish()
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        Log_OC.e(TAG, e.message, e)
                        finish()
                    }
                }
                scanner?.close()
            }
            return source
        }

        @SuppressFBWarnings("STT")
        override fun onPostExecute(stringWriter: StringWriter) {
            val textView = textViewReference.get()
            if (textView != null) {
                originalText = stringWriter.toString()
                setText(textView, originalText, file, requireActivity(), false, false, viewThemeUtils)
                if (searchView != null) {
                    searchView?.setOnQueryTextListener(this@PreviewTextFileFragment)
                    if (searchOpen) {
                        searchView?.setQuery(searchQuery, true)
                    }
                }
                textView.visibility = View.VISIBLE
            }
            val progress = progressViewReference.get()
            progress?.visibility = View.GONE
        }

        private val paramLength = 1
    }

    /**
     * {@inheritDoc}
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.custom_menu_placeholder, menu)
        val menuItem = menu.findItem(R.id.action_search)
        menuItem.isVisible = true
        searchView = MenuItemCompat.getActionView(menuItem) as SearchView
        searchView!!.maxWidth = Int.MAX_VALUE
        viewThemeUtils!!.androidx.themeToolbarSearchView(searchView!!)
        if (searchOpen) {
            searchView!!.isIconified = false
            searchView!!.setQuery(searchQuery, false)
            searchView!!.clearFocus()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.custom_menu_placeholder_item) {
            val file = file
            if (containerActivity.storageManager != null && file != null) {
                // Update the file
                val updatedFile = containerActivity.storageManager.getFileById(file.fileId)
                setFile(updatedFile)
                val fileNew = getFile()
                if (fileNew != null) {
                    showFileActions(file, item.itemId)
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFileActions(file: OCFile, itemId: Int) {
        val additionalFilter: MutableList<Int> = ArrayList(
            listOf(
                R.id.action_rename_file,
                R.id.action_sync_file,
                R.id.action_move_or_copy,
                R.id.action_favorite,
                R.id.action_unset_favorite,
                R.id.action_pin_to_homescreen
            )
        )
        if (getFile() != null && getFile().isSharedWithMe && !getFile().canReshare()) {
            additionalFilter.add(R.id.action_send_share_file)
        }
        val fragmentManager = childFragmentManager
        newInstance(file, false, additionalFilter)
            .setResultListener(
                fragmentManager,
                this,
                object : ResultListener {
                    override fun onResult(actionId: Int) {
                        onFileActionChosen(itemId)
                    }
                }
            )
            .show(fragmentManager, "actions")
    }

    private fun onFileActionChosen(itemId: Int) {
        if (itemId == R.id.action_send_share_file) {
            if (file.isSharedWithMe && !file.canReshare()) {
                DisplayUtils.showSnackMessage(view, R.string.resharing_is_not_allowed)
            } else {
                containerActivity.fileOperationsHelper.sendShareFile(file)
            }
        } else if (itemId == R.id.action_open_file_with) {
            openFile()
        } else if (itemId == R.id.action_remove_file) {
            val dialog = RemoveFilesDialogFragment.newInstance(file)
            dialog.show(requireFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION)
        } else if (itemId == R.id.action_see_details) {
            seeDetails()
        } else if (itemId == R.id.action_sync_file) {
            containerActivity.fileOperationsHelper.syncFile(file)
        } else if (itemId == R.id.action_edit) {
            containerActivity.fileOperationsHelper.openFileWithTextEditor(file, context)
        }
    }

    /**
     * Update the file of the fragment with file value
     *
     * @param file The new file to set
     */
    fun updateFile(file: OCFile?) {
        setFile(file)
    }

    private fun seeDetails() {
        containerActivity.showDetails(file)
    }

    /**
     * Opens the previewed file with an external application.
     */
    private fun openFile() {
        containerActivity.fileOperationsHelper.openFile(file)
        finish()
    }

    override fun onStop() {
        super.onStop()
        Log_OC.e(TAG, "onStop")
        textLoadAsyncTask?.cancel(true)
    }

    companion object {
        private const val EXTRA_FILE = "FILE"
        private const val EXTRA_USER = "USER"
        private const val EXTRA_OPEN_SEARCH = "SEARCH"
        private const val EXTRA_SEARCH_QUERY = "SEARCH_QUERY"
        private val TAG = PreviewTextFileFragment::class.java.simpleName

        @JvmStatic
        fun create(user: User?, file: OCFile?, openSearch: Boolean, searchQuery: String?): PreviewTextFileFragment {
            val args = Bundle()
            args.putParcelable(EXTRA_FILE, file)
            args.putParcelable(EXTRA_USER, user)
            args.putBoolean(EXTRA_OPEN_SEARCH, openSearch)
            args.putString(EXTRA_SEARCH_QUERY, searchQuery)
            val fragment = PreviewTextFileFragment()
            fragment.arguments = args
            return fragment
        }

        /**
         * Helper method to test if an [OCFile] can be passed to a [PreviewTextFileFragment] to be previewed.
         *
         * @param file File to test if can be previewed.
         * @return 'True' if the file can be handled by the fragment.
         */
        @JvmStatic
        fun canBePreviewed(file: OCFile?): Boolean {
            val unsupportedTypes: MutableList<String> = LinkedList()
            unsupportedTypes.add("text/richtext")
            unsupportedTypes.add("text/rtf")
            unsupportedTypes.add("text/calendar")
            unsupportedTypes.add("text/vnd.abc")
            unsupportedTypes.add("text/vnd.fmi.flexstor")
            unsupportedTypes.add("text/vnd.rn-realtext")
            unsupportedTypes.add("text/vnd.wap.wml")
            unsupportedTypes.add("text/vnd.wap.wmlscript")
            unsupportedTypes.add("text/html")
            return file != null && file.isDown && MimeTypeUtil.isText(file) &&
                !unsupportedTypes.contains(file.mimeType) &&
                !unsupportedTypes.contains(MimeTypeUtil.getMimeTypeFromPath(file.remotePath))
        }
    }
}
