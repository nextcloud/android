/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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

package com.nextcloud.client;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.facebook.testing.screenshot.ScreenshotRunner;
import com.github.tmurakami.dexopener.DexOpener;

import com.karumi.shot.ShotTestRunner;

public class ScreenshotTestRunner extends ShotTestRunner {

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        /*
         * Initialize DexOpener only on API below 28 to enable mocking of Kotlin classes.
         * On API 28+ the platform supports mocking natively.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            DexOpener.install(this);
        }

        return super.newApplication(cl, className, context);
    }
}
