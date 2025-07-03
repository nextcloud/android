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
import android.content.Context
import androidx.annotation.OpenForTesting
import androidx.test.runner.AndroidJUnitRunner
import com.nextcloud.test.TestMainApp

@OpenForTesting
class TestRunner : AndroidJUnitRunner() {
    @Throws(ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return newApplication(TestMainApp::class.java, context)
    }
}
