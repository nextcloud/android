/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.test;

import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import android.test.AndroidTestCase;


public class AccountUtilsTest extends AndroidTestCase {
    
    public void testGetWebdavPathAndOCVersion() {
        OwnCloudVersion ocv12 = new OwnCloudVersion(0x010200);
        OwnCloudVersion ocv12s = new OwnCloudVersion("1.2");
        OwnCloudVersion ocv22 = new OwnCloudVersion(0x020200);
        OwnCloudVersion ocv30 = new OwnCloudVersion(0x030000);
        OwnCloudVersion ocv33s = new OwnCloudVersion("3.3.3");
        OwnCloudVersion ocv45 = new OwnCloudVersion(0x040500);
        OwnCloudVersion ocv70 = new OwnCloudVersion(0x070000);

        assertTrue(AccountUtils.getWebdavPath(ocv12, false, false).equals("/webdav/owncloud.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv12s, false, false).equals("/webdav/owncloud.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv22, false, false).equals("/files/webdav.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv30,false, false).equals("/files/webdav.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv33s, false, false).equals("/files/webdav.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv45, false, false).equals("/remote.php/webdav"));
        assertTrue(AccountUtils.getWebdavPath(ocv70, false, false).equals("/remote.php/webdav"));
        assertNull(AccountUtils.getWebdavPath(null, false, false));
        assertTrue(AccountUtils.getWebdavPath(ocv12, true, false).equals("/remote.php/odav"));
        assertTrue(AccountUtils.getWebdavPath(ocv12s, true, false).equals("/remote.php/odav"));
        assertTrue(AccountUtils.getWebdavPath(ocv22, true, false).equals("/remote.php/odav"));
        assertTrue(AccountUtils.getWebdavPath(ocv30, true, false).equals("/remote.php/odav"));
        assertTrue(AccountUtils.getWebdavPath(ocv33s, true, false).equals("/remote.php/odav"));
        assertTrue(AccountUtils.getWebdavPath(ocv45, true, false).equals("/remote.php/odav"));
        assertTrue(AccountUtils.getWebdavPath(ocv70, true, false).equals("/remote.php/odav"));

        OwnCloudVersion invalidVer = new OwnCloudVersion("a.b.c");
        assertFalse(invalidVer.isVersionValid());

        assertTrue(ocv45.toString().equals("4.5.0"));
    }

}
