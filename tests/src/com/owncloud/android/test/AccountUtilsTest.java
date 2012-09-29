package com.owncloud.android.test;

import org.junit.Test;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.utils.OwnCloudVersion;

import junit.framework.TestCase;

public class AccountUtilsTest extends TestCase {
    
    @Test
    public void testGetWebdavPath() {
        OwnCloudVersion ocv12 = new OwnCloudVersion(0x010200);
        OwnCloudVersion ocv22 = new OwnCloudVersion(0x020200);
        OwnCloudVersion ocv30 = new OwnCloudVersion(0x030000);
        OwnCloudVersion ocv45 = new OwnCloudVersion(0x040500);
        OwnCloudVersion ocv70 = new OwnCloudVersion(0x070000);
        assertTrue(AccountUtils.getWebdavPath(ocv12).equals("/webdav/owncloud.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv22).equals("/files/webdav.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv30).equals("/files/webdav.php"));
        assertTrue(AccountUtils.getWebdavPath(ocv45).equals("/remote.php/webdav"));
        assertTrue(AccountUtils.getWebdavPath(ocv70).equals("/remote.php/webdav"));
    }

}
