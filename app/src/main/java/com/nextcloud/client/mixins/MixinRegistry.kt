/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.mixins

import android.content.Intent
import android.os.Bundle

/**
 * Mix-in registry allows forwards lifecycle calls to all
 * registered mix-ins.
 *
 * Once instantiated, all [android.app.Activity] lifecycle methods
 * must call relevant registry companion methods.
 *
 * Calling the registry from [android.app.Application.ActivityLifecycleCallbacks] is
 * not possible as not all callbacks are supported by this interface.
 */
class MixinRegistry : ActivityMixin {

    private val mixins = mutableListOf<ActivityMixin>()

    fun add(vararg mixins: ActivityMixin) {
        mixins.forEach { this.mixins.add(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mixins.forEach { it.onNewIntent(intent) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mixins.forEach { it.onSaveInstanceState(outState) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mixins.forEach { it.onCreate(savedInstanceState) }
    }

    override fun onRestart() {
        super.onRestart()
        mixins.forEach { it.onRestart() }
    }

    override fun onStart() {
        super.onStart()
        mixins.forEach { it.onStart() }
    }

    override fun onResume() {
        super.onResume()
        mixins.forEach { it.onResume() }
    }

    override fun onPause() {
        super.onPause()
        mixins.forEach { it.onPause() }
    }

    override fun onStop() {
        super.onStop()
        mixins.forEach { it.onStop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mixins.forEach { it.onDestroy() }
    }
}
