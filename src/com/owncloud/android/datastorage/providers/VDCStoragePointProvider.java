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

import com.owncloud.android.datastorage.StoragePoint;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class VDCStoragePointProvider extends AbstractCommandLineStoragePoint {

    static private final String TAG = VDCStoragePointProvider.class.getSimpleName();

    static private final String[] sVDCVolListCommand = new String[]{ "/system/bin/vdc", "volume", "list" };
    static private final int sVDCVolumeList = 110;


    @Override
    public Vector<StoragePoint> getAvailableStoragePoint() {
        Vector<StoragePoint> result = new Vector<>();

        result.addAll(getPaths(getCommandLineResult()));

        return result;
    }

    @Override
    protected String[] getCommand() {
        return sVDCVolListCommand;
    }

    private Vector<StoragePoint> getPaths(String vdcResources) {
        Vector<StoragePoint> result = new Vector<>();

        for (String line : vdcResources.split("\n")) {
            String vdcLine[] = line.split(" ");
            try {
                int status = Integer.parseInt(vdcLine[0]);
                if (status != sVDCVolumeList)
                    continue;
                final String description = vdcLine[1];
                final String path = vdcLine[2];

                if (canBeAddedToAvailableList(result, path))
                    result.add(new StoragePoint(description, path));

            } catch (NumberFormatException e) {
                Log_OC.e(TAG, "Incorrect VDC output format " + e);
            } catch (Exception e) {
                Log_OC.e(TAG, "Unexpected exception on VDC parsing " + e);
            }
        }

        return result;
    }

}
