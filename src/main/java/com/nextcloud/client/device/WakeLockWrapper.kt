/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.device

import android.os.PowerManager

internal class WakeLockWrapper(val lock: PowerManager.WakeLock?) : WakeLock {

    override val isHeld: Boolean
        get() = lock?.isHeld ?: false

    override fun release() {
        lock?.release()
    }

    override fun runAndRelease(runnable: Runnable) {
        try {
            runnable.run()
        } finally {
            lock?.release()
        }
    }
}
