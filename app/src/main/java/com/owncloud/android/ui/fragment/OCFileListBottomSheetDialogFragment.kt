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

package com.owncloud.android.ui.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.nextcloud.client.account.User
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.di.Injectable
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.theme.ThemeUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class OCFileListBottomSheetDialogFragment(
    private val fileActivity: FileActivity,
    private val actions: OCFileListBottomSheetActions,
    private val deviceInfo: DeviceInfo,
    private val user: User,
    private val file: OCFile
) : DialogFragment(), Injectable {

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var editorUtils: EditorUtils

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return OCFileListBottomSheetDialog(
            fileActivity,
            actions,
            deviceInfo,
            user,
            file,
            themeUtils,
            viewThemeUtils,
            editorUtils
        )
    }
}
