/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2023 Álvaro Brey
 *  Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
