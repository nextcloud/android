/**
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 Nextcloud
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

package com.owncloud.android.authentication;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Tests to check the parser logic of the login data URL parser.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class AuthenticatorDataUrlTest {
    private String schemeUrl = "nextcloud://login/";
    private String plus = "&";

    private String userValue = "testuser123";
    private String userUrlPart = "user:" + userValue;

    private String passwordValue = "testpassword123";
    private String passwordUrlPart = "password:" + passwordValue;

    private String addressValue = "testserver123";
    private String addressUrlPart = "server:" + addressValue;

    private String[] urlStarts = new String[]{schemeUrl};

    @Test
    public void allDataUrlElements() {
        String dataUrl = userUrlPart + plus + passwordUrlPart + plus + addressUrlPart;

        for (String urlStart : urlStarts) {
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            System.out.println(urlStart + dataUrl);
            Assert.assertEquals("Incorrect user value in " + urlStart + " url parsing", userValue, info.username);
            Assert.assertEquals("Incorrect user value in " + urlStart + " url parsing", passwordValue, info.password);
            Assert.assertEquals("Incorrect user value in " + urlStart + " url parsing", addressValue, info.serverAddress);
        }
    }

    @Test
    public void oneElement() {

        for (String urlStart : urlStarts) {
            System.out.println(urlStart + userUrlPart);
            Assert.assertEquals("Incorrect user value in " + urlStart + " url parsing", userValue,
                    AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + userUrlPart).username);
            System.out.println(urlStart + passwordUrlPart);
            Assert.assertEquals("Incorrect user value in " + urlStart + " url parsing", passwordValue,
                    AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + passwordUrlPart).password);
            System.out.println(urlStart + addressUrlPart);
            Assert.assertEquals("Incorrect user value in " + urlStart + " url parsing", addressValue,
                    AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + addressUrlPart).serverAddress);
        }
    }

    @Test
    public void twoDataUrlElements() {

        String dataUrl = userUrlPart + plus + passwordUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", userValue, info
                    .username);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", passwordValue, info
                    .password);
        }

        dataUrl = userUrlPart + plus + addressUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", userValue, info
                    .username);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", addressValue,
                    info.serverAddress);
        }


        dataUrl = addressUrlPart + plus + userUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", userValue, info
                    .username);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", addressValue,
                    info.serverAddress);
        }

        dataUrl = addressUrlPart + plus + passwordUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", passwordValue, info
                    .password);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", addressValue, info
                    .serverAddress);
        }

        dataUrl = passwordUrlPart + plus + userUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", userValue, info
                    .username);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", passwordValue, info
                    .password);
        }

        dataUrl = passwordUrlPart + plus + addressUrlPart;
        for (String urlStart : urlStarts) {
            System.out.println(urlStart + dataUrl);
            LoginUrlInfo info = AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", passwordValue, info
                    .password);
            Assert.assertEquals("Incorrect user value in " + urlStart + dataUrl + " url parsing", addressValue, info
                    .serverAddress);
        }
    }

    @Test
    public void tooManyDataUrlElements() {
        String dataUrl = userUrlPart + plus + passwordUrlPart + plus + addressUrlPart + plus + "notexist:boom";

        for (String urlStart : urlStarts) {
            try {
                System.out.println(urlStart + dataUrl);
                AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart + dataUrl);
                Assert.fail("Illegal Argument Exception expected!");
            } catch (IllegalArgumentException e) {
                // all well
            }
        }
    }

    @Test
    public void tooLittleDataUrlElements() {
        for (String urlStart : urlStarts) {
            try {
                System.out.println(urlStart);
                AuthenticatorActivity.parseLoginDataUrl(urlStart, urlStart);
                Assert.fail("Illegal Argument Exception expected!");
            } catch (IllegalArgumentException e) {
                // all well
            }
        }
    }
}
