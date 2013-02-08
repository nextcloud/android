/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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

package com.owncloud.android.utils;

public class OwnCloudVersion implements Comparable<OwnCloudVersion> {
    public static final OwnCloudVersion owncloud_v1 = new OwnCloudVersion(
            0x010000);
    public static final OwnCloudVersion owncloud_v2 = new OwnCloudVersion(
            0x020000);
    public static final OwnCloudVersion owncloud_v3 = new OwnCloudVersion(
            0x030000);
    public static final OwnCloudVersion owncloud_v4 = new OwnCloudVersion(
            0x040000);
    public static final OwnCloudVersion owncloud_v4_5 = new OwnCloudVersion(
            0x040500);

    // format is in version
    // 0xAABBCC
    // for version AA.BB.CC
    // ie version 2.0.3 will be stored as 0x030003
    private int mVersion;
    private boolean mIsValid;

    public OwnCloudVersion(int version) {
        mVersion = version;
        mIsValid = true;
    }

    public OwnCloudVersion(String version) {
        mVersion = 0;
        mIsValid = false;
        parseVersionString(version);
    }

    public String toString() {
        return ((mVersion >> 16) % 256) + "." + ((mVersion >> 8) % 256) + "."
                + ((mVersion) % 256);
    }

    public boolean isVersionValid() {
        return mIsValid;
    }

    @Override
    public int compareTo(OwnCloudVersion another) {
        return another.mVersion == mVersion ? 0
                : another.mVersion < mVersion ? 1 : -1;
    }

    private void parseVersionString(String version) {
        try {
            String[] nums = version.split("\\.");
            if (nums.length > 0) {
                mVersion += Integer.parseInt(nums[0]);
            }
            mVersion = mVersion << 8;
            if (nums.length > 1) {
                mVersion += Integer.parseInt(nums[1]);
            }
            mVersion = mVersion << 8;
            if (nums.length > 2) {
                mVersion += Integer.parseInt(nums[2]);
            }
            mIsValid = true;
        } catch (Exception e) {
            mIsValid = false;
        }
    }
}
