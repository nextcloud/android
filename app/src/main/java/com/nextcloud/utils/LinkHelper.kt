/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 ZetaTom <70907959+ZetaTom@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.owncloud.android.lib.common.utils.Log_OC
import java.util.Locale

object LinkHelper {
    private const val TAG = "LinkHelper"

    fun isHttpOrHttpsLink(link: String?): Boolean = link?.lowercase(Locale.getDefault())?.let {
        it.startsWith("http://") || it.startsWith("https://")
    } == true

    /**
     * Open app store page of specified app or search for specified string. Will attempt to open browser when no app
     * store is available.
     *
     * @param string packageName or url-encoded search string
     * @param search false -> show app corresponding to packageName; true -> open search for string
     */
    fun openAppStore(string: String, search: Boolean = false, context: Context) {
        var suffix = (if (search) "search?q=" else "details?id=") + string
        val intent = Intent(Intent.ACTION_VIEW, "market://$suffix".toUri())
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // all is lost: open google play store web page for app
            if (!search) {
                suffix = "apps/$suffix"
            }
            intent.setData("https://play.google.com/store/$suffix".toUri())
            context.startActivity(intent)
        }
    }

    // region Validation
    private const val HTTP = "http"
    private const val HTTPS = "https"

    /**
     * Validates if a URL string is valid
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun validateAndGetURL(url: String?): String? {
        if (url.isNullOrBlank()) {
            Log_OC.w(TAG, "Given url is null or blank")
            return null
        }

        return try {
            val uri = url.toUri()
            if (uri.scheme == null) {
                return null
            }
            val validSchemes = listOf(HTTP, HTTPS)
            if (uri.scheme in validSchemes) url else null
        } catch (e: Exception) {
            Log_OC.e(TAG, "Invalid URL: $url -- $e")
            null
        }
    }
    // endregion
}
