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
import android.net.Uri
import androidx.core.net.toUri
import com.nextcloud.client.account.User
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import java.util.Locale
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

object LinkHelper {
    const val APP_NEXTCLOUD_NOTES = "it.niedermann.owncloud.notes"
    const val APP_NEXTCLOUD_TALK = "com.nextcloud.talk2"
    private const val TAG = "LinkHelper"

    fun isHttpOrHttpsLink(link: String?): Boolean = link?.lowercase(Locale.getDefault())?.let {
        it.startsWith("http://") || it.startsWith("https://")
    } == true

    /**
     * Open specified app and, if not installed redirect to corresponding download.
     *
     * @param packageName of app to be opened
     * @param user to pass in intent
     */
    fun openAppOrStore(packageName: String, user: Optional<User>, context: Context) {
        openAppOrStore(packageName, user.getOrNull(), context)
    }

    /**
     * Open specified app and, if not installed redirect to corresponding download.
     *
     * @param packageName of app to be opened
     * @param user to pass in intent
     */
    fun openAppOrStore(packageName: String, user: User?, context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            // app installed - open directly
            // TODO handle null user?
            intent.putExtra(FileDisplayActivity.KEY_ACCOUNT, user.hashCode())
            context.startActivity(intent)
        } else {
            // app not found - open market (Google Play Store, F-Droid, etc.)
            openAppStore(packageName, false, context)
        }
    }

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
        } catch (activityNotFoundException1: ActivityNotFoundException) {
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
    private const val FILE = "file"
    private const val CONTENT = "content"

    /**
     * Validates if a string can be converted to a valid URI
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    fun validateAndGetURI(uriString: String?): Uri? {
        if (uriString.isNullOrBlank()) {
            Log_OC.w(TAG, "Given uriString is null or blank")
            return null
        }

        return try {
            val uri = uriString.toUri()
            if (uri.scheme == null) {
                return null
            }

            val validSchemes = listOf(HTTP, HTTPS, FILE, CONTENT)
            if (uri.scheme in validSchemes) uri else null
        } catch (e: Exception) {
            Log_OC.e(TAG, "Invalid URI string: $uriString -- $e")
            null
        }
    }

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
