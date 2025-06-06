/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl

fun AppCompatActivity.isDialogFragmentReady(fragment: Fragment): Boolean = isActive() && !fragment.isStateSaved

fun AppCompatActivity.isActive(): Boolean = !isFinishing && !isDestroyed

fun AppCompatActivity.fragments(): List<Fragment> = supportFragmentManager.fragments

fun AppCompatActivity.lastFragment(): Fragment = fragments().last()

fun Activity.showShareIntent(text: String?) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

@Suppress("DEPRECATION")
val Activity.appPref: AppPreferences
    get() = AppPreferencesImpl.fromContext(this)
