/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.services.observer;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileEntry;

import java.io.File;
import java.io.FileFilter;

/**
 * Magical file observer
 */

public class FileAlterationMagicObserver extends FileAlterationObserver {

    public FileAlterationMagicObserver(String directoryName) {
        super(directoryName);
    }

    public FileAlterationMagicObserver(String directoryName, FileFilter fileFilter) {
        super(directoryName, fileFilter);
    }

    public FileAlterationMagicObserver(String directoryName, FileFilter fileFilter, IOCase caseSensitivity) {
        super(directoryName, fileFilter, caseSensitivity);
    }

    public FileAlterationMagicObserver(File directory) {
        super(directory);
    }

    public FileAlterationMagicObserver(File directory, FileFilter fileFilter) {
        super(directory, fileFilter);
    }

    public FileAlterationMagicObserver(File directory, FileFilter fileFilter, IOCase caseSensitivity) {
        super(directory, fileFilter, caseSensitivity);
    }

    public FileAlterationMagicObserver(FileEntry rootEntry, FileFilter fileFilter, IOCase caseSensitivity) {
        super(rootEntry, fileFilter, caseSensitivity);
    }
}
