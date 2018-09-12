/**
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

import com.owncloud.android.datastorage.StoragePoint;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.List;
import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class VDCStoragePointProvider extends AbstractCommandLineStoragePoint {

    static private final String TAG = VDCStoragePointProvider.class.getSimpleName();

    static private final String[] sVDCVolListCommand = new String[]{ "/system/bin/vdc", "volume", "list" };
    static private final int sVDCVolumeList = 110;


    @Override
    public List<StoragePoint> getAvailableStoragePoint() {
        List<StoragePoint> result = new Vector<>();

        result.addAll(getPaths(getCommandLineResult()));

        return result;
    }

    @Override
    protected String[] getCommand() {
        return sVDCVolListCommand;
    }

    private List<StoragePoint> getPaths(String vdcResources) {
        List<StoragePoint> result = new Vector<>();

        for (String line : vdcResources.split("\n")) {
            String vdcLine[] = line.split(" ");
            try {
                int status = Integer.parseInt(vdcLine[0]);
                if (status != sVDCVolumeList) {
                    continue;
                }
                final String description = vdcLine[1];
                final String path = vdcLine[2];

                if (canBeAddedToAvailableList(result, path)) {
                    result.add(new StoragePoint(description, path, StoragePoint.StorageType.EXTERNAL,
                            StoragePoint.PrivacyType.PRIVATE));
                }

            } catch (NumberFormatException e) {
                Log_OC.e(TAG, "Incorrect VDC output format " + e);
            } catch (Exception e) {
                Log_OC.e(TAG, "Unexpected exception on VDC parsing " + e);
            }
        }

        return result;
    }

}
