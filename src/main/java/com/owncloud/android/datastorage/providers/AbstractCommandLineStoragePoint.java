/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datastorage.providers;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.InputStream;
import java.util.Arrays;

/**
 * @author Bartosz Przybylski
 */
abstract class AbstractCommandLineStoragePoint extends AbstractStoragePointProvider {
    private static final String TAG = AbstractCommandLineStoragePoint.class.getSimpleName();

    private static final int COMMAND_LINE_OK_RETURN_VALUE = 0;

    protected abstract String[] getCommand();

    @Override
    public boolean canProvideStoragePoints() {
        Process process;
        try {
            process = new ProcessBuilder().command(Arrays.asList(getCommand())).start();
            process.waitFor();
        } catch (Exception e) {
            return false;
        }
        return process != null && process.exitValue() == COMMAND_LINE_OK_RETURN_VALUE;
    }

    String getCommandLineResult() {
        StringBuilder s = new StringBuilder();
        try {
            final Process process = new ProcessBuilder().command(getCommand()).redirectErrorStream(true).start();

            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte buffer[] = new byte[1024];
            while (is.read(buffer) != -1) {
                s.append(new String(buffer, "UTF8"));
            }
            is.close();
        } catch (final Exception e) {
            Log_OC.e(TAG, "Error retrieving command line results!", e);
        }
        return s.toString();
    }
}
