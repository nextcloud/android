/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 Bartosz Przybylski
 *
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.datastorage.providers;

import com.owncloud.android.datastorage.StoragePoint;

import java.util.List;

public interface IStoragePointProvider {

    /**
     *  This method is used for querying storage provider to check if it can provide
     *  usable and reliable data storage places.
     *
     *  @return true if provider can reliably return storage path
     */
    boolean canProvideStoragePoints();

    /**
     *
     * @return available storage points
     */
    List<StoragePoint> getAvailableStoragePoint();
}
