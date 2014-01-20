/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/) 
 *   Copyright (C) 2012  Bartek Przybylski
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.utils;

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
