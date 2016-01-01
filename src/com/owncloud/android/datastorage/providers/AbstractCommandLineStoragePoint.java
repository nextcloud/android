/**
 *   ownCloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.datastorage.providers;

import java.io.InputStream;
import java.util.Arrays;

/**
 * @author Bartosz Przybylski
 */
abstract public class AbstractCommandLineStoragePoint extends AbstractStoragePointProvider {

    static protected final int sCommandLineOKReturnValue = 0;

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
        return process != null && process.exitValue() == sCommandLineOKReturnValue;
    }

    protected String getCommandLineResult() {
        String s = "";
        try {
            final Process process = new ProcessBuilder().command(getCommand())
                    .redirectErrorStream(true).start();

            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte buffer[] = new byte[1024];
            while (is.read(buffer) != -1)
                s += new String(buffer);
            is.close();
        } catch (final Exception e) { }
        return s;
    }

}
