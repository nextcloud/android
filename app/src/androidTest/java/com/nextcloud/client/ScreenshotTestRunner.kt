/*
 * Nextcloud Android client application
 *
 *  @author Tobias Kaminsky
 *  @author Álvaro Brey
 *  Copyright (C) 2019 Tobias Kaminsky
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
package com.nextcloud.client

import android.app.Application
import android.app.Instrumentation
import android.content.Context
import android.os.Build
import com.github.tmurakami.dexopener.DexOpener
import com.karumi.shot.ShotTestRunner
import com.nextcloud.test.TestMainApp

class ScreenshotTestRunner : ShotTestRunner() {
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
