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
import com.nextcloud.client.account.User
import com.owncloud.android.ui.activity.FileDisplayActivity
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

object LinkHelper {
    const val APP_NEXTCLOUD_NOTES = "it.niedermann.owncloud.notes"
    const val APP_NEXTCLOUD_TALK = "com.nextcloud.talk2"

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
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://$suffix"))
        try {
            context.startActivity(intent)
        } catch (activityNotFoundException1: ActivityNotFoundException) {
            // all is lost: open google play store web page for app
            if (!search) {
                suffix = "apps/$suffix"
            }
            intent.setData(Uri.parse("https://play.google.com/store/$suffix"))
            context.startActivity(intent)
        }
    }
}
