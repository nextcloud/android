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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by srkunze on 28.08.17.
 */

public class FileSortOrder {
    public static final FileSortOrder sort_a_to_z = new FileSortOrderByName("sort_a_to_z", true);
    public static final FileSortOrder sort_z_to_a = new FileSortOrderByName("sort_z_to_a", false);
    public static final FileSortOrder sort_old_to_new = new FileSortOrderByDate("sort_old_to_new", true);
    public static final FileSortOrder sort_new_to_old = new FileSortOrderByDate("sort_new_to_old", false);
    public static final FileSortOrder sort_small_to_big = new FileSortOrderBySize("sort_small_to_big", true);
    public static final FileSortOrder sort_big_to_small = new FileSortOrderBySize("sort_big_to_small", false);

    public static final Map<String, FileSortOrder> sortOrders;
    static {
        sortOrders = new HashMap<String, FileSortOrder>();
        sortOrders.put(sort_a_to_z.mName, sort_a_to_z);
        sortOrders.put(sort_z_to_a.mName, sort_z_to_a);
        sortOrders.put(sort_old_to_new.mName, sort_old_to_new);
        sortOrders.put(sort_new_to_old.mName, sort_new_to_old);
        sortOrders.put(sort_small_to_big.mName, sort_small_to_big);
        sortOrders.put(sort_big_to_small.mName, sort_big_to_small);
    }

    public String mName;
    public boolean mAscending;

    public FileSortOrder(String name, boolean ascending) {
        mName = name;
        mAscending = ascending;
    }

    public Vector<OCFile> sortCloudFiles(Vector<OCFile> files) {
        return sortCloudFilesByFavourite(files);
    }

    public File[] sortLocalFiles(File[] files) {
        return files;
    }

    /**
     * Sorts list by Favourites.
     *
     * @param files files to sort
     */
    public static Vector<OCFile> sortCloudFilesByFavourite(Vector<OCFile> files) {
        Collections.sort(files, new Comparator<OCFile>() {
            public int compare(OCFile o1, OCFile o2) {
                if (o1.getIsFavorite() && o2.getIsFavorite()) {
                    return 0;
                } else if (o1.getIsFavorite()) {
                    return -1;
                } else if (o2.getIsFavorite()) {
                    return 1;
                }
                return 0;
            }
        });

        return files;
    }
}
