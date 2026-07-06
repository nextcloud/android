/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.File
import java.net.URLDecoder
import java.util.UUID

class RichDocumentDownloader(private val activity: AppCompatActivity) {
    private val client = OkHttpClient()

    fun download(uri: Uri, filename: String?, userAgent: String?) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                // if filename not provided get filename from header
                if (filename == null) {
                    val url = uri.toString()
                    downloadWithOkHttp(url, userAgent)
                } else {
                    downloadWithDownloadManager(uri, filename)
                }
            }
                .onFailure { Log_OC.e(TAG, "download failed: $it") }
                .getOrDefault(false)
        }
    }

    private suspend fun downloadWithDownloadManager(url: Uri, filename: String) = withContext(Dispatchers.Main) {
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return@withContext showMessage(false)

        DownloadManager.Request(url).apply {
            @Suppress("DEPRECATION")
            allowScanningByMediaScanner()
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        }.let(downloadManager::enqueue)
    }

    private fun downloadWithOkHttp(url: String, userAgent: String?) {
        val requestBuilder = Request.Builder().url(url).get()

        CookieManager.getInstance().getCookie(url)?.let { requestBuilder.header(COOKIE_HEADER, it) }
        userAgent?.let { requestBuilder.header(USER_AGENT_HEADER, it) }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful) {
                Log_OC.e(TAG, "server returned ${response.code} for download")
                showMessage(false)
                return
            }

            val disposition = response.header(CONTENT_DISPOSITION_HEADER)
            val resolvedName = resolveFileName(disposition)
            val result = saveToDownloads(body, resolvedName)
            showMessage(result)
        }
    }

    private fun showMessage(success: Boolean) {
        activity.runOnUiThread {
            val message = if (success) {
                R.string.downloader_download_succeeded_ticker
            } else {
                R.string.failed_to_download
            }

            DisplayUtils.showSnackMessage(activity, message)
        }
    }

    private fun saveToDownloads(body: ResponseBody, filename: String): Boolean {
        val mimeType = body.contentType()?.let { "${it.type}/${it.subtype}" } ?: DEFAULT_MIME_TYPE
        val tempFile = File.createTempFile(TEMP_PREFIX, null, activity.cacheDir)

        return try {
            tempFile.outputStream().use { output -> body.byteStream().copyTo(output) }
            FileExportUtils().exportFile(filename, mimeType, activity.contentResolver, null, tempFile)
            true
        } finally {
            tempFile.delete()
        }
    }

    private fun resolveFileName(disposition: String?): String =
        extractFilenameFromDisposition(disposition) ?: randomFilename()

    private fun extractFilenameFromDisposition(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        return extractExtendedFilename(disposition) ?: extractPlainFilename(disposition)
    }

    private fun extractExtendedFilename(disposition: String): String? {
        val regex = EXTENDED_FILENAME_REGEX.toRegex(RegexOption.IGNORE_CASE)
        val value = regex.find(disposition)?.groupValues?.get(1)?.trim() ?: return null
        return runCatching { URLDecoder.decode(value, EXTENDED_FILENAME_ENCODER) }.getOrNull()
    }

    private fun extractPlainFilename(disposition: String): String? {
        val regex = PLAIN_FILENAME_REGEX.toRegex(RegexOption.IGNORE_CASE)
        return regex.find(disposition)?.groupValues?.get(1)?.trim()
    }

    private fun randomFilename(): String = "file_" + UUID.randomUUID().toString().take(RANDOM_NAME_LENGTH)

    companion object {
        private const val TAG = "RichDocumentDownloader"
        private const val COOKIE_HEADER = "Cookie"
        private const val USER_AGENT_HEADER = "User-Agent"
        private const val CONTENT_DISPOSITION_HEADER = "Content-Disposition"
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
        private const val TEMP_PREFIX = "richdocument_download"
        private const val RANDOM_NAME_LENGTH = 8
        private const val EXTENDED_FILENAME_REGEX = """filename\*\s*=\s*[^']*''([^;]+)"""
        private const val PLAIN_FILENAME_REGEX = """filename\s*=\s*"?([^";]+)"?"""
        private const val EXTENDED_FILENAME_ENCODER = "UTF-8"
    }
}
