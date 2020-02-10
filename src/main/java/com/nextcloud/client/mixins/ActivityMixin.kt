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
 * Interface allowing to implement part of [android.app.Activity] logic as
 * a mix-in.
 */
interface ActivityMixin {
    fun onNewIntent(intent: Intent) { /* no-op */ }
    fun onSaveInstanceState(outState: Bundle) { /* no-op */ }
    fun onCreate(savedInstanceState: Bundle?) { /* no-op */ }
    fun onRestart() { /* no-op */ }
    fun onStart() { /* no-op */ }
    fun onResume() { /* no-op */ }
    fun onPause() { /* no-op */ }
    fun onStop() { /* no-op */ }
    fun onDestroy() { /* no-op */ }
}
