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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sort order
 */

public class FileSortOrder {
    public static final String sort_a_to_z_id = "sort_a_to_z";
    public static final String sort_z_to_a_id = "sort_z_to_a";
    public static final String sort_old_to_new_id = "sort_old_to_new";
    public static final String sort_new_to_old_id = "sort_new_to_old";
    public static final String sort_small_to_big_id = "sort_small_to_big";
    public static final String sort_big_to_small_id = "sort_big_to_small";

    public static final FileSortOrder sort_a_to_z = new FileSortOrderByName(sort_a_to_z_id, true);
    public static final FileSortOrder sort_z_to_a = new FileSortOrderByName(sort_z_to_a_id, false);
    public static final FileSortOrder sort_old_to_new = new FileSortOrderByDate(sort_old_to_new_id, true);
    public static final FileSortOrder sort_new_to_old = new FileSortOrderByDate(sort_new_to_old_id, false);
    public static final FileSortOrder sort_small_to_big = new FileSortOrderBySize(sort_small_to_big_id, true);
    public static final FileSortOrder sort_big_to_small = new FileSortOrderBySize(sort_big_to_small_id, false);

    public static final Map<String, FileSortOrder> sortOrders;

    public enum Type {
        trashBinView, localFileListView, uploadFilesView
    }
    static {
        HashMap<String, FileSortOrder> temp = new HashMap<>();
        temp.put(sort_a_to_z.name, sort_a_to_z);
        temp.put(sort_z_to_a.name, sort_z_to_a);
        temp.put(sort_old_to_new.name, sort_old_to_new);
        temp.put(sort_new_to_old.name, sort_new_to_old);
        temp.put(sort_small_to_big.name, sort_small_to_big);
        temp.put(sort_big_to_small.name, sort_big_to_small);

        sortOrders = Collections.unmodifiableMap(temp);
    }

    public String name;
    public boolean isAscending;

    public FileSortOrder(String name, boolean ascending) {
        this.name = name;
        isAscending = ascending;
    }

    public List<OCFile> sortCloudFiles(List<OCFile> files) {
        return sortCloudFilesByFavourite(files);
    }

    public List<File> sortLocalFiles(List<File> files) {
        return files;
    }

    public List<TrashbinFile> sortTrashbinFiles(List<TrashbinFile> files) {
        return files;
    }

    /**
     * Sorts list by Favourites.
     *
     * @param files files to sort
     */
    public static List<OCFile> sortCloudFilesByFavourite(List<OCFile> files) {
        Collections.sort(files, (o1, o2) -> {
            if (o1.isFavorite() && o2.isFavorite()) {
                return 0;
            } else if (o1.isFavorite()) {
                return -1;
            } else if (o2.isFavorite()) {
                return 1;
            }
            return 0;
        });

        return files;
    }
}
