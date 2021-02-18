/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Nextcloud GmbH
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
