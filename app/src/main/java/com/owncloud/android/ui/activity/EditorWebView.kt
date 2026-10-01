/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.User
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.extensions.getSmallThumbnail
import com.nextcloud.utils.extensions.isPNG
import com.nextcloud.utils.thumbnail.VideoOverlayGenerator
import com.owncloud.android.R
import com.owncloud.android.databinding.RichdocumentsWebviewBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderObserver
import com.owncloud.android.ui.asynctasks.TextEditorLoadUrlTask
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.RichDocumentDownloader
import com.owncloud.android.utils.WebViewUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

abstract class EditorWebView : ExternalSiteWebView() {
    private lateinit var binding: RichdocumentsWebviewBinding

    protected lateinit var fileName: String

    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private var loadingSnackbar: Snackbar? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uris = if (result.resultCode == RESULT_OK) {
                parseChosenFiles(result.resultCode, result.data)
            } else {
                null
            }
            completeFileChooser(uris)
        }

    protected open fun loadUrl(url: String?) {
        onUrlLoaded(url)
    }

    protected fun hideLoading() {
        binding.run {
            thumbnail.isVisible = false
            filename.isVisible = false
            progressBar2.isVisible = false
            webView.isVisible = true
        }
        loadingSnackbar?.dismiss()
    }

    fun onUrlLoaded(loadedUrl: String?) {
        url = loadedUrl

        if (loadedUrl.isNullOrEmpty()) {
            SnackbarUtil.show(this, R.string.richdocuments_failed_to_load_document)
            finish()
            return
        }

        lifecycleScope.launch {
            WebViewUtil().setProxyKKPlus(webView)
            delay(PROXY_SETUP_DELAY)

            if (loadedUrl != webView.url) {
                webView.loadUrl(loadedUrl)
            }

            delay(LOADING_TIMEOUT)
            if (!webView.isVisible) {
                showLoadingTimeoutSnackbar()
            }
        }
    }

    private fun showLoadingTimeoutSnackbar() {
        val snackbar = SnackbarUtil.create(
            findViewById(android.R.id.content),
            R.string.timeout_richDocuments,
            Snackbar.LENGTH_INDEFINITE
        ) ?: return

        snackbar.setAction(R.string.common_cancel) { closeView() }
        viewThemeUtils.material.themeSnackbar(snackbar)
        loadingSnackbar = snackbar
        snackbar.show()
    }

    private fun closeView() {
        webView.destroy()
        finish()
    }

    private fun reload() {
        val user = user.orElse(null)
        val file = file
        if (!webView.isVisible || user == null || file == null) {
            return
        }

        TextEditorLoadUrlTask(this, user, file, editorUtils).execute()
    }

    override fun bindView() {
        binding = RichdocumentsWebviewBinding.inflate(layoutInflater)
    }

    override val isWebViewBound: Boolean
        get() = ::binding.isInitialized

    override fun postOnCreate() {
        super.postOnCreate()

        viewThemeUtils.platform.colorCircularProgressBar(binding.progressBar2, ColorRole.PRIMARY)
        webView.webChromeClient = createFileChooserClient()

        file = intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
        val file = file ?: run {
            SnackbarUtil.show(this, R.string.richdocuments_failed_to_load_document)
            finish()
            return
        }
        fileName = file.fileName

        val user = user.orElse(null) ?: run {
            finish()
            return
        }
        setThumbnailView(file, user)
        binding.filename.text = fileName
    }

    private fun createFileChooserClient() = object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            completeFileChooser(null)
            uploadMessage = filePathCallback

            val intent = fileChooserParams.createIntent().apply {
                type = IMAGE_MIME_TYPE_FILTER
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            return try {
                fileChooserLauncher.launch(intent)
                true
            } catch (_: ActivityNotFoundException) {
                uploadMessage = null
                SnackbarUtil.show(this@EditorWebView, R.string.editor_web_view_cannot_open_file)
                false
            }
        }
    }

    private fun parseChosenFiles(resultCode: Int, data: Intent?): Array<Uri>? {
        val clipData = data?.clipData ?: return FileChooserParams.parseResult(resultCode, data)
        return Array(clipData.itemCount) { clipData.getItemAt(it).uri }
    }

    private fun completeFileChooser(uris: Array<Uri>?) {
        uploadMessage?.onReceiveValue(uris)
        uploadMessage = null
    }

    override val webView: WebView
        get() = binding.webView

    override val rootView: View
        get() = binding.root

    override fun showToolbarByDefault(): Boolean = false

    private fun openShareDialog() {
        val intent = Intent(this, ShareActivity::class.java).apply {
            putExtra(EXTRA_FILE, file)
            putExtra(EXTRA_USER, user.orElseThrow { RuntimeException() })
        }
        startActivity(intent)
    }

    private fun setThumbnailView(file: OCFile, user: User) {
        // Todo minimize: only icon by mimetype
        when {
            file.isFolder -> binding.thumbnail.setImageDrawable(getFolderIcon(file, user))

            MimeTypeUtil.isImageOrVideo(file) && file.remoteId != null -> showCachedThumbnail(file)

            else -> binding.thumbnail.setImageDrawable(
                MimeTypeUtil.getFileTypeIcon(file.mimeType, file.fileName, applicationContext, viewThemeUtils)
            )
        }
    }

    private fun getFolderIcon(file: OCFile, user: User): Drawable {
        val isAutoUploadFolder = SyncedFolderObserver.isAutoUploadFolder(file, user)
        val overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder)
        return MimeTypeUtil.getFolderIcon(preferences.isDarkModeEnabled, overlayIconId, this, viewThemeUtils)
    }

    private fun showCachedThumbnail(file: OCFile) {
        val thumbnail = file.getSmallThumbnail()
        if (thumbnail != null && !file.isUpdateThumbnailNeeded) {
            val bitmap = if (MimeTypeUtil.isVideo(file)) {
                VideoOverlayGenerator.addOverlay(thumbnail, this)
            } else {
                thumbnail
            }
            binding.thumbnail.setImageBitmap(bitmap)
        }

        if (file.isPNG()) {
            binding.thumbnail.setBackgroundColor(getColor(R.color.bg_default))
        }
    }

    protected fun downloadFile(uri: Uri, filename: String?) {
        // downloadAs is invoked from the WebView JavaScript bridge thread, but WebView methods
        // (getSettings) must run on the main thread, so read the user agent there.
        lifecycleScope.launch {
            RichDocumentDownloader(this@EditorWebView).download(uri, filename, webView.settings.userAgentString)
        }
    }

    open inner class MobileInterface {
        @JavascriptInterface
        fun close() {
            lifecycleScope.launch { closeView() }
        }

        @JavascriptInterface
        fun share() {
            openShareDialog()
        }

        @JavascriptInterface
        fun loaded() {
            lifecycleScope.launch { hideLoading() }
        }

        @JavascriptInterface
        fun reload() {
            this@EditorWebView.reload()
        }
    }

    companion object {
        private const val IMAGE_MIME_TYPE_FILTER = "image/*"
        private val PROXY_SETUP_DELAY = 1.seconds
        private val LOADING_TIMEOUT = 10.seconds
    }
}
