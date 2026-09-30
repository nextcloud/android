/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateObserver
import kotlinx.coroutines.launch

fun AppCompatActivity.isDialogFragmentReady(fragment: Fragment): Boolean = isActive() && !fragment.isStateSaved

fun AppCompatActivity.isActive(): Boolean = !isFinishing && !isDestroyed

fun AppCompatActivity.fragments(): List<Fragment> = supportFragmentManager.fragments

fun AppCompatActivity.lastFragment(): Fragment? = supportFragmentManager.fragments.lastOrNull { it.isVisible }

fun Activity.showShareIntent(text: String?) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    startActivity(shareIntent)
}

fun ComponentActivity.observeWorker(onCollect: (WorkerState?) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            WorkerStateObserver.events.collect {
                onCollect(it)
            }
        }
    }
}
