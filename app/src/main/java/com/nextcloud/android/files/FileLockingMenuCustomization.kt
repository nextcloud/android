/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
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

package com.nextcloud.android.files

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.style.StyleSpan
import android.view.Menu
import android.view.MenuItem
import com.nextcloud.utils.TimeConstants
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.utils.DisplayUtils

/**
 * Customizes a Menu to show locking information
 */
class FileLockingMenuCustomization(val context: Context) {
    fun customizeMenu(menu: Menu, file: OCFile) {
        if (file.isLocked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                menu.setGroupDividerEnabled(true)
            }
            menu.findItem(R.id.action_locked_by).title = getLockedByText(file)
            showLockedUntil(menu, file)
        }
    }

    private fun getLockedByText(file: OCFile): CharSequence {
        val username = file.lockOwnerDisplayName ?: file.lockOwnerId
        val resource = when (file.lockType) {
            FileLockType.COLLABORATIVE -> R.string.locked_by_app
            else -> R.string.locked_by
        }
        return DisplayUtils.createTextWithSpan(
            context.getString(resource, username),
            username,
            StyleSpan(Typeface.BOLD)
        )
    }

    private fun showLockedUntil(menu: Menu, file: OCFile) {
        val lockedUntil: MenuItem = menu.findItem(R.id.action_locked_until)
        if (file.lockTimestamp == 0L || file.lockTimeout == 0L) {
            lockedUntil.isVisible = false
        } else {
            lockedUntil.title =
                context.getString(R.string.lock_expiration_info, getExpirationRelativeText(file))
            lockedUntil.isVisible = true
        }
    }

    private fun getExpirationRelativeText(file: OCFile): CharSequence? {
        val expirationTimestamp = (file.lockTimestamp + file.lockTimeout) * TimeConstants.MILLIS_PER_SECOND
        return DisplayUtils.getRelativeTimestamp(context, expirationTimestamp, true)
    }
}
