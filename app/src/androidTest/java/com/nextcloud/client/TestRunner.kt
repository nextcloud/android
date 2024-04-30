/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2019-2024 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import androidx.test.runner.AndroidJUnitRunner
import com.github.tmurakami.dexopener.DexOpener
import com.nextcloud.test.TestMainApp

class TestRunner : AndroidJUnitRunner() {
    @Throws(ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        /*
         * Initialize DexOpener only on API below 28 to enable mocking of Kotlin classes.
         * On API 28+ the platform supports mocking natively.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            DexOpener.install(this)
        }
        return Instrumentation.newApplication(TestMainApp::class.java, context)
    }
}
