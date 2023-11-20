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
package com.owncloud.android.ui.activity

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.databinding.RichdocumentsWebviewBinding
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import javax.inject.Inject

abstract class EditorWebView : ExternalSiteWebView() {
    var uploadMessage: ValueCallback<Array<Uri>>? = null
    private var loadingSnackbar: Snackbar? = null
    protected var fileName: String? = null
    lateinit var binding: RichdocumentsWebviewBinding

    @JvmField
    @Inject
    var syncedFolderProvider: SyncedFolderProvider? = null

    private var activityResult: ActivityResultLauncher<Intent>? = null

    protected open fun loadUrl(url: String?) {
        onUrlLoaded(url)
    }

    protected fun hideLoading() {
        binding.thumbnail.visibility = View.GONE
        binding.filename.visibility = View.GONE
        binding.progressBar2.visibility = View.GONE
        webView.visibility = View.VISIBLE
        loadingSnackbar?.dismiss()
    }

    @Suppress("MagicNumber")
    fun onUrlLoaded(loadedUrl: String?) {
        url = loadedUrl

        if (url.isNotEmpty()) {
            this.webView.loadUrl(url)
            Handler(Looper.getMainLooper()).postDelayed({
                if (this.webView.visibility != View.VISIBLE) {
                    val snackbar = DisplayUtils.createSnackbar(
                        findViewById(android.R.id.content),
                        R.string.timeout_richDocuments,
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction(R.string.common_cancel) { v: View? -> closeView() }
                    viewThemeUtils.material.themeSnackbar(snackbar)
                    setLoadingSnackbar(snackbar)
                    snackbar.show()
                }
            }, (10 * 1000).toLong())
        } else {
            Toast.makeText(applicationContext, R.string.richdocuments_failed_to_load_document, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    fun closeView() {
        webView.destroy()
        finish()
    }

    override fun bindView() {
        binding = RichdocumentsWebviewBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        registerActivityResult()
    }

    override fun postOnCreate() {
        super.postOnCreate()
        webView.webChromeClient = object : WebChromeClient() {
            val activity = this@EditorWebView
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (uploadMessage != null) {
                    uploadMessage?.onReceiveValue(null)
                    uploadMessage = null
                }
                activity.uploadMessage = filePathCallback
                val intent = fileChooserParams.createIntent()
                intent.type = "image/*"
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                try {
                    activityResult?.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    uploadMessage = null
                    Toast.makeText(baseContext, "Cannot open file chooser", Toast.LENGTH_LONG).show()
                    return false
                }
                return true
            }
        }

        file = intent.getParcelableExtra(EXTRA_FILE)
        if (file == null) {
            Toast.makeText(
                applicationContext,
                R.string.richdocuments_failed_to_load_document,
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
        if (file != null) {
            fileName = file.fileName
        }
        val user = user
        if (!user.isPresent) {
            finish()
            return
        }

        initLoadingScreen(user.get())
    }

    private fun registerActivityResult() {
        activityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (RESULT_OK != result.resultCode) {
                    uploadMessage?.onReceiveValue(null)
                    uploadMessage = null
                }

                if (uploadMessage != null) {
                    uploadMessage?.onReceiveValue(FileChooserParams.parseResult(result.resultCode, result.data))
                    uploadMessage = null
                }
            }
    }

    override fun getWebView(): WebView {
        return binding.webView
    }

    override fun getRootView(): View {
        return binding.root
    }

    override fun showToolbarByDefault(): Boolean {
        return false
    }

    private fun initLoadingScreen(user: User?) {
        setThumbnailView(user)
        binding.filename.text = fileName
    }

    private fun openShareDialog() {
        val intent = Intent(this, ShareActivity::class.java)
        intent.putExtra(EXTRA_FILE, file)
        intent.putExtra(EXTRA_USER, user.orElseThrow { RuntimeException() })
        startActivity(intent)
    }

    @Suppress("NestedBlockDepth")
    private fun setThumbnailView(user: User?) {
        // Todo minimize: only icon by mimetype
        val file = file
        if (file.isFolder) {
            val isAutoUploadFolder = SyncedFolderProvider.isAutoUploadFolder(syncedFolderProvider, file, user)
            val overlayIconId = file.getFileOverlayIconId(isAutoUploadFolder)
            val drawable = MimeTypeUtil.getFileIcon(preferences.isDarkModeEnabled, overlayIconId, this, viewThemeUtils)
            binding.thumbnail.setImageDrawable(drawable)
        } else {
            if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) && file.remoteId != null) {
                // Thumbnail in cache?
                val thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.remoteId
                )
                if (thumbnail != null && !file.isUpdateThumbnailNeeded) {
                    if (MimeTypeUtil.isVideo(file)) {
                        val withOverlay = ThumbnailsCacheManager.addVideoOverlay(thumbnail, this)
                        binding.thumbnail.setImageBitmap(withOverlay)
                    } else {
                        binding.thumbnail.setImageBitmap(thumbnail)
                    }
                }
                if ("image/png".equals(file.mimeType, ignoreCase = true)) {
                    binding.thumbnail.setBackgroundColor(resources.getColor(R.color.bg_default, theme))
                }
            } else {
                val icon = MimeTypeUtil.getFileTypeIcon(
                    file.mimeType,
                    file.fileName,
                    applicationContext,
                    viewThemeUtils
                )
                binding.thumbnail.setImageDrawable(icon)
            }
        }
    }

    protected fun downloadFile(url: Uri?) {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(url)
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        downloadManager.enqueue(request)
    }

    private fun setLoadingSnackbar(loadingSnackbar: Snackbar?) {
        this.loadingSnackbar = loadingSnackbar
    }

    open inner class MobileInterface {
        @JavascriptInterface
        fun close() {
            runOnUiThread { closeView() }
        }

        @JavascriptInterface
        fun share() {
            openShareDialog()
        }

        @JavascriptInterface
        fun loaded() {
            runOnUiThread { hideLoading() }
        }
    }
}
