/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.test

import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.utils.Log_OC
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector

/**
 * The purpose of this class is to allow overriding injections in Android classes (which use parameter injection instead
 * of constructor injection).
 *
 * To automate its usage, pair with [InjectionOverrideRule]; or call [addTestInjector] manually for more control.
 */
class TestMainApp : MainApp() {

    val foo = "BAR"
    private var overrideInjectors: MutableMap<Class<*>, AndroidInjector<*>> = mutableMapOf()

    /**
     * If you call this before a test please remember to call [clearTestInjectors] afterwards
     */
    fun addTestInjector(clazz: Class<*>, injector: AndroidInjector<*>) {
        Log_OC.d(TAG, "addTestInjector: added injector for $clazz")
        overrideInjectors[clazz] = injector
    }

    fun clearTestInjectors() {
        overrideInjectors.clear()
    }

    override fun androidInjector(): AndroidInjector<Any> {
        @Suppress("UNCHECKED_CAST")
        return InjectorWrapper(dispatchingAndroidInjector, overrideInjectors as Map<Class<*>, AndroidInjector<Any>>)
    }

    class InjectorWrapper(
        private val baseInjector: DispatchingAndroidInjector<Any>,
        private val overrideInjectors: Map<Class<*>, AndroidInjector<Any>>
    ) : AndroidInjector<Any> {
        override fun inject(instance: Any) {
            baseInjector.inject(instance)
            overrideInjectors[instance.javaClass]?.let { customInjector ->
                Log_OC.d(TAG, "Injecting ${instance.javaClass} with ${customInjector.javaClass}")
                customInjector.inject(instance)
            }
        }
    }

    companion object {
        private const val TAG = "TestMainApp"
    }
}
