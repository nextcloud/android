/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.nextcloud.android.common.ui.util.PlatformThemeUtil
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.R
import com.owncloud.android.ui.asynctasks.TextEditorLoadUrlTask
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ThemeUtils
import javax.inject.Inject

class TextEditorWebView : EditorWebView() {
    @Inject
    lateinit var appInfo: AppInfo

    @Inject
    lateinit var deviceInfo: DeviceInfo

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var editorUtils: EditorUtils

    private var pendingBase64Data: ByteArray? = null

    @SuppressLint("AddJavascriptInterface") // suppress warning as webview is only used > Lollipop
    override fun postOnCreate() {
        super.postOnCreate()

        if (!user.isPresent) {
            DisplayUtils.showSnackMessage(this, R.string.failed_to_start_editor)
            finish()
        }

        val editor = editorUtils.getEditor(user.get(), file?.mimeType)

        if (editorUtils.usesOfficeUserAgent(editor)) {
            webView.settings.userAgentString = generateOfficeUserAgent()
        }

        webView.addJavascriptInterface(MobileInterface(), "DirectEditingMobileInterface")

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(
                webView.settings,
                WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY
            )
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) && PlatformThemeUtil.isDarkMode(this)) {
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
        }

        webView.setDownloadListener { url, _, _, mimeType, _ ->
            if (url.startsWith("data:")) {
                saveBase64Data(url, mimeType)
            } else {
                downloadFile(url.toUri(), fileName)
            }
        }

        installShowSaveFilePickerPolyfill()

        loadUrl(intent.getStringExtra(EXTRA_URL))
    }

    private fun installShowSaveFilePickerPolyfill() {
        val existingClient = webView.webViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                existingClient.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(SAVE_FILE_PICKER_POLYFILL, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                existingClient.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return existingClient.shouldOverrideUrlLoading(view, request)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                existingClient.onReceivedError(view, request, error)
            }
        }
    }

    private fun saveBase64Data(dataUrl: String, mimeType: String?) {
        val base64Prefix = ";base64,"
        val base64Index = dataUrl.indexOf(base64Prefix)
        if (base64Index == -1) {
            DisplayUtils.showSnackMessage(webView, getString(R.string.failed_to_download))
            return
        }

        val base64String = dataUrl.substring(base64Index + base64Prefix.length)
        pendingBase64Data = try {
            Base64.decode(base64String, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            DisplayUtils.showSnackMessage(webView, getString(R.string.failed_to_download))
            return
        }

        val resolvedMimeType = mimeType
            ?: dataUrl.substringAfter("data:").substringBefore(";").ifEmpty { "application/octet-stream" }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(resolvedMimeType) ?: "png"
        val suggestedName = "${fileName.substringBeforeLast(".", fileName)}_image.$extension"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = resolvedMimeType
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
        startActivityForResult(intent, REQUEST_SAVE_BASE64_FILE)
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SAVE_BASE64_FILE) {
            handleBase64SaveResult(data)
        } else {
            super.handleActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleBase64SaveResult(data: Intent?) {
        val uri = data?.data
        val bytes = pendingBase64Data
        pendingBase64Data = null

        if (uri == null || bytes == null) {
            DisplayUtils.showSnackMessage(webView, getString(R.string.failed_to_download))
            return
        }

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
            }
            DisplayUtils.showSnackMessage(webView, getString(R.string.downloader_download_succeeded_ticker))
        } catch (_: Exception) {
            DisplayUtils.showSnackMessage(webView, getString(R.string.failed_to_download))
        }
    }

    override fun loadUrl(url: String?) {
        if (url.isNullOrEmpty()) {
            TextEditorLoadUrlTask(this, user.get(), file, editorUtils).execute()
        }
    }

    private fun generateOfficeUserAgent(): String {
        val userAgent = applicationContext.resources.getString(R.string.office_user_agent)

        return String.format(userAgent, deviceInfo.androidVersion, appInfo.getAppVersion(this))
    }

    companion object {
        private const val SAVE_FILE_PICKER_POLYFILL = """
            (function() {
                if (window.__nc_savePickerPatched) return;
                window.__nc_savePickerPatched = true;
                window.showSaveFilePicker = function(options) {
                    return Promise.resolve({
                        createWritable: function() {
                            var chunks = [];
                            return Promise.resolve({
                                write: function(data) {
                                    chunks.push(data);
                                    return Promise.resolve();
                                },
                                close: function() {
                                    var blob = new Blob(chunks, {type: (options && options.types && options.types[0] &&
                                        options.types[0].accept && Object.values(options.types[0].accept).flat()[0]) || 'application/octet-stream'});
                                    var reader = new FileReader();
                                    reader.onloadend = function() {
                                        var a = document.createElement('a');
                                        a.href = reader.result;
                                        a.download = (options && options.suggestedName) || 'download';
                                        document.body.appendChild(a);
                                        a.click();
                                        document.body.removeChild(a);
                                    };
                                    reader.readAsDataURL(blob);
                                    return Promise.resolve();
                                }
                            });
                        }
                    });
                };
            })();
        """
    }
}
