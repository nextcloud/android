/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.test

import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class GrantStoragePermissionRule private constructor() {

    companion object {
        @JvmStatic
        fun grant(): TestRule = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> GrantPermissionRule.grant(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
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
