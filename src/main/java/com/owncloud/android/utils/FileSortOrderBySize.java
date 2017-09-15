/**
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Created by srkunze on 28.08.17.
 */

public class FileSortOrderBySize extends FileSortOrder {

    public FileSortOrderBySize(String name, boolean ascending) {
        super(name, ascending);
    }

    /**
     * Sorts list by Size.
     *
     * @param files list of files to sort
     */
    public Vector<OCFile> sortCloudFiles(Vector<OCFile> files) {
        final int multiplier = mAscending ? 1 : -1;

        Collections.sort(files, new Comparator<OCFile>() {
            @SuppressFBWarnings(value = "Bx")
            public int compare(OCFile o1, OCFile o2) {
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
            }
        });

        return super.sortCloudFiles(files);
    }

    /**
     * Sorts list by Size.
     *
     * @param filesArray list of files to sort
     */
    public File[] sortLocalFiles(File[] filesArray) {
        final int multiplier = mAscending ? 1 : -1;

        List<File> files = new ArrayList<File>(Arrays.asList(filesArray));

        Collections.sort(files, new Comparator<File>() {
            @SuppressFBWarnings(value = "Bx")
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isDirectory()) {
                    // Long obj1 = getFolderSize(o1);
                    // return multiplier * obj1.compareTo(getFolderSize(o2));
                    return o1.getPath().toLowerCase().compareTo(o2.getPath().toLowerCase());
                } else if (o1.isDirectory()) {
                    return -1;
                } else if (o2.isDirectory()) {
                    return 1;
                } else {
                    Long obj1 = o1.length();
                    return multiplier * obj1.compareTo(o2.length());
                }
            }
        });

        File[] returnArray = new File[files.size()];
        return files.toArray(returnArray);
    }

}
