/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */

package com.owncloud.android.datastorage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class UniqueStorageList extends Vector<StoragePoint> {
    private static final long serialVersionUID = -6504937852826767050L;

    @Override
    public boolean add(StoragePoint sp) {
        try {
            for (StoragePoint s : this) {
                String thisCanonPath = new File(s.getPath()).getCanonicalPath();
                String otherCanonPath = new File(sp.getPath()).getCanonicalPath();
                if (thisCanonPath.equals(otherCanonPath)) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return super.add(sp);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends StoragePoint> collection) {
        for (StoragePoint sp : collection) {
            add(sp);
        }
        return true;
    }
}
