/*
 * Nextcloud Android client application
 *
 * @author Sven R. Kunze
 * Copyright (C) 2017 Sven R. Kunze
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
 */

package com.owncloud.android.utils;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;

import java.io.File;
import java.util.Collections;
import java.util.List;


/**
 * Sorts files by sizes
 */
public class FileSortOrderBySize extends FileSortOrder {

    FileSortOrderBySize(String name, boolean ascending) {
        super(name, ascending);
    }

    /**
     * Sorts list by Size.
     *
     * @param files list of files to sort
     */
    public List<OCFile> sortCloudFiles(List<OCFile> files) {
        final int multiplier = isAscending ? 1 : -1;

        Collections.sort(files, (o1, o2) -> {
            if (o1.isFolder() && o2.isFolder()) {
                Long obj1 = o1.getFileLength();
                return multiplier * obj1.compareTo(o2.getFileLength());
            } else if (o1.isFolder()) {
                return -1;

            } else if (o2.isFolder()) {
                return 1;
            } else {
                Long obj1 = o1.getFileLength();
                return multiplier * obj1.compareTo(o2.getFileLength());
            }
        });

        return super.sortCloudFiles(files);
    }

    /**
     * Sorts list by Size.
     *
     * @param files list of files to sort
     */
    @Override
    public List<TrashbinFile> sortTrashbinFiles(List<TrashbinFile> files) {
        final int multiplier = isAscending ? 1 : -1;

        Collections.sort(files, (o1, o2) -> {
            if (o1.isFolder() && o2.isFolder()) {
                Long obj1 = o1.getFileLength();
                return multiplier * obj1.compareTo(o2.getFileLength());
            } else if (o1.isFolder()) {
                return -1;

            } else if (o2.isFolder()) {
                return 1;
            } else {
                Long obj1 = o1.getFileLength();
                return multiplier * obj1.compareTo(o2.getFileLength());
            }
        });

        return super.sortTrashbinFiles(files);
    }

    /**
     * Sorts list by Size.
     *
     * @param files list of files to sort
     */
    @Override
    public List<File> sortLocalFiles(List<File> files) {
        final int multiplier = isAscending ? 1 : -1;

        Collections.sort(files, (o1, o2) -> {
            if (o1.isDirectory() && o2.isDirectory()) {
                Long obj1 = FileStorageUtils.getFolderSize(o1);
                return multiplier * obj1.compareTo(FileStorageUtils.getFolderSize(o2));
            } else if (o1.isDirectory()) {
                return -1;
            } else if (o2.isDirectory()) {
                return 1;
            } else {
                Long obj1 = o1.length();
                return multiplier * obj1.compareTo(o2.length());
            }
        });

        return files;
    }

}
