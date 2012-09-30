package com.owncloud.android.test;

import android.test.AndroidTestCase;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.utils.OwnCloudVersion;

public class AccountUtilsTest extends AndroidTestCase {
    
    public void testGetWebdavPathAndOCVersion() {
        OwnCloudVersion ocv12 = new OwnCloudVersion(0x010200);
        OwnCloudVersion ocv12s = new OwnCloudVersion("1.2");
        OwnCloudVersion ocv22 = new OwnCloudVersion(0x020200);
        OwnCloudVersion ocv30 = new OwnCloudVersion(0x030000);
        OwnCloudVersion ocv33s = new OwnCloudVersion("3.3.3");
        OwnCloudVersion ocv45 = new OwnCloudVersion(0x040500);
        OwnCloudVersion ocv70 = new OwnCloudVersion(0x070000);

        assertTrue(AccountUtils.getWebdavPath(ocv12).equals("/webdav/owncloud.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv12s).equals("/webdav/owncloud.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv22).equals("/files/webdav.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv30).equals("/files/webdav.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv33s).equals("/files/webdav.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv45).equals("/remote.php/webdav"));
        assertTrue(AccountUtils.getWebdavPath(ocv70).equals("/remote.php/webdav"));
        assertNull(AccountUtils.getWebdavPath(null));

        OwnCloudVersion invalidVer = new OwnCloudVersion("a.b.c");
        assertFalse(invalidVer.isVersionValid());

        assertTrue(ocv45.toString().equals("4.5.0"));
    }

}
