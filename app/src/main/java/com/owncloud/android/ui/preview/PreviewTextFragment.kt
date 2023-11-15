/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.preview

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.TextFilePreviewBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.StringUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListDrawable
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.syntax.Prism4jTheme
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import io.noties.prism4j.annotations.PrismBundle
import javax.inject.Inject

@Suppress("MaxLineLength")
@PrismBundle(
    include = [
        "c", "clike", "clojure", "cpp", "csharp", "css", "dart", "git", "go", "groovy", "java",
        "javascript", "json", "kotlin", "latex", "makefile", "markdown", "markup", "python", "scala",
        "sql", "swift", "yaml"
    ],
    grammarLocatorClassName = ".MarkwonGrammarLocator"
)
abstract class PreviewTextFragment : FileFragment(), SearchView.OnQueryTextListener, Injectable {

    @JvmField
    protected var searchView: SearchView? = null

    @JvmField
    protected var searchQuery = ""

    @JvmField
    protected var searchOpen = false

    @JvmField
    protected var handler: Handler? = null

    @JvmField
    protected var originalText: String? = null

    @JvmField
    @Inject
    var accountManager: UserAccountManager? = null

    @JvmField
    @Inject
    var deviceInfo: DeviceInfo? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    @JvmField
    protected var binding: TextFilePreviewBinding? = null

    /**
     * {@inheritDoc}
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        Log_OC.e(TAG, "onCreateView")
        binding = TextFilePreviewBinding.inflate(inflater, container, false)
        binding?.emptyListProgress?.visibility = View.VISIBLE
        return binding?.root
    }

    override fun onStart() {
        super.onStart()
        Log_OC.e(TAG, "onStart")
        loadAndShowTextPreview()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    abstract fun loadAndShowTextPreview()
    override fun onQueryTextSubmit(query: String): Boolean {
        performSearch(query, 0)
        return true
    }

    @Suppress("MagicNumber")
    override fun onQueryTextChange(newText: String): Boolean {
        performSearch(newText, 500)
        return true
    }

    @Suppress("ReturnCount")
    private fun performSearch(query: String, delay: Int) {
        handler?.removeCallbacksAndMessages(null)
        if (originalText != null) {
            if (activity is FileDisplayActivity) {
                val fileDisplayActivity = activity as FileDisplayActivity?
                fileDisplayActivity?.setSearchQuery(query)
            }
            handler?.postDelayed({ markText(query) }, delay.toLong())
        }
        if (delay == 0) {
            searchView?.clearFocus()
        }
    }

    private fun markText(query: String) {
        // called asynchronously - must check preconditions in case of UI detachment
        if (binding == null) {
            return
        }
        val activity = activity ?: return
        val resources = activity.resources ?: return
        if (!TextUtils.isEmpty(query)) {
            val coloredText = StringUtils.searchAndColor(
                originalText,
                query,
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
            @Suppress("MaxLineLength")
            binding?.textPreview?.text =
                HtmlCompat.fromHtml(coloredText.replace("\n", "<br \\>"), HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            setText(binding!!.textPreview, originalText, file, activity, false, false, viewThemeUtils)
        }
    }

    /**
     * Finishes the preview
     */
    protected fun finish() {
        requireActivity().runOnUiThread { requireActivity().onBackPressed() }
    }

    companion object {
        private val TAG = PreviewTextFragment::class.java.simpleName

        @Suppress("ReturnCount")
        protected fun getRenderedMarkdownText(
            activity: Activity?,
            markdown: String?,
            viewThemeUtils: ViewThemeUtils?
        ): Spanned? {
            val prism4j = Prism4j(MarkwonGrammarLocator())
            val prism4jTheme: Prism4jTheme = Prism4jThemeDefault.create()
            val drawable = TaskListDrawable(Color.GRAY, Color.GRAY, Color.WHITE)
            viewThemeUtils?.platform?.tintDrawable(activity!!, drawable, ColorRole.PRIMARY)

            viewThemeUtils ?: return null
            activity ?: return null

            val markwon = Markwon.builder(activity)
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder.linkColor(viewThemeUtils.platform.primaryColor(activity))
                        builder.headingBreakHeight(0)
                    }

                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        builder.linkResolver { _: View?, link: String? ->
                            DisplayUtils.startLinkIntent(
                                activity,
                                link
                            )
                        }
                    }
                })
                .usePlugin(TablePlugin.create(activity))
                .usePlugin(TaskListPlugin.create(drawable))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .build()

            return markwon.toMarkdown(markdown!!)
        }

        @JvmStatic
        @Suppress("LongParameterList")
        fun setText(
            textView: TextView,
            text: String?,
            file: OCFile?,
            activity: Activity?,
            ignoreMimetype: Boolean,
            preview: Boolean,
            viewThemeUtils: ViewThemeUtils?
        ) {
            if (text == null) {
                return
            }

            @Suppress("ComplexCondition")
            if ((ignoreMimetype || file != null && MimeTypeUtil.MIMETYPE_TEXT_MARKDOWN == file.mimeType) &&
                activity != null
            ) {
                if (!preview) {
                    // clickable links prevent to open full view of rich workspace
                    textView.movementMethod = LinkMovementMethod.getInstance()
                }
                textView.text = getRenderedMarkdownText(activity, text, viewThemeUtils) ?: ""
            } else {
                textView.text = text
            }
        }
    }
}
