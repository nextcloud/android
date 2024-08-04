/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.test

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import dagger.android.AndroidInjector
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class InjectionOverrideRule(private val overrideInjectors: Map<Class<*>, AndroidInjector<*>>) : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()
            val testApp = instrumentation.targetContext.applicationContext as TestMainApp
            overrideInjectors.entries.forEach {
                testApp.addTestInjector(it.key, it.value)
            }
            base.evaluate()
            testApp.clearTestInjectors()
        }
    }
}
