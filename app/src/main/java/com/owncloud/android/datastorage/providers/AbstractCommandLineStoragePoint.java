/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
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
            final byte[] buffer = new byte[1024];
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
