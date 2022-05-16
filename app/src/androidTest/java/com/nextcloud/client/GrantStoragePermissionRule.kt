/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2021 Álvaro Brey Vilas
 * Copyright (C) 2021 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.utils.PermissionUtil
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class GrantStoragePermissionRule private constructor() {

    companion object {
        @JvmStatic
        fun grant(): TestRule = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> GrantPermissionRule.grant(
                PermissionUtil
                    .getExternalStoragePermission()
            )
            else -> GrantManageExternalStoragePermissionRule()
        }
    }

    private class GrantManageExternalStoragePermissionRule : TestRule {
        override fun apply(base: Statement, description: Description): Statement = object : Statement() {
            override fun evaluate() {
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                    "appops set --uid ${InstrumentationRegistry.getInstrumentation().targetContext.packageName} " +
                        "MANAGE_EXTERNAL_STORAGE allow"
                )
                base.evaluate()
            }
        }
    }
}
